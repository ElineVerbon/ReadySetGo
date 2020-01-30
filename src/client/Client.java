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
	 * @param message, a String received from the server.
	 */
	public void handleServerMessage(String message);
	
	/**
	 * Method to handle the start message.
	 * Prints a welcome message to the client and starts the GUI.
	 */
	
	public void startGame(String board, String assignedColor);
	
	/** 
	 * Send a valid move decided on by the player to the server.
	 */
	public void doMove(String board, String opponentsMove);
	
	/**
	 * Get a valid move, should be implemented in all subclasses.
	 * 
	 * @return a String, representing a move
	 */
	public abstract String getMove(String theOpponentsMove, int theBoardDimension, 
										String theBoard, char theColor, List<String> thePrevBoards);
	
	/**
	 * Shows the result of the player's last move.
	 * 
	 * @param validity, boolean that is true if the move is valid, otherwise false
	 * @param boardOrMessage, a String containing the board (if valid move) or 
	 * 				(if invalid move) null or a message
	 * @param theDoublePass, boolean indicating whether there were were two subsequent passes
	 */
	public void getResult(String validity, String boardOrMessage, boolean theDoublePass);
	
	/**
	 * Handles the end game message from the server and communicates the result to the player.
	 * 
	 * @param reasonEnd, a one-character-long String indicating why the game ended
	 * @param winner, either W or B, indicating who won
	 * @param scoreBlack, the score of B
	 * @param scoreWhite, the score of W
	 */
	public void endGame(String reasonEnd, String winner, String scoreBlack, String scoreWhite);
	
	/**
	 * Update the GUI to show the current board state.
	 * 
	 * @param theBoard, a String representation of the board
	 */
	public void showCurrentBoardState(String theBoard);
}
