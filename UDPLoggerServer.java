
import java.io.*;
import java.net.*;

/*
 * TO DO:
 * 	- need to add sending the ACK message to the client that sent the message
 */

public class UDPLoggerServer {
	private DatagramSocket socket = null;

	public UDPLoggerServer(Integer portNumber) {
		try { 
			File file = new File("logfile.txt");
			file.createNewFile();
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

		if(new String(packetReceive.getData()) != null) {
			sendAck(packetReceive.getPort());
		}
		//System.out.println("received line: " + new String(packetReceive.getData()));
		return new String(packetReceive.getData());
	}
	
	public void saveToFile(String message) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter("logfile.txt", true));

		out.println(message);
		out.close();
	}
	
	public void sendAck(int port) {
		try {
			DatagramSocket socket = new DatagramSocket();
			InetAddress local = InetAddress.getLocalHost();
		
			DatagramPacket packetSent = new DatagramPacket("ACK".getBytes(), "ACK".length(),local, port);
			socket.send(packetSent);
			//System.out.println("Ack Sent");
		} catch(UnknownHostException e) {
			System.out.println("UnknownHostException thrown in UDPLoggerServer.sendAck due to: ");
			e.printStackTrace();
		} catch(IOException e) {
			System.out.println("IOException thrown in UDPLoggerServer.sendAck due to: ");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new UDPLoggerServer(Integer.parseInt(args[0]));
	}
}