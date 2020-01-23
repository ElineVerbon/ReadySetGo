package movechecker;

import java.util.ArrayList;
import java.util.List;

import protocol.ProtocolMessages;

/**
 * Check whether stones need to be removed due to the move.
 * 
 * This consists of three subsequent methods
 * 1. determine new board. This will loop through all locations on the board
 * and will call:
 * 2. checkAllNeighbors. gets all neighboring locations and for the locations 
 * that have not yet been checked, it will call:
 * 3. checkColor. checks whether the color is of the currently being looked-for
 * color. If so, it is added to the group and the neighbors of this locations
 * will be checked via a call to 2. If it is unoccupied, the boolean variable 
 * surrounded for this group is set to false.
 */

public class MoveResult {
	
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
	//(cleared at the end of looking through this color
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
	 * If a stone of the color is found, set surrounded to true and check all the
	 * neighboring locations. If after checking all the neighbors surrounded is still
	 * true, change the stones of the found captured group to unoccupied. 
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
				//add it to the checked places, indepedent of the color
				checkedPlaces.add(numberInStringRepresentation);
				
				//check whether this location has a stone of the currently checked color
				if (board.charAt(numberInStringRepresentation) == currentlyCheckedColor) {
					//set surrounded to true (will be set to false if unoccupied neighbor is found)
					surrounded = true;
					surroundedStones.clear();
					checkedPlaces.clear();
					
					//check all surrounding stones
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
		//add this stone (= a stone of the currently checked color) to the list of stones of this 
		//group and the list of stones of this color that were checked this turn
		surroundedStones.add(numberInStringRepresentation);
		checkedStonesThisColor.add(numberInStringRepresentation);
		
		//check all surrounding locations. Only check when they fall within the board.
		int locationToTheLeft = numberInStringRepresentation - 1;
		int locationToTheRight = numberInStringRepresentation + 1;
		int locationAbove = numberInStringRepresentation - boardDimension;
		int locationBelow = numberInStringRepresentation + boardDimension;
		List<Integer> locationsToCheck = new ArrayList<Integer>();
		if (locationToTheLeft >= 0 && locationToTheLeft < boardDimension * boardDimension) {
			locationsToCheck.add(locationToTheLeft);
		}
		if (locationToTheRight >= 0 && locationToTheRight < boardDimension * boardDimension) {
			locationsToCheck.add(locationToTheRight);
		}
		if (locationAbove >= 0 && locationAbove < boardDimension * boardDimension) {
			locationsToCheck.add(locationAbove);
		}
		if (locationBelow >= 0 && locationBelow < boardDimension * boardDimension) {
			locationsToCheck.add(locationBelow);
		}
		
		//check for all surrounding places whether they are unoccupied, of the current player
		//or also of the opponent. If they are also of the opponent: add to the current group
		//only check locations that are on the board
		for (int location : locationsToCheck) {
			//only check locations if they haven't been checked before
			if (!checkedPlaces.contains(location)) {
				checkColor(location, currentlyCheckedColor);
			}
		}
	}
	
	/**
	 * Check the color of a stone.
	 * 
	 * If the stone is:
	 * - also of the currently checked color: add to group, go to all neighboring locations
	 * - of other color: go to next location
	 * - unoccupied: set surrounded to false, but keep looking to collect all stones of a group.
	 * 
	 * @param toBeCheckedLocation
	 * @param currentlyCheckedColor
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
			//don't break, keep looking until all connected opponent's stones 
			//have been added to 'checkedPlaces'
		} //if the location is of the other color, do nothing
	}
}
