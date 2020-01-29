package client;

import java.util.ArrayList;
import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.BoardUpdater;
import ruleimplementations.MoveValidator;
import ruleimplementations.ScoreCalculator;

/**
 * A computer player that can play Go, optimized for a 19 by 19 board.
 * 
 * If the board is 19 by 19, the first stone will be placed at a 4-4 point.
 * The second at a 4-4 point adjacent to it. 
 * 
 * It will first check whether the other player has to pass with the current board state or has just passed. 
 * If so, it will check whether it is currently winning. If so, it will pass.
 * 
 * Otherwise, it will look in the area where it put its first two moves for all
 * unoccupied locations that are valid moves. It will randomly choose one of those moves.
 * 
 * If there are no valid moves, it will pass.
 */

public class Smart3ComputerPlayer extends AbstractClient {
	private int nextComputerPlayerNumber = 0;
	private int computerPlayerNumber;

	private MoveValidator moveValidator = new MoveValidator();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	private BoardUpdater boardUpdater = new BoardUpdater();
	
	private int currentMove;
	private int openingsMove;
	private String side = "";
	
	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public Smart3ComputerPlayer()  {
		super();
		computerPlayerNumber = nextComputerPlayerNumber;
		nextComputerPlayerNumber++;
		currentMove = 0;
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new Smart3ComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	@Override
	public void doHandshake() {
		serverHandler.doHandshake("Smart3Computer" + computerPlayerNumber, ProtocolMessages.BLACK);
	}
	

	/**
	 * Decide on a move
	 * 
	 * @param opponentsMove
	 * @param boardDimension
	 * @param board, a String representation of the current board state
	 * @param color, the color of the player
	 * @param prevBoards, a list of all already seen previous board states
	 */
	@Override
	public String getMove(String opponentsMove, int boardDimension, 
			String board, char color, List<String> prevBoards) {
		
		/** Set good opening locations. */
		int first4_4 = 3 + 3 * boardDimension; 
											//intersection 3,3 (counting from 0)
		int second4_4 = boardDimension - 1 - 3 + 3 * boardDimension; 
											//intersection four from right, 3
		int third4_4 = 3 + (boardDimension - 1 - 3) * boardDimension; 
											//intersection four from right, 3
		int fourth4_4 = boardDimension - 1 - 3 + 3 * (boardDimension - 1 - 3) * boardDimension; 
											//intersection four from right, 3
		
		String move = "";
		
		if (boardDimension == 19) {
			if (currentMove == 0) {
				int location = getOpeningsMove(board, first4_4, second4_4);
				openingsMove = location;
				currentMove++;
				move = Integer.toString(location);
				return move;
			}
			
			if (currentMove == 1) {
				int location = getSecondMove(board, boardDimension, first4_4, second4_4, third4_4, fourth4_4);
				currentMove++;
				move = Integer.toString(location);
				return move;
			}
		}
		
		boolean opponentCanDoAMove = canOpponentDoAMove(move, boardDimension, 
																		board, color, prevBoards);
		boolean opponentPassed = opponentsMove.equals(Character.toString(ProtocolMessages.PASS));
		char winner = currentWinner(board, KOMI);
		if ((!opponentCanDoAMove || opponentPassed) && winner == color) {
			return Character.toString(ProtocolMessages.PASS);
		}
		
		if (side.equals("left")) {
			move = findSpotLeftSide(board, boardDimension, color, prevBoards);
		} else if (side.equals("right")) {
			move = findSpotRightSide(board, boardDimension, color, prevBoards);
		} else if (side.equals("top")) {
			move = findSpotTopSide(board, boardDimension, color, prevBoards);
		}
		if (move == null) {
			move = findSpotOnBoard(board, boardDimension, color, prevBoards);
		}
		
		if (move != null) {
			return move;
		}
		return Character.toString(ProtocolMessages.PASS);
	}
	
	/**
	 * Get the first move.
	 * 
	 * @return either the top left or top right position at 4 intersections from the close sides
	 */
	public int getOpeningsMove(String board, int first4_4, int second4_4) {

		if (board.charAt(first4_4) == ProtocolMessages.UNOCCUPIED) {
			return first4_4;
		} else {
			return second4_4;
		}
	}
	
	/**
	 * Get the second move.
	 * 
	 * @return either an adjacent 4_4 corner position to the first move or a location close to it
	 */
	public int getSecondMove(String board, int boardDimension, int first4_4, int second4_4, 
																	int third4_4, int fourth4_4) {
		if (openingsMove == first4_4) {
			if (board.charAt(second4_4) == ProtocolMessages.UNOCCUPIED) {
				side = "top";
				return second4_4;
			} else if (board.charAt(third4_4) == ProtocolMessages.UNOCCUPIED) {
				side = "left";
				return third4_4;
			} else {
				int newLocation = first4_4 - boardDimension + 3;
				side = "top";
				return newLocation;
			}
		} else if (openingsMove == second4_4) {
			if (board.charAt(first4_4) == ProtocolMessages.UNOCCUPIED) {
				side = "top";
				return first4_4;
			} else if (board.charAt(third4_4) == ProtocolMessages.UNOCCUPIED) {
				side = "rigth";
				return third4_4;
			} else {
				int otherNewLocation = second4_4 - boardDimension - 3;
				side = "top";
				return otherNewLocation;
			}
		}
		
		return 0;
	}
	
	/**
	 * Find a valid location on the left side of the board.
	 * 
	 * @return a String representation of a location
	 */
	public String findSpotLeftSide(String board, int boardDimension, char color, List<String> prevBoards) {
		List<String> possibleLocations = new ArrayList<String>();
		
		for (int x = 1; x <= 4; x++) {
			for (int y = 1; x <= boardDimension - 2; y++) {
				int location = x + y * boardDimension;
				if (isValidMove(board, boardDimension, color, prevBoards, location)) {
					possibleLocations.add(Integer.toString(location));
				}
			}
		}
		
		if (possibleLocations.size() != 0) {
			int randomInt = (int) (Math.random() * possibleLocations.size());
			return possibleLocations.get(randomInt);
		} 
		
		return null;
	}
	
	/**
	 * Find a valid location on the right side of the board.
	 * 
	 * @return a String representation of a location
	 */
	public String findSpotRightSide(String board, int boardDimension, char color, List<String> prevBoards) {
		List<String> possibleLocations = new ArrayList<String>();
		
		for (int x = boardDimension - 5; x <= boardDimension - 2; x++) {
			for (int y = 1; x <= boardDimension - 2; y++) {
				int location = x + y * boardDimension;
				if (isValidMove(board, boardDimension, color, prevBoards, location)) {
					possibleLocations.add(Integer.toString(location));
				}
			}
		}
		
		if (possibleLocations.size() != 0) {
			int randomInt = (int) (Math.random() * possibleLocations.size());
			return possibleLocations.get(randomInt);
		} 
		
		return null;
	}
	
	/**
	 * Find a valid location on the top side of the board.
	 * 
	 * @return a String representation of a location
	 */
	
	public String findSpotTopSide(String board, int boardDimension, char color, List<String> prevBoards) {
		List<String> possibleLocations = new ArrayList<String>();
		
		for (int x = 1; x <= boardDimension - 1; x++) {
			for (int y = 1; y <= 4; y++) {
				int location = x + y * boardDimension;
				if (isValidMove(board, boardDimension, color, prevBoards, location)) {
					possibleLocations.add(Integer.toString(location));
				}
			}
		}
		
		if (possibleLocations.size() != 0) {
			int randomInt = (int) (Math.random() * possibleLocations.size());
			return possibleLocations.get(randomInt);
		} 
		
		return null;
	}
	
	/**
	 * Find a valid location somewhere on the board.
	 * 
	 * @return a String representation of a location
	 */
	
	public String findSpotOnBoard(String board, int boardDimension, char color, List<String> prevBoards) {
		List<String> possibleLocations = new ArrayList<String>();
		
		
		for (int c = 0; c < board.length(); c++) {
			if (isValidMove(board, boardDimension, color, prevBoards, c)) {
				possibleLocations.add(Integer.toString(c));
			}
		}
		
		if (possibleLocations.size() != 0) {
			int randomInt = (int) (Math.random() * possibleLocations.size());
			return possibleLocations.get(randomInt);
		} 
		
		return null;
	}
	
	/**
	 * Check whether a move is a valid move and does not only reduce the own score.
	 * 
	 * @return true if valid and does not only reduce own score
	 */
	public boolean isValidMove(String board, int boardDimension, char color, 
														List<String> prevBoards, int c) {
		String move = "";
		boolean valid;
		
		if (board.charAt(c) == ProtocolMessages.UNOCCUPIED) {
			move = Integer.toString(c);
		}
		valid = moveValidator.processMove(move, boardDimension, board, color, prevBoards);
		if (valid) {
			boolean suicideMove = doesMoveReduceOwnScore(c, board, color, boardDimension);
			if (!suicideMove) {
				return true;
			}
		} 
		return false;
	}
	
	/**
	 * Checks whether the opponent can do a move
	 * 
	 * @return true when the opponent can do a move, false when it cannot
	 */
	
	public boolean canOpponentDoAMove(String opponentsMove, int boardDimension, 
			String board, char color, List<String> prevBoards) {
		
		boolean valid = true;
		boolean opponentCanDoAMove = true;
		String move = "";
		char opponentsColor = 'x';
		if (color == ProtocolMessages.BLACK) {
			opponentsColor = ProtocolMessages.WHITE;
		} else {
			opponentsColor = ProtocolMessages.BLACK;
		}
		
		//go through board from top left to bottom right
		for (int c = 0; c < board.length(); c++) {
			if (board.charAt(c) == ProtocolMessages.UNOCCUPIED) {
				move = Integer.toString(c);
			}
			
			valid = moveValidator.processMove(move, boardDimension, board, 
															opponentsColor, prevBoards);
			if (valid) {
				break;
			} 
		}
		//if no valid moves found
		if (!valid) {
			opponentCanDoAMove = false;
		}
		
		return opponentCanDoAMove;
	}
	
	/**
	 * Given a board and a komi, returns which color currently has the highest score.
	 * 
	 * @param board, s string representation of the current board
	 * @param komi, a double that represents the points subtracted from black's score
	 * @return a char, representing the color that currently has the highest score
	 */
	public char currentWinner(String board, double komi) {
		
		scoreCalculator.calculateScores(board, komi);
		double scoreBlack = scoreCalculator.getScoreBlack();
		double scoreWhite = scoreCalculator.getScoreWhite();
		
		if (scoreBlack > scoreWhite) {
			return ProtocolMessages.BLACK;
		} else {
			return ProtocolMessages.WHITE;
		}
	}
	
	/**
	 * Given a certain board, a move and the current player's color, determines
	 * whether the move leads to a reduction in the own score.
	 * 
	 * @param move, an integer of the location where is stone is to be placed
	 * @param board, a string representation of the current board
	 * @param color, char indicating the color of the stones of the current player
	 * @return true if only stones of the player his/herself are removed
	 */
	public boolean doesMoveReduceOwnScore(int move, String board, char color, int boardDimension) {
		
		scoreCalculator.calculateScores(board, 0.5);
		double scoreBlack = scoreCalculator.getScoreBlack();
		double scoreWhite = scoreCalculator.getScoreWhite();
		
		String newBoard = board.substring(0, move) + color + board.substring(move + 1);
		newBoard = boardUpdater.determineNewBoard(newBoard, color);
		scoreCalculator.calculateScores(newBoard, KOMI);
		double scoreBlackNew = scoreCalculator.getScoreBlack();
		double scoreWhiteNew = scoreCalculator.getScoreWhite();
		
		if (color == ProtocolMessages.BLACK) {
			if (scoreWhite == scoreWhiteNew && scoreBlack > scoreBlackNew) {
				return true;
			}
		} else if (color == ProtocolMessages.WHITE) {
			if (scoreBlack == scoreBlackNew && scoreWhite > scoreWhiteNew) {
				return true;
			}
		}
		
		return false;
	}
	
	
}
