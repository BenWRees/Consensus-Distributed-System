import java.util.*;
import java.io.*;


//this code is fucking awful - spaghetti af
public class ParticipantLoggerThread extends Thread {
    private int loggerServerPort;
    private int processId;
    private int timeout;    

    private Deque<String> token = new ArrayDeque<String>();

    private Deque<Integer> coordId = new ArrayDeque<Integer>();

    private Deque<Integer> acceptedPortId = new ArrayDeque<Integer>();

    private Deque<Integer> establishedPortId = new ArrayDeque<Integer>();

    private Deque<Integer> crashedParticipant = new ArrayDeque<Integer>();

    private Deque<Integer> destinationId = new ArrayDeque<Integer>();

    private Deque<List<Integer>> participantsIds = new ArrayDeque<List<Integer>>();

    private Deque<List<String>> voteOptions = new ArrayDeque<List<String>>();   

    private Deque<List<Integer>> outcomePortsDecided = new ArrayDeque<List<Integer>>();

    private Deque<List<Integer>> outcomePortsNotified = new ArrayDeque<List<Integer>>();

    private Deque<Integer> beginRound = new ArrayDeque<Integer>();

    private Deque<Integer> endRound = new ArrayDeque<Integer>();

    private Deque<List<Vote>> votesSent = new ArrayDeque<List<Vote>>();

    private Deque<List<Vote>> votesReceived = new ArrayDeque<List<Vote>>();

    private Deque<String> outcomeVoteDecided = new ArrayDeque<String>();

    private Deque<String> outcomeVoteNotified = new ArrayDeque<String>();

    private Deque<Integer> senderId = new ArrayDeque<Integer>();

    private Deque<Integer> senderIds = new ArrayDeque<Integer>();

    private Deque<Integer> destinationIds = new ArrayDeque<Integer>();

    private Deque<String> messages = new ArrayDeque<String>(); 


    ParticipantLoggerThread(int loggerServerPort, int processId, int timeout) {
        this.loggerServerPort = loggerServerPort;
        this.processId = processId;
        this.timeout = timeout;
    }

    @Override
    public void run() {

        try {
            ParticipantLogger.initLogger(loggerServerPort, processId, timeout);
        } catch(IOException e) {
            //System.out.println("IOException caught at coordinator logger");
            e.printStackTrace();
        }

        
        while(true) {
            synchronized(this) { 
                if(!token.isEmpty()) {
                    switch (getToken()) {

                        case "MESSAGE_SENT" :
                            ParticipantLogger.getLogger().messageSent(getSenderPort(), getMessage());
                            //senderIds.removeLast();
                            //messages.removeLast();
                            //token.removeLast();
                            continue;

                        case "MESSAGE_RECEIVED" :
                            ParticipantLogger.getLogger().messageReceived(getDestinationPort(), getMessage());
                            //destinationIds.removeLast();
                            //messages.removeLast();
                            //token.removeLast();
                            continue;

                        case "JOIN" :
                            ParticipantLogger.getLogger().joinSent(getCoordId());
                            //coordId.removeLast();
                            //System.out.println(token + " LOGGING SENT");
                            //token.removeLast();
                            continue;

                        case "DETAILS" :
                            ParticipantLogger.getLogger().detailsReceived(getParticipantsIds());
                            //System.out.println(token + " LOGGING SENT");
                            //participantsIds.removeLast();
                            //token.removeLast();
                            continue;

                        case "VOTE_OPTION" :
                            ParticipantLogger.getLogger().voteOptionsReceived(getVoteOptions());
                            //System.out.println(token + " LOGGING SENT");
                            //voteOptions.removeLast();
                            //token.removeLast();
                            continue;

                        case "BEGIN_ROUND" :
                            ParticipantLogger.getLogger().beginRound(getRoundBegin());
                            //System.out.println(token + " LOGGING SENT");
                            //beginRound.removeLast();
                            //token.removeLast();
                            continue;

                        case "END_ROUND" :
                            ParticipantLogger.getLogger().endRound(getRoundEnd());
                            //System.out.println(token + " LOGGING SENT");
                            //endRound.removeLast();
                            //token.removeLast();
                            continue;
                        
                        case "VOTES_SENT" :
                            //keeps on throwing an error - get sender id is null
                            //System.out.println(getSenderId());
                            ParticipantLogger.getLogger().votesSent(getSenderId(), getVotesSent());
                            //System.out.println(token + " LOGGING SENT");
                            //senderId.removeLast();
                            //votesSent.removeLast();
                            //token.removeLast();
                            continue;

                        case "VOTES_RECEIVED" :
                            ParticipantLogger.getLogger().votesReceived(getDestinationId(), getVotesReceived());
                            //System.out.println(token + " LOGGING SENT");
                            //destinationId.removeLast();
                            //votesReceived.removeLast();
                            //token.removeLast();
                            continue;

                        case "OUTCOME_DECIDED" :
                            ParticipantLogger.getLogger().outcomeDecided(getOutcomeVoteDecided(), getOutcomePortsDecided());
                            //System.out.println(token + " LOGGING SENT");
                            //outcomePortsDecided.removeLast();
                            //outcomeVoteDecided.removeLast();
                            //token.removeLast();
                            continue;

                        case "OUTCOME_NOTIFIED" :
                            ParticipantLogger.getLogger().outcomeNotified(getOutcomeVoteNotified(), getOutcomePortsNotified());
                            //System.out.println(token + " LOGGING SENT");
                            //outcomePortsNotified.removeLast();
                            //outcomeVoteNotified.removeLast();
                            //token.removeLast();
                            continue;

                        case "PARTICIPANT_CRASHED" :
                            ParticipantLogger.getLogger().participantCrashed(getCrashedParticipant());
                            //System.out.println(token + " LOGGING SENT");
                            //crashedParticipant.removeLast();
                            //token.removeLast();
                            continue;

                        case "STARTED_LISTENING" :
                            ParticipantLogger.getLogger().startedListening();
                            //System.out.println(token + " LOGGING SENT");
                            //token.removeLast();
                            continue;

                        case "CONNECTION_ACCEPTED" :
                            ParticipantLogger.getLogger().connectionAccepted(getConnectionAcceptedId());
                            //System.out.println(token + " LOGGING SENT");
                            //acceptedPortId.removeLast();
                            //token.removeLast();
                            continue;

                        case "CONNECTION_ESTABLISHED" :
                            ParticipantLogger.getLogger().connectionEstablished(getConnectionEstablishedId());
                            //System.out.println(token + " LOGGING SENT");
                            //establishedPortId.removeLast();
                            //token.removeLast();
                            continue;
                    }              
                }
            }
        }
        
    }

    synchronized public void setToken(String newToken) {
        token.addFirst(newToken);
    }

    synchronized public String getToken() {
        return token.removeLast();
    }

    synchronized public void setCoordId(Integer newPortId) {
        coordId.addFirst(newPortId);
    }

    synchronized public Integer getCoordId() {
        return coordId.removeLast();
    }

    synchronized public void setConnectionAcceptedId(Integer newPortId) {
        acceptedPortId.addFirst(newPortId);
    }

    synchronized public Integer getConnectionAcceptedId() {
        return acceptedPortId.removeLast();
    }

    synchronized public void setConnectionEstablishedId(Integer newPortId) {
        establishedPortId.addFirst(newPortId);
    }

    synchronized public Integer getConnectionEstablishedId() {
        return establishedPortId.removeLast();
    }

    synchronized public Integer getRoundBegin() {
        return beginRound.removeLast();
    }

    synchronized public void setRoundBegin(Integer newRound) {
        beginRound.addFirst(newRound);
    }

    synchronized public Integer getRoundEnd() {
        return endRound.removeLast();
    }

    synchronized public void setRoundEnd(Integer newRound) {
        endRound.addFirst(newRound);
    }    

    synchronized public void setVotesReceived(List<Vote> newVotes) {
        votesReceived.addFirst(newVotes);
    }

    synchronized public List<Vote> getVotesReceived()  {
        return votesReceived.removeLast();
    }

    synchronized public void setVotesSent(List<Vote> newVotes) {
        votesSent.addFirst(newVotes);
    }

    synchronized public List<Vote> getVotesSent()  {
        return votesSent.removeLast();
    }

    synchronized public void setDestinationId(Integer newId) {
        destinationId.addFirst(newId);
    } 

    synchronized public Integer getDestinationId() {
        return destinationId.removeLast();
    }


    synchronized public void setSenderId(Integer newId) {
        senderId.addFirst(newId);
    } 

    synchronized public Integer getSenderId() {
        return senderId.removeLast();
    }

    synchronized public void setCrashedParticipant(Integer newCrashedParticipant) {
        crashedParticipant.addFirst(newCrashedParticipant);
    }

    synchronized public Integer getCrashedParticipant() {
        return crashedParticipant.removeLast();
    }

    synchronized public void setParticipantsIds(List<Integer> newCrashedParticipant) {
        participantsIds.addFirst(newCrashedParticipant);
    }

    synchronized public List<Integer> getParticipantsIds() {
        return participantsIds.removeLast();
    }

    synchronized public void setOutcomePortsDecided(List<Integer> newOutcomePortDecided) {
        outcomePortsDecided.addFirst(newOutcomePortDecided);
    }

    synchronized public List<Integer> getOutcomePortsDecided() {
        return outcomePortsDecided.removeLast();
    }

    synchronized public void setOutcomePortsNotified(List<Integer> newOutcomePortNotified) {
        outcomePortsNotified.addFirst(newOutcomePortNotified);
    }

    synchronized public List<Integer> getOutcomePortsNotified() {
        return outcomePortsNotified.removeLast();
    }

    synchronized public void setVoteOptions(List<String> newVotes) {
        voteOptions.addFirst(newVotes);
    }

    synchronized public List<String> getVoteOptions() {
        return voteOptions.removeLast();
    }  

    synchronized public void setOutcomeVoteDecided(String newVote) {
        outcomeVoteDecided.addFirst(newVote);
    }

    synchronized public String getOutcomeVoteDecided() {
        return outcomeVoteDecided.removeLast();
    } 

    synchronized public void setOutcomeVoteNotified(String newVote) {
        outcomeVoteNotified.addFirst(newVote);
    }

    synchronized public String getOutcomeVoteNotified() {
        return outcomeVoteNotified.removeLast();
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

    synchronized public void joinSent(int coordinatorId) {
        //ParticipantLogger.getLogger().joinSent(coordinatorId);
        setToken("JOIN");
        setCoordId(coordinatorId);
        //System.out.println(token + " LOGGING");
    }

    synchronized public void detailsReceived(List<Integer> participantIds) {
        //ParticipantLogger.getLogger().detailsReceived(participantIds);
        setToken("DETAILS");
        setParticipantsIds(participantIds);
        //System.out.println(token + " LOGGING");

    }

    synchronized public void voteOptionsReceived(List<String> votingOptions) {
        //ParticipantLogger.getLogger().voteOptionsReceived(votingOptions);
        setToken("VOTE_OPTION");
        setVoteOptions(votingOptions);
        //System.out.println(token + " LOGGING");
    }

    synchronized public void beginRound(int round) {
        //ParticipantLogger.getLogger().beginRound(round);
        setToken("BEGIN_ROUND");
        //System.out.println(token + " LOGGING");
        setRoundBegin(round);
    }

    synchronized public void endRound(int round) {
       //ParticipantLogger.getLogger().endRound(round);
        setToken("END_ROUND");
        setRoundEnd(round);
    }

    synchronized public void votesSent(int destinationParticipantId, List<Vote> votes) {
        //ParticipantLogger.getLogger().votesSent(destinationParticipantId, votes);
        setToken("VOTES_SENT");
        setVotesSent(votes);
        setSenderId(destinationParticipantId); 
        //System.out.println(token + " LOGGING");

    }

    synchronized public void votesReceived(int senderParticipantId, List<Vote> votes) {
        //ParticipantLogger.getLogger().votesReceived(senderParticipantId, votes);
        setToken("VOTES_RECEIVED");
        setVotesReceived(votes);
        setDestinationId(senderParticipantId);
        //System.out.println(token + " LOGGING");
    }

    synchronized public void outcomeDecided(String vote, List<Integer> participantIds) {
        //ParticipantLogger.getLogger().outcomeDecided(vote, participantIds);
        setToken("OUTCOME_DECIDED");
        setOutcomePortsDecided(participantIds);
        //System.out.println(token + " LOGGING");
        setOutcomeVoteDecided(vote);
    }

    synchronized public void outcomeNotified(String vote, List<Integer> participantIds) {
        //ParticipantLogger.getLogger().outcomeNotified(vote, participantIds);
        setToken("OUTCOME_NOTIFIED");
        setOutcomePortsNotified(participantIds);
        //System.out.println(token + " LOGGING");
        setOutcomeVoteNotified(vote);
    }

    synchronized public void participantCrashed(int crashedParticipantId) {
        //ParticipantLogger.getLogger().participantCrashed(crashedParticipantId);
        setToken("PARTICIPANT_CRASHED");
        //System.out.println(token + " LOGGING");
        setCrashedParticipant(crashedParticipantId);
    }

    synchronized public void startedListening() {
        //ParticipantLogger.getLogger().startedListening();
        setToken("STARTED_LISTENING");
        //System.out.println(token + " LOGGING");
    }

    synchronized public void connectionAccepted(int otherPort) {
        //ParticipantLogger.getLogger().connectionAccepted(otherPort);
        setToken("CONNECTION_ACCEPTED");
        //System.out.println(token + " LOGGING");
        setConnectionAcceptedId(otherPort);
    }

    synchronized public void connectionEstablished(int otherPort) {
        //ParticipantLogger.getLogger().connectionEstablished(otherPort);
        setToken("CONNECTION_ESTABLISHED");
        //System.out.println(token + " LOGGING");
        setConnectionEstablishedId(otherPort);
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