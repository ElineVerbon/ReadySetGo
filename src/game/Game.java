package game;

import java.util.*;

import movechecker.*;
import protocol.ProtocolMessages;
import server.GoClientHandler;

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
	GoClientHandler goClientHandler1; //access to player1
	GoClientHandler goClientHandler2; //access to player2
	
	/** Variable to keep track of game states. */
	private boolean complete = false; //two players have been added
	private boolean passed = false; //first pass has occurred
	private boolean firstPlayersTurn = true; //turn of player1
	private boolean gameEnded = false; //game has ended
	
	/** The board and all previous boards, represented as strings. */
	private String board;
	private List<String> prevBoards = new ArrayList<String>();
	
	/** Set classes to check move results. */
	private MoveValidator moveValidator = new MoveValidator();
	private MoveResult moveResult = new MoveResult();
	
	
	
	/** 
	 * Constructor, sets game number. 
	 */
	public Game(int number) {
		gameNumber = number;
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
	public String addPlayer(String name, GoClientHandler goClientHandler, String wantedColor) {
		String message = "";
		
		//if no player1 yet:
		if (namePlayer1 == null) {
			namePlayer1 = name;
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
			goClientHandler1 = goClientHandler;
			message = name + " was added to game " + gameNumber + " as the first player.";
		
		//if there is already a player in the game
		} else {
			namePlayer2 = name;
			goClientHandler2 = goClientHandler;
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
	 */
	public void runGame() {
		
		/** 
		 * Create a string representation of the empty board.
		 * Create a char array of a specified length, fills it with UNOCCUPIED
		 * and change it into a String.
		 */
        char[] charArray = new char[boardDimension * boardDimension];
        Arrays.fill(charArray, ProtocolMessages.UNOCCUPIED);
        board = new String(charArray);

		
		//TODO check whether both players are still connected
		
        /**
         * Send start game message to both players (via their clientHandler).
         * Include the string representation of the board and the assigned color.
         * 
         * PROTOCOL.GAME + PROTOCOL.DELIMITER + bord + PROTOCOL.DELIMITER + color
         */
		String startMessage1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer1;
		String startMessage2 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer2;
		goClientHandler1.sendMessageToClient(startMessage1);
		goClientHandler2.sendMessageToClient(startMessage2);
		
		/**
		 * Give the first turn to the black player to start the game.
		 * doTurn will also process the move and send the result back to the player.
		 * At the end, it will set the current player to the other player.
		 */
		if (colorPlayer1 == ProtocolMessages.BLACK) {
			firstPlayersTurn = true;
			doTurn();
		} else {
			firstPlayersTurn = false;
			doTurn();
		}
		
		/**
		 * Keep calling doTurn until the game has ended.
		 * doTurn() will ensure that the players are alternated.
		 */
		while (!gameEnded) {
			doTurn();
		}
		
		/**
		 * TODO end the game.
		 */
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
			reply = goClientHandler1.sendAndReceiveMessage(turnMessage);
		} else {
			reply = goClientHandler2.sendAndReceiveMessage(turnMessage);
		}
		
		String[] commands = reply.split(ProtocolMessages.DELIMITER);
		if (commands[0].length() != 1) {
			//TODO return invalid command
			return;
		}
		if (commands[0].equals("Q")) {
			//TODO give reason why the game is quit
			//TODO quit should be able to be called at any moment
			//now it is only possible in one's turn
			endGame(ProtocolMessages.QUIT);
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
		
		/** Check whether the player passed. */
		if (move.equals(Character.toString(ProtocolMessages.PASS))) {
			/** Check whether it is the second or the first pass. */
			if (passed == true) {
				//second pass: the game is over
				//TODO decide on how to do this depending on how we decide to in the protocol
				//i.e. end directly or first finish the turn & send the result
				gameEnded = true;
				endGame(ProtocolMessages.FINISHED);
			} else {
				passed = true;
				giveResult(validness);
			}
			return;
		} 
		
		/** If player did not pass. */
		passed = false;
		
		//check validity of the move, end game if not valid
		valid = moveValidator.checkValidityBeforeRemoving(move, boardDimension, board);
		if (!valid) {
			validness = ProtocolMessages.INVALID;
			giveResult(validness);
			endGame(ProtocolMessages.CHEAT);
			return;
		} 
		
		prevBoards.add(board);
		
		//parse move to an integer (it has already been checked whether that is possible)
		int location;
		location = Integer.parseInt(move);
		
		//determine what the board looks like after removing stones because of placing the new stone
		if (firstPlayersTurn) {
			board = board.substring(0, location) + colorPlayer1 + board.substring(location + 1);
			board = moveResult.determineNewBoard(board, colorPlayer1);
		} else {
			board = board.substring(0, location) + colorPlayer2 + board.substring(location + 1);
			board = moveResult.determineNewBoard(board, colorPlayer2);
		}
		
		//check whether the new board is not the same as a previous board
		valid = moveValidator.checkValidityAfterRemoving(board, prevBoards);
		if (!valid) {
			validness = ProtocolMessages.INVALID;
		} 
		giveResult(validness);
	}
	
	/**
	 * Set the result message and send to the correct player.
	 * @param validness, character indicating a valid or an invalid move
	 * @param newBoard, String-representation of the new board.
	 */
	
	public void giveResult(char validness) {
		String resultMessage = "";
		
		/** Set result message. */
		if (validness == ProtocolMessages.VALID) {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ validness + ProtocolMessages.DELIMITER + board;
		} else {
			//TODO different message with different invalid reasons?
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ validness + ProtocolMessages.DELIMITER + "Your move was invalid!";
		}
		
		/** Send the result to the correct player. */
		if (firstPlayersTurn) {
			goClientHandler1.sendMessageToClient(resultMessage);
		} else {
			goClientHandler2.sendMessageToClient(resultMessage);
		}
	}
	
	/**
	 * End the game. 
	 * 
	 * @param reason One of the ProtocolMessages indicating the reason for ending the game
	 */
	public void endGame(char reason) {
		//TODO add ProtocolMessages.DISCONNECT as a possible reason 
		//to end the game somewhere in the code
		
		String winner = "";
		String scoreBlack = "";
		String scoreWhite = "";
		
		//Decide on winner depending on why the game was ended
		switch (reason) {
			case ProtocolMessages.FINISHED:
				//TODO
				break;
			//if one of the other options: the not-current player wins
			case ProtocolMessages.CHEAT:
			case ProtocolMessages.DISCONNECT:
			case ProtocolMessages.QUIT:
				if (firstPlayersTurn) {
					winner = namePlayer2;
				} else {
					winner = namePlayer1;
				}
				break;
			default:
				//TODO should never get here. ProtocolException?
		}
		
		String endOfGameMessage = ProtocolMessages.END + ProtocolMessages.DELIMITER + reason + 
				ProtocolMessages.DELIMITER + winner + ProtocolMessages.DELIMITER + scoreBlack +
				ProtocolMessages.DELIMITER + scoreWhite;
		
		gameEnded = true;
		
		goClientHandler1.sendMessageToClient(endOfGameMessage);
		goClientHandler2.sendMessageToClient(endOfGameMessage);
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
	
	
}
