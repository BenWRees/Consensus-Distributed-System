
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A scanner and parser for requests.
 */

class ReqTokenizer {
    
    /**
     * Parses requests. - change to a switch statement so it's faster
     */
    Token getToken(String req) {
    	StringTokenizer sTokenizer = new StringTokenizer(req);
    	if (!(sTokenizer.hasMoreTokens()))
    		return null;
    	String firstToken = sTokenizer.nextToken();
    	
    	//reading JOIN port    	
    	if (firstToken.equals("JOIN")) {
    		return new JoinToken(req, sTokenizer.nextToken());
    	}
    	
    	//reading Details <ports>
    	if (firstToken.equals("DETAILS")) {
    		String msg = "";
    		while (sTokenizer.hasMoreTokens()) {
    			msg += " " + sTokenizer.nextToken();
    		}
    		return new DetailsToken(req, msg);
    	}
    	
    	//reading Vote Options <options>
    	if (firstToken.equals("VOTE_OPTIONS")) {
    		String voteOptions = "";
    		while (sTokenizer.hasMoreTokens()) {
    			voteOptions += " " + sTokenizer.nextToken();
    		}
    		return new VoteOptionsToken(req, voteOptions);
    	}
    	
    	//reading Voting
    	if (firstToken.equals("VOTING")) {
    		return new VotingToken(req);
    	}
    	
    	//reading Outcome <Vote> [<ports>]
    	if (firstToken.equals("OUTCOME")) {
    		String voteOutcome = sTokenizer.nextToken();
    		String portsConsidered = "";
    		while(sTokenizer.hasMoreTokens()) {
    			portsConsidered += " " + sTokenizer.nextToken();
    		}
    		return new OutcomeToken(req, voteOutcome, portsConsidered);
    	}
    	
    	if(firstToken.contentEquals("VOTING_ROUNDS")) {
    		String message = sTokenizer.nextToken();
    		return new VotingRoundsToken(req,message);
    	}
    	
    	return null; // Ignore request..
    }
}

/** 
 * The Token Prototype.
 */
abstract class Token {
    String identifier;
}

/**
 * Syntax: JOIN &lt;name&gt;
 * Syntax: JOIN port
 */
class JoinToken extends Token {
    String name;
    
    JoinToken(String req, String name) {
    	this.identifier = req;
    	this.name = name;
    }
    
    public String getName() {
    	return name;
    }
}

/**
 * Syntax: DETAILS &lt;msg&gt;
 * Syntax: DETAILS [<ports>]
 */
class DetailsToken extends Token {
    private String ports;
    
    DetailsToken(String req, String ports) {
    	this.identifier = req;
    	this.ports = ports;
    }
    
    public String getPorts() {
    	return ports;
    }
}

/**
 *
 * Syntax: VOTE_OPTIONS [<options>]
 */
class VoteOptionsToken extends Token {
    private String options;
    
    VoteOptionsToken(String req, String options) {
    	this.identifier = req;
    	this.options = options;
    }
    
    public String getOptions() {
    	return options;
    }
}

/**
 * Syntax : VOTING_ROUNDS : msg
 * msg = will either be "BEGIN" or "END"
 * @author benjaminrees
 *
 */
class VotingRoundsToken extends Token {
	private String msg;
	
	VotingRoundsToken(String req, String msg) {
		this.identifier = req;
		this.msg = msg;
	}
	
	public String getMessage() {
		return this.msg;
	}
}

/**
 * Syntax : VOTING_ROUNDS : msg
 * msg = will either be "BEGIN" or "END"
 * @author benjaminrees
 *
 */
class VotingToken extends Token {
	
	VotingToken(String req) {
		this.identifier = req;
	}
}

/**
 * Syntax: OUTCOME VOTE_CHOICE <ports in consideration>
 */
class OutcomeToken extends Token {
	String voteChoice;
	String portsConsidered;
    OutcomeToken(String req, String voteChoice, String portsConsidered) { 
    	this.identifier = req; 
    	this.voteChoice = voteChoice;
    	this.portsConsidered = portsConsidered;
    }
    
    public String getVoteChoice() {
    	return voteChoice;
    }
    
    public String getPortsConsidered() {
    	return portsConsidered;
    }
}

/**
 * Syntax: EXIT
*/
class ExitToken extends Token {
	
	ExitToken(String req) { 
		this.identifier = req; 
	}
}

class BeginRoundToken extends Token {

}

class EndRoundToken extends Token {

}

class VotesReceivedToken extends Token {

}

class VotesSentToken extends Token {

}


