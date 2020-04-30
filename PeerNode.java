import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class PeerNode {
	private String peerPortNumber = "";
	private HashSet<String> messages = new HashSet<String>();
	private ServerSocket serverSock = null;
	private Map<Socket, PrintWriter> socketToOutput = new HashMap<Socket,PrintWriter>(); 
	private ArrayList<Socket> connectionsToOtherPorts = new ArrayList<Socket>();
	private ArrayList<Integer> portsConnectedToPeer = new ArrayList<Integer>();
	private ArrayList<Socket> crashedPeer = new ArrayList<Socket>();
	private ArrayList<Integer> initPorts;
	private Integer timeout;


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
	 * DOESN'T NEED TO THROW IO ERROR
	 */
	synchronized public void multicastSend(HashSet<String> msg) {
		String txt = "VOTE ";
		//iterate through each vote 
		for(String currentVote : msg) {
			txt += currentVote + " ";
			//message format: VOTE port vote port vote
		}
		//check if the 
		if(socketToOutput.isEmpty()) {
			System.out.println("This Participant has been crashed");
			return;
		}	
		Iterator<PrintWriter> outputIt = socketToOutput.values().iterator();
		while (outputIt.hasNext()) {
	    		PrintWriter pw = (PrintWriter) outputIt.next();
	    		pw.println(txt);
	    		System.out.println("MESSAGES SENT TO OTHER PEERS " + txt);
	    		pw.flush();
		}
	}

	public ArrayList<Integer> getPortsInSend() {
		ArrayList<Integer> portsSentTo = new ArrayList<Integer>();

		for(Socket sock : socketToOutput.keySet()) {
			portsSentTo.add(sock.getLocalPort());
		}

		return portsSentTo;
	}

	//go through every single socket, check if connected. If it's connected we read the output on the socket 
	/**
	 * need to work out a way to check if for a socket you're receiving a message from if their server socket is closed
	 * GET THIS TO THROW THE IOEXCEPTION ITSELF AND HANDLE IT ACCORDINGLY
	 */
	synchronized public HashSet<String> multicastReceive(Integer timeout) {
		messages.clear();
		for(Socket socket: connectionsToOtherPorts) {
			try {
				if(socket.isConnected() && (!crashedPeer.contains(socket))) {
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String msg = in.readLine();
					
					if(msg != null) {
						for(int i=1; i < msg.split("\\s").length; i=i+2) {
							messages.add(msg.split("\\s")[i] + " " + msg.split("\\s")[i+1]);
						}
					} else {continue;}

				}

			//due to their being no message etc. - should consider that port crashed 
			} catch(IOException e) {
				//stop receiving from this socket
				//connectionsToOtherPorts.remove(socket);
				crashedPeer.add(socket);
				updateSocketsBeingReceivedFrom();
				//stop sending to this socket
				socketToOutput.remove(socket);
			}
		}
		//messages.remove(0);
		return messages;
	}

	public void updateSocketsBeingReceivedFrom() {
		for(Socket crashedSocket : crashedPeer) {
			if(connectionsToOtherPorts.contains(crashedSocket)) {
				connectionsToOtherPorts.remove(crashedSocket);
			}
		}
	}

	public ArrayList<Integer> getConnectionsToOtherPorts() {
		ArrayList<Integer> connectionPorts = new ArrayList<Integer>();

		for(Socket sock : connectionsToOtherPorts) {
			connectionPorts.add(sock.getPort());
		}

		return connectionPorts;
	}

	public ArrayList<Integer> getPortsConnectedToPeers() {

		return portsConnectedToPeer;
	}

	public ArrayList<Integer> getCrashedPeers(ArrayList<Integer> participantPorts) {
		ArrayList<Integer> crashedPorts = new ArrayList<Integer>();
		for(Integer participantPort : participantPorts) {
			if(!initPorts.contains(participantPort)) {
				crashedPorts.add(participantPort);
			}
		}
		return crashedPorts;
	}

	public ArrayList<Integer> getCrashedPeersInRound() {
		ArrayList<Integer> getCrashedPeers = new ArrayList<Integer>();

		for(Socket sock : crashedPeer) {
			getCrashedPeers.add(sock.getLocalPort());
		}
		crashedPeer.clear();

		return getCrashedPeers;
	}

	public void startServersTimeout(ArrayList<Integer> participants) throws SocketTimeoutException {
		try {
			serverSock.setSoTimeout(timeout*participants.size());
		} catch(SocketException e) {
			System.out.println("Cant get the timeout working");
			return;
		}
	}

	//need to stop hanging if a participant has crashed -
	public void startListening(Integer port, ArrayList<Integer> otherPorts, Integer timeout) {
		this.timeout = timeout;
		try {
			initPorts = new ArrayList<Integer>(otherPorts);
			serverSock = new ServerSocket(port);
			System.out.println("PORT SERVERSOCK IS ON: " + serverSock.getLocalPort());
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch(InterruptedException e) {
				System.out.println("Sleeping has been interrupted in PeerNode.startListening");
			}
			//System.out.println(otherPorts.toString());
			//create sockets to connect to other peers - for receiving messages 
			for(Integer portToConnectTo : otherPorts) {
				boolean flag = true;
				Socket socket = null;
				//if the port takes too long, then skip it 
				while(flag) {
					try {
						socket = new Socket("localhost", portToConnectTo);
						connectionsToOtherPorts.add(socket);
						System.out.println("PEER CONNECTED SOCKET: " + socket.getPort());
						flag = false;
					} catch(IOException e) {
						System.out.println(portToConnectTo + " HAS CRASHED");
						initPorts.remove(portToConnectTo);
						flag = false;
						continue;
					}
				}	
			}

			//let sockets from other peers connect to this peer - for sending messages 
			boolean flag = true;
			//if the port takes too long, then skip it 
			while(flag) {
				if(portsConnectedToPeer.size() == initPorts.size()) {
					flag = false;
					break;
				}
				Socket client = serverSock.accept();
				System.out.println("CLIENT CONNECTED: " + client.getPort());
				portsConnectedToPeer.add(client.getLocalPort());
				new PeerThread(client);
					
			}
			//System.out.println("PORTS CONNECTED TO PEER: " + portsConnectedToPeer.toString());
		}catch(IOException e) {
			System.out.println("IOException caught in PeerNode.startListening due to: ");
			e.printStackTrace();
		}
	}



}