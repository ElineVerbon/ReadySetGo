package server;

import java.net.SocketTimeoutException;
import java.util.*;

import protocol.MessageGenerator;
import protocol.ProtocolMessages;
import ruleimplementations.*;

public class Game {
	/** Variables required for proper game flow. */
	private int gameNumber;
	private String version;
	private int boardDimension;
	public static final double KOMI = 0.5;
	private char reasonGameEnd;
	private String opponentsMove = null;
	
	/** Variable to keep track of and connect to the players. */
	@SuppressWarnings("unused")
	private String namePlayer1 = null;
	@SuppressWarnings("unused")
	private String namePlayer2 = null;
	private char colorPlayer1 = 'x';
	private char colorPlayer2 = 'x';
	private char currentPlayersColor = 'x';
	
	private Handler goClientHandlerPlayer1;
	private Handler goClientHandlerPlayer2;
	private Handler currentPlayersHandler;
	private MessageGenerator messageGenerator;
	
	/** Variable to keep track of game states. */
	private boolean twoPlayers = false;
	private boolean started = false;
	private boolean passed = false;
	private boolean firstPlayersTurn = true;
	private boolean gameEnded = false;
	
	/** The board and all previous boards, represented as strings. */
	private String board;
	private List<String> prevBoards = new ArrayList<String>();
	
	/** Make objects of the classes that implement the GO rules. */
	private MoveValidator moveValidator = new MoveValidator();
	private BoardUpdater moveResult = new BoardUpdater();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	
	/** 
	 * Constructor, creates string representation of an empty board. 
	 */
	public Game(int number, String chosenVersion, int boardSize) {
		gameNumber = number;
		version = chosenVersion;
		boardDimension = boardSize;
		messageGenerator = new MessageGenerator();
		
		// Create a string representation of the empty board.
        char[] charArray = new char[boardDimension * boardDimension];
        Arrays.fill(charArray, ProtocolMessages.UNOCCUPIED);
        board = new String(charArray);
	}
	
	/**
	 * Getters and setters for the instance variables of Game.
	 */
	public void setColorPlayer1(char color) {
		colorPlayer1 = color;
	}
	
	public void setColorPlayer2(char color) {
		colorPlayer2 = color;
	}
	
	public char getColorPlayer1() {
		return colorPlayer1;
	}
	
	public char getColorPlayer2() {
		return colorPlayer2;
	}
	
	public void setNamePlayer1(String name) {
		namePlayer1 = name;
	}
	
	public void setNamePlayer2(String name) {
		namePlayer2 = name;
	}
	
	public void setClientHandlerPlayer1(Handler goClientHandler) {
		goClientHandlerPlayer1 = goClientHandler;
	}
	
	public void setClientHandlerPlayer2(Handler goClientHandler) {
		goClientHandlerPlayer2 = goClientHandler;
	}
	
	public Handler getClientHandlerPlayer1() {
		return goClientHandlerPlayer1;
	}
	
	public Handler getClientHandlerPlayer2() {
		return goClientHandlerPlayer2;
	}
	
	public String getBoard() {
		return board;
	}
	
	public int getGameNumber() {
		return gameNumber;
	}
	
	public boolean getStarted() {
		return started;
	}
	
	public void setTwoPlayers(boolean twoPlayers) {
		this.twoPlayers = twoPlayers;
	}
	
	public boolean hasTwoPlayers() {
		return twoPlayers;
	}
	
	public boolean hasEnded() {
		return gameEnded;
	}
	
	/**
	 * Runs the game.
	 * Starts the game (send start messages, send first turn message), 
	 * keeps sending turns to alternating players until game end.
	 */
	public void runGame() {
		started = true;
		startGame();
		while (!gameEnded) {
			doTurn();
			processReply();
		}
		endGame();
	}
	
	/**
     * Send start game message to player 2 - the message is already sent to player 1
     * as a still-connected check when player 2 was added to the game.
     * 
     * Give the first turn to the player who plays with black.
     */
	public void startGame() {
		String startGameMessage = messageGenerator.startGameMessage(board, colorPlayer2);
		goClientHandlerPlayer2.sendMessageToClient(startGameMessage);
		
		if (colorPlayer1 == ProtocolMessages.BLACK) {
			firstPlayersTurn = true;
			doTurn();
			processReply();
		} else {
			firstPlayersTurn = false;
			doTurn();
			processReply();
		}
	}
	
	/**
	 * Send message to a player to tell him/her that its his/her turn.
	 */
	public void doTurn() {
		if (firstPlayersTurn) {
			currentPlayersHandler = goClientHandlerPlayer1;
			currentPlayersColor = colorPlayer1;
		} else {
			currentPlayersHandler = goClientHandlerPlayer2;
			currentPlayersColor = colorPlayer2;
		}
		
		String doTurnMessage = messageGenerator.doTurnMessage(board, opponentsMove);
		currentPlayersHandler.sendMessageToClient(doTurnMessage);
	}
	
	/**
	 * Only process replies received within 1 minute (reply == null).
	 * Check whether first component of a player's reply is one character & is not 'Q' 
	 * (for quit). If this is the case (protocol is kept), send the move (second component) to 
	 * processMove()
	 */
	public void processReply() {
		String move = "";
		
		String reply = getReply();
		
		if (reply == null) {
			return;
		}
		
		// Check 1st component of the move message received from the player is of length 1. 
		String[] components = reply.split(ProtocolMessages.DELIMITER);
		if (components[0].length() != 1) {
			String errorMessage = messageGenerator.errorMessage("Player did not keep to "
					+ "the protocol: the first part of its move message ( " + reply + ") "
					+ "was not a single character.", version);
			currentPlayersHandler.sendMessageToClient(errorMessage);
			gameEnded = true;
			reasonGameEnd = ProtocolMessages.CHEAT;
			endGame();
			return;
		}
		
		// Check which kind of message is received (indicated by first component of the message).
		char command = components[0].charAt(0);
		switch (command) {
			case ProtocolMessages.QUIT:
				reasonGameEnd = ProtocolMessages.QUIT;
				gameEnded = true;
				return;
				
			case ProtocolMessages.MOVE:
				move = components[1];
				processMove(move);
		
				if (!gameEnded) {
					if (firstPlayersTurn) {
						firstPlayersTurn = false;
					} else {
						firstPlayersTurn = true;
					}
				}
				break;
				
			case ProtocolMessages.ERROR:
				boolean validity = false;
				String resultMessage = messageGenerator.resultMessage(validity, 
											"You did not understand our message, we will stop.");
				currentPlayersHandler.sendMessageToClient(resultMessage);
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.CHEAT;
				return;
				
			default:
				String errorMessage = messageGenerator.errorMessage("Player did not keep to the "
					+ "protocol: '" + ProtocolMessages.QUIT + "', '" + ProtocolMessages.ERROR 
					+ "' or '" + ProtocolMessages.MOVE + "' expected as first part of the message, "
					+ "but '" +	command + "' received.", version);
				currentPlayersHandler.sendMessageToClient(errorMessage);
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.CHEAT;
				endGame();
				return;
		}
	}
	
	/**
	 * Get a reply from the client.
	 * 
	 * If the reply does not come within a minute, or the reply is null, set gameEnded to true and 
	 * return null
	 * 
	 * @return reply, the reply of the client or null if no reply
	 */
	public String getReply() {
		String reply;
		
		// End game if player took more than 1 minute to respond.
		try {
			reply = currentPlayersHandler.getReply();
		} catch (SocketTimeoutException e) {
			boolean validity = false;
			String resultMessage = messageGenerator.resultMessage(validity, 
										"You took more than 1 minute to decide on a move.");
			currentPlayersHandler.sendMessageToClient(resultMessage);
			gameEnded = true;
			reasonGameEnd = ProtocolMessages.CHEAT;
			return null;
		}
		
		// End game if client disconnected. 
		if (reply == null) {
			reasonGameEnd = ProtocolMessages.DISCONNECT;
			gameEnded = true;
			return reply;
		}
		
		// Return the reply
		return reply;
	}
	
	/**
	 * Process the move that was received.
	 * 
	 * First check whether the player passed. If so:
	 * - if second pass: return move is valid message, break out of doTurn-loop to endGame
	 * - if first pass: return move is valid message and continue the game
	 * 
	 * If player did not pass:
	 * - check validity of move (if not valid, return move is invalid message and end game)
	 * - if valid: determine the new board
	 */
	
	public void processMove(String move) {
		
		boolean valid = true;
		
		if (move.equals(Character.toString(ProtocolMessages.PASS))) {
			if (passed) {
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.FINISHED;
			} else {
				passed = true;
			}
		} else {
			passed = false;
			
			prevBoards.add(board);
			valid = moveValidator.processMove(move, boardDimension, board, currentPlayersColor, 
																					prevBoards);
			if (valid) {
				int location = Integer.parseInt(move);
				board = board.substring(0, location) + currentPlayersColor + 
																board.substring(location + 1);
				board = moveResult.determineNewBoard(board, currentPlayersColor);
			} else {
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.CHEAT;
			}
		}
		
		giveResult(valid);
		opponentsMove = move;
	}
	
	/**
	 * Set the result message and send to the correct player.
	 * @param validness, character indicating a valid or an invalid move
	 */
	
	public void giveResult(boolean valid) {
		String message = "";
		
		//Sleep to allow the GUI time to update the board in between moves.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		
		if (valid) {
			message = board;
		} else {
			message = "Your move was invalid. You lose the game.";
		}
		String resultMessage = messageGenerator.resultMessage(valid, message);
		if (firstPlayersTurn) {
			goClientHandlerPlayer1.sendMessageToClient(resultMessage);
		} else {
			goClientHandlerPlayer2.sendMessageToClient(resultMessage);
		}
	}
	
	/**
	 * End the game. 
	 * Calculate the scores of the players and determine the winner. Send an end of game message
	 * to both clients.
	 */
	public void endGame() {
		
		char winner = 'x';
		
		scoreCalculator.calculateScores(board, KOMI);
		double scoreBlack = scoreCalculator.getScoreBlack();
		double scoreWhite = scoreCalculator.getScoreWhite();
		if (scoreBlack > scoreWhite) {
			winner = ProtocolMessages.BLACK;
		} else {
			winner = ProtocolMessages.WHITE;
		}
		
		switch (reasonGameEnd) {
			//if game ended after a double pass, decide on winner based on the scores
			case ProtocolMessages.FINISHED:
				String endGameMessage = messageGenerator.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				goClientHandlerPlayer1.sendMessageToClient(endGameMessage);
				goClientHandlerPlayer2.sendMessageToClient(endGameMessage);
				break;
			//if one of the other game end reasons: the not-current player wins
			case ProtocolMessages.CHEAT:
			case ProtocolMessages.QUIT:
				if (firstPlayersTurn) {
					winner = colorPlayer2;
				} else {
					winner = colorPlayer1;
				}
				endGameMessage = messageGenerator.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				goClientHandlerPlayer1.sendMessageToClient(endGameMessage);
				goClientHandlerPlayer2.sendMessageToClient(endGameMessage);
				break;
			//in case of a disconnect, only the still-connected player gets a message
			case ProtocolMessages.DISCONNECT:
				if (firstPlayersTurn) {
					winner = colorPlayer2;
					endGameMessage = messageGenerator.endGameMessage(reasonGameEnd, winner, 
							Double.toString(scoreBlack), Double.toString(scoreWhite));
					goClientHandlerPlayer2.sendMessageToClient(endGameMessage);
				} else {
					winner = colorPlayer1;
					endGameMessage = messageGenerator.endGameMessage(reasonGameEnd, winner, 
							Double.toString(scoreBlack), Double.toString(scoreWhite));
					goClientHandlerPlayer1.sendMessageToClient(endGameMessage);
				}
				break;
			default:
				//this should never happen, print message for debugging
				String errorMessage = messageGenerator.errorMessage("Sorry, the winner cannot be "
					+ "decided. Reason game end (" + reasonGameEnd + ") is unknown.", version);
				goClientHandlerPlayer1.sendMessageToClient(errorMessage);
				goClientHandlerPlayer2.sendMessageToClient(errorMessage);
		}
	}
}
