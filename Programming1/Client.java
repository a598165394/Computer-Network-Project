import java.io.BufferedReader;  

import java.io.InputStreamReader;  
import java.io.PrintWriter;  
import java.net.InetAddress;
import java.net.Socket;  
import java.util.concurrent.ExecutorService;  
import java.util.concurrent.Executors;  
public class Client {
	 
	    private static ExecutorService executorService = Executors.newCachedThreadPool();  
	    public static int loginNumber =0;
	   
	  
	    public Client(String serverIP, String serverPortNumber) {  
	        try {  
	        	 /*try to connect with the server*/
	            Socket socket = new Socket(serverIP,Integer.valueOf(serverPortNumber));  
	            executorService.execute(new clientSenderThread(socket));  
	            System.out.println("Username:");
	            String message;  
	            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket  
	                    .getInputStream()));  
	           
	            while ((message = bufferedReader.readLine()) != null) {  
	                System.out.println(message); 
	                if (message.trim().equals("logout")) { 
	                	 /*if message is logout, the client will left*/       	
	                    bufferedReader.close();  
	                    executorService.shutdownNow();  
	                    System.exit(0);
	                    socket.close();
	                    break;  
	                }  
	            }  
	        } catch (Exception e) {  
	  
	        }  
	        
	      
	   
	    }  
	    
	   
	  
	    /*The Thread for client for sender and receiver message*/   
	    static class clientSenderThread implements Runnable {  
	        private Socket socket;  
	  
	        public clientSenderThread(Socket socket) {  
	            this.socket = socket;  
	        }  
	  
	        public void run() {  
	            try {  
	            	/*Reading from the command line*/   
	                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(  
	                        System.in));  
	                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);  
	                String message;  
	  
	                while (true) {  
	                    message = bufferedReader.readLine();  
	                    printWriter.println(message);
	              
	  
	                    if (message.trim().equals("logout")) { 
	                   	 /*if message is logout, the client will left*/   
	                        printWriter.close();  
	                        bufferedReader.close();  
	                        executorService.shutdownNow();
	                        System.exit(0);	
	                        break;  
	                    }  
	                }  
	            } catch (Exception e) {  
	                e.printStackTrace();  
	            }  
	        }  
	    }

	    public static void main(String[] args)  {  
	    	try{
	    		new Client(args[0],args[1]); 
	    		/*new Client("127.0.0.1","8888");*/
	    	}catch (Exception e){
	    		e.printStackTrace();
	    	}
	        
	    }  
	 
}
