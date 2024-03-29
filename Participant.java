import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.time.*;


/*
 * Java Participant ⟨cport⟩ ⟨lport⟩ ⟨pport⟩ ⟨timeout⟩
 * - ⟨cport⟩ is the port number that the coordinator is listening on 
 * - ⟨lport⟩ is the port number that the logger server is listening on
 * - ⟨pport⟩ is the port number that this participant will be listening on
 * - ⟨timeout⟩ is a timeout in milliseconds. The timeout should be used by a participant when waiting for a message 
 * 	 from another process, in order to decide whether that process has failed.
 */
public class Participant {

	private String participantPorts = "";
	private ArrayList<Integer> participants = new ArrayList<Integer>();
	private ArrayList<Integer> participantsNotCrashed = new ArrayList<Integer>();
	
	private String votingOptions = "";
	private ArrayList<String> votingOptionsArr = new ArrayList<String>();
	
	private BufferedReader participantInChannel = null;
	private PrintWriter participantOutChannel = null;
	
	private BufferedReader input = null;

	
	private Socket participantSocket;
	
	//the value of the participantPortNumber
	private Integer participantPortNumberLog;
	private Integer coordinatorPortNumberLog;
	
	//private PeerNode participantPeerNode = null;
	
	private String outcome = "";
	private String voteDecided = "";
	private List<Integer> portsConsidered = new ArrayList<Integer>();;
	private Integer timeout;
	private PeerNode peer = null;

	private ParticipantLoggerThread logger = null;

	private ArrayList<Integer> randomLocalPorts = new ArrayList<Integer>();

	
	/*
	 * establishes a TCP connection with Coordinator object and sends a byte stream to 
	 * coordinator object of "JOIN <port>"
	 * need to get it to wait until the coordinator socket exists befoer 
	 */
	Participant(Integer coordinatorPortNumber, Integer loggerPortNumber, Integer participantPortNumber, Integer timeOut) {
		logger = new ParticipantLoggerThread(loggerPortNumber, participantPortNumber, timeOut);
		logger.start();
		peer = new PeerNode();
		try {
			participantPortNumberLog = participantPortNumber;
			coordinatorPortNumberLog = coordinatorPortNumber;
			this.timeout = timeOut;
			openConnection(coordinatorPortNumber, participantPortNumber);
			//participantSocket.setSoTimeout(timeOut);
			System.out.println("NEW SOCKET CREATED"); 
			
			//data stream established
			participantOutChannel = new PrintWriter(participantSocket.getOutputStream(), true);
			participantInChannel = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));
			
			//once the client is connected start handshaking
			if(participantSocket.isConnected()) {
				logger.messageSent(coordinatorPortNumberLog, "JOIN " + participantSocket.getLocalPort());
				logger.joinSent(participantSocket.getLocalPort());
				//stringRead = participantInChannel.readLine();
				handshakingprotocols();
			}		
		} catch(IOException e) {
			System.out.println("IO Exception thrown due to: " + e.getMessage());
			e.printStackTrace();
			
			logger.participantCrashed(participantPortNumberLog);
		} catch(Exception e) {
			System.out.println("Exception thrown due to: " + e.getMessage());
			e.printStackTrace();
			
			logger.participantCrashed(participantPortNumberLog);
		}
	}
	
	/**
	 * Tries to connect to the server socket, if it can't it will just wait for 2 seconds and try again
	 * @param coordinatorPortNumber
	 * @param participantPortNumber
	 */
	public void openConnection(Integer coordinatorPortNumber, Integer participantPortNumber) {
		boolean flag = true;
		while(flag) {
			try {
				participantSocket = new Socket("localhost", coordinatorPortNumber, null, participantPortNumber);
				participantSocket.setReuseAddress(true);
				flag = false;
			} catch(IOException e) {
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public Integer getPortNumber() {
		return participantPortNumberLog;
	}

	/**
	 * Method that establishes the handshaking protocols with the server 
	 * Sends the JOIN Message to the server and receives the DETAILS and VOTE_OPTIONS messages from the server
	 * @param inputString is an @String 
	 * @throws IOException 
	 * @throws Exception 
	 */
	public void handshakingprotocols() throws IOException {
		//sending the JOIN MESSAGE
		System.out.println("Participant Socket Connected"); //this is fine
		participantOutChannel.println("JOIN " + participantSocket.getLocalPort());
		System.out.println("Sent JOIN message: " + "JOIN " + participantSocket.getLocalPort());
		//tell logger we've sent the join message
		
		Token token = null;
		ReqTokenizer reqTokenizer = new ReqTokenizer();
		
		String lineRead = participantInChannel.readLine();
		token = reqTokenizer.getToken(lineRead);
		
		while(true) {
			//MIGHT CHANGE THIS TO A SWITCH STATEMENT
			//if the server is sending the details
			if(token instanceof DetailsToken) {
				System.out.println("Details exchange");
				receiveDetailsMessage(((DetailsToken) token).getPorts().toString());
				
			}
			
			//if the server is sending votingOptions
			if(token instanceof VoteOptionsToken) {
				System.out.println("Vote Options exchange");
				receiveVoteMessage((((VoteOptionsToken) token).getOptions().toString()));
				System.out.println("Begin Voting Rounds");
				//logger.messageReceived(participantSocket.getPort(), "Voting rounds Begun");
				outcome += votingProtocol();
				System.out.println(outcome);
				if(outcome.equals("NULL")) {
					return;
				} else { 
					sendOutcome();
					return;
				}
				
			}		

			token = reqTokenizer.getToken(participantInChannel.readLine());

		}
								
	}
	
	/**
	 * method to receive the details and store them appropriately 
	 * @throws IOException 
	 */
	public void receiveDetailsMessage(String details) throws IOException {	
		//print out every port - needs to be split by "[", "]" and ","
		for(int i=1; i < details.split("\\s").length; i++) {
			System.out.println("Port Numbers received " + details.split("\\s")[i] + " ");
			participantPorts += details.split("\\s")[i] + " ";
			participants.add(Integer.parseInt(details.split("\\s")[i]));
		}						
		
		System.out.println("We have DETAILS: " + participantPorts);	
		System.out.println("participants: " + participants.toString());
		
		//send message about receiving participants
		logger.messageReceived(coordinatorPortNumberLog, "DETAILS" + details);
		logger.detailsReceived(participants);
	}
	
	/**
	 * method to receive the votes and store them appropriately 
	 * @throws IOException 
	 */
	public void receiveVoteMessage(String votes) throws IOException {
		for(int i=1; i < votes.split("\\s").length; i++) {
			//System.out.println("Options: " + stringRead.split("\\s")[i] + " ");
			votingOptions = votingOptions + " " + votes.split("\\s")[i];
			votingOptionsArr.add(votes.split("\\s")[i]);
		}
			
		System.out.println("We have VOTING_OPTIONS:" + votingOptions + " for " + getPortNumber());
		//send message about receiving votes

		logger.messageReceived(coordinatorPortNumberLog, votes);
		logger.voteOptionsReceived(votingOptionsArr);
	}


	synchronized public void networkStartUp() {
		ArrayList<Integer> participantPortNumbers = new ArrayList<Integer>(participants);
		ArrayList<Integer> participantPortNumbersToRemove = new ArrayList<Integer>();
		//participantsNotCrashed.addAll(participants);

		logger.startedListening();
		peer.setUpServer(participantPortNumberLog, participants, timeout);
		
		try {
			TimeUnit.MILLISECONDS.sleep(100*participants.size());
		}catch(InterruptedException e) {}

		peer.startReachingOut(participantPortNumberLog, participants, timeout);
		for(Integer port : peer.getPortsEstablished()) {
			logger.connectionEstablished(port);
		}

		for(Integer port : peer.getCrashedPeers(participantPortNumbers)) {
			participantPortNumbersToRemove.add(port);
			logger.participantCrashed(port);
			System.out.println("Crashed Ports: " + port);
		}

		participantPortNumbers.removeAll(participantPortNumbersToRemove);

		peer.startListening(participantPortNumberLog, participants, timeout);
		for(Integer port : peer.getPortsConnectedToPeers()) {
			logger.connectionAccepted(port);
		}

		for(Integer port : peer.getCrashedPeers(participantPortNumbers)) {
			logger.participantCrashed(port);
		}

		try {
			TimeUnit.MILLISECONDS.sleep(100*participants.size());
		}catch(InterruptedException e) {}

		String logMessages = "";
		for(String logMessage : peer.getConnectionToOtherPortsLocalPorts()) {
			logMessages += logMessage.split("=")[1] + " ";
		}
		logMessages = logMessages.trim();

		//Logging messages for the 
		for(Integer ports : peer.getAllClientConnectedPorts()) {
			logger.messageSent(ports, logMessages);
		}

		Map<Integer, String> socketsToPorts = new HashMap<Integer, String>();
		for(String messages : peer.getMessages()) {
			if(socketsToPorts.containsKey(Integer.parseInt(messages.split("=")[0]))) {
				String message = socketsToPorts.get(Integer.parseInt(messages.split("=")[0])) + " " + messages.split("=")[1];
				socketsToPorts.put(Integer.parseInt(messages.split("=")[0]), message);
			} else {
				socketsToPorts.put(Integer.parseInt(messages.split("=")[0]), messages.split("=")[1]);
			}
		}

		Iterator<String> messagesIt = socketsToPorts.values().iterator();
		Iterator<Integer> portsIt = socketsToPorts.keySet().iterator();
		while(messagesIt.hasNext() && portsIt.hasNext()) {
			logger.messageReceived(portsIt.next(), messagesIt.next());
		}

		try {
			TimeUnit.MILLISECONDS.sleep(100*participants.size());
		}catch(InterruptedException e) {}


	}

	synchronized public String votingProtocol() {

		networkStartUp();
		try {
			TimeUnit.MILLISECONDS.sleep(100*participants.size());
		}catch(InterruptedException e) {}

		HashSet<String> values = new HashSet<String>();
		HashSet<String> valuesOfPreviousRound = new HashSet<String>();
		HashSet<String> valuesOfNextRound = new HashSet<String>();
		
		String initialVoteChoice = chooseVote();
		values.add(participantPortNumberLog + " " + initialVoteChoice);

		System.out.println("initial votes in values: " + values);


		for(int round = 1; round <= (participants.size()+2); round++) {

			Instant start = Instant.now();
			long roundStartTime = System.currentTimeMillis();
	
			logger.beginRound(round);

			System.out.println("\nCURRENT ROUND IS: " + round);
			System.out.println("current Values are: " + values + " for " + participantPortNumberLog);
			HashSet<String> valuesToSend = new HashSet<String>(values);
			valuesToSend.removeAll(valuesOfPreviousRound);
			
			peer.multicastSend(valuesToSend);
			
			for(Integer port : peer.getConnectionsToOtherPorts()) {
				ArrayList<Vote> votesSent = new ArrayList<Vote>();
				String message = "";
				for(String vote : valuesToSend) {
					message += vote + " ";
				}
				message = message.trim();
				logger.messageSent(port, message);

				for(String valuesToRecord : valuesToSend) {
					for(int i=0; i<valuesToRecord.split("\\s").length; i=i+2) {
						votesSent.add(new Vote(Integer.parseInt(valuesToRecord.split("\\s")[i]), valuesToRecord.split("\\s")[i+1]));
					}
				}
				if(votesSent.isEmpty()) {
					continue;
				} else {
					logger.votesSent(port, votesSent);
				}
			}

			try {
				TimeUnit.MILLISECONDS.sleep(100*participants.size());
			}catch(InterruptedException e) {}
			
			valuesOfNextRound.clear();
			valuesOfNextRound.addAll(values);

			ArrayList<String> messagesReceived = new ArrayList<String>(peer.multicastReceive(timeout));
			ArrayList<String> messagesReceivedDivided = new ArrayList<String>();

			for(String messageFromPeer : messagesReceived) {
				for(int i=0; i < messageFromPeer.split("\\s").length; i=i+2) {
					if(messageFromPeer.split("\\s").length < 2) {
						break;
					} else {
						messagesReceivedDivided.add(messageFromPeer.split("\\s")[i] + " " + messageFromPeer.split("\\s")[i+1]);
					}
				}	
			}
			valuesOfNextRound.addAll(messagesReceivedDivided);

			if(messagesReceived.isEmpty()) {
				return outcomeDecision(values);
			}
			
			for(Integer portNum : peer.getHashMapOfMessages().keySet()) {
				ArrayList<Vote> votesReceived = new ArrayList<Vote>();
				logger.messageReceived(portNum, peer.getHashMapOfMessages().get(portNum));
				for(int i=0; i<peer.getHashMapOfMessages().get(portNum).split("\\s").length; i=i+2) {
					if(peer.getHashMapOfMessages().get(portNum).split("\\s").length < 2) {
						break;
					} else {
						//check if port number doesn't work
						ArrayList<Integer> allPorts = new ArrayList<Integer>(participants);
						allPorts.add(participantPortNumberLog);
						if(!allPorts.contains(Integer.parseInt(peer.getHashMapOfMessages().get(portNum).split("\\s")[i]))) {
							System.out.println("Port Number doesn't exist");
							continue;

						}
						//check if vote doesn't work
						if(!votingOptionsArr.contains(peer.getHashMapOfMessages().get(portNum).split("\\s")[i+1])) {
							System.out.println("Vote doesn't exist");
							continue;
						}
						votesReceived.add(new Vote(Integer.parseInt(peer.getHashMapOfMessages().get(portNum).split("\\s")[i]), peer.getHashMapOfMessages().get(portNum).split("\\s")[i+1]));
					}
				}

				if(votesReceived.isEmpty()) {
					continue;
				} else {
			
					logger.votesReceived(portNum, votesReceived);
				}
			}

			try {
				TimeUnit.MILLISECONDS.sleep(100*participants.size());
			}catch(InterruptedException e) {}

			for(Integer port : peer.getCrashedPeersInRound()) {
				participantsNotCrashed.remove(port);
				logger.participantCrashed(port);
			}

			
			//round r values become round r-1
			valuesOfPreviousRound.clear();
			valuesOfPreviousRound.addAll(values);

			values.clear();
			values.addAll(valuesOfNextRound);

			System.out.println("Next round's Values: " + values + " for " + participantPortNumberLog);
			System.out.println("Next Round's previous values: " + valuesOfPreviousRound + "\n");

			//literally added to allow participants to catch up 
			Instant end = Instant.now();
			Duration interval = Duration.between(start, end);
			
			System.out.println("Duration of round: " + interval.toMillis());
			logger.endRound(round);
			
			if(values.equals(valuesOfPreviousRound)) {
				return outcomeDecision(values);
			}
			
			try {
				TimeUnit.MILLISECONDS.sleep(100*participants.size());
			}catch (InterruptedException e) {}
			

			if(interval.toMillis() >= ((participants.size()*timeout) + (300*participants.size()))) {
				System.out.println("Round Timed out");
				participantsNotCrashed.remove(participantPortNumberLog);
				logger.participantCrashed(participantPortNumberLog);
				return "NULL";
			}

			if(round > (participants.size()+1)) {
				return outcomeDecision(values);
			}

			/*
			HashSet<String> completedRound = new HashSet<String>();
			completedRound.add(participantPortNumberLog.toString());
			peer.multicastSend(completedRound);
			*/ 

		}

		return "NULL";

	}
	

	public String outcomeDecision(HashSet<String> values) {
	//System.out.println("NOW DECIDING ON OUTCOME USING " + values.toString());
		//create a string of ports involved
		String portsInvolved = "";
		ArrayList<Integer> portsInOutcome = new ArrayList<Integer>();
		//get the final ports involved
		for(String vote : values) {
			if(vote.split("\\s")[0].equals(participantPortNumberLog.toString())) {
				portsInOutcome.add(0,Integer.parseInt(vote.split("\\s")[0]));
				continue;
			} else {
				portsInvolved += vote.split("\\s")[0] + " " ;
				portsInOutcome.add(Integer.parseInt(vote.split("\\s")[0]));
			}
		}

		voteDecided += voteDecider(values);
		//System.out.println("ports involved: " + portsInvolved);
		//System.out.println("Decided Vote: " + voteDecider(values) + " from " + participantPortNumber);
		//decided vote should be the maximum of all the votes collected from values
		
		logger.outcomeDecided(voteDecided, portsInOutcome);
		return "OUTCOME " + voteDecided + " " +  participantPortNumberLog + " " + portsInvolved;
	}
	

	/**
	 * A simple method to choose a random vote from the vote options - WORKS FINE
	 * @return a vote that was chosen at random
	 */
	
	public String chooseVote() {
		while(!votingOptionsArr.isEmpty()) {
			Random rand = new Random();
			String voteChoice = votingOptionsArr.get(rand.nextInt(votingOptionsArr.size()));
			//System.out.println("VOTE CHOSEN: " + voteChoice);
			return voteChoice;
		}
		return null;
	}
	
	
	/**
	 * method to help find the deciding vote, iterate over the values and find which ones occur the most -- this works fine
	 * if two are the max, then choose the lexicographical order
	 * @param values : the f+1 round of votes and ports to be analysed 
	 * @return the outcome vote to be sent to the coordinator by the votingProtocol
	 */
	
	public String voteDecider(HashSet<String> votes) {
		//votes is of element format PORT_NUMBER:VOTE
		Map<String,Integer> frequencyOfVotes = new HashMap<String,Integer>();
		
		//populate the frequency hashmap
		for(String currentVote : votes) {
			// currentVote is in the form PORT_NUMBER:VOTE_OPTION
			if(frequencyOfVotes.containsKey(currentVote.split("\\s")[1])) {
				Integer currentFreq = frequencyOfVotes.get(currentVote.split("\\s")[1]);
				Integer newFreq = currentFreq + 1;
				frequencyOfVotes.replace(currentVote.split("\\s")[1], currentFreq,newFreq);
			} else {
				frequencyOfVotes.put(currentVote.split("\\s")[1],1);
			}
		}
		
		//finding most frequent Vote
		Set<Entry<String,Integer>> entrySet = frequencyOfVotes.entrySet();
		Integer frequency = 1;
		String vote = "Z";
		for(Entry<String,Integer> entry : entrySet) {
			//case where the vote occurs more that the current most frequent
			if(entry.getValue() > frequency) {
				vote = entry.getKey();
				frequency = entry.getValue();
			}

			//case where you should compare lexicographical order
			if(entry.getValue() == frequency) {
				String currentKey = entry.getKey();
				//case where currentKey is greater lex. than element
				if(currentKey.compareTo(vote) < 0) {
					vote = currentKey;
				} 
			}
		}
		return vote;
	}
	
	/**
	 * just sends the outcome message to the coordinator
	 */
	public void sendOutcome() {
		participantOutChannel.println(outcome);
		for(int i=2; i < outcome.split("\\s").length;i++ ) {
			portsConsidered.add(Integer.parseInt(outcome.split("\\s")[i]));
		}
		logger.messageSent(coordinatorPortNumberLog, outcome);
		logger.outcomeNotified(voteDecided, portsConsidered);
		participantOutChannel.flush();

	}
	
	
	public static void main(String[] args) throws IOException {
		//portNumber = Integer.parseInt(args[2]);
		Integer coordinatorPortNumber = Integer.parseInt(args[0]);
		Integer loggerPortNumber = Integer.parseInt(args[1]);
		Integer participantPortNumber = Integer.parseInt(args[2]);
		Integer timeout = Integer.parseInt(args[3]);
		//ParticipantLogger.initLogger(loggerPortNumber, participantPortNumber, timeout);
		Participant currentParticipant = new Participant(coordinatorPortNumber, loggerPortNumber, participantPortNumber, timeout);
		
	}
	
	
	
}