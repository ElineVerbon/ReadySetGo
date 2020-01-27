package server;

import java.net.SocketTimeoutException;
import java.util.*;

import protocol.MessageGenerator;
import protocol.ProtocolMessages;
import ruleimplementations.*;

public class Game {
	/** Set board dimension (= length of board). */
	//TODO possibly let user of server set this instead
	private int boardDimension = 5;
	private double komi = 0.5;
	
	/** Save the game number, might need it later for a leader board. */
	private int gameNumber;
	private String version;
	
	/** Variable to keep track of and connect to the players. */
	private String namePlayer1 = null; //name of player1, used by server
	private String namePlayer2 = null; //name of player2, used by server
	private char colorPlayer1 = 'x'; //color of player1
	private char colorPlayer2 = 'x'; //color of player2
	
	private Handler goClientHandlerPlayer1;
	private Handler goClientHandlerPlayer2;
	private Handler currentPlayersHandler;
	private MessageGenerator messageGenerator;
	
	/** Variable to keep track of game states. */
	private boolean twoPlayers = false; //two players have been added
	private boolean started = false;
	private boolean passed = false; //first pass has occurred
	private boolean firstPlayersTurn = true; //turn of player1
	private boolean gameEnded = false; //game has ended
	private char reasonGameEnd;
	private String opponentsMove = null;
	
	/** The board and all previous boards, represented as strings. */
	private String board;
	private List<String> prevBoards = new ArrayList<String>();
	
	/** Make objects of the classes that implement the GO rules. */
	private MoveValidator moveValidator = new MoveValidator();
	private BoardUpdater moveResult = new BoardUpdater();
	private ScoreCalculator scoreCalculator = new ScoreCalculator();
	
	/** 
	 * Constructor, creates string representation of the board. 
	 */
	public Game(int number, String chosenVersion) {
		gameNumber = number;
		version = chosenVersion;
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
     * Send start game message to both players (via their clientHandler).
     * Include the string representation of the board and the assigned color.
     * PM.GAME + PM.DELIMITER + board + PM.DELIMITER + color
     * 
     * Give the first turn to the player who plays with black to start the game.
	 * doTurn will also process the move and send the result back to the player.
	 * At the end, it will set the current player to the other player.
     */
	public void startGame() {
		//Send start message to player 2 
		String startGameMessage = messageGenerator.startGameMessage(board, colorPlayer2);
		goClientHandlerPlayer2.sendMessageToClient(startGameMessage);
		
		//Send first turn to the player whose stones are black
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
	 * Send message to a player to tell him/her that its their turn.
	 */
	public void doTurn() {
		if (firstPlayersTurn) {
			currentPlayersHandler = goClientHandlerPlayer1;
		} else {
			currentPlayersHandler = goClientHandlerPlayer2;
		}
		
		String doTurnMessage = messageGenerator.doTurnMessage(board, opponentsMove);
		currentPlayersHandler.sendMessageToClient(doTurnMessage);
	}
	
	/**
	 * Only process a reply if a player responded within 1 minute (no SocketTimeoutException).
	 * If so: check whether first component of a player's reply is one character & is not 'Q' 
	 * (for quit). If this is the case (protocol is kept), send the move (second component) to 
	 * processMove()
	 */
	public void processReply() {
		String reply = "";
		String move = "";
		
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
			return;
		}
		
		// End game if client disconnected. 
		if (reply == null) {
			reasonGameEnd = ProtocolMessages.DISCONNECT;
			gameEnded = true;
			return;
		}
		
		// Check 1st component of the move message received from the player is of length 1. 
		String[] components = reply.split(ProtocolMessages.DELIMITER);
		if (components[0].length() != 1) {
			//TODO not sure what to do after sending an error message. doTurn() again?
			String errorMessage = messageGenerator.errorMessage("Player did not keep to "
					+ "the protocol: the first part of its move message ( " + reply + ") "
					+ "was not a single character.", version);
			currentPlayersHandler.sendMessageToClient(errorMessage);
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
											"You did not understand our message, we have to stop.");
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
				return;
		}
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
	 * - determine the new board
	 * - check whether board has not been seen before (if it has: return move is invalid 
	 *   message and end game)
	 */
	
	public void processMove(String move) {
		
		boolean valid = true;
		
		// Check whether the player passed. 
		if (move.equals(Character.toString(ProtocolMessages.PASS))) {
			if (passed) {
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.FINISHED;
			} else {
				passed = true;
			}
		} else {
			// If player did not pass, do validity checks and update board.
			passed = false;
			
			prevBoards.add(board);
			
			// Check validity of the move
			if (firstPlayersTurn) {
				valid = moveValidator.processMove(move, boardDimension, board, 
															colorPlayer1, prevBoards);
			} else {
				valid = moveValidator.processMove(move, boardDimension, board, 
															colorPlayer2, prevBoards);
			}
			
			// If the move was valid, update the current board.
			if (valid) {
				int location;
				location = Integer.parseInt(move);
				if (firstPlayersTurn) {
					board = board.substring(0, location) + colorPlayer1 + 
													board.substring(location + 1);
					board = moveResult.determineNewBoard(board, colorPlayer1);
				} else {
					board = board.substring(0, location) + colorPlayer2 + 
													board.substring(location + 1);
					board = moveResult.determineNewBoard(board, colorPlayer2);
				}
			}
		}
		
		if (!valid) {
			gameEnded = true;
			reasonGameEnd = ProtocolMessages.CHEAT;
		}
		//give the result to the player
		opponentsMove = move;
		giveResult(valid);
	}
	
	/**
	 * Set the result message and send to the correct player.
	 * @param validness, character indicating a valid or an invalid move
	 * @param newBoard, String-representation of the new board.
	 */
	
	public void giveResult(boolean valid) {
		String message = "";
		
		// Set result message.
		if (valid) {
			message = board;
		} else {
			message = "Your move was invalid. You lose the game.";
		}
		String resultMessage = messageGenerator.resultMessage(valid, message);
		
		// Send the result to the current player.
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
		
		scoreCalculator.calculateScores(board, komi);
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
				//TODO not sure what to do after sending the error messages. Maybe nothing, only
				//used for debugging?
				String errorMessage = messageGenerator.errorMessage("Sorry, the winner cannot be "
					+ "decided. Reason game end (" + reasonGameEnd + ") is unknown.", version);
				goClientHandlerPlayer1.sendMessageToClient(errorMessage);
				goClientHandlerPlayer2.sendMessageToClient(errorMessage);
		}
	}
}
