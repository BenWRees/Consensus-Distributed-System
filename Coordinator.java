import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;


public class Coordinator
{
    // Max # of clients. 
    int _MAXCLIENTS;
    // number of clients currently registered. 
    private int _numOfClients = 0;
    private String votingOptions;
    
    /** 
     * Maps name to socket. Key is clientName, value is clientOut
     */
    private Map<String, PrintWriter > clientPorts = new HashMap<String, PrintWriter>();
    
    //inverse of clientPorts
    private Map<PrintWriter,String> clientPortsInverse = new HashMap<PrintWriter, String>();
    
    /**
     * Maps a participant to its outcome. Key is clientName, value is outcome
     */
    private Map<String, String> outcomes = new HashMap<String,String>();
    private Map<String, ArrayList<String>> portsConsidered = new HashMap<String, ArrayList<String>>();
    
    /**
     * For each client we create a thread that handles
     * all i/o with that client.
     */
    private class ServerThread extends Thread {
		private Socket clientSocket;
		private String clientPort;
		private BufferedReader clientInput;
		private PrintWriter clientOutput;
		
		ServerThread(Socket client) throws IOException {
			clientSocket = client;  
		    // Open I/O steams
		    clientInput = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
		    clientOutput = new PrintWriter( new OutputStreamWriter( client.getOutputStream() ) );
		    // Welcome message.
		}
		
		//need to rewrite this
		public void run() {
			System.out.println("Thread Running");
		    try {
		    	System.out.println("debug line 1");
		    	Token token = null;
		    	ReqTokenizer reqTokenizer = new ReqTokenizer();
			
		    	// First, the client must register. If it doesn't send  join, close it
		    	String lineRead = clientInput.readLine();
		    	System.out.println(lineRead);
		    	token = reqTokenizer.getToken(lineRead);
		    	
		    	if (!(token instanceof JoinToken)) {
		    		clientSocket.close();
		    		return;
		    	}
			
			
		    	// If this succeeds, process requests until client exits.
		    	token = reqTokenizer.getToken(lineRead);
		    	while (!(token instanceof OutcomeToken)) {
				
		    			//if the server is being asked by the client to join then send out the details and the Vote Options
		    		//if(token instanceof JoinToken) {
		    			System.out.println("Found Join message");
						if(!(register(((JoinToken) token).getName(), clientOutput))) {
							clientSocket.close();
							return;
						}
						System.out.println("Got Join message: " + ((JoinToken) token).getName());
						register(clientPort = ((JoinToken) token).getName(), clientOutput);
						System.out.println("Number of participants connected: " + _numOfClients);
						if(_numOfClients == _MAXCLIENTS) {
							detailsMessage();
							voteOptionsMessage();	
						}
	
		    		//check for next token from the client
		    		token = reqTokenizer.getToken(clientInput.readLine());
		    	}
		    
		    	//if Outcome is sent to the coordinator
		    	if(token instanceof OutcomeToken) {		    		
		    		outcomeMessage(clientPort, ((OutcomeToken) token).getVoteChoice(), ((OutcomeToken) token).getPortsConsidered());
		    		
		    		for(String port : outcomes.keySet()) {
		    			System.out.println("Outcome " + outcomes.get(port) + " from " + port + " with ports considered: " +
		    					((OutcomeToken) token).getPortsConsidered());
		    		}
		    	
		    		clientSocket.close();
		    	}
		    	unregister(clientPort);
		    } catch (IOException e) {
		    	System.out.println("Caught I/O Exception  due to: " + e.getMessage());
		    	unregister(clientPort);
		    } catch (NullPointerException e) {
		    	unregister(clientPort);
		    	System.out.println("Null pointer by: " + clientPort + " due to: " + e.getMessage());
		    }
		}
  }


    /**
     * Attempts to register the client under the specified name with a particular output stream (so we can send individual messages to it)
     * @returns true if successful.
     */
    public boolean register(String name, PrintWriter out) {  
    	
    	//fail if too many clients connected
    	if (_numOfClients >= _MAXCLIENTS) {
    		System.out.println("Unexpected number of participants");
    		return false;
    	}
    	
    	//don't allow for multiples ports of the same port 
    	if (clientPorts.containsKey(name)) {
    		System.out.println("ChatServer: Port already joined.");
    		return false;
    	}
    	
    	try {
    		clientPorts.put(name,out);
    		clientPortsInverse.put(out, name);
    	} catch (NullPointerException e) {
    		System.out.println("Null Pointer exception thrown due to: " + e.getMessage());
    		return false;
    	}
    	_numOfClients++;
    	return true;
    }

    /**
     * Unregisters the client with the specified name.
     */
    public void unregister(String name) {
    	clientPorts.remove(name);
		_numOfClients--;
    }

    
    /** 
     * Send the details message to all clients - need to work out how to remove a port
     * HOW TO REMOVE PORT
     */
    synchronized public void detailsMessage() {
    	String detailsMessage = "DETAILS ";
    	
    	
    	//add every port to details
    	Iterator<String> clientPortIt = clientPorts.keySet().iterator();
   		while(clientPortIt.hasNext()) {
   			String clientPort = clientPortIt.next();
   			detailsMessage = detailsMessage + clientPort + " ";
   		}
   		System.out.println("line sent: " + detailsMessage);			
    	//print to each individual client and remove their name 
    	Iterator<PrintWriter> clientOutputIt = clientPorts.values().iterator();
    	while (clientOutputIt.hasNext()) {
   			//output stream for particular client
   			PrintWriter pw = clientOutputIt.next();
   			
   			//need to initialise Port with p
   			String portToRemove = clientPortsInverse.get(pw);
   			
   			detailsMessage = detailsMessage.replaceAll(portToRemove, "");
   			
   			pw.println(detailsMessage);
   			pw.flush();
   			
   			detailsMessage = detailsMessage + portToRemove;
   		}
    	System.out.println("sent details");
    }

    /**
     * Send the vote Options to every client
     */
    synchronized public void voteOptionsMessage() {
    	String voteOptions = "VOTE_OPTIONS  " + votingOptions;
    	System.out.println("line sent: " + voteOptions);
    	
    	Iterator<PrintWriter> clientOutputIt = clientPorts.values().iterator();
    	while (clientOutputIt.hasNext()) {
    		//output stream for particular client
    		PrintWriter pw = clientOutputIt.next();
    		pw.println(voteOptions);
    		pw.flush();
    	}
    	System.out.println("sent voting options");
    }
    
    /*
     * receive the outcome message
     * receives 
     */
    public void outcomeMessage(String participant, String voteOutcome, String ports) {
    	outcomes.put(participant, voteOutcome);
    	ArrayList<String> portsArr = new ArrayList<String>();
    	
    	for(String port : ports.split("\\s")) {
    		portsArr.add(port);
    	}
    	
    	portsConsidered.put(participant, portsArr);
    	
    }

    /**
     * Wait for a connection request. Sets up the server
     */
    public void startListening(Integer coordinatorPortNumber, Integer loggerPortNumber, Integer numberOfClients, String voteOptions, Integer timeOut) throws IOException {
    	ServerSocket listener = new ServerSocket(coordinatorPortNumber);
    	System.out.println("SERVER ONLINE AT PORT " + listener.getLocalPort());
    	_MAXCLIENTS = numberOfClients;
    	votingOptions = voteOptions;
	
    	while (true) {
    		Socket client = listener.accept();
    		System.out.println("socket accepted");
    		System.out.println("sock port number is " + client.getPort());
    		new ServerThread(client).start();
    	}
    }

    public static void main(String[] args) throws IOException {
    	if (args.length != 5) {
    		System.out.println("Error: Not enough arguments");
    		return;
    	}
    	Integer coordinatorPortNumber = Integer.parseInt(args[0]); 
    	Integer loggerPortNumber = Integer.parseInt(args[1]);
    	Integer numberOfClients = Integer.parseInt(args[2]);
    	String voteOptions = args[3];
    	Integer timeOut = Integer.parseInt(args[4]);
    	new Coordinator().startListening(coordinatorPortNumber, loggerPortNumber, numberOfClients, voteOptions, timeOut);
    }
} 








