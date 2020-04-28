import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.lang.*;


public class PeerNode {
	private String peerPortNumber = "";
	private HashSet<String> messages = new HashSet<String>();
	private ServerSocket serverSock = null;
	private Map<Socket, PrintWriter> socketToOutput = new HashMap<Socket,PrintWriter>(); 
	private ArrayList<Socket> connectionsToOtherPorts = new ArrayList<Socket>();
	private ArrayList<Socket> portsConnectedToPeer = new ArrayList<Socket>();

	//class represents other sockets trying to reach out and send a message in 
	private class PeerThread  {
		private Socket client;
		private PrintWriter out; 

		//accepts a connection from a socket from another port trying to reach out and receives its message
		public PeerThread(Socket client) throws IOException {
			this.client = client;
			out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
			socketToOutput.put(client,out);

		}
	}

	/*
	 *	go through every other port number on the server and connect a socket to their server socket
	 *  and send the message 
	 *
	 */
	synchronized public void multicastSend(HashSet<String> msg) throws IOException {
		String txt = "VOTE ";
		//iterate through each vote 
		for(String currentVote : msg) {
			txt += currentVote + " ";
			//message format: VOTE port vote port vote
		}
		Iterator<PrintWriter> outputIt = socketToOutput.values().iterator();
		while (outputIt.hasNext()) {
	    	PrintWriter pw = (PrintWriter)outputIt.next();
	    	pw.println(txt);
	    	pw.flush();
		}
	}


	//go through every single socket, check if connected. If it's connected we read the output on the socket 
	synchronized public HashSet<String> multicastReceive() throws IOException {
		messages.clear();
		for(Socket socket: connectionsToOtherPorts) {
			if(socket.isConnected()) {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String msg = in.readLine();
				if(msg != null) {
					for(int i=1; i < msg.split("\\s").length; i=i+2) {
						messages.add(msg.split("\\s")[i] + " " + msg.split("\\s")[i+1]);
					}
				}
			}
		}
		//messages.remove(0);
		return messages;
	}

	public ArrayList<Integer> getConnectionsToOtherPorts() {
		ArrayList<Integer> connectionPorts = new ArrayList<Integer>();

		for(Socket sock : connectionsToOtherPorts) {
			connectionPorts.add(sock.getLocalPort());
		}

		return connectionPorts;
	}

	public ArrayList<Integer> getPortsConnectedToPeers() {
		ArrayList<Integer> portsConnected = new ArrayList<Integer>();

		for(Socket sock : portsConnectedToPeer) {
			portsConnected.add(sock.getLocalPort());
		}

		return portsConnected;
	}

	//need to stop hanging if a participant has crashed -
	public void startListening(Integer port, ArrayList<Integer> otherPorts, Integer timeout) {
		try {
			ServerSocket serverSock = new ServerSocket(port);

			//create sockets to connect to other peers
			for(Integer portToConnectTo : otherPorts) {
				boolean flag = true;
				Socket socket = null;
				long startTime = System.currentTimeMillis();
				//if the port takes too long, then skip it 
				while(flag) {
					try {
						socket = new Socket("localhost", portToConnectTo);
						flag = false;
					} catch(IOException e) {
						try {
							TimeUnit.SECONDS.sleep(2);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}	
				connectionsToOtherPorts.add(socket);
			}

			//let sockets from other peers connect to this peer
			boolean flag = true;
			while(flag) {
				if(portsConnectedToPeer.size() == otherPorts.size()) {
					flag = false;
					break;
				}
				Socket client = serverSock.accept();
				portsConnectedToPeer.add(client);
				new PeerThread(client);
					
			}
		}catch(IOException e) {
			System.out.println("IOException caught in PeerNode.startListening due to: ");
			e.printStackTrace();
		}
	}


}