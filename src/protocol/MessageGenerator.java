package protocol;

public class MessageGenerator implements GeneratorInterface {
	
	/**
	 * Message sent by both client and server.
	 */
	
	public String errorMessage(String message, String version) {
		String errorMessage = ProtocolMessages.ERROR + ProtocolMessages.DELIMITER + 
						version + ProtocolMessages.DELIMITER + message;
		return errorMessage;
	}
	
	/**
	 * Message sent by the client only.
	 */
	
	public String clientHandshakeMessage(String wantedVersion, String nameClient, 
																		char wantedColor) {
		String handshakeToServer = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER + 
				wantedVersion + ProtocolMessages.DELIMITER + nameClient + 
				ProtocolMessages.DELIMITER + wantedColor;
		return handshakeToServer;
	}
	
	public String moveMessage(String move) {
		
		String moveMessage = ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + move;
		return moveMessage;
	}
	
	/**
	 * Messages sent by the server only.
	 */
	
	public String startGameMessage(String board, char color) {
		String startMessage = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + color;
		return startMessage;
	}
	
	public String startGameMessagePart1()  {
		//Check whether player1 has disconnected by sending the start message in two parts (if
		//disconnected, the second flush will give an IO exception)
		String startMessage1part1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER;
		return startMessage1part1;
	}
	
	public String startGameMessagePart2(String board, char color)  {
		//Check whether player1 has disconnected by sending the start message in two parts (if
		//disconnected, the second flush will give an IO exception)
		String startMessage1part2 = board + ProtocolMessages.DELIMITER + color;
		return startMessage1part2;
	}
	
	public String doTurnMessage(String board, String opponentsMove) {
		String turnMessage = ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board + 
				ProtocolMessages.DELIMITER + opponentsMove;
		return turnMessage;
	}
	
	public String resultMessage(boolean valid, String msg) {
		String resultMessage = "";
		
		if (valid) {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ ProtocolMessages.VALID + ProtocolMessages.DELIMITER + msg;
		} else {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ ProtocolMessages.INVALID + ProtocolMessages.DELIMITER + msg;
		}
		return resultMessage;
	}
	
	public String endGameMessage(char reasonGameEnd, char winner, 
									String scoreBlack, String scoreWhite) {
		String endOfGameMessage = ProtocolMessages.END + ProtocolMessages.DELIMITER + reasonGameEnd
				+ ProtocolMessages.DELIMITER + winner + ProtocolMessages.DELIMITER + 
				scoreBlack + ProtocolMessages.DELIMITER + 
				scoreWhite;
		return endOfGameMessage;
		
	}
}
