import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class PeerNode {
	private String peerPortNumber = "";
	private ArrayList<String> messages = new ArrayList<String>();
	private ServerSocket serverSock = null;
	private Map<Socket, PrintWriter> socketToOutput = new HashMap<Socket,PrintWriter>(); 
	private ArrayList<Socket> connectionsToOtherPorts = new ArrayList<Socket>();
	private ArrayList<Socket> portsConnectedToPeer = new ArrayList<Socket>();
	private ArrayList<Socket> crashedPeer = new ArrayList<Socket>();
	private ArrayList<Integer> initPorts;
	private Integer timeout;
	private Map<Integer,String> portToMessagePortSent = new HashMap<Integer,String>();


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
		String txt = "VOTE";
		//iterate through each vote 
		for(String currentVote : msg) {
			txt += " " + currentVote;
			//message format: VOTE port vote port vote
		}
		//check if the 
		if(socketToOutput.isEmpty()) {
			System.out.println("This Participant has crashed");
			return;
		}	
		Iterator<PrintWriter> outputIt = socketToOutput.values().iterator();
		while (outputIt.hasNext()) {
	    		PrintWriter pw = (PrintWriter) outputIt.next();
	    		pw.println(txt);
	    		System.out.println("MESSAGES SENT: " + txt);
	    		pw.flush();
		}
	}

	//go through every single socket, check if connected. If it's connected we read the output on the socket 
	/**
	 * need to work out a way to check if for a socket you're receiving a message from if their server socket is closed
	 * GET THIS TO THROW THE IOEXCEPTION ITSELF AND HANDLE IT ACCORDINGLY
	 */
	synchronized public ArrayList<String> multicastReceive(Integer timeout) {
		messages.clear();
		for(Socket socket: connectionsToOtherPorts) {
			try {
				socket.setSoTimeout(timeout);
				if(socket.isConnected() && (!crashedPeer.contains(socket))) {
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String msg = in.readLine();
					if(msg != null) {
						System.out.println("MESSAGE RECEIVED: " + msg + " from " + socket.getPort());
						//need to remove the "Vote" 
						msg = msg.replace("VOTE ", "");
						if(msg.equals("VOTE")) {
							msg = "";
						}
						messages.add(msg);
						portToMessagePortSent.put(socket.getPort(), msg);
						msg = "";
					} else {
						System.out.println(socket.getPort() + " has crashed");
						portToMessagePortSent.remove(socket.getPort());
						socketToOutput.remove(socket);
						crashedPeer.add(socket);
						continue;
					}
				}
			//due to their being no message etc. - should consider that port crashed 
			}catch(SocketTimeoutException e) {
				System.out.println("Took too long to receive message");
				System.out.println(socket.getPort() + " has crashed");
				portToMessagePortSent.remove(socket.getPort());
				socketToOutput.remove(socket);
				crashedPeer.add(socket);
				continue;
			} catch(IOException e) {
				continue;
			}	
			try {
				socket.setSoTimeout(0);
			} catch(SocketException e) {
				continue;
			}
		}
		
		//messages.remove(0);
		return messages;
	}

	public Map<Integer,String> getHashMapOfMessages() {
		return portToMessagePortSent;
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
		ArrayList<Integer> connectionPorts = new ArrayList<Integer>();

		for(Socket sock : portsConnectedToPeer) {
			connectionPorts.add(sock.getPort());
		}

		return connectionPorts;
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
		connectionsToOtherPorts.removeAll(crashedPeer);
		for(Socket sock : crashedPeer) {
			getCrashedPeers.add(sock.getPort());
		}
		crashedPeer.clear();

		return getCrashedPeers;
	}

	//need to stop hanging if a participant has crashed -
	synchronized public void startListening(Integer port, ArrayList<Integer> otherPorts, Integer timeout) {
		this.timeout = timeout;
		try {
			initPorts = new ArrayList<Integer>(otherPorts);
			serverSock = new ServerSocket(port);
			System.out.println("PORT SERVERSOCK IS ON: " + serverSock.getLocalPort());
			
			try {
				TimeUnit.MILLISECONDS.sleep(4*timeout);
			} catch(InterruptedException e) {
				System.out.println("Sleeping has been interrupted in PeerNode.startListening");
			}
		
			//create sockets to connect to other peers - for receiving messages 
			for(Integer portToConnectTo : otherPorts) {
				boolean flag = true;
				Socket socket = null;
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
				portsConnectedToPeer.add(client);
				new PeerThread(client);
					
			}
			//System.out.println("PORTS CONNECTED TO PEER: " + portsConnectedToPeer.toString());
		}catch(IOException e) {
			System.out.println("IOException caught in PeerNode.startListening due to: ");
			e.printStackTrace();
		}
	}

	public void cleanUp() {
		try {
			//close the serverSocket
			serverSock.close();

			//close all sockets that the peer receeives messages
			Iterator<Socket> sockReceiveIt = connectionsToOtherPorts.iterator();
			while(sockReceiveIt.hasNext()) {
				sockReceiveIt.next().close();
			} 

			//close all sockets that the peer sends out messages
			Iterator<Socket> sockSendIt = socketToOutput.keySet().iterator();
			while(sockSendIt.hasNext()) {
				sockSendIt.next().close();
			}

		} catch(IOException e) {
			System.out.println("Problem with the cleanup of the Peers Sockets");
		}
	}



}