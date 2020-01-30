package client;

import java.util.List;

import protocol.*;
import ruleimplementations.MoveValidator;

/**
 * A class representing a player that makes moves based on user input.
 */

public class HumanPlayer extends AbstractClient {
	
	private MoveValidator moveValidator = new MoveValidator();
	
	/**
	 * Constructor.
	 */
	public HumanPlayer() {
		super();
	}
	
	/**
	 * Start a human player.
	 */
	public static void main(String[] args) {
		(new HumanPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	
	@Override
	public void doHandshake() {
		serverHandler.doHandshakeWithUserInput();
	}
	
	/**
	 * Get a valid move from the player via the console.
	 * 
	 * First show a message indicating it's the player's turn and what the other player did.
	 * Then, ask for input for a move until a valid move is chosen.
	 */
	
	@Override
	public String getMove(String opponentsMove, int boardDimension, 
						String board, char color, List<String> prevBoards) {
		
		String move = "";
		boolean validInput = false;
		boolean valid;
		
		if (opponentsMove.equals("null")) {
			clientTUI.showMessage("\nYou get the first move! " + 
					"Please check the GUI for the board size.");
		} else {
			if (opponentsMove.equals(Character.toString(ProtocolMessages.PASS))) {
				clientTUI.showMessage("\nThe other player passed. " + 
						"If you pass as well, the game is over.");
			} else {
				int location = Integer.parseInt(opponentsMove);
				clientTUI.showMessage("\nThe other player placed a stone in location " + location +
					". Please check the GUI for the current board state.");
			}
		}
		
		while (!validInput) {
			
			move = clientTUI.getMove();
			
			if (move.equals(Character.toString(ProtocolMessages.PASS)) || 
								move.equals(Character.toString(ProtocolMessages.QUIT))) {
				validInput = true;
			} else if (move.equals("N")) {
				showHint(board, boardDimension, color, prevBoards);
			} else {
				valid = moveValidator.processMove(move, boardDimension, board, color, prevBoards);
				if  (!valid) {
					clientTUI.showMessage("Your move was invalid. "
								+ "Please try again.");
				} else {
					validInput = true;
				}
			} 
		}
		return move;
	}
		
	/**
	 * Shows an option for a valid move or tells the user it can only pass.
	 */
	public void showHint(String board, int boardDimension, char color, List<String> prevBoards) {
			
		String move = "";
		int location = 0;
		boolean valid = false;
		
		for (int c = 0; c < board.length(); c++) {
			if (board.charAt(c) == ProtocolMessages.UNOCCUPIED) {
				location = c;
				move = Integer.toString(c);
			}
			
			valid = moveValidator.processMove(move, boardDimension, board, color, prevBoards);
			if (valid) {
				break;
			} 
		}
		if (!valid) {
			clientTUI.showMessage("No valid moves are left, you can only pass.");
		} else {
			clientTUI.showMessage("Check the board for a possible valid move!");
			int hintX = location % boardDimension;
			int hintY = location / boardDimension;
			gogui.addHintIndicator(hintX, hintY);
		}
	}
		
	/**
	 * Communicate the result to the client and update the GUI.
	 * 
	 * @param validity, a one-character String ("V" or "I") indicating validity of the move
	 * @param boardOrMessage, a String containing the board (if valid move) or
	 * 					(in case of an invalid move) either a message or nothing
	 */
	public void getResult(String validity, String boardOrMessage, boolean doublePass) {
		
		super.getResult(validity, boardOrMessage, doublePass);
		
		if (Character.toString(ProtocolMessages.VALID).equals(validity)) {
			clientTUI.showMessage("Your move was valid. Check GUI for what the board looks like.");
			if (!doublePass) {
				clientTUI.showMessage("It's now the other player's turn. Please wait.");
			}
		}
	}
}
