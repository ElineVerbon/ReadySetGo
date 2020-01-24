package protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class ServerMessageGenerator {
	
	private BufferedReader in;
	private BufferedWriter out;
	
	public ServerMessageGenerator(BufferedReader givenIn, BufferedWriter givenOut) {
		in = givenIn;
		out = givenOut;
	}
	
	public void errorMessage(String message, String version) {
		String errorMessage = ProtocolMessages.ERROR + ProtocolMessages.DELIMITER + 
						version + ProtocolMessages.DELIMITER + message;
		sendMessageToClient(errorMessage);
	}
	
	public void startGameMessage() {
		
	}
	
	public void doTurnMessage() {
		
	}
	
	public void giveResultMessage() {
		
	}
	
	public void endGameMessage() {
		
	}
	
	/**
	 * Send message from game to client.
	 */
	public void sendMessageToClient(String msg) {
		try {
			out.write(msg);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
	}
	
	/**
	 * Send message to and get message from client.
	 */
	public String sendAndReceiveMessage(String msg, BufferedWriter out, BufferedReader in) {
		String reply = "";
		try {
			out.write(msg);
			out.newLine();
			out.flush();
			reply = in.readLine();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
		return reply;
	}
	
}
