package server;

import java.util.*;

import protocol.ProtocolMessages;
import ruleimplementations.*;

public class Game {
	/** Set board dimension (= length of board). */
	//TODO possibly let user of server set this instead
	private int boardDimension = 5;
	
	/** Save the game number, might need it later for a leader board. */
	private int gameNumber;
	private String version;
	
	/** Variable to keep track of and connect to the players. */
	private String namePlayer1 = null; //name of player1
	private String namePlayer2 = null; //name of player2
	private char colorPlayer1 = 'x'; //color of player1
	private char colorPlayer2 = 'x'; //color of player2
	
	private Handler goClientHandlerPlayer1;
	private Handler goClientHandlerPlayer2;
	private Handler currentPlayersHandler;
	
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
	
	/** Set classes to check move results. */
	private MoveValidator moveValidator = new MoveValidator();
	private MoveResultGenerator moveResult = new MoveResultGenerator();
	
	/** 
	 * Constructor, sets game number & creates string representation of the board. 
	 */
	public Game(int number, String chosenVersion) {
		gameNumber = number;
		version = chosenVersion;
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
		goClientHandlerPlayer2.startGameMessage(board, colorPlayer2);
		
		//Send first turn to the player whose stones are black
		if (colorPlayer1 == ProtocolMessages.BLACK) {
			firstPlayersTurn = true;
			doTurn();
		} else {
			firstPlayersTurn = false;
			doTurn();
		}
	}
	
	/**
	 * Send message to a player to tell him/her that its their turn & receive reply.
	 * Check whether first component of the reply is one character & is not 'Q' (for quit)
	 * If this is the case (protocol is kept), send the move (second component) to 
	 * processMove()
	 */
	public void doTurn() {

		String reply;
		String move = "";
		
		if (firstPlayersTurn) {
			currentPlayersHandler = goClientHandlerPlayer1;
		} else {
			currentPlayersHandler = goClientHandlerPlayer2;
		}
		reply = currentPlayersHandler.doTurnMessage(board, opponentsMove);
		
		// End game if client disconnected. 
		if (reply == null) {
			reasonGameEnd = ProtocolMessages.DISCONNECT;
			gameEnded = true;
			return;
		}
		
		// Check 1st component of the move message received from the player. 
		String[] components = reply.split(ProtocolMessages.DELIMITER);
		if (components[0].length() != 1) {
			//TODO not sure what to do after sending an error message. doTurn() again?
			currentPlayersHandler.errorMessage("Player did not keep to the protoc l: the first "
						+ "part of its move message ( " + reply + ") was not a single character.");
			return;
		}
		
		char command = components[0].charAt(0);
		switch (command) {
			case ProtocolMessages.QUIT:
				reasonGameEnd = ProtocolMessages.QUIT;
				gameEnded = true;
				return;
			case ProtocolMessages.MOVE:
				move = components[1];
				break;
			case ProtocolMessages.ERROR:
				//TODO what to do if the player did not understand a message? End game?
			default:
				//TODO not sure what to do after sending an error message. doTurn() again?
				currentPlayersHandler.errorMessage("Player did not keep to the protocol: '"
					+ ProtocolMessages.QUIT + "', '" + ProtocolMessages.ERROR + "' or '" +
					ProtocolMessages.MOVE + "' expected as first part of the message, but '" +
					command + "' received.");
				return;
		}
		
		processMove(move);
		
		/** Turn is over and processed: set turn to other player if game has not ended. */
		if (!gameEnded) {
			if (firstPlayersTurn) {
				firstPlayersTurn = false;
			} else {
				firstPlayersTurn = true;
			}
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
		
		// Send the result to the current player.
		if (firstPlayersTurn) {
			goClientHandlerPlayer1.giveResultMessage(valid, message);
		} else {
			goClientHandlerPlayer2.giveResultMessage(valid, message);
		}
	}
	
	/**
	 * End the game. 
	 * 
	 * @param reason One of the ProtocolMessages indicating the reason for ending the game
	 */
	public void endGame() {
		
		char winner = 'x';
		double scoreBlack = 0;
		double scoreWhite = 0;
		
		switch (reasonGameEnd) {
			//if game ended after a double pass, decide on winner based on the scores
			case ProtocolMessages.FINISHED:
				//TODO change to actual winner
				winner = 'B';
				goClientHandlerPlayer1.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				goClientHandlerPlayer2.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				break;
			//if one of the other game end reasons: the not-current player wins
			case ProtocolMessages.CHEAT:
			case ProtocolMessages.QUIT:
				if (firstPlayersTurn) {
					winner = colorPlayer2;
				} else {
					winner = colorPlayer1;
				}
				goClientHandlerPlayer1.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				goClientHandlerPlayer2.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				break;
			//in case of a disconnect, only the still-connected player gets a message
			case ProtocolMessages.DISCONNECT:
				if (firstPlayersTurn) {
					winner = colorPlayer2;
					goClientHandlerPlayer2.endGameMessage(reasonGameEnd, winner, 
							Double.toString(scoreBlack), Double.toString(scoreWhite));
				} else {
					winner = colorPlayer1;
					goClientHandlerPlayer1.endGameMessage(reasonGameEnd, winner, 
							Double.toString(scoreBlack), Double.toString(scoreWhite));
				}
				break;
			default:
				//TODO not sure what to do after sending the error messages. Maybe nothing, only
				//used for debugging?
				goClientHandlerPlayer1.errorMessage("Sorry, the winner cannot be decided. Known " +
						"reason game end (" + reasonGameEnd + ") is unknown.");
				goClientHandlerPlayer2.errorMessage("Sorry, the winner cannot be decided. Known " +
						"reason game end (" + reasonGameEnd + ") is unknown.");
		}
	}
}
