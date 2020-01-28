package client;

import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.MoveValidator;
import ruleimplementations.ScoreCalculator;
import ruleimplementations.BoardUpdater;

/**
 * A computer player that can play Go 
 * 
 * It will first check whether the other player has to pass with the current board state. 
 * If so, it will check whether it is currently winning. If so, it will pass.
 * 
 * Otherwise, it will look through the board look from top left to bottom right for an
 * unoccupied location that is a valid move. 
 * Once found, it will check whether the move is only suicide (removal of own stones, 
 * no removal of other stones). If it is, it will continue looking for a move. 
 * 
 * NB: but that does not make it much smarter! Because white might then put a stone there
 * and remove it.
 * 
 * If there are no valid moves that are not only suicide, it will pass.
 */

public class Smart2ComputerPlayer extends AbstractClient {
	private int nextComputerPlayerNumber = 0;
	private int computerPlayerNumber;

	private MoveValidator moveValidator = new MoveValidator();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	private BoardUpdater boardUpdater = new BoardUpdater();
	
	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public Smart2ComputerPlayer()  {
		super();
		computerPlayerNumber = nextComputerPlayerNumber;
		nextComputerPlayerNumber++;
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new Smart2ComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	public void doHandshake() {
		serverHandler.doHandshake("Smart2Computer" + computerPlayerNumber, ProtocolMessages.BLACK);
	}
	

	/**
	 * Go from top left to bottom right of the board, looking for an unoccupied spot 
	 * that is a valid move.
	 * 
	 * Check whether the opponent can still do a move. If not, calculate score. If highest score:
	 * pass.
	 * 
	 * @param opponentsMove
	 * @param boardDimension
	 * @param board, a String representation of the current board state
	 * @param color, the color of the player
	 * @param prevBoards, a list of all already seen previous board states
	 */
	public String getMove(String opponentsMove, int boardDimension, 
			String board, char color, List<String> prevBoards) {
		
		String move = "";
		boolean valid = false;
		
		boolean opponentCanDoAMove = canOpponentDoAMove(move, boardDimension, 
																		board, color, prevBoards);
		//TODO get komi from game!
		char winner = currentWinner(board, 0.5);
		
		if (!opponentCanDoAMove && winner == color) {
			return Character.toString(ProtocolMessages.PASS);
		}
				
		for (int c = 0; c < board.length(); c++) {
			if (board.charAt(c) == ProtocolMessages.UNOCCUPIED) {
				move = Integer.toString(c);
			}
			valid = moveValidator.processMove(move, boardDimension, board, color, prevBoards);
			if (valid) {
				boolean suicideMove = doesMoveReduceOwnScore(c, board, color);
				if (!suicideMove) {
					return move;
				}
				
			} 
		}
		return Character.toString(ProtocolMessages.PASS);
	}
	
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
	public boolean doesMoveReduceOwnScore(int move, String board, char color) {
		
		scoreCalculator.calculateScores(board, 0.5);
		double scoreBlack = scoreCalculator.getScoreBlack();
		double scoreWhite = scoreCalculator.getScoreWhite();
		
		String newBoard = board.substring(0, move) + color + board.substring(move + 1);
		newBoard = boardUpdater.determineNewBoard(newBoard, color);
		scoreCalculator.calculateScores(newBoard, 0.5);
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
