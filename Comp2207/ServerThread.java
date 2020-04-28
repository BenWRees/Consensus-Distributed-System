 
import java.net.*;
import java.io.*;
import java.util.*; 

//Class represents a peer's server 
 public class ServerThread extends Thread {
 	private ServerSocket serverSocket;
 	//set of all the clients connected to the peer's server
 	private Set<ServerThreadThread> serverThreadThreads = new HashSet<ServerThreadThread>();

 	public ServerThread(String portNumber) throws IOException {
 		serverSocket = new ServerSocket(Integer.valueOf(portNumber));
 	}

 	@Override
 	public void run() {
 		try {
 			//for any new peer connecting to this peer, the peer creates a serverThreadThread for it 
 			while(true) {
 				ServerThreadThread serverThreadThread = new ServerThreadThread(serverSocket.accept(), this);
 				serverThreadThreads.add(serverThreadThread);
 				serverThreadThread.start();
 			}
 		} catch (Exception e) {
 			System.out.println("Exception thrown due to: " + e.getMessage());
 		}
 	}

 	//sends a message to each client connected to the 
 	public void sendMessage(String message) {
 		try {
 			serverThreadThreads.forEach(serverThreadThread -> serverThreadThread.getPrintWriter().println(message));
 			//serverThreadThreads.forEach(serverThreadThread -> serverThreadThread.getPrintWriter().flush());
 		} catch(Exception e) {
 			System.out.println("Exception thrown due to: " + e.getMessage());
 		}
 	}

 	public Set<ServerThreadThread> getServerThreadThreads() {
 		return serverThreadThreads;
 	}
 	
 	public class ServerThreadThread extends Thread {
 		private ServerThread serverThread;
 		private Socket socket;
 		private PrintWriter printWriter;

 		public ServerThreadThread(Socket socket, ServerThread serverThread) {
 			this.socket = socket;
 			this.serverThread = serverThread;
 		} 

 		@Override
 		public void run() {
 			try {
 				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
 				this.printWriter = new PrintWriter(socket.getOutputStream(), true);
 				while(true) {
 					serverThread.sendMessage(input.readLine());
 				}
 			} catch(Exception e) {
 				serverThread.getServerThreadThreads().remove(this);
 			}
 		}

 		public PrintWriter getPrintWriter() {
 			return printWriter; 
 		}
 	}
 }