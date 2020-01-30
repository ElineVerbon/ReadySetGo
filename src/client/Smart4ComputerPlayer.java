package client;

import java.util.ArrayList;
import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.BoardState;
import ruleimplementations.BoardUpdater;
import ruleimplementations.MoveValidator;
import ruleimplementations.ScoreCalculator;

/**
 * A computer player that can play Go. 
 * 
 * It will first check whether the other player has to pass with the current board state 
 * or it has just passed. If so, the player will check whether it is currently winning. 
 * If so, it will pass.
 * 
 * It will then attempt to fill the left five columns, while leaving little islands (an
 * empty location fully surrounded by the own color). Once the first five columns are filled, 
 * it will move on to the next five until the board it filled.
 */

public class Smart4ComputerPlayer extends AbstractClient {

	private MoveValidator moveValidator = new MoveValidator();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	private BoardUpdater boardUpdater = new BoardUpdater();
	private BoardState boardState = new BoardState();
	
	/**
	 * Constructor.
	 */
	public Smart4ComputerPlayer()  {
		super();
	}
	
	/**
	 * Starts a computer player. 
	 */
	public static void main(String[] args) {
		(new Smart4ComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	
	@Override
	public void doHandshake() {
		serverHandler.doHandshake("Smart4Computer", ProtocolMessages.BLACK);
	}
	

	/**
	 * Decide on a move.
	 * 
	 * First check whether the other player has to pass with the current board state or it has just
	 * passed. If so, check whether it is currently winning. If so, it will pass.
	 * 
	 * Then attempt to fill the left five columns, while leaving little islands (an empty location
	 * fully surrounded by the own color). Once the first five columns are filled, move on to the
	 * next five columns until the board it filled.
	 * 
	 * @param opponentsMove
	 * @param boardDimension
	 * @param board, a String representation of the current board state
	 * @param color, the color of the player
	 * @param prevBoards, a list of all already seen previous board states
	 * 
	 * @return a String which is either a String representation of an integer between 0 and the 
	 * (number of intersections - 1) or P (for 'pass').
	 */
	@Override
	public String getMove(String opponentsMove, int boardDimension, 
			String board, char color, List<String> prevBoards) {
		
		boolean opponentCanDoAMove = canOpponentDoAMove(boardDimension, board, color, prevBoards);
		boolean opponentPassed = opponentsMove.equals(Character.toString(ProtocolMessages.PASS));
		char winner = boardState.currentWinner(board);
		if ((!opponentCanDoAMove || opponentPassed) && winner == color) {
			return Character.toString(ProtocolMessages.PASS);
		}
		
		boolean noMovesLeft = false;
		List<String> possibleLocations = new ArrayList<String>();
		//fill up left five columns, then move on
		int xmin = 0;
		int xmax = 4;
		while (!noMovesLeft) {
			for (int x = xmin; x <= xmax; x++) {
				for (int y = 0; y <= boardDimension - 1; y++) {
					int location = x + y * boardDimension;
					if (isValidMove(board, boardDimension, color, prevBoards, location)) {
						if (!neighborsAllOwnColor(board, boardDimension, color, location)) {
							possibleLocations.add(Integer.toString(location));
						}
					}
				}
			}
			if (possibleLocations.size() != 0) {
				int randomInt = (int) (Math.random() * possibleLocations.size());
				return possibleLocations.get(randomInt);
			} 
			xmin = xmin + 5;
			xmax = xmax + 5;
			if (xmin > boardDimension - 1) {
				noMovesLeft = true;
			} else if (xmax > boardDimension - 1) {
				xmax = boardDimension - 1;
			}
		}
		
		return Character.toString(ProtocolMessages.PASS);
	}
	

	/**
	 * Check whether all neighbors of a location are of current player.
	 * 
	 * @return boolean, false when not all neighbors are of current color
	 */
	
	public boolean neighborsAllOwnColor(String board, int boardDimension, 
															char color, int location) {
		boolean neighborOnBoard;
		int locationToTheLeft = location - 1;
		neighborOnBoard = boardState.checkNextLocationBoard(locationToTheLeft, location, 
																			boardDimension);
		if (neighborOnBoard) {
			if (board.charAt(locationToTheLeft) != color) {
				return false;
			}
		}
		int locationToTheRight = location + 1;
		neighborOnBoard = boardState.checkNextLocationBoard(locationToTheRight, location, 
																			boardDimension);
		if (neighborOnBoard) {
			if (board.charAt(locationToTheRight) != color) {
				return false;
			}
		}
		int locationAbove = location - boardDimension;
		neighborOnBoard = boardState.checkNextLocationBoard(locationAbove, location, 
																			boardDimension);
		if (neighborOnBoard) {
			if (board.charAt(locationAbove) != color) {
				return false;
			}
		}
		int locationBelow = location + boardDimension;
		neighborOnBoard = boardState.checkNextLocationBoard(locationBelow, location, 
																			boardDimension);
		if (neighborOnBoard) {
			if (board.charAt(locationBelow) != color) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Check whether a move is a valid move.
	 * 
	 * @return true if valid and it does not only reduce own score
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
	 * Checks whether the opponent can do a move.
	 * 
	 * @return true when the opponent can do a move, false when it cannot
	 */
	
	public boolean canOpponentDoAMove(int boardDimension, String board, char color, 
																List<String> prevBoards) {
		boolean valid = true;
		boolean opponentCanDoAMove = true;
		String move = "";
		char opponentsColor = 'x';
		if (color == ProtocolMessages.BLACK) {
			opponentsColor = ProtocolMessages.WHITE;
		} else {
			opponentsColor = ProtocolMessages.BLACK;
		}
		
		//go through board from top left to bottom right to find a valid move
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
	 * Given a certain board, a move and the current player's color, determines
	 * whether the move leads to a reduction in the own score.
	 * 
	 * @param move, an integer of the location where is stone is to be placed
	 * @param board, a string representation of the current board
	 * @param color, char indicating the color of the stones of the current player
	 * @return true if only stones of the player his/herself are removed
	 */
	public boolean doesMoveReduceOwnScore(int move, String board, char color, int boardDimension) {
		
		scoreCalculator.calculateScores(board);
		double scoreBlack = scoreCalculator.getScoreBlack();
		double scoreWhite = scoreCalculator.getScoreWhite();
		
		String newBoard = board.substring(0, move) + color + board.substring(move + 1);
		newBoard = boardUpdater.determineNewBoard(newBoard, color);
		scoreCalculator.calculateScores(newBoard);
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
