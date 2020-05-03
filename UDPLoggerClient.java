import java.io.*;
import java.net.*;
import java.util.*;

public class UDPLoggerClient {
	
	private final int loggerServerPort;
	private final int processId;
	private final int timeout;
	private int fails = 0;
	/**
	 * @param loggerServerPort the UDP port where the Logger process is listening o
	 * @param processId the ID of the Participant/Coordinator, i.e. the TCP port where the Participant/Coordinator is listening on
	 * @param timeout the timeout in milliseconds for this process 
	 */
	//this requires UDPLoggerServer to be up first - need to change that
	public UDPLoggerClient(int loggerServerPort, int processId, int timeout) {
		this.loggerServerPort = loggerServerPort;
		this.processId = processId;
		this.timeout = timeout;
	} 
	
	public int getLoggerServerPort() {
		return loggerServerPort;
	}

	public int getProcessId() {
		return processId;
	}
	
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sends a log message to the Logger process and waits for a Acknowledgement message 
	 * If it doesn't receive an Ack message in the space of timeout, send up to two more
	 * time. If, after three attempts, Ack is not received - assume server crashed and stop
	 * sending
	 * @param message the log message
	 * @throws IOException once we've been forced to se
	 */
	
	/*
	 * socket.receive(packetReceived) keeps on hanging 
	 */
	public void logToServer(String message) throws IOException {
		if(fails >= 3) {
			//System.out.println("UDPLOGGERSERVER HAS CRASHED");
			return;
		}

		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(getTimeout());

			String messageToSend = getProcessId() + " " + System.currentTimeMillis() + " " + message;
			InetAddress local = InetAddress.getLocalHost();
			
			DatagramPacket packetSent = new DatagramPacket(messageToSend.getBytes(), messageToSend.length(),local, getLoggerServerPort());
			socket.send(packetSent);
			//System.out.println("message sent");
			long timeSentStart = System.currentTimeMillis();

			DatagramPacket packetReceived = new DatagramPacket("ACK".getBytes(), "ACK".length());
			socket.receive(packetReceived);
			String ackMessage = new String(packetReceived.getData());
			//System.out.println("RECEIVED: " + ackMessage);
			
		} catch(SocketTimeoutException e) {
			//System.out.println("Failed to receive ack, number of fails: " + fails);
			fails++;
			logToServer(message);
		}
	}

}

