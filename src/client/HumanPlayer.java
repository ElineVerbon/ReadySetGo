package client;

import java.util.List;

import protocol.*;
import ruleimplementations.MoveValidator;

/**
 * Class that allows someone play GO by using the console to input moves
 * and checking the status of the game on a GUI. 
 *
 * User needs to now the IP address of the server and the port on which the server
 * is listening to start the game.
 */

public class HumanPlayer extends AbstractClient {
	
	private MoveValidator moveValidator = new MoveValidator();
	
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
	public void doHandshake() {
		serverHandler.doHandshakeWithUserInput();
	}
	
	/**
	 * Get a valid move from the player via the console.
	 * 
	 * First show a message indicating it's the player's turn and what the other player did.
	 * Then, ask for input for a move until a valid move is chosen.
	 */
	public String getMove(String opponentsMove, int boardDimension, 
						String board, char color, List<String> prevBoards) {
		
		String move = "";
		boolean validInput = false;
		boolean valid;
		
		/** Let the player know its his/her turn and what the opponent did. */
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
	 * Communicate the result to the client and update the GUI.
	 * 
	 * @param validity, a character indicating validity of the move
	 * @param boardOrMessage, a String containing the board (if valid move) or (if invalid move)
	 * 					a message or nothing
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
