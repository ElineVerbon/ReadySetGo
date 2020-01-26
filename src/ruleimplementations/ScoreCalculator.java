package ruleimplementations;

import java.util.ArrayList;
import java.util.List;

import protocol.ProtocolMessages;

/**
 * A class that calculates the score of two player on a given board.
 */

public class ScoreCalculator {

	private double scoreWhite;
	private double scoreBlack;
	
	private int boardDimension;
	private String board;
	
	List<Integer> checkedUnoccupiedPlaces = new ArrayList<Integer>();
	List<Integer> surroundedArea = new ArrayList<Integer>();
	List<Integer> checkedPlacesInAndAroundSurroundedArea = new ArrayList<Integer>();
	char areaSurrounder = 'x';
	boolean surrounded;
	
	public synchronized double getScoreWhite() {
		return scoreWhite;
	}

	public synchronized double getScoreBlack() {
		return scoreBlack;
	}
	
	/**
	 * Determines the empty area surrounded by the player.
	 * @param board
	 * @param komi
	 * @return
	 */
	public synchronized void calculateScores(String givenBoard, double komi) {
		scoreWhite = 0.0;
		scoreBlack = 0.0;
		
		this.board = givenBoard;
		boardDimension = (int) Math.sqrt(board.length());
		
		calculateEmptyArea();
		
		countStones();
		
		scoreBlack -= komi;
	}
	
	/**
	 * Determines the empty area surrounded by the player.
	 */
	public void calculateEmptyArea() {
		
		//go through all stones from top left to bottom to look for an unoccupied location
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
				//get the location within the string representation. 
				int numberInStringRepresentation = x  + y * boardDimension;
				
				//if this location has been already seen as part of another group: go to next y
				if (checkedUnoccupiedPlaces.contains(numberInStringRepresentation)) {
					continue;
				}
				
				//if this location is unoccupied, check all neighbors
				if (board.charAt(numberInStringRepresentation) == ProtocolMessages.UNOCCUPIED) {
					surrounded = true;
					surroundedArea.clear();
					checkedPlacesInAndAroundSurroundedArea.clear();
					
					surroundedArea.add(numberInStringRepresentation);
					checkedUnoccupiedPlaces.add(numberInStringRepresentation);
					checkedPlacesInAndAroundSurroundedArea.add(numberInStringRepresentation);
					areaSurrounder = 'x';
					
					checkAllNeighbors(numberInStringRepresentation);
				}
				
				//if surrounded is true at this point, the current group is surrounded
				//and the number of locations is added to the score of the areaSurrounder.
				if (surrounded == true) {
					if (areaSurrounder == ProtocolMessages.BLACK) {
						scoreBlack += surroundedArea.size();
					} else if (areaSurrounder == ProtocolMessages.WHITE) {
						scoreWhite += surroundedArea.size();
					}
					surrounded = false;
				}
			}
		}
	}
	
	/**
	 * Add the stone as the first of a new possibly-surrounded group, check whether the stone's 
	 * neighbors are part of the same group and whether that group is surrounded.
	 * 
	 * @param numberInStringRepresentation
	 */
	public void checkAllNeighbors(int numberInStringRepresentation) {
		
		//Add all surrounding locations that fall within the board to a to-be-checked list.
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
		
		//check the occupation status of all locations in the list that weren't already checked
		for (int location : locationsToCheck) {
			if (!checkedPlacesInAndAroundSurroundedArea.contains(location)) {
				checkColor(location);
			}
		}
	}
	
	/**
	 * Check the occupation status of a location (unoccupied, black or white).
	 * 
	 * If the location
	 * - is unoccupied: add to group, check all neighboring locations
	 * - has a stone: if 1st stone, set areaSurrounder, if 2nd stone:
	 * 	if not of color areaSurrounder, this area does not belong to one of the two colors
	 *  do keep looking for other unoccupied locations and add those to the group
	 * 
	 * @param toBeCheckedLocation, the location whose occupation status is checked
	 */
	public void checkColor(int toBeCheckedLocation) {
		checkedPlacesInAndAroundSurroundedArea.add(toBeCheckedLocation);
		
		if (board.charAt(toBeCheckedLocation) == ProtocolMessages.UNOCCUPIED) {
			surroundedArea.add(toBeCheckedLocation);
			checkedUnoccupiedPlaces.add(toBeCheckedLocation);
			
			checkAllNeighbors(toBeCheckedLocation);
		} else if (board.charAt(toBeCheckedLocation) == ProtocolMessages.BLACK) {
			if (areaSurrounder == 'x') {
				areaSurrounder = ProtocolMessages.BLACK;
			} else if (areaSurrounder == ProtocolMessages.WHITE) {
				surrounded = false;
			}
		} else if (board.charAt(toBeCheckedLocation) == ProtocolMessages.WHITE){
			if (areaSurrounder == 'x') {
				areaSurrounder = ProtocolMessages.WHITE;
			} else if (areaSurrounder == ProtocolMessages.BLACK) {
				surrounded = false;
			}
		} //otherwise (if no stones on the board), areaSurrounder remains 'x'
	}
	
	/**
	 * Determines the number of stones of each player.
	 * @param board
	 * @param komi
	 * @param color
	 * @return
	 */
	
	public void countStones() {
		int numberStonesBlack = 0;
		int numberStonesWhite = 0;
		
		//go through all stones from top left to bottom to look for an unoccupied location
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
				//get the location within the string representation. 
				int numberInStringRepresentation = x  + y * boardDimension;
				
				if (board.charAt(numberInStringRepresentation) == ProtocolMessages.BLACK) {
					numberStonesBlack += 1;
				} else if (board.charAt(numberInStringRepresentation) == ProtocolMessages.WHITE) {
					numberStonesWhite += 1;
				}
			}
		}
		scoreBlack += numberStonesBlack;
		scoreWhite += numberStonesWhite;
	}
}
