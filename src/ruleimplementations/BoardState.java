package ruleimplementations;

/**
 * Class with methods that relate to the current board state.
 * 
 * @author eline.verbon
 *
 */

public class BoardState {
	/**
	 * Checks whether a location is located on the board adjacent to the previous location.
	 * 
	 * @param nextLocation
	 * @param previousLocation
	 * @return onBoard, a boolean that is true when the location is a neighbor on the board
	 */
	public boolean checkNextLocationBoard(int nextLocation, 
													int previousLocation, int boardDimension) {
		boolean onBoard = false;
		
		// Location is below 0 or above the last intersection
		if (nextLocation < 0 || nextLocation >= boardDimension * boardDimension) {
			return onBoard;
		}
		
		int locationNextX = nextLocation % boardDimension;
		int locationNextY = nextLocation / boardDimension;
		
		int locationPreviousX = previousLocation % boardDimension;
		int locationPreviousY = previousLocation / boardDimension;
		
		// x can be 1 off and y the same, or y can be 1 off and x the same for it to be a neighbor
		if (locationPreviousX == locationNextX) {
			if (Math.abs(locationPreviousY - locationNextY) == 1) {
				onBoard = true;
				return onBoard;
			}
		} else if (locationPreviousY == locationNextY) {
			if (Math.abs(locationPreviousX - locationNextX) == 1) {
				onBoard = true;
				return onBoard;
			}
		}
		
		return onBoard;
	}
}
