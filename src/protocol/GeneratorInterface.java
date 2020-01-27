package protocol;

public interface GeneratorInterface {
		
	/**
	 * Message sent by both client and server.
	 */
	
	public String errorMessage(String message, String version);
	
	/**
	 * Message sent by the client only.
	 */
	
	public String moveMessage(String move);
	
	/**
	 * Messages sent by the server only.
	 */
	public String startGameMessage(String board, char color);
	
	public String startGameMessagePart1();
	
	public String startGameMessagePart2(String board, char color);
	
	public String doTurnMessage(String board, String opponentsMove);
	
	public String resultMessage(boolean valid, String msg);
	
	public String endGameMessage(char reasonGameEnd, char winner, 
									String scoreBlack, String scoreWhite);
}
