

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Receiver {
	private static ExecutorService executorService = Executors.newCachedThreadPool();  
	Queue<Integer> seqQueue = new LinkedList<Integer>();
	Queue<Integer> ackQueue = new LinkedList<Integer>();
	public static boolean connectionExit = false;
	public static boolean arrivedRight = false;
	public static int loop = 0;
	public int recLog =1;
	private int failNum=0;

	private PrintWriter printWriter;
	private Socket socket;
	public Receiver(String fileName, String listeningPort, String senderIP,
			String senderPort, String logFile) {
		DatagramPacket dp = null;
		DatagramSocket ds = null;
		DataOutputStream fileOutput = null;
		DataOutputStream logfileStream = null ;
		int buffersize =150;
		int firsttime=0;
		

		String line;
		
		byte[] receiveBuffer = new byte[buffersize];
		try {
			ds = new DatagramSocket(Integer.parseInt(listeningPort));
			logfileStream= new DataOutputStream(new BufferedOutputStream(new FileOutputStream(logFile)));
			fileOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		} catch (NumberFormatException e2) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e2.printStackTrace();
		} catch (SocketException e2) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e2.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e.printStackTrace();
		}
	

		while(true){
		try {
				int datalength ;
				int expectAck= 0;
				int lastAck =-1;
				boolean firstClose =false;
				dp = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				ds.receive(dp);
				datalength = dp.getLength();
				if(datalength!=0 &&firsttime==0){
					socket = new Socket(senderIP,Integer.valueOf(senderPort));
//					System.out.println(firsttime);
					firsttime+=1;
//					System.out.println("Send a socket connection");
		            printWriter = new PrintWriter(socket.getOutputStream(), true);         
				}
					while(receiveBuffer!=null){
						firsttime+=1;
						loop+=1;
						arrivedRight =false;
						connectionExit = true;
						
						byte[] buf =dp.getData();
		//				receiveBuffer = new byte[buffersize];
						byte[] header = new byte[20];
						System.arraycopy(buf, 0, header, 0, 20);
					
						byte[] data = new byte[buf.length-20];
						System.arraycopy(buf, 20, data, 0, buf.length-20);
						byte[] seq = new byte[4];
						System.arraycopy(header, 4, seq, 0, 4);
						byte[] checksum = new byte[2];
						System.arraycopy(header, 16, checksum, 0, 2);
						int sendlength = Byte2Int2(checksum);
						byte[] receiveChecksum = calcCheckSum(data);
						int receivelength = Byte2Int2(receiveChecksum);				
						int sequenceNumber = Byte2Int4(seq);

						if(sequenceNumber==expectAck && sendlength==receivelength){
							expectAck+=1;
							lastAck+=1;
		                    //Received FIN
							if(header[13]==(byte) 0x1){
								printWriter.flush();
								printWriter.println(lastAck);
								printWriter.flush();
								Thread.sleep(300);
								fileOutput.flush();
								logfileStream.flush();
								fileOutput.close();
								if(logFile.trim().equals("stdout")){
									Timestamp ts = new Timestamp(System.currentTimeMillis());
									System.out.println(ts+" "+ InetAddress.getLocalHost().toString()+" "+senderIP + " Sequence # "+String.valueOf(sequenceNumber)+" ACK #: "+String.valueOf(expectAck)
											+" FLAG 000010001"+ "\n");
								}else{
									logWrite(logfileStream, senderIP, sequenceNumber, lastAck," FLAG 000010001");
								}
//			                    System.out.println("The number of fail: "+failNum);
//			                    System.out.println(lastAck);
			                    logfileStream.flush();

			                    printWriter.println("close");
								executorService.shutdownNow(); 
								break;
							}else{
								printWriter.println(lastAck);
								if(logFile.trim().equals("stdout")){
									Timestamp ts = new Timestamp(System.currentTimeMillis());
									System.out.println(ts+" "+  InetAddress.getLocalHost().toString()+" "+senderIP + "  Sequence # "+String.valueOf(sequenceNumber)+" ACK #: "+String.valueOf(expectAck)
											+" FLAG 000010000"+ "\n");
								}else{
									logWrite(logfileStream, senderIP, sequenceNumber, lastAck," FLAG 000010000");
								}
			                    logfileStream.flush();
			                    int lenbuff = 0;
			                    for(int i=0;i<buffersize-20;i++){	
			                    	if(data[i]==0x00){
			                    		lenbuff = i-1;
			                    		 byte[] lastPart = new byte[lenbuff+1];
			 		                	System.arraycopy(data, 0, lastPart, 0, lenbuff);
			 		                	lastPart[lenbuff] = data[lenbuff];
			 		                	fileOutput.write(lastPart);
			 		                	i=buffersize;
			                    		break;
			                    	}
			                    	if(i==buffersize-21){
			                    		lenbuff = buffersize-20;
			                    		fileOutput.write(data);
					                    fileOutput.flush();
			                    	}
			                    }
			                    fileOutput.flush();
							}
					//	     printWriter.println(lastAck);
						}else{
						    logfileStream.flush();
							if(logFile.trim().equals("stdout")){
								Timestamp ts = new Timestamp(System.currentTimeMillis());
								System.out.println(ts+" "+ InetAddress.getLocalHost().toString()+" "+ senderIP +"  Sequence # "+String.valueOf(sequenceNumber)+" ACK #: "+String.valueOf(expectAck)
										+"FLAG 000010000"+ "\n");
							}else{
								logWrite(logfileStream, senderIP, sequenceNumber, lastAck,"FLAG 000010000");
							}
		                    logfileStream.flush();
		                    failNum+=1;
						    printWriter.println(lastAck);
						}
						receiveBuffer = new byte[buffersize];
						dp = new DatagramPacket(receiveBuffer, receiveBuffer.length);
						ds.receive(dp);
						datalength = dp.getLength();
			
					}
					ds.close();
					System.out.println("Delivery completed successfully");
					socket.close();
					System.exit(0);		
		} catch (FileNotFoundException e) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e.printStackTrace();
		} catch(NullPointerException e1){
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
		} catch (InterruptedException e) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e.printStackTrace();
		}
		}
	}
	private void logWrite(DataOutputStream logfileStream, String senderIP, int sequenceNumber, int expectAck,String flag) {
		   Timestamp ts = new Timestamp(System.currentTimeMillis());
           try {
        	   logfileStream.flush();
			logfileStream.write(ts.toString().getBytes(),0,ts.toString().getBytes().length);
			logfileStream.flush();
	        logfileStream.writeBytes( " "+InetAddress.getLocalHost().toString()+" " + senderIP+"  Sequence # "+String.valueOf(sequenceNumber)
	        		+" ACK # " + String.valueOf(expectAck)+" "+flag+" "+ "\n");
	        recLog+=1;
	       
	        logfileStream.flush();
		} catch (IOException e) {
			System.out.println("unable to create right file");
			System.out.println("Please re open the Receiver and sender!");
			System.exit(0);
			e.printStackTrace();
		}
           
		
	}
	private byte[] calcCheckSum(byte[] sendDateByte) {
		byte[] checksum = new byte[2];
		for (int i = 0; i < sendDateByte.length; i += 2) {  
            checksum[0] ^= sendDateByte[i];  
            checksum[1] ^= sendDateByte[i + 1];  
        }  
        checksum[0] = (byte) ~checksum[0];  
        checksum[1] = (byte) ~checksum[1];  		
		return checksum;
	}
	


	public static void main(String[] args) {
		new Receiver(args[0],args[1],args[2],args[3],args[4]);
//		new Receiver("file2.txt","41194","127.0.0.1","41195","logfileReceiver.txt");
	}
	 public static byte[] Int2Byte4(int number) {   
		  byte[] byteArray = new byte[4];   
		  byteArray[0] = (byte)((number >> 24) & 0xFF);
		  byteArray[1] = (byte)((number >> 16) & 0xFF);
		  byteArray[2] = (byte)((number >> 8) & 0xFF); 
		  byteArray[3] = (byte)(number & 0xFF);
		  return byteArray;
	}
	 
	 
	 public  static int Byte2Int4(byte[] byteArray) {
		 int number = byteArray[3] & 0xFF;
		 number |= ((byteArray[2] << 8) & 0xFF00);
		 number |= ((byteArray[1] << 16) & 0xFF0000);
		 number |= ((byteArray[0] << 24) & 0xFF000000);
		 return number;		 
	}
	 public static byte[] Int2Byte2(int number) {   
		  byte[] byteArray = new byte[2];   
		  byteArray[0] = (byte)(number & 0x00ff);
		  byteArray[1] = (byte)((number & 0xff00)>>8);
		  return byteArray;
	}
	 
	 
	 public  static int Byte2Int2(byte[] byteArray) {
		 return  ((byteArray[1] << 8) & 0xff00) | (byteArray[0] & 0xff);
	}
	 

}
