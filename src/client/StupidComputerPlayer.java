package client;

import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.MoveValidator;

public class StupidComputerPlayer extends AbstractClient {
	
	private int nextComputerPlayerNumber = 0;
	private int computerPlayerNumber;

	private MoveValidator moveValidator = new MoveValidator();
	
	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public StupidComputerPlayer() {
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
		(new StupidComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	public void doHandshake() {
		serverHandler.doHandshake("StupidComputer" + computerPlayerNumber, ProtocolMessages.BLACK);
	}
	

	/**
	 * Get a valid move from the player via the console.
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
