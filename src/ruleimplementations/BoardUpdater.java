package ruleimplementations;

import java.util.ArrayList;
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
 * location will be checked via a call to 3. If it is unoccupied, the boolean variable 
 * 'surrounded' for this group is set to false. If it is of the opposing color, nothing happens.
 * 
 * Once a recursive call gets back to 2 with 'surrounded = true', all locations in this group are 
 * set to unoccupied and the next location is checked. Once all location are checked in 2, the 
 * procedure is repeated from starting from method 1, but this time for the opposite color.
 */

public class BoardUpdater {
	
	private boolean surrounded;
	private char opponentsColor = 'x';
	private int boardDimension;
	private String board;
	
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
	 * 
	 * First check the not-current-player's color for captured stones,
	 * as capturing of a group goes before suicide.
	 */
	
	public String determineNewBoard(String oldBoard, char currentlyPlayersColor) {
		board = oldBoard;
		
		boardDimension = (int) Math.sqrt(board.length());
		
		//get the not-current-player's color
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
	 * If a stone of that color is found, set surrounded to true and check all the
	 * neighboring locations (& if it has stones of the same color as neighbor, also the neigboring
	 * locations of that stone / those stones). If after checking all the neighbors 'surrounded' is 
	 * still true, change the stones of the found captured group to unoccupied. 
	 * 
	 * Once a stone has been checked, it is added to the checkedStones List. Only stones not
	 * in the list will be checked. 
	 * 
	 * @param currentlyCheckedColor
	 */
	public void checkStonesOfOneColor(char currentlyCheckedColor) {
		
		//go through all stones from top left to bottom
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
				//get the location within the string representation. 
				int numberInStringRepresentation = x  + y * boardDimension;
				
				//if this location has been already seen as part of another group: go to next y
				if (checkedStonesThisColor.contains(numberInStringRepresentation)) {
					continue;
				}
				
				/**
				 * Check whether this location has a stone of the currently being checked color.
				 * If so, set 'surrounded' to true and check all neighbors to see whether this 
				 * stone or group of stones has been captured. Once a unoccupied location is 
				 * found as a neighbor, 'surrounded' is set to false.
				 */
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
	 * Check whether a stone's neighbors are also part of the same group 
	 * and whether that group is surrounded.
	 * 
	 * @param numberInStringRepresentation
	 * @return
	 */
	public void checkAllNeighbors(int numberInStringRepresentation, char currentlyCheckedColor) {
		boolean neighborOnBoard;
		
		//add this stone (= a stone of the currently checked color) to the list of stones of this 
		//group and the list of stones of this color that were checked this turn (= after last move)
		surroundedStones.add(numberInStringRepresentation);
		checkedStonesThisColor.add(numberInStringRepresentation);
		
		//Add all surrounding locations that fall within the board to a to-be-checked list.
		List<Integer> locationsToCheck = new ArrayList<Integer>();
		int locationToTheLeft = numberInStringRepresentation - 1;
		neighborOnBoard = checkNextLocationBoard(locationToTheLeft, numberInStringRepresentation);
		if (neighborOnBoard) {
			locationsToCheck.add(locationToTheLeft);
		}
		int locationToTheRight = numberInStringRepresentation + 1;
		neighborOnBoard = checkNextLocationBoard(locationToTheRight, numberInStringRepresentation);
		if (neighborOnBoard) {
			locationsToCheck.add(locationToTheRight);
		}
		int locationAbove = numberInStringRepresentation - boardDimension;
		neighborOnBoard = checkNextLocationBoard(locationAbove, numberInStringRepresentation);
		if (neighborOnBoard) {
			locationsToCheck.add(locationAbove);
		}
		int locationBelow = numberInStringRepresentation + boardDimension;
		neighborOnBoard = checkNextLocationBoard(locationBelow, numberInStringRepresentation);
		if (neighborOnBoard) {
			locationsToCheck.add(locationBelow);
		}
		
		//check the occupation status of all locations in the list that weren't already checked
		for (int location : locationsToCheck) {
			if (!checkedPlaces.contains(location)) {
				checkColor(location, currentlyCheckedColor);
			}
		}
	}
	
	public boolean checkNextLocationBoard(int nextLocation, int previousLocation) {
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
		
		//if the location is occupied by the currently checked color, it is added to the group
		if (board.charAt(toBeCheckedLocation) == currentlyCheckedColor) {
			surroundedStones.add(toBeCheckedLocation);
			checkAllNeighbors(toBeCheckedLocation, currentlyCheckedColor);
		//if the location is unoccupied, surrounded is set to false
		} else if (board.charAt(toBeCheckedLocation) == ProtocolMessages.UNOCCUPIED) {
			surrounded = false;
		} 
		//if the location is of the other color, do nothing
	}
}
