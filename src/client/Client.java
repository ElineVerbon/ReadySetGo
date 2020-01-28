package client;

import java.util.List;

public interface Client {
	
	/**
	 * Connects to the server, checks the protocol via a handshake and starts the game.
	 */
	public void start();
	
	/**
	 * Do handshake, implementation depends on the client type.
	 */
	public abstract void doHandshake();
	
	/**
	 * Handles messages received from the server.
	 * 
	 * Checks whether all components are in the message and sends it to the correct method.
	 * 
	 * @param message, a String received from the server.
	 */
	public void handleServerMessage(String message);
	
	/**
	 * Method to handle the start message.
	 * Start message should be formatted as follows:
	 * ProtocolMessages.GAME + ProtocolMessages.DELIMITER + board + ProtocolMessages.DELIMITER + 
	 * 			ProtocolMessages.BLACK / ProtocolMessages.WHITE
	 * 
	 * Separates the messages and sets the color and dimensions of the board.
	 * Prints a welcome message to the client and starts the GUI.
	 */
	
	public void startGame(String board, String assignedColor);
	
	/** 
	 * Send a valid move to the server.
	 * 
	 * Show the current board state in the GUI and ask for a move. 
	 * Send appropriate message to the server.
	 */
	public void doMove(String board, String opponentsMove);
	
	/**
	 * Get a valid move, should be implemented in all subclasses.
	 * 
	 * @return
	 */
	public abstract String getMove(String theOpponentsMove, int theBoardDimension, 
										String theBoard, char theColor, List<String> thePrevBoards);
	
	/**
	 * 
	 * @param validity, boolean that is true if the move is valid, otherwise false
	 * @param boardOrMessage, a String containing the board (if valid move) or 
	 * 				(if invalid move) null or a message
	 * @param theDoublePass, boolean indicating whether there were were two subsequent passes
	 */
	public void getResult(String validity, String boardOrMessage, boolean theDoublePass);
	
	/**
	 * Get an end game message from the server.
	 * Communicate the result to the player.
	 * 
	 * Called upon receiving a turn message from the server, formatted like this.
	 * ProtocolMessages.END + ProtocolMessages.DELIMITER + reasonEnd + ProtocolMessages.DELIMITER 
	 *  + winner + ProtocolMessages.DELIMITER + scoreBlack + ProtocolMessages.DELIMITER + scoreWhite
	 * 
	 * @param components, an array of strings extracted from the server's message
	 */
	public void endGame(String reasonEnd, String winner, String scoreBlack, String scoreWhite);
	
	/**
	 * Updates the GUI to show the current board state.
	 * 
	 * @param theBoard, a String representation of the board
	 */
	public void showCurrentBoardState(String theBoard);
}
