import java.net.*;
import java.io.*;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Coordinator
{
    // Max # of clients. 
    int _MAXCLIENTS;
    // number of clients currently registered. 
    private int _numOfClients = 0;
    private String votingOptions;
    private ArrayList<String> votingOptionsArr = new ArrayList<String>();
    
    /** 
     * Maps name to socket. Key is clientName, value is clientOut - can't change string for integer because it breaks it (lol)
     */
    private Map<String, PrintWriter > clientPorts = new HashMap<String, PrintWriter>();
    
    //inverse of clientPorts - can't change string for integer as breaks it
    private Map<PrintWriter,String> clientPortsInverse = new HashMap<PrintWriter, String>();
    
    private ArrayList<Integer> portsInDetails = new ArrayList<Integer>();
    /**
     * Maps a participant to its outcome. Key is clientName, value is outcome
     */
    private Map<String, String> outcomes = new HashMap<String,String>();
    private Map<String, ArrayList<String>> portsConsidered = new HashMap<String, ArrayList<String>>();
    
    private CoordinatorLoggerThread logger = null;

    //private Integer coordinatorPortNumberLog;
    //private Network network;
    
    /**
     * For each client we create a thread that handles
     * all i/o with that client.
     */
    private class ClientInterface extends Thread {
		private Socket clientSocket;
		private String clientPort = "";
		private BufferedReader clientInput;
		private PrintWriter clientOutput;
        private Integer timeout;
		
		ClientInterface(Socket client, Integer timeout) throws IOException {
			clientSocket = client;
            this.timeout = timeout;
		    // Open I/O steams
		    clientInput = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
		    clientOutput = new PrintWriter( new OutputStreamWriter( client.getOutputStream() ) );
		    // Welcome message.
		}
		
		//need to rewrite this
		public void run() {
			System.out.println("Thread Running");
		    try {
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
				    	if(token instanceof JoinToken) {
                            clientPort += ((JoinToken) token).getName();
				    		boolean registerState = register(clientPort, clientOutput);
                            if(!registerState) {
                                 clientSocket.close();
                                 return;
                            }
				    		
                            logger.joinReceived(Integer.parseInt(((JoinToken) token).getName()));
				    	}

                        System.out.println("NUMBER OF CLIENTS IN THE SERVER " + _numOfClients);
						
						
						if(_numOfClients == _MAXCLIENTS) {
							detailsMessage();
							voteOptionsMessage();
							tellParticipantsToStartVoting();
							//this.interrupt();
						}
	
		    		//check for next token from the client
		    		token = reqTokenizer.getToken(clientInput.readLine());
		    	}
		    
		    	//if Outcome is sent to the coordinator
		    	if(token instanceof OutcomeToken) {		
		    		outcomeMessage(clientPort, ((OutcomeToken) token).getVoteChoice(), ((OutcomeToken) token).getPortsConsidered());
		    		clientSocket.close();

                    //System.exit(0);

		    	}
		    	unregister(clientPort);
		    } catch (IOException e) {
                
                logger.participantCrashed(Integer.parseInt(clientPort));
		    	unregister(clientPort);
		    } catch (NullPointerException e) {
                //throws a number format exception if it's between connection and join message
                
                logger.participantCrashed(Integer.parseInt(clientPort));
                unregister(clientPort);
		    } catch (Exception e) {
                
                logger.participantCrashed(Integer.parseInt(clientPort));
		    	unregister(clientPort);
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
    		portsInDetails.add(Integer.parseInt(name));
    		
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
		//_numOfClients--;
    }

    
    /** 
     * Send the details message to all clients - need to work out how to remove a port
     * HOW TO REMOVE PORT
     */
    synchronized public void detailsMessage() {
    	String detailsMessage = "";
    	
    	
    	//add every port to details
    	Iterator<String> clientPortIt = clientPorts.keySet().iterator();
   		while(clientPortIt.hasNext()) {
   			String clientPort = clientPortIt.next();
   			detailsMessage += clientPort + " ";
   		}		
    	//print to each individual client and remove their name 
    	Iterator<PrintWriter> clientOutputIt = clientPorts.values().iterator();
    	while (clientOutputIt.hasNext()) {
   			//output stream for particular client
   			PrintWriter pw = clientOutputIt.next();
   			
   			//need to initialise Port with p
   			String portToRemove = clientPortsInverse.get(pw);
   			
   			detailsMessage = detailsMessage.replaceAll(portToRemove, "");
   			
   			System.out.println("line sent: DETAILS" + detailsMessage);	
   			pw.println("DETAILS " + detailsMessage);
   			
   			logger.detailsSent(Integer.parseInt(clientPortsInverse.get(pw)), remove(Integer.parseInt(clientPortsInverse.get(pw)),portsInDetails));
            pw.flush();
   			
   			detailsMessage = detailsMessage + portToRemove + " ";

   		}
    	System.out.println("sent details");
    }
    
    public ArrayList<Integer> remove(Integer valueToRemove, ArrayList<Integer> removeFrom) {
    	ArrayList<Integer> array = new ArrayList<Integer>();
    	array.addAll(removeFrom);
    	array.remove(valueToRemove);
    	
    	return array;
    }
    
    /**
     * Send the vote Options to every client
     * @throws IOException 
     */
    synchronized public void voteOptionsMessage() throws IOException {
    	String voteOptions = "VOTE_OPTIONS  " + votingOptions + " ";
    	System.out.println("line sent: " + voteOptions + " ");
    	
    	Iterator<PrintWriter> clientOutputIt = clientPorts.values().iterator();
    	while (clientOutputIt.hasNext()) {
    		//output stream for particular client
    		PrintWriter pw = clientOutputIt.next();
    		pw.println(voteOptions);
    		
    		logger.voteOptionsSent(Integer.parseInt(clientPortsInverse.get(pw)), votingOptionsArr);
            pw.flush();
    	}
    	System.out.println("sent voting options");
    }
    
    /**
     * Method that sends out to every participant the message "VOTING_ROUNDS BEGIN"
     * @throws Exception 
     */
	synchronized public void tellParticipantsToStartVoting() throws Exception {
    	Iterator<PrintWriter> clientOutputIt = clientPorts.values().iterator();
    	//tell the clients to start the voting rounds
    	while (clientOutputIt.hasNext()) {
    		//output stream for particular client
    		PrintWriter pw = clientOutputIt.next();
    		pw.println("VOTING_ROUNDS BEGIN");
    		pw.flush();
    	}
    	System.out.println("VOTING_ROUNDS BEGIN");
    	
    }
    
    /*
     * receive the outcome message
     * receives 
     */
    public void outcomeMessage(String participant, String voteOutcome, String ports) {
    	outcomes.put(participant, voteOutcome + " " + ports);
    	ArrayList<String> portsArr = new ArrayList<String>();
    	
    	for(String port : ports.split("\\s")) {
    		portsArr.add(port);
    	}
    	
    	portsConsidered.put(participant, portsArr);

        System.out.println("OUTCOME " + voteOutcome + ports);

    	
    	logger.outcomeReceived(Integer.parseInt(participant), voteOutcome);
    }

    /**
     * Wait for a connection request. Sets up the server
     */
    public void startListening(Integer coordinatorPortNumber, Integer loggerPortNumber, Integer numberOfClients, Integer timeOut, String voteOptions) {
    	ServerSocket listener = null;
        try {
    		listener = new ServerSocket(coordinatorPortNumber);
    		//coordinatorPortNumberLog = coordinatorPortNumber;
    		System.out.println("SERVER ONLINE AT PORT " + listener.getLocalPort());
    	} catch(IOException e) {
            //IOException thrown due to ServerSocket
        } 
        
        logger = new CoordinatorLoggerThread(loggerPortNumber, coordinatorPortNumber, timeOut);
        logger.start();
        logger.startedListening(listener.getLocalPort());
            _MAXCLIENTS = numberOfClients;
    		votingOptions = voteOptions;

    		for(int i=0; i<voteOptions.split("\\s").length; i++) {
               votingOptionsArr.add(voteOptions.split("\\s")[i]);
            }
	
    		while (true) {
                Socket client = null;
                try {
                    client = listener.accept();
                    System.out.println("socket accepted");
                    System.out.println("sock port number is " + client.getPort());
                  
                    logger.connectionAccepted(client.getPort());
                    new ClientInterface(client, timeOut).start();
                } catch(IOException e) {
                    //IOException thrown by participant crashing out
                   
                    logger.participantCrashed(client.getPort());
                    _numOfClients++;
                }
    		}
    }


    public static void main(String[] args) {
    	if (args.length != 5) {
    		System.out.println("Error: Not enough arguments");
    		return;
    	}
    	Integer coordinatorPortNumber = Integer.parseInt(args[0]); 
    	Integer loggerPortNumber = Integer.parseInt(args[1]);
    	Integer numberOfClients = Integer.parseInt(args[2]);
    	String voteOptions = "";
    	Integer timeOut = Integer.parseInt(args[3]);
        
        HashSet<String> voteOptionsNoDuplicates = new HashSet<String>();
        //removing duplicate vote options from args[4]
        for(String vote : args[4].split("\\s")) {
            voteOptionsNoDuplicates.add(vote);
        }

        for(String vote : voteOptionsNoDuplicates) {
            voteOptions += vote + " ";
        }

    	new Coordinator().startListening(coordinatorPortNumber, loggerPortNumber, numberOfClients, timeOut, voteOptions);

    }
} 








