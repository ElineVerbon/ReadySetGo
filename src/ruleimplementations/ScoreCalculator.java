package ruleimplementations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import protocol.ProtocolMessages;

/**
 * A class that calculates the score of two players on a given board.
 */

public class ScoreCalculator {

	private double scoreWhite;
	private double scoreBlack;
	
	private double komi;
	private int boardDimension;
	private String board;
	char areaSurrounder = 'x';
	boolean surrounded;
	
	private BoardState boardState = new BoardState();
	
	List<Integer> checkedUnoccupiedPlaces = new ArrayList<Integer>();
	List<Integer> surroundedArea = new ArrayList<Integer>();
	List<Integer> checkedPlacesInAndAroundSurroundedArea = new ArrayList<Integer>();
	
	public synchronized double getScoreWhite() {
		return scoreWhite;
	}

	public synchronized double getScoreBlack() {
		return scoreBlack;
	}
	
	public void setKomi(double komi) {
		this.komi = komi;
	}
	
	/**
	 * Calculates the scores of the two players.
	 * 
	 * @param board
	 * @param komi
	 */
	public synchronized void calculateScores(String givenBoard) {
		scoreWhite = 0.0;
		scoreBlack = 0.0;
		
		this.board = givenBoard;
		boardDimension = (int) Math.sqrt(board.length());
		
		calculateEmptyArea();
		
		countStones();
		
		scoreBlack -= komi;
	}
	
	/**
	 * Identify all empty areas surrounded by one of the two player.
	 */
	private void calculateEmptyArea() {
		checkedUnoccupiedPlaces.clear();
		surroundedArea.clear();
		checkedPlacesInAndAroundSurroundedArea.clear();
		
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
				int numberInStringRepresentation = x  + y * boardDimension;
				
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
	 * Check the neigbors of a given location for being on the board. If so, check 
	 * their color.
	 * 
	 * @param numberInStringRepresentation, an int indicating an intersection on the board
	 */
	private void checkAllNeighbors(int numberInStringRepresentation) {
		
		int locationToTheLeft = numberInStringRepresentation - 1;
		int locationToTheRight = numberInStringRepresentation + 1;
		int locationAbove = numberInStringRepresentation - boardDimension;
		int locationBelow = numberInStringRepresentation + boardDimension;
		List<Integer> possibleDirectNeighbors = Arrays.asList(locationToTheLeft, locationToTheRight,
															locationAbove, locationBelow);
		List<Integer> locationsToCheck = new ArrayList<Integer>();
		
		//add all neighbors on the board to the to-be-checked list
		for (int neighbor : possibleDirectNeighbors) {
			boolean neighborOnBoard = boardState.checkNextLocationBoard(neighbor, 
													numberInStringRepresentation, boardDimension);
			if (neighborOnBoard) {
				locationsToCheck.add(neighbor);
			}
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
	private void checkColor(int toBeCheckedLocation) {
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
		} else if (board.charAt(toBeCheckedLocation) == ProtocolMessages.WHITE) {
			if (areaSurrounder == 'x') {
				areaSurrounder = ProtocolMessages.WHITE;
			} else if (areaSurrounder == ProtocolMessages.BLACK) {
				surrounded = false;
			}
		}
	}
	
	/**
	 * Counts the number of stones of each player.
	 */
	
	private void countStones() {
		int numberStonesBlack = 0;
		int numberStonesWhite = 0;
		
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
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
