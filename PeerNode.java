import java.net.*;
import java.io.*;
import java.util.*;
import java.time.*;
import java.util.concurrent.TimeUnit;


public class PeerNode {
	private String peerPortNumber = "";
	private ArrayList<String> messages = new ArrayList<String>();
	private ServerSocket serverSock = null;
	private Map<Socket, PrintWriter> socketToOutput = new HashMap<Socket,PrintWriter>();
	private Map<PrintWriter, Socket> outputToSocket = new HashMap<PrintWriter,Socket>();  
	private ArrayList<Socket> connectionsToOtherPorts = new ArrayList<Socket>();
	private ArrayList<Socket> portsConnectedToPeer = new ArrayList<Socket>();
	private ArrayList<Socket> crashedPeer = new ArrayList<Socket>();
	private ArrayList<Integer> initPorts;
	private Integer timeout;
	private Map<Integer,String> portToMessagePortSent = new HashMap<Integer,String>();
	private ArrayList<PeerThread> listOfPeers = new ArrayList<PeerThread>();
	private ArrayList<Integer> portsAccepted = new ArrayList<Integer>();
	private HashSet<String> connectionToOtherPortsLocalPorts = new HashSet<String>();



	//class represents other sockets trying to reach out and send a message in 
	private class PeerThread  {
		private Socket client;
		private PrintWriter out; 

		//accepts a connection from a socket from another port trying to reach out and receives its message
		public PeerThread(Socket client) throws IOException {
			this.client = client;
			out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
			socketToOutput.put(client,out);
			outputToSocket.put(out, client);
		}

		public void startPeer() {
			socketToOutput.put(client,out);
			outputToSocket.put(out, client);
		}

		public void destroyPeer() {
			socketToOutput.remove(client);
			outputToSocket.remove(out);
		}

		public Socket getClient() {
			return client;
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
			System.out.println("This Participant has no one to send to");
			return;
		}	
		Iterator<PrintWriter> outputIt = socketToOutput.values().iterator();
		while (outputIt.hasNext()) {
	    		PrintWriter pw = (PrintWriter) outputIt.next();

	    		if(pw.checkError()) {
					System.out.println(outputToSocket.get(pw).getPort() + " has crashed FROM SENDING");
					portToMessagePortSent.remove(outputToSocket.get(pw).getPort());
					//connectionsToOtherPorts.remove(outputToSocket.get(pw));
					//crashedPeer.add(outputToSocket.get(pw));
					continue;
	    		} else {

		    		pw.println(txt);
		    		System.out.println("MESSAGES SENT: " + txt + " to " + outputToSocket.get(pw).getPort());
		    		pw.flush();
		    	}
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
			System.out.println("trying to receive from: " + socket.getPort());
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				boolean flag = true;
				Instant start = Instant.now();
				while(flag) {
					socket.setSoTimeout(timeout);
					try {
						String msg = in.readLine();
						if(msg != null) {
							flag = false;
							System.out.println("MESSAGE RECEIVED: " + msg);

							//need to remove the "Vote" 
							msg = msg.replace("VOTE ", "");
							
							if(msg.split("\\s").length % 2 == 1) {
								System.out.println("Vote message is formatted improperly. Message being discarded");
								msg = "";
							}
							
							if(msg.equals("VOTE")) {
								msg = "";
							}

							messages.add(msg);
							portToMessagePortSent.put(socket.getPort(), msg);
							msg = "";
							socket.setSoTimeout(0);
						}
					} catch(SocketTimeoutException e) {
						flag = false;
						//System.out.println("Took too long to receive message");
						System.out.println(socket.getPort() + " has crashed");
						portToMessagePortSent.remove(socket.getPort());
						socketToOutput.remove(socket);
						crashedPeer.add(socket);
						socket.setSoTimeout(0);
						continue;
					}
				}
			
			//due to their being no message etc. - should consider that port crashed 
			} catch(IOException e) {
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

	public ArrayList<String> getMessages() {
		return messages;
	}

	public ArrayList<Integer> getAllClientConnectedPorts() {
		ArrayList<Integer> connectionPorts = new ArrayList<Integer>();
		for(Socket sock : portsConnectedToPeer) {
			if(portsAccepted.contains(sock.getPort())) {
				connectionPorts.add(sock.getPort());
			} else {continue;}
		}

		return connectionPorts;
	} 

	public ArrayList<String> getConnectionToOtherPortsLocalPorts() {
		ArrayList<String> connects = new ArrayList<String>();
		connects.addAll(connectionToOtherPortsLocalPorts);
		return connects;
	}
	//need to remove sockets that aren't connected to the ports from the connections to other ports
	public ArrayList<Integer> getPortsConnectedToPeers() {
		ArrayList<Integer> connectionPorts = new ArrayList<Integer>();
		//send out an empty message and see which ports send - the ports that send still exist 
		//System.out.println
		for(Socket sock : portsConnectedToPeer) {
			if(portsAccepted.contains(sock.getPort())) {
				connectionPorts.add(sock.getPort());
			} else {continue;}
		}

		System.out.println("connectionPorts: " + connectionPorts.toString());
		return connectionPorts;
	}

	public ArrayList<Integer> getPortsEstablished() {
		ArrayList<Integer> connectionPorts = new ArrayList<Integer>();

		for(Socket sock : connectionsToOtherPorts) {
			connectionPorts.add(sock.getLocalPort());
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

		//remove all crashed participants from connectionsToOtherPorts
		
		ArrayList<Socket> socketsToRemove = new ArrayList<Socket>();
		for(Socket sock : connectionsToOtherPorts) {
			if(crashedPorts.contains(sock.getPort())) {
				socketsToRemove.add(sock);
			}
		}
		connectionsToOtherPorts.removeAll(socketsToRemove);
		

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

	synchronized public void setUpServer(Integer port, ArrayList<Integer> otherPorts, Integer timeout) {
		this.timeout = timeout;
		try {
			serverSock = new ServerSocket(port); 
			System.out.println("Server Socket: " + serverSock.toString());
			try {
				serverSock.setSoTimeout(timeout);
			} catch(SocketException e) {
				System.out.println("Error with Server Sockets underlying protocol");
			}
			System.out.println("PORT SERVERSOCK IS ON: " + serverSock.getLocalPort());
		}catch(IOException e) {
			System.out.println("IOException caught in PeerNode.startListening due to: ");
			e.printStackTrace();
		}
	}

	synchronized public void startReachingOut(Integer port, ArrayList<Integer> otherPorts, Integer timeout) {
			initPorts = new ArrayList<Integer>();
			for(Integer portToConnectTo : otherPorts) {
				boolean flag = true;
				Socket socket = null;
				while(flag) {
					try {
						socket = new Socket("localhost", portToConnectTo);
						connectionsToOtherPorts.add(socket);
						System.out.println("PEER CONNECTED SOCKET: " + socket.getPort());
						initPorts.add(portToConnectTo);
						flag = false;
					} catch(IOException e) {
						System.out.println(portToConnectTo + " HAS CRASHED");
						//initPorts.remove(portToConnectTo);
						flag = false;
						continue;
					}
				}
				//Unnecessary wait - remove
				/*
				try {
					TimeUnit.MILLISECONDS.sleep(3000);
				}catch(InterruptedException e) {}
				*/
			}

			ArrayList<Integer> portsToRemove = new ArrayList<Integer>();
			for(Integer ports : initPorts) {
				if(hasPortCrashed(ports)) {
					//remove the port from initPorts
					portsToRemove.add(ports);
				}
			}
			initPorts.removeAll(portsToRemove);
			System.out.println("InitPorts: " + initPorts.toString());

			//having this in creates a bug with participants crashing
			ArrayList<Socket> socketsToRemove = new ArrayList<Socket>();
			for(Socket sock : connectionsToOtherPorts) {
				if(!initPorts.contains(sock.getPort())) {
					socketsToRemove.add(sock);
				}
			}
			connectionsToOtherPorts.removeAll(socketsToRemove);
			System.out.println("connectionsToOtherPorts: " + connectionsToOtherPorts.toString());
	}

	synchronized public void startListening(Integer port, ArrayList<Integer> otherPorts, Integer timeout) {
		boolean flag = true;
			//if the port takes too long, then skip it - need to add 
			while(flag) {
				Socket client = null;
				try {	
					client = serverSock.accept();
				} catch(SocketTimeoutException e) {
					System.out.println("No more sockets to connect");
					flag = false;
					break;
				} catch(IOException e) {
					System.out.println("problem connecting with a socket");
					continue;
				}

				System.out.println("CLIENT CONNECTED: " + client.getPort());
				
				portsConnectedToPeer.add(client);
				try {
					new PeerThread(client);
				} catch(IOException e){
					System.out.println("problem starting up peer thread for client: " + client.getPort() + "\n removing client from network");
					portsConnectedToPeer.remove(client);
				}

			}

			System.out.println("portsConnectedToPeer: " + portsConnectedToPeer.toString());
			sendConnectionsToOtherPorts(port);
			portsAccepted.addAll(receiveConnectionsToOtherPorts());

					
	}

	public void sendConnectionsToOtherPorts(Integer port) {
		for(Socket sock : connectionsToOtherPorts) {
			Integer portNum = sock.getLocalPort();
			Integer partPortNum = sock.getPort();
			connectionToOtherPortsLocalPorts.add(port.toString()+"="+portNum.toString());
		} 

		multicastSend(connectionToOtherPortsLocalPorts);
	}

	public ArrayList<Integer> receiveConnectionsToOtherPorts() {
		ArrayList<String> connectionPorts = new ArrayList<String>();
		connectionPorts.addAll(multicastReceiveInitial());
		ArrayList<Integer> connectionPortsInteger = new ArrayList<Integer>();
		
		for(String ports : connectionPorts){
			for(String port : ports.split("\\s")) {
				connectionPortsInteger.add(Integer.parseInt(port.split("=")[1]));
			}
		}

		System.out.println("connectionPortsInteger: "+ connectionPortsInteger.toString());

		ArrayList<Integer> portsStillWorking = new ArrayList<Integer>();
		for(String ports : connectionPorts) {
			for(String port : ports.split("\\s")){
				if(!portsStillWorking.contains(Integer.parseInt(port.split("=")[0]))) {
					portsStillWorking.add(Integer.parseInt(port.split("=")[0]));
				}
			}
		}

		System.out.println("portsStillWorking: " + portsStillWorking.toString());

		//
		ArrayList<Integer> initPortsToRemove = new ArrayList<Integer>();
		for(Integer ports : initPorts) {
			if(!portsStillWorking.contains(ports)){
				initPortsToRemove.add(ports);
			}
		}
		initPorts.removeAll(initPortsToRemove);

		System.out.println("initPorts: " + initPorts.toString());


		System.out.println("connectionPortsInteger: " + connectionPortsInteger.toString());
		return connectionPortsInteger;
	}


		synchronized public ArrayList<String> multicastReceiveInitial() {
		messages.clear();
		for(Socket socket: connectionsToOtherPorts) {
			System.out.println("trying to receive from: " + socket.getPort());
			try {
				if(socket.isConnected()) {
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					boolean flag = true;
					Instant start = Instant.now();
					while(flag) {

						Instant currentTime = Instant.now();
						//System.out.println("currentTime: " + currentTime);
						Duration interval = Duration.between(start,currentTime);
						if(interval.toMillis() >= 1000) {
							flag = false;
							//System.out.println("Took too long to receive message");
							System.out.println(socket.getPort() + " hasn't received");
							continue;
						} 

						String msg = in.readLine();
						if(msg != null) {
							flag = false;
							System.out.println("MESSAGE RECEIVED: " + msg);

							//need to remove the "Vote" 
							msg = msg.replace("VOTE ", "");
							
							if(msg.equals("VOTE")) {
								msg = "";
							}

							for(String message : msg.split("\\s")){
								if(message.split("=").length != 2){
									System.out.println("discard faulty statement");
									continue;
								} else {
									messages.add(message);
								}
							}
							msg = "";
						} else {
							continue;
						}
					}
				
				}
				
			//due to their being no message etc. - should consider that port crashed 
			} catch(IOException e) {
				continue;
			}	
		}
		
		System.out.println(messages);
		return messages;
	}

	//checks if the server socket on a specific port has crashed or not
	// creates a socket and connects to it
	//returns true if it has crashed, false if it's able to connect
	public boolean hasPortCrashed(Integer portToCheck) {
		Socket sock = null;
		try {
			sock = new Socket("localhost", portToCheck);
			return false;
		} catch(IOException e) {
			return true;
		} finally {
			if(sock != null) {
				try {
					sock.close();
				} catch (IOException e) {}
			}
		}
	}

}