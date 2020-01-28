package client;

import java.util.List;

/**
 * A computer player that can play Go. 
 * 
 * It will first check whether the other player has to pass with the current board state. 
 * If so, it will check whether it is currently winning. If so, it will pass.
 * 
 * Otherwise, it will look through the board look from top left to bottom right for all
 * unoccupied locations that are valid moves. It will choose the move that leads to the greatest
 * enhancement in score.
 * 
 * If there are no valid moves, it will pass.
 */

public class Smart3ComputerPlayer extends AbstractClient {

	@Override
	public void doHandshake() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getMove(String theOpponentsMove, int theBoardDimension, String theBoard, 
																				char theColor,
			List<String> thePrevBoards) {
		// TODO Auto-generated method stub
		return null;
	}

}
