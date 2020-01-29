package client;

import java.util.List;

import protocol.ProtocolMessages;
import ruleimplementations.MoveValidator;
import ruleimplementations.ScoreCalculator;

/**
 * A computer player that can play Go. 
 * 
 * It will first check whether the other player can do another move. If not, it will 
 * check whether it is currently winning. If so, it will pass.
 * Otherwise, it will look through the board and choose the first available unoccupied 
 * spot for its move if not invalid. If there are no valid moves left, it will pass.
 */

public class Smart1ComputerPlayer extends AbstractClient {
	private int nextComputerPlayerNumber = 1;
	private int computerPlayerNumber;

	private MoveValidator moveValidator = new MoveValidator();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	
	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public Smart1ComputerPlayer()  {
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
		(new Smart1ComputerPlayer()).start();
	}
	
	/**
	 * Do the handshake.
	 */
	public void doHandshake() {
		serverHandler.doHandshake("Smart1Computer" + computerPlayerNumber, ProtocolMessages.BLACK);
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
				return move;
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
}
