package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import protocol.ProtocolMessages;
import ruleimplementations.*;

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
	 * When second player connects, try to send startGame message to the first player to check
	 * whether he/she didn't disconnect while waiting for the second player.
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
			return message;
		}
		
		//Check whether player1 has disconnected by sending the start message in two parts (if
		//disconnected, the second flush will give an IO exception)
		try {
			String startMessage1part1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER;
			out1.write(startMessage1part1);
			out1.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Need to wait, otherwise it does not go into the exception
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		//If the other has disconnected, set this player as player 1
		try {
			String startMessage1part2 = board + ProtocolMessages.DELIMITER + colorPlayer1;
			out1.write(startMessage1part2);
			out1.newLine();
			out1.flush();
		} catch (IOException e) {
			//player 1 has disconnected
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
			message = "Player 1 disconnected, " + name + " was added to game " + 
									gameNumber + " as the first player.";
			return message;
		}
		
		//if there is already a connected player1, set this player as player2
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
		//Send start messages to the players 
//		String startMessage1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
//				+ board + ProtocolMessages.DELIMITER + colorPlayer1;
//		sendMessageToClient(startMessage1, out1);
		String startMessage2 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer2;
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
	 * Check whether first component of the reply is one character & is not 'Q' (for quit)
	 * If this is the case (protocol is kept), send the move (second component) to 
	 * processMove()
	 */
	public void doTurn() {

		String reply;
		String move = "";
		
		/** Send turn message to client & wait for reply. */
		String turnMessage = ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board + 
						ProtocolMessages.DELIMITER + opponentsMove;
		if (firstPlayersTurn) {
			reply = sendAndReceiveMessage(turnMessage, out1, in1);
		} else {
			reply = sendAndReceiveMessage(turnMessage, out2, in2);
		}
		
		/** End game if client disconnected. */
		if (reply == null) {
			reasonGameEnd = ProtocolMessages.DISCONNECT;
			gameEnded = true;
			return;
		}
		
		/** Check 1st component of the reply. If of length 1 and not Q, send to 'processMove()'. */
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
		
		/** Turn is over and processed: set turn to other player. */
		if (firstPlayersTurn) {
			firstPlayersTurn = false;
		} else {
			firstPlayersTurn = true;
		}
	}
	
	/**
	 * Process the move that was received.
	 * Move was already extracted from the message.
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
		
		/** 
		 * Check whether the player passed. 
		 * If so, check whether it is the second pass. If it is: set gameEnded to true
		 * to break out of doTurn loop. 
		 * Whether second pass or not: send valid move message back to the player.
		 */
		if (move.equals(Character.toString(ProtocolMessages.PASS))) {
			if (passed) {
				gameEnded = true;
				reasonGameEnd = ProtocolMessages.FINISHED;
			} else {
				passed = true;
			}
		} else {
			/** 
			 * If player did not pass, do validity checks and update board.
			 * 
			 * First validity checks check whether move is an integer, falls within the board
			 * and points to an unoccupied location.
			 * Next, the stone is added to the board and stones are removed if necessary.
			 * Finally, it is checked whether the move results in a replication of a previous board.
			 */
			passed = false;
			
			prevBoards.add(board);
			
			if (firstPlayersTurn) {
				valid = moveValidator.processMove(move, boardDimension, board, 
															colorPlayer1, prevBoards);
			} else {
				valid = moveValidator.processMove(move, boardDimension, board, 
															colorPlayer2, prevBoards);
			}
			
			/**
			 * If the move was valid, add the previous board to the prevBoards list
			 * and update the current board.
			 */
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
		
		opponentsMove = move;
		giveResult(valid);
	}
	
	/**
	 * Set the result message and send to the correct player.
	 * @param validness, character indicating a valid or an invalid move
	 * @param newBoard, String-representation of the new board.
	 */
	
	public void giveResult(boolean valid) {
		String resultMessage = null;
		
		/** Set result message. */
		if (valid) {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ ProtocolMessages.VALID + ProtocolMessages.DELIMITER + board;
		} else {
			//TODO different message with different invalid reasons?
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ ProtocolMessages.INVALID + ProtocolMessages.DELIMITER + 
					"Your move was invalid. You lose the game.";
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
		if (reasonGameEnd == ProtocolMessages.DISCONNECT) {
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
