package client;

import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.MoveValidator;

/**
 * A computer player that can play Go. It will look through the board and choose the first
 * available unoccupied spot for its move if not invalid. If not valid moves, pass.
 * @author eline.verbon
 *
 */

public class StupidNonRandomComputerPlayer extends AbstractClient {
	
	private int nextComputerPlayerNumber = 0;
	private int computerPlayerNumber;

	private MoveValidator moveValidator = new MoveValidator();
	
	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public StupidNonRandomComputerPlayer() {
		super();
		computerPlayerNumber = nextComputerPlayerNumber;
		nextComputerPlayerNumber++;
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new StupidNonRandomComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	public void doHandshake() {
		serverHandler.doHandshake("StupidNonRandomComputer" + computerPlayerNumber, 
																ProtocolMessages.BLACK);
	}
	

	/**
	 * Go from top left to bottom right of the board, looking for an unoccupied spot 
	 * that is a valid move.
	 * 
	 * @param opponentsMove
	 * @param boardDimension
	 * @param board, a String representation of the current board state
	 * @param color, the color of the player
	 * @param prevBoards, a list of all already seen previous board states
	 */
	public String getMove(String opponentsMove, int boardDimension, 
			String board, char color, List<String> prevBoards) {
		
		String move = "";
		boolean valid = false;
		
		for (int c = 0; c < board.length(); c++) {
			if (board.charAt(c) == ProtocolMessages.UNOCCUPIED) {

				move = Integer.toString(c);
			}
			
			valid = moveValidator.processMove(move, boardDimension, board, color, prevBoards);
			if (valid) {
				break;
			} 
		}
		if (!valid) {
			move = Character.toString(ProtocolMessages.PASS);
		}
		
		return move;
	}
}
