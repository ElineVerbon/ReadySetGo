package movechecker;

import java.util.List;

import protocol.ProtocolMessages;

/**
 * A class that checks whether a move is valid.
 * 
 * Can be used by both the players and the game.
 * @author eline.verbon
 *
 */

public class MoveValidator {

	/**
	 * Checks whether a move is valid. If so, changes the board according to the move. 
	 * 
	 * It is invalid if:
	 * 1. the move cannot be parsed to an integer
	 * 2. the move is not within the board
	 * 3. the location is not currently empty
	 * 
	 * @param move
	 * @return validness, a boolean that is true is the move is valid, otherwise false
	 */
	
	//TODO maybe give a move here instead of a String? Then check before whether it is maybe a PASS.
	public boolean checkValidityBeforeRemoving(String move, int boardDimension, String board) {
		
		boolean validness = true;
		int location;
		
		//Check whether the move can be parsed to an integer
		//has been checked in game / player whether it is a pass
		try {
			location = Integer.parseInt(move);
		} catch (NumberFormatException e) {
			validness = false;
			return validness;
		}
		
		//Check if the move is within the board
		if (location < 0 || location >= boardDimension * boardDimension) {
			validness = false;
			return validness;
		}
		
		//Check whether the location is currently empty
		if (board.charAt(location) != ProtocolMessages.UNOCCUPIED) {
			validness = false;
			return validness;
		}
		
		return validness;
	}
	
	/**
	 * Checks whether the board generated by the move (after all steps of the play have been 
	 * completed) does not create a board that was seen before in the game.
	 * 
	 * @param newBoard, the board that was generated by the move
	 * @param prevBoards, all boards that were seen before
	 * @return validness, a boolean: true if the move does not result in a board seen before
	 */
	public boolean checkValidityAfterRemoving(String newBoard, List<String> prevBoards) {
		boolean validness = true;
		
		//check whether the move results in a board that was seen before
		for (String aPrevBoard : prevBoards) {
			if (newBoard.equals(aPrevBoard)) {
				validness = false;
				break;
			}
		}
		
		return validness;
	}
}