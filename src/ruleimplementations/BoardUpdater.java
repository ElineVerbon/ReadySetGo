package ruleimplementations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import protocol.ProtocolMessages;

/**
 * Check whether stones need to be removed in a given GO board.
 * 
 * This procedure consists of four subsequent methods
 * 1. determineNewBoard. This will loop through all locations on the board
 * and will call (first for the not-current-player's color, then the current-player's-color):
 * 2. checkStonesOfOneColor. Goes through the board from top left to bottom right and identifies
 * stones of the currently-checked-color. Once such a stone is found, it calls:
 * 3. checkAllNeighbors. Adds the current location to the 'surroundedStones' group and gets all 
 * neighboring locations of the identified stone. For those location (if not checked already) it 
 * will call:
 * 4. checkColor. Checks the occupation status of a location to see if it is of the currently being 
 * looked-for color. If so, it is added to the surroundedStones' group and the neighbors of this 
 * location will be checked via a call to method number 3. If it is unoccupied, the boolean variable
 * 'surrounded' for this group is set to false. If it is of the opposing color, nothing happens.
 * 
 * Once a recursive call gets back to method number 2 with 'surrounded = true', all locations in
 * this group are set to unoccupied and the next location is checked. Once all locations on the
 * board are checked in method number 2, the procedure is repeated from starting from method 1, but
 * this time for the opposite color.
 */

public class BoardUpdater {
	
	private boolean surrounded;
	private char opponentsColor = 'x';
	private int boardDimension;
	private String board;
	
	private BoardState boardState = new BoardState();
	
	//variable to keep track of places checked per group (cleared at end of group)
	private List<Integer> checkedPlaces;
	//variable to keep track of stones that are part of the currently checked group 
	//(cleared at end of group)
	private List<Integer> surroundedStones;
	//variable to keep track of the stones of the currently checked color that have been checked 
	//(cleared at the end of looking through this color)
	private List<Integer> checkedStonesThisColor;
	
	/**
	 * Determine which stones are captured and should be removed.
	 * First check the not-current-player's color for captured stones,
	 * as capturing of a group goes before suicide.
	 */
	
	public String determineNewBoard(String oldBoard, char currentlyPlayersColor) {
		board = oldBoard;
		
		boardDimension = (int) Math.sqrt(board.length());
		
		if (currentlyPlayersColor == ProtocolMessages.BLACK) {
			opponentsColor = ProtocolMessages.WHITE;
		} else {
			opponentsColor = ProtocolMessages.BLACK;
		}
		
		//check the opponent's stones for captured groups
		checkedPlaces = new ArrayList<Integer>();
		surroundedStones = new ArrayList<Integer>();
		checkedStonesThisColor = new ArrayList<Integer>();
		checkStonesOfOneColor(opponentsColor);
		
		//check current player's stones for captured groups (suicide is allowed)
		checkedPlaces = new ArrayList<Integer>();
		surroundedStones = new ArrayList<Integer>();
		checkedStonesThisColor = new ArrayList<Integer>();
		if (opponentsColor == ProtocolMessages.WHITE) {
			checkStonesOfOneColor(ProtocolMessages.BLACK);
		} else {
			checkStonesOfOneColor(ProtocolMessages.WHITE);
		}
		
		return board;
		
	}
	
	/**
	 * Check the stones of one color for captured groups.
	 * 
	 * If a stone of that color is found, surrounded is set to true and the neighboring locations
	 * are checked for being surrounded or not. If after checking all the neighbors 'surrounded' is 
	 * still true, the stones of the found captured group are changed to unoccupied. 
	 * 
	 * @param currentlyCheckedColor
	 */
	public void checkStonesOfOneColor(char currentlyCheckedColor) {
		
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
				int numberInStringRepresentation = x  + y * boardDimension;
				
				//if this location has been already seen as part of another group: go to next y
				if (checkedStonesThisColor.contains(numberInStringRepresentation)) {
					continue;
				}
				
				//If there is a stone of the current color in this location, set surrounded to 
				//true and check all neighbors
				if (board.charAt(numberInStringRepresentation) == currentlyCheckedColor) {
					surrounded = true;
					surroundedStones.clear();
					checkedPlaces.clear();
					
					checkAllNeighbors(numberInStringRepresentation, currentlyCheckedColor);
				}
				
				//if surrounded is true at this point, the current group is surrounded
				//and each of its stones should be removed.
				if (surrounded == true) {
					for (int location : surroundedStones) {
						board = board.substring(0, location) + ProtocolMessages.UNOCCUPIED
								 + board.substring(location + 1);
					}
				}
			}
		}
	}
	
	/**
	 * Get a stone's direct neighbors and check the color of all neighbors.
	 * 
	 * @param numberInStringRepresentation
	 */
	public void checkAllNeighbors(int numberInStringRepresentation, char currentlyCheckedColor) {
		
		surroundedStones.add(numberInStringRepresentation);
		checkedStonesThisColor.add(numberInStringRepresentation);
		
		int locationToTheLeft = numberInStringRepresentation - 1;
		int locationToTheRight = numberInStringRepresentation + 1;
		int locationAbove = numberInStringRepresentation - boardDimension;
		int locationBelow = numberInStringRepresentation + boardDimension;
		List<Integer> possibleDirectNeighbors = Arrays.asList(locationToTheLeft, locationToTheRight,
															locationAbove, locationBelow);
		List<Integer> locationsToCheck = new ArrayList<Integer>();
		
		for (int neighbor : possibleDirectNeighbors) {
			boolean neighborOnBoard = boardState.checkNextLocationBoard(neighbor, 
													numberInStringRepresentation, boardDimension);
			if (neighborOnBoard) {
				locationsToCheck.add(neighbor);
			}
		}
		
		for (int location : locationsToCheck) {
			if (!checkedPlaces.contains(location)) {
				checkColor(location, currentlyCheckedColor);
			}
		}
	}
	
	/**
	 * Check the occupation status of a location (unoccupied, black or white).
	 * 
	 * If the stone is
	 * - of the currently checked color: add to group, check all neighboring locations
	 * - of the opponent's color: do nothing (next location will be called in 'checkAllNeighbors()')
	 * - unoccupied: set surrounded to false (next location will be called in 'checkAllNeighbors()')
	 * 
	 * @param toBeCheckedLocation, the location whose occupation status is checked
	 * @param currentlyCheckedColor, the color of which captured groups are being sought
	 */
	public void checkColor(int toBeCheckedLocation, char currentlyCheckedColor) {
		checkedPlaces.add(toBeCheckedLocation);
		
		if (board.charAt(toBeCheckedLocation) == currentlyCheckedColor) {
			surroundedStones.add(toBeCheckedLocation);
			checkAllNeighbors(toBeCheckedLocation, currentlyCheckedColor);
		} else if (board.charAt(toBeCheckedLocation) == ProtocolMessages.UNOCCUPIED) {
			surrounded = false;
		} 
	}
}
