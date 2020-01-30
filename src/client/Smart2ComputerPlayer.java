package client;

import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.MoveValidator;
import ruleimplementations.ScoreCalculator;
import ruleimplementations.BoardState;
import ruleimplementations.BoardUpdater;

/**
 * A computer player that can play Go. 
 * 
 * It will first check whether the other player has to pass with the current board state. 
 * If so, it will check whether it is currently winning. If so, it will pass.
 * 
 * Otherwise, it will look through the board look from top left to bottom right for an
 * unoccupied location that is a valid move. 
 * Once found, it will check whether the move is only suicide (removal of own stones, 
 * no removal of other stones). If it is, it will continue looking for a move. 
 * If there are no valid moves that are not only suicide, it will pass.
 */

public class Smart2ComputerPlayer extends AbstractClient {
	private int nextComputerPlayerNumber = 0;
	private int computerPlayerNumber;

	private MoveValidator moveValidator = new MoveValidator();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	private BoardUpdater boardUpdater = new BoardUpdater();
	private BoardState boardState = new BoardState();
	
	/**
	 * Constructor.
	 */
	public Smart2ComputerPlayer()  {
		super();
		computerPlayerNumber = nextComputerPlayerNumber;
		nextComputerPlayerNumber++;
	}
	
	/**
	 * Starts a computer player. 
	 */
	public static void main(String[] args) {
		(new Smart2ComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	
	@Override
	public void doHandshake() {
		serverHandler.doHandshake("Smart2Computer" + computerPlayerNumber, ProtocolMessages.BLACK);
	}
	

	/**
	 * Decide on the next move.
	 * 
	 * Check whether the opponent can still do a move. If not, calculate score. If highest score:
	 * pass.Go from top left to bottom right of the board, looking for an unoccupied spot 
	 * that is a valid move.
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
		
		boolean opponentCanDoAMove = canOpponentDoAMove(boardDimension, board, color, prevBoards);
		char winner = boardState.highestScore(board);
		if (!opponentCanDoAMove && winner == color) {
			return Character.toString(ProtocolMessages.PASS);
		}
		
		String move = "";
		boolean valid = false;		
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
	
	/**
	 * Check whether the opponent can still do a move.
	 * 
	 * @param boardDimension
	 * @param board, a String representation of the current board state
	 * @param color, the color of the player
	 * @param prevBoards, a list of all already seen previous board states
	 * @return true when the opponent can still do a move, false if it cannot
	 */
	public boolean canOpponentDoAMove(int boardDimension, 
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
	 * whether the move leads to a reduction of only the own score.
	 * 
	 * @param move, an integer of the location where is stone is to be placed
	 * @param board, a string representation of the current board
	 * @param color, char indicating the color of the stones of the current player
	 * @return true if only stones of the player his/herself are removed
	 */
	public boolean doesMoveReduceOwnScore(int move, String board, char color) {
		
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
