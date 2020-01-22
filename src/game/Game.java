package game;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import movechecker.*;
import protocol.ProtocolMessages;

public class Game {
	/** Set board dimension (= length of board). */
	//TODO possibly let user of server set this instead
	private int boardDimension = 5;
	
	/** Save the game number, might need it later for a leader board. */
	private int gameNumber;
	
	/** Variable to keep track of and connect to the players. */
	private String namePlayer1 = null; //name of player1
	private String namePlayer2 = null; //name of player2
	private char colorPlayer1 = 'x'; //color of player1
	private char colorPlayer2 = 'x'; //color of player2
	private BufferedReader in1;
	private BufferedWriter out1;
	private BufferedReader in2;
	private BufferedWriter out2;
	
	/** Variable to keep track of game states. */
	private boolean complete = false; //two players have been added
	private boolean passed = false; //first pass has occurred
	private boolean firstPlayersTurn = true; //turn of player1
	private boolean gameEnded = false; //game has ended
	private char reasonGameEnd;
	
	/** The board and all previous boards, represented as strings. */
	private String board;
	private List<String> prevBoards = new ArrayList<String>();
	
	/** Set classes to check move results. */
	private MoveValidator moveValidator = new MoveValidator();
	private MoveResult moveResult = new MoveResult();
	
	/** 
	 * Constructor, sets game number & creates string representation of the board. 
	 */
	public Game(int number) {
		gameNumber = number;
		
		// Create a string representation of the empty board.
        char[] charArray = new char[boardDimension * boardDimension];
        Arrays.fill(charArray, ProtocolMessages.UNOCCUPIED);
        board = new String(charArray);
	}
	
	/** 
	 * Adds a player to the game & sets the color of the player's stones.
	 * First player will get what he / she requested, otherwise BLACK, second player will get the 
	 * remaining color.
	 * 
	 * Is called by the server, will only be called if the game is not full yet.
	 * TODO need to synchronize?
	 * 
	 * @param name, the name of the player
	 * @param goClientHandler, the handler of the player
	 * @param wantedColor, the color requested by the player (null if not specified by the player)
	 * @return message, a String that shows the Server user that a player was added to the game
	 */
	public String addPlayer(String name, BufferedReader in, BufferedWriter out, 
																	String wantedColor) {
		String message = "";
		
		//if no player1 yet:
		if (namePlayer1 == null) {
			namePlayer1 = name;
			in1 = in;
			out1 = out;
			//if no provided wanted color (or of length 1, give black)
			if (wantedColor == null || wantedColor.length() != 1) {
				colorPlayer1 = ProtocolMessages.BLACK;
			} else {
				if (wantedColor.charAt(0) == ProtocolMessages.BLACK) {
					colorPlayer1 = ProtocolMessages.BLACK;
				} else if (wantedColor.charAt(0) == ProtocolMessages.WHITE) {
					colorPlayer1 = ProtocolMessages.WHITE;
				} else {
					colorPlayer1 = ProtocolMessages.BLACK;
				}
			}
			message = name + " was added to game " + gameNumber + " as the first player.";
		
		//if there is already a player in the game
		} else {
			namePlayer2 = name;
			in2 = in;
			out2 = out;
			//give player 2 the other color than player 1
			if (colorPlayer1 == ProtocolMessages.BLACK) {
				colorPlayer2 = ProtocolMessages.WHITE;
			} else {
				colorPlayer2 = ProtocolMessages.BLACK;
			}
			complete = true;
			message = name + " was added to game " + gameNumber + 
					" as the second player. The game can start!";
		}
		
		//return message to let user of the server know what has happened.
		return message;
	}
	
	/**
	 * Runs the game.
	 * Starts the game (send start messages, send first turn message), 
	 * keep sending turns to alternating players until game end. Then end game.
	 */
	public void runGame() {

		//TODO check whether both players are still connected
		
        // Send start game message to both players (via their clientHandler).
		startGame();
		
		// Keep calling doTurn until the game has ended.
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
		//Send start message to both players
		String startMessage1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer1;
		String startMessage2 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer2;
		sendMessageToClient(startMessage1, out1);
		sendMessageToClient(startMessage2, out2);
		
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
	 */
	public void doTurn() {
		// Set turn message.
		String turnMessage = ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board;
		String reply;
		String move = "";
		
		/** Send message to client & wait for reply. */
		if (firstPlayersTurn) {
			reply = sendAndReceiveMessage(turnMessage, out1, in1);
		} else {
			reply = sendAndReceiveMessage(turnMessage, out2, in2);
		}
		
		if (reply == null) {
			//if player disconnected, end game
			reasonGameEnd = ProtocolMessages.DISCONNECT;
			gameEnded = true;
			return;
		}
		
		String[] commands = reply.split(ProtocolMessages.DELIMITER);
		if (commands[0].length() != 1) {
			//TODO return invalid command
			return;
		}
		if (commands[0].equals("Q")) {
			//TODO quit should be able to be called at any moment
			reasonGameEnd = ProtocolMessages.QUIT;
			gameEnded = true;
			return;
		} else if (commands[0].equals(Character.toString(ProtocolMessages.MOVE))) {
			move = commands[1];
		} else if (commands[0].equals("?")) {
			//TODO
		} else {
			//TODO ProtocolException, not kept to protocol, send ? back.
		}
		processMove(move);
		
		//set turn to the other
		if (firstPlayersTurn) {
			firstPlayersTurn = false;
		} else {
			firstPlayersTurn = true;
		}
	}
	
	/**
	 * Process the move that was received.
	 * Move was already extracted from the message!
	 * 
	 * First check whether the player passed. If so:
	 * - if second pass: end the game
	 * - if first pass: move is valid, finish turn and continue the game
	 * 
	 * If player did not pass:
	 * - check validity of move (if not valid, end game)
	 * - determine the new board
	 * - check whether board has not been seen before (if it has: end game)
	 */
	
	public void processMove(String move) {
		
		boolean valid;
		char validness = ProtocolMessages.VALID;
		String invalidMessage = null;
		
		/** Check whether the player passed. */
		if (move.equals(Character.toString(ProtocolMessages.PASS))) {
			/** If second pass, game is over. Return result, then end doTurn loop. */
			if (passed) {
				//TODO decide on how to do this depending on how we decide to in the protocol
				//i.e. end directly or first finish the turn & send the result
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.FINISHED;
//				return; //depends on decision protocol
			} else {
				passed = true;
			}
		} else {
			/** If player did not pass. */
			passed = false;
			
			//check validity of the move
			valid = moveValidator.checkValidityBeforeRemoving(move, boardDimension, board);
			if (!valid) {
				validness = ProtocolMessages.INVALID;
				reasonGameEnd = ProtocolMessages.CHEAT;
				gameEnded = true;
				invalidMessage = "Your move was invalid (not an integer, outside of board, " +
							"or location already occupied).";
			} else {
			
				prevBoards.add(board);
				
				//parse move to an integer (it has already been checked whether that is possible)
				int location;
				location = Integer.parseInt(move);
				
				//determine what the board looks like after removing stones
				if (firstPlayersTurn) {
					board = board.substring(0, location) + colorPlayer1 + 
													board.substring(location + 1);
					board = moveResult.determineNewBoard(board, colorPlayer1);
				} else {
					board = board.substring(0, location) + colorPlayer2 + 
													board.substring(location + 1);
					board = moveResult.determineNewBoard(board, colorPlayer2);
				}
				
				//check whether the new board is not the same as a previous board
				valid = moveValidator.checkValidityAfterRemoving(board, prevBoards);
				if (!valid) {
					validness = ProtocolMessages.INVALID;
					reasonGameEnd = ProtocolMessages.CHEAT;
					gameEnded = true;
					invalidMessage = "Your move was invalid: it resulted in a board seen before.";
				} 
			}
		}
		giveResult(validness, invalidMessage);
	}
	
	/**
	 * Set the result message and send to the correct player.
	 * @param validness, character indicating a valid or an invalid move
	 * @param newBoard, String-representation of the new board.
	 */
	
	public void giveResult(char validness, String invalidMessage) {
		String resultMessage = null;
		
		/** Set result message. */
		if (validness == ProtocolMessages.VALID) {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ validness + ProtocolMessages.DELIMITER + board;
		} else {
			//TODO different message with different invalid reasons?
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ validness + ProtocolMessages.DELIMITER + invalidMessage;
		}
		
		/** Send the result to the correct player. */
		if (firstPlayersTurn) {
			sendMessageToClient(resultMessage, out1);
		} else {
			sendMessageToClient(resultMessage, out2);
		}
	}
	
	/**
	 * End the game. 
	 * 
	 * @param reason One of the ProtocolMessages indicating the reason for ending the game
	 */
	public void endGame() {
		//TODO add ProtocolMessages.DISCONNECT as a possible reason 
		//to end the game somewhere in the code
		
		char winner = 'x';
		int scoreBlack = 0;
		int scoreWhite = 0;
		
		//Decide on winner depending on why the game was ended
		switch (reasonGameEnd) {
			case ProtocolMessages.FINISHED:
				//TODO change to actual winner
				winner = 'B';
				break;
			//if one of the other options: the not-current player wins
			case ProtocolMessages.CHEAT:
			case ProtocolMessages.DISCONNECT:
			case ProtocolMessages.QUIT:
				if (firstPlayersTurn) {
					winner = colorPlayer2;
				} else {
					winner = colorPlayer1;
				}
				break;
			default:
				//TODO should never get here. ProtocolException?
		}
		
		String endOfGameMessage = ProtocolMessages.END + ProtocolMessages.DELIMITER + reasonGameEnd
				+ ProtocolMessages.DELIMITER + winner + ProtocolMessages.DELIMITER + 
				Integer.toString(scoreBlack) + ProtocolMessages.DELIMITER + 
				Integer.toString(scoreWhite);
		
		//in case of a disconnect, only send the end-of-game message to the not disconnected player
		if (reasonGameEnd == ProtocolMessages.DISCONNECT ) {
			if (firstPlayersTurn) {
				sendMessageToClient(endOfGameMessage, out2);
			} else {
				sendMessageToClient(endOfGameMessage, out1);
			}
		} else {
			//otherwise, send to both players
			sendMessageToClient(endOfGameMessage, out1);
			sendMessageToClient(endOfGameMessage, out2);
		}
		
		//TODO add possibility to play another game against the same player?
	}
	
	/**
	 * Calculates the scores of the two players
	 */
	
	
	/** returns whether two players were added to this game. */
	public boolean getCompleteness() {
		return complete;
	}
	
	/** return identified (=number) of the game. */
	public int getNumber() {
		return gameNumber;
	}
	
	/**
	 * Send message from game to client.
	 */
	public void sendMessageToClient(String msg, BufferedWriter out) {
		try {
			out.write(msg);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
	}
	
	/**
	 * Send message to and get message from client.
	 */
	public String sendAndReceiveMessage(String msg, BufferedWriter out, BufferedReader in) {
		String reply = "";
		try {
			out.write(msg);
			out.newLine();
			out.flush();
			reply = in.readLine();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
		return reply;
	}
	
	
	
}
