import java.util.*;
import java.io.*;

public class CoordinatorLoggerThread extends Thread {
    private int loggerServerPort;
    private int processId;
    private int timeout;   

    private Deque<String> tokens = new ArrayDeque<String>();

    private Deque<Integer> participantsListening = new ArrayDeque<Integer>();

    private Deque<Integer> joinMessages = new ArrayDeque<Integer>();

    private Deque<Integer> destinationPortsDetails = new ArrayDeque<Integer>();

    private Deque<List<Integer>> detailsMessages = new ArrayDeque<List<Integer>>(); 

    private Deque<Integer> destinationPortsVote = new ArrayDeque<Integer>(); 

    private Deque<List<String>> voteMessages = new ArrayDeque<List<String>>(); 

    private Deque<Integer> outcomeParticipants = new ArrayDeque<Integer>();

    private Deque<String> outcomeVotes = new ArrayDeque<String>();  

    private Deque<Integer> connectionAcceptedPorts = new ArrayDeque<Integer>();

    private Deque<Integer> crashedParticipants = new ArrayDeque<Integer>();

    private Deque<Integer> senderIds = new ArrayDeque<Integer>();

    private Deque<Integer> destinationIds = new ArrayDeque<Integer>();

    private Deque<String> messages = new ArrayDeque<String>();    

    CoordinatorLoggerThread(int loggerServerPort, int processId, int timeout) {
        this.loggerServerPort = loggerServerPort;
        this.processId = processId;
        this.timeout = timeout;
        
    }

    @Override
    public void run() {
        
        try {
            CoordinatorLogger.initLogger(loggerServerPort, processId, timeout);
        } catch(IOException e) {
            System.out.println("IOException caught at coordinator logger");
            e.printStackTrace();
        }
        
        while(true) {
            synchronized(this) {
                if(!tokens.isEmpty()) {
                    switch(getToken()) {

                        case "LISTENING" :
                            CoordinatorLogger.getLogger().startedListening(getListeningPort());
                            //participantsListening.remove();
                            //tokens.remove();
                            continue;
                        
                        case "JOIN" :
                            CoordinatorLogger.getLogger().joinReceived(getJoinPort());
                            //joinMessages.remove();
                            //tokens.remove();
                            continue;

                        case "DETAILS" :
                            CoordinatorLogger.getLogger().detailsSent(getDestinationPortDetails(), getDetailsMessage());
                            //destinationPortsDetails.remove();
                            //detailsMessages.remove();
                            //tokens.remove();
                            continue;

                        case "VOTES" :
                            CoordinatorLogger.getLogger().voteOptionsSent(getDestinationPortVote(), getVoteMessage());
                            //destinationPortsVote.remove();    
                            //voteMessages.remove();
                            //tokens.remove();
                            continue;

                        case "OUTCOME" :
                            CoordinatorLogger.getLogger().outcomeReceived(getOutcomeParticipant(), getOutcomeVote());
                            //outcomeParticipants.remove();
                            //outcomeVotes.remove();
                            //tokens.remove();
                            continue;

                        case "ACCEPTED" :
                            CoordinatorLogger.getLogger().connectionAccepted(getConnectionAcceptedPort());
                            //connectionAcceptedPorts.remove();    
                            //tokens.remove();
                            continue;

                        case "CRASHED" :
                            CoordinatorLogger.getLogger().participantCrashed(getCrashedParticipant());
                            //crashedParticipants.remove();
                            //tokens.remove();
                            continue;

                        case "MESSAGE_SENT" :
                            CoordinatorLogger.getLogger().messageSent(getSenderPort(), getMessage());
                            //senderIds.remove();
                            //messages.remove();
                            //tokens.remove();
                            continue;

                        case "MESSAGE_RECEIVED" :
                            CoordinatorLogger.getLogger().messageReceived(getDestinationPort(), getMessage());
                            //destinationIds.remove();
                            //messages.remove();
                            //tokens.remove();
                            continue;
                    }
                }
                
            }
        }
        
    }

    synchronized public void setToken(String newToken) {
        tokens.addFirst(newToken);
    }

    synchronized public String getToken() {
        return tokens.removeLast();
    }

    synchronized public void setListeningPort(Integer newPort) {
        participantsListening.addFirst(newPort);
    }

    synchronized public Integer getListeningPort() {
        return participantsListening.removeLast();
    }

    synchronized public void setJoinPort(Integer newPort) {
        joinMessages.addFirst(newPort);
    }

    synchronized public Integer getJoinPort() {
        return joinMessages.removeLast();
    }

    synchronized public void setDestinationPortDetails(Integer newPort) {
        destinationPortsDetails.addFirst(newPort);
    }

    synchronized public Integer getDestinationPortDetails() {
        return destinationPortsDetails.removeLast();
    }

    synchronized public void setDetailsMessage(List<Integer> newDetailsMessage) {
        detailsMessages.addFirst(newDetailsMessage);
    }

    synchronized public List<Integer> getDetailsMessage() {
        return detailsMessages.removeLast();
    }  

    synchronized public void setDestinationPortVote(Integer newPort) {
        destinationPortsVote.addFirst(newPort);
    }

    synchronized public Integer getDestinationPortVote() {
        return destinationPortsVote.removeLast();
    }   

    synchronized public void setVoteMessage(List<String> newVoteMessage) {
        voteMessages.addFirst(newVoteMessage);
    }

    synchronized public List<String> getVoteMessage() {
        return voteMessages.removeLast();
    }      

    synchronized public void setOutcomeParticipant(Integer newOutcomeParticipant) {
        outcomeParticipants.addFirst(newOutcomeParticipant);
    }

    synchronized public Integer getOutcomeParticipant() {
        return outcomeParticipants.removeLast();
    }   

    synchronized public void setOutcomeVote(String newOutcomeVote) {
        outcomeVotes.addFirst(newOutcomeVote);
    }

    synchronized public String getOutcomeVote() {
        return outcomeVotes.removeLast();
    }

    synchronized public void setConnectionAcceptedPort(Integer newPort) {
        connectionAcceptedPorts.addFirst(newPort);
    }

    synchronized public Integer getConnectionAcceptedPort() {
        return connectionAcceptedPorts.removeLast();
    } 

    synchronized public void setCrashedParticipant(Integer newPort) {
        crashedParticipants.addFirst(newPort);
    }

    synchronized public Integer getCrashedParticipant() {
        return crashedParticipants.removeLast();
    }

    synchronized public void setSenderPort(Integer senderPort) {
        senderIds.addFirst(senderPort);
    }      

    synchronized public Integer getSenderPort() {
        return senderIds.removeLast();
    }

    synchronized public void setDestinationPort(Integer destinationId) {
        destinationIds.addFirst(destinationId);
    }      

    synchronized public Integer getDestinationPort() {
        return destinationIds.removeLast();
    }

    synchronized public void setMessage(String message) {
        messages.addFirst(message);
    }

    synchronized public String getMessage() {
        return messages.removeLast();
    }

    synchronized public void startedListening(int port) {
        //CoordinatorLogger.getLogger().startedListening(port);
        setListeningPort(port);
        setToken("LISTENING");
        
    }

    synchronized public void joinReceived(int participantId) {
        //CoordinatorLogger.getLogger().joinReceived(participantId);
        setToken("JOIN"); 
        setJoinPort(participantId);
    }

    synchronized public void detailsSent(int destinationParticipantId, List<Integer> participantIds) {
        //CoordinatorLogger.getLogger().detailsSent(destinationParticipantId, participantIds);
        setDetailsMessage(participantIds);
        setToken("DETAILS");
        setDestinationPortDetails(destinationParticipantId);
    }

    synchronized public void voteOptionsSent(int destinationParticipantId, List<String> votingOptions) {
        //CoordinatorLogger.getLogger().voteOptionsSent(destinationParticipantId, votingOptions);
        setVoteMessage(votingOptions);
        setToken("VOTES");
        setDestinationPortVote(destinationParticipantId);
    }

    synchronized public void outcomeReceived(int participantId, String vote) {
        //CoordinatorLogger.getLogger().outcomeReceived(participantId, vote);
        setToken("OUTCOME"); 
        setOutcomeParticipant(participantId);
        setOutcomeVote(vote);
    }

    synchronized public void connectionAccepted(int otherPort) {
        //CoordinatorLogger.getLogger().connectionAccepted(otherPort);
        setToken("ACCEPTED");
        setConnectionAcceptedPort(otherPort);
    }

    synchronized public void participantCrashed(int crashedParticipantId) {
        //CoordinatorLogger.getLogger().participantCrashed(crashedParticipantId);
        setToken("CRASHED");
        setCrashedParticipant(crashedParticipantId);
    }

    synchronized void messageSent(int senderPort, String message) {
        setSenderPort(senderPort);
        setMessage(message);
        setToken("MESSAGE_SENT");
    }

    synchronized void messageReceived(int destinationId, String message) {
        setDestinationPort(destinationId);
        setMessage(message);
        setToken("MESSAGE_RECEIVED");
    }

}