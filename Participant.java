/*
 * 1. Register with coordinator - establish TCP connection and send byte stream "JOIN <Port>]"
 * 2. Get Details on other participants - message from coordinator as byte stream "DETAILS [<Port>] 
 * 3. get vote options from coordinator - message from coordinator on voting in byte stream 
 * 	  "VOTE_OPTIONS [<option>]"
 * 4. I. participant sends and receives to and from all other participants and  
 * 	  II. DON'T FULLY UNDERSTAND THIS BIT
 * 5. Decide on the vote outcome with a majority - if tie pick the first option according to an 
 * 	  ascendant lexicographic order of the options that have tied majority
 * 6. Inform Coordinator of the outcome - send message to coordinator on same connection established during 
 * 	  the initial stage. Message should be of structure 
 * 	  "OUTCOME <Vote Choice> [<ports taken into consideration>]"
 */
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/*
 * Java Participant ⟨cport⟩ ⟨lport⟩ ⟨pport⟩ ⟨timeout⟩
 * Where ⟨cport⟩ is the port number that the coordinator is listening on 
 * - ⟨lport⟩ is the port number that the logger server is listening on
 * - ⟨pport⟩ is the port number that this participant will be listening on
 * - ⟨timeout⟩ is a timeout in milliseconds. The timeout should be used by a participant when waiting for a message 
 * 	 from another process, in order to decide whether that process has failed.
 */
public class Participant {
	//vote being made by participant
	private String vote;
	//port number of participant
	private List<Integer> participantPorts;
	private List<String> votingOptions;
	private static Integer portNumber;
	private HashMap<String, Integer> votesInRound;
	private BufferedReader participantInChannel = null;
	private PrintWriter participantOutChannel = null;
	private Socket participantSocket;
	//State of the participant
	private String state;
	
	Participant() throws UnknownHostException, IOException {	
		//establish the TCP connection with the server and send the server a byteStream stating its port number
		join();
	}
	
	/*
	 * establishes a TCP connection with Coordinator object and sends a byte stream to 
	 * coordinator object of "JOIN <port>"
	 */
	public void join() {
		try {
			
			participantSocket = new Socket("localhost", portNumber);
			participantOutChannel = new PrintWriter(participantSocket.getOutputStream(), true);
			participantInChannel = new BufferedReader(new InputStreamReader(participantSocket.getInputStream()));
			
			//if the participantSocket connects to the coordinator, then send it the message "JOIN <Port>"
			if(participantSocket.isConnected()) {
				participantOutChannel.println("JOIN" + participantSocket.getPort());
					getParticipants();
					getVoteOptions();
					decideVote();
			}
		} catch(UnknownHostException e) {
			System.out.println("Server not found: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO Exception thrown due: " + e.getMessage());
		}	
	}
	
	//Get Details on other participants - message from coordinator as byte stream "DETAILS [<Port>] 
	//populates the list, "participantPorts" with the ports of all other participants 
	public void getParticipants() {
		try {
			String line;
			line = participantInChannel.readLine();
				
			//checks if the incoming message is the details message from the server
			if(line.split("\\s")[0].equals("DETAILS")) {
				String[] splitInputLine = line.split("\\s");
					
				//populate the list of port numbers
				for(String port : splitInputLine) {
					participantPorts.add(Integer.parseInt(port));
				}
				//removes "DETAILS" from the list
				participantPorts.remove(0);
			}
		//return participantPorts;
		
		} catch (IOException e) {
			System.out.println("IO Exception due to: " + e.getMessage());
		
		}
		//return participantPorts;
	}
	
	//get vote options from coordinator - message from coordinator on voting in byte stream "VOTE_OPTIONS [<option>]"
	public void getVoteOptions() {
		try {
			String line;
			line = participantInChannel.readLine();
				
			//checks if the incoming message is the details message from the server
			if(line.split("\\s")[0].equals("VOTE_OPTIONS")) {
				String[] splitInputLine = line.split("\\s");
					
				//populate the list of vote options
				for(String voteOption : splitInputLine) {
					votingOptions.add(voteOption);
				}
				//removes "VOTE_OPTIONS" from the list
				votingOptions.remove(0);
			}
		//return votingOptions;
		
		} catch (IOException e) {
			System.out.println("IO Exception due to: " + e.getMessage());
		
		}
	}
	
	//method for participant to decide vote - every time it's called it resets the choice of the vote
	public void decideVote() {
		Random randNum = new Random();
		vote = votingOptions.get(randNum.nextInt(votingOptions.size()));
	}
	
	//method that sends out voting choice to all other participants
	public void broadcastVote() {
		
	}
	
	public static void main(String args[]) throws UnknownHostException, IOException {
		//portNumber = Integer.parseInt(args[2]);
		portNumber = 40000;
		Participant currentParticipant = new Participant();
	}
	
	//might need to create each participant on a separate thread 
	public class ClientThread implements Runnable {
		public void run() {
			int var = 10;
		}
	}
	
	
}