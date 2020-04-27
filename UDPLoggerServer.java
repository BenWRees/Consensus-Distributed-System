
import java.io.*;
import java.net.*;

/*
 * TO DO:
 * 	- need to add sending the ACK message to the client that sent the message
 */

public class UDPLoggerServer extends Thread {
	private DatagramSocket socket;
	
	public UDPLoggerServer(Integer portNumber) {
		try {
			socket = new DatagramSocket(portNumber);
			
			while(true) {
				String lineReceived = receiveLine();
				if(lineReceived != null) {
					saveToFile(lineReceived);
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String receiveLine() throws IOException {
		byte[] buf = new byte[1024];
		DatagramPacket packetReceive = new DatagramPacket(buf, buf.length);
		socket.receive(packetReceive);
		
		return new String(packetReceive.getData());
	}
	
	public void saveToFile(String message) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter("logfile.txt", true));

		out.println(message);
		out.close();
	}
	
	public void sendAck() {
		
	}
	
	public static void main(String[] args) {
		new UDPLoggerServer(Integer.parseInt(args[0]));
	}
}