package protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
	
	public void startGameMessage(String board, char color) {
		String startMessage = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + color;
		sendMessageToClient(startMessage);
	}
	
	public void startGameMessageInTwoParts(String board, char color) throws IOException {
		//Check whether player1 has disconnected by sending the start message in two parts (if
		//disconnected, the second flush will give an IO exception)
		String startMessage1part1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER;
		String startMessage1part2 = board + ProtocolMessages.DELIMITER + color;
		try {
			out.write(startMessage1part1);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Need to wait, otherwise it does not go into the exception
		try {
			TimeUnit.SECONDS.sleep(1); //TODO try with shorter time step
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		out.write(startMessage1part2);
		out.newLine();
		out.flush();
	}
	
	public String doTurnMessage(String board, String opponentsMove) {
		String turnMessage = ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board + 
				ProtocolMessages.DELIMITER + opponentsMove;
		sendMessageToClient(turnMessage);
		
		String reply = getReply();
		return reply;
	}
	
	public void giveResultMessage(String msg) {
		String resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
				+ ProtocolMessages.INVALID + ProtocolMessages.DELIMITER + msg;
		sendMessageToClient(resultMessage);
	}
	
	public void endGameMessage(char reasonGameEnd, char winner, 
									String scoreBlack, String scoreWhite) {
		String endOfGameMessage = ProtocolMessages.END + ProtocolMessages.DELIMITER + reasonGameEnd
				+ ProtocolMessages.DELIMITER + winner + ProtocolMessages.DELIMITER + 
				scoreBlack + ProtocolMessages.DELIMITER + 
				scoreWhite;
		sendMessageToClient(endOfGameMessage);
		
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
	public String getReply() {
		String reply = "";
		try {
			reply = in.readLine();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
		return reply;
	}
}
