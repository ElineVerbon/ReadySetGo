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
	
	public boolean getTwoPlayers() {
		return twoPlayers;
	}
	
	/**
	 * Runs the game.
	 * Starts the game (send start messages, send first turn message), 
	 * keep sending turns to alternating players until game end. Then end game.
	 */
	public void runGame() {

		started = true;
		
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
			reply = goClientHandlerPlayer1.doTurnMessage(board, opponentsMove);
		} else {
			reply = goClientHandlerPlayer2.doTurnMessage(board, opponentsMove);
		}
		
		
		/** 
		 * End game if client disconnected. 
		 */
		if (reply == null) {
			reasonGameEnd = ProtocolMessages.DISCONNECT;
			gameEnded = true;
			return;
		}
		
		/** 
		 * Check 1st component of the reply. 
		 * If of length 1 and not Q, send to 'processMove()'. 
		 */
		String[] commands = reply.split(ProtocolMessages.DELIMITER);
		if (commands[0].length() != 1) {
			//TODO return invalid command
			return;
		}
		if (commands[0].equals("Q")) {
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
			
			/**
			 * Check validity of the move
			 */
			if (firstPlayersTurn) {
				valid = moveValidator.processMove(move, boardDimension, board, 
															colorPlayer1, prevBoards);
			} else {
				valid = moveValidator.processMove(move, boardDimension, board, 
															colorPlayer2, prevBoards);
			}
			
			/**
			 * If the move was valid, update the current board.
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
		String message = "";
		
		/** Set result message. */
		if (valid) {
			message = board;
		} else {
			message = "Your move was invalid. You lose the game.";
		}
		
		/** Send the result to the current player. */
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
		//TODO add ProtocolMessages.DISCONNECT as a possible reason 
		//to end the game somewhere in the code
		
		char winner = 'x';
		double scoreBlack = 0;
		double scoreWhite = 0;
		
		//Decide on winner depending on why the game was ended
		switch (reasonGameEnd) {
			case ProtocolMessages.FINISHED:
				//TODO change to actual winner
				winner = 'B';
				goClientHandlerPlayer1.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				goClientHandlerPlayer2.endGameMessage(reasonGameEnd, winner, 
						Double.toString(scoreBlack), Double.toString(scoreWhite));
				break;
			//if one of the other options: the not-current player wins
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
			//in case of a disconnect, only the other player gets a message
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
				if (firstPlayersTurn) {
					goClientHandlerPlayer1.errorMessage("Protocol exception: reason game end 'F', "
							+ "'C', 'D' or 'Q' expected, was: " + reasonGameEnd);
				} else {
					goClientHandlerPlayer2.errorMessage("Protocol exception: reason game end 'F', "
							+ "'C', 'D' or 'Q' expected, was: " + reasonGameEnd);
				}
		}
	}
}
