
/*
 * 1. wait for number of participants specified to join - number is given as a parameter to the main function
 * 2. Send participant details to each participant once all participants have joined as "DETAILS [<Port>]"
 * 3. Send Requests for votes to all participants 
 * 4. Receive votes from participants 
 */

import java.io.*;
import java.net.*;
/*
 * java Coordinator ⟨port⟩ ⟨lport⟩ ⟨parts⟩ ⟨timeout⟩ [⟨option⟩]
 * - ⟨port⟩ is the port number that the coordinator is listening on
 * - ⟨lport⟩ is the port number that the logger server is listening on
 * - ⟨parts⟩ is the number of participants that the coordinator expects
 * - [⟨option⟩] is a set (no duplicates) of options separated by spaces. 
 * - (timeout) should be used by the coordinator when waiting for a message from a participant, in order to decide whether 
 * 	 that participant has failed. For example, if we want to start the coordinator listening on port 12345, expecting the 
 * 	 logger server listening on port 12344, expecting 4 participants, with a timeout of 500 milliseconds and where the 
 * 	 options are A, B and C, then it will be executed as:  
 * 
 */
public class Coordinator {
	private ServerSocket coordinatorSocket;
	private Integer port;
	
	
	Coordinator() {
		
	}
	//
	public void connect() throws IOException {
		coordinatorSocket = new ServerSocket(port);
		
		while(true) {
			
		}
	}
	
	/*
	 * As we will be using separate threads for each request, lets understand the working and implementation of the ClientHandler class 
	 * extending Threads. An object of this class will be instantiated each time a request comes.
	 * 1) this class extends Thread so that its objects assumes all properties of Threads.
	 * 2) the constructor of this class takes three parameters, which can uniquely identify any incoming request, 
	 * 	  i.e. a Socket, a DataInputStream to read from and a DataOutputStream to write to. Whenever we receive any request of client, 
	 * 	  the server extracts its port number, the DataInputStream object and DataOutputStream object and creates a new thread object of 
	 * 	  this class and invokes start() method on it.
	 * Note : Every request will always have a triplet of socket, input stream and output stream. This ensures that each object of this class 
	 * writes on one specific stream rather than on multiple streams.
	 * 3) Inside the run() method of this class, it performs three operations: request the user to specify whether time or date needed, 
	 * 	  read the answer from input stream object and accordingly write the output on the output stream object.
	 */
	class ClientHandler extends Thread {
		/*
		 * performs three operations:
		 * 1) requests the user to specify whether time or date needed 
		 * 2) reads the answer from the input stream object
		 * 3) writes the output on the output stream object
		 */
		@Override
		public void run() {
			
		}
	}
}