package ruleimplementations;

import protocol.ProtocolMessages;

/**
 * Class with methods that relate to the current board state.
 */

public class BoardState {
	
	private ScoreCalculator scoreCalculator;
	
	/**
	 * Constructor.
	 */
	public BoardState(ScoreCalculator scoreCalculator) {
		this.scoreCalculator = scoreCalculator;
	}
	
	/**
	 * Check whether a location is located on the board, adjacent to the previous location.
	 * 
	 * @param nextLocation
	 * @param previousLocation
	 * @return onBoard, a boolean that is true when the location is a neighbor on the board
	 */
	public boolean checkNextLocationBoard(int nextLocation, 
													int previousLocation, int boardDimension) {

		if (nextLocation < 0 || nextLocation >= boardDimension * boardDimension) {
			return false;
		}
		
		int locationNextX = nextLocation % boardDimension;
		int locationNextY = nextLocation / boardDimension;
		
		int locationPreviousX = previousLocation % boardDimension;
		int locationPreviousY = previousLocation / boardDimension;
		
		// x can be 1 off and y the same, or y can be 1 off and x the same for it to be a neighbor
		if (locationPreviousX == locationNextX) {
			if (Math.abs(locationPreviousY - locationNextY) == 1) {
				return true;
			}
		} else if (locationPreviousY == locationNextY) {
			if (Math.abs(locationPreviousX - locationNextX) == 1) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determine which player (W or B) currently has the highest score.
	 * 
	 * @param board, a string representation of the board
	 * @return a char indicating who has the highest score (W or B)
	 */
	public char highestScore(String board) {
		
		scoreCalculator.calculateScores(board);
		double scoreBlack = scoreCalculator.getScoreBlack();
		double scoreWhite = scoreCalculator.getScoreWhite();
		
		if (scoreBlack > scoreWhite) {
			return ProtocolMessages.BLACK;
		} else {
			return ProtocolMessages.WHITE;
		}
	}
}
