package client;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.nedap.go.gui.GoGUIIntegrator;

import protocol.*;
import ruleimplementations.MoveValidator;

/**
 * Class that allows someone play GO by using the console to input moves
 * and checking the status of the game on a GUI. 
 *
 * User needs to now the IP address of the server and the port on which the server
 * is listening to start the game.
 */

public class HumanClientGamePlayer {
	
	//variables used to start a connection with the server
	private HumanClientTUI clientTUI;
	private HumanClientServerCommunicator serverHandler;
	private MessageGenerator messageGenerator;
	
	//variables to play a game
	private char color;
	private int boardDimension;
	private GoGUIIntegrator gogui;
	private String version;
	
	/** The board and all previous boards, represented as strings. */
	private String board;
	private List<String> prevBoards;
	
	/** Set classes to check move results. */
	private MoveValidator moveValidator = new MoveValidator();
	
	/** Variables to keep track of game states. */
	boolean gameEnded;
	boolean doublePass;
	boolean misunderstood;
	boolean firstGame;

	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public HumanClientGamePlayer() {
		clientTUI = new HumanClientTUI();
		serverHandler = new HumanClientServerCommunicator(clientTUI);
		messageGenerator = new MessageGenerator();
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new HumanClientGamePlayer()).start();
	}
	
	/**
	 * Start method.
	 * 
	 * 1) Creates a connection with a server.
	 * 2) Sends a handshake as defined in the protocol. 
	 * 3) Waits for the start-game message, start playing
	 * While game did not end {
	 * 4) Waits for turn message, returns move
	 * 5) Waits for result message, prints results
	 * }
	 * 
	 * When errors occur, or when the user terminates a server connection, the
	 * user is asked whether a new connection should be made.
	 */
	public void start() {
		
		gameEnded = false;
		doublePass = false;
		misunderstood = false;
		prevBoards = new ArrayList<String>();
		
		/** Create a connection and do handshake. */
		serverHandler.startServerConnection();
		version = serverHandler.getVersion();
		if (version == null) { 
			return; //server has disconnected
		} 
		
		/** 
		 * Play the game
		 */
		while (!gameEnded) {
			String message = serverHandler.readLineFromServer();
			if (message == null) {
				return; //server has disconnected
			}
			handleServerMessage(message);
		}
		
		firstGame = false;
	}	
	
	/**
	 * Handles messages received from the server.
	 * @param message
	 */
	private void handleServerMessage(String message) {
		String[] components = message.split(ProtocolMessages.DELIMITER);
		
		//First part of a server message should one character
		if (components[0].length() != 1) {
			String errorMessage = messageGenerator.errorMessage("Protocol error: "
					+ "First component of the message is not a single character.", version);
			serverHandler.sendToGame(errorMessage);
		}
		
		char command = components[0].charAt(0);
		switch (command) {
			case ProtocolMessages.ERROR:
				//if an error message is received twice in a row:
				if (misunderstood) {
					clientTUI.showMessage("Server did not understand again. We cannot communicate. "
							+ "We will send 'quit' to end the game. Sorry!");
					serverHandler.sendToGame(Character.toString(ProtocolMessages.QUIT));
				}
				clientTUI.showMessage("Server did not understand the message. Please try again.");
				//TODO not sure what to do after this. Cannot go to another method, because I don't 
				//have the message components. Maybe I should save the previous message somewhere?
				break;
			case ProtocolMessages.GAME:
				misunderstood = false;
				startGame(components);
				break;
			case ProtocolMessages.TURN:
				misunderstood = false;
				doMove(components);
				break;
			case ProtocolMessages.RESULT:
				misunderstood = false;
				getResult(components);
				break;
			case ProtocolMessages.END:
				misunderstood = false;
				endGame(components);
				break;
			default:
				String errorMessage = messageGenerator.errorMessage("Protocol error: 1st component "
					+ "of the server message (" + command + ") is not a known command.", version);
				serverHandler.sendToGame(errorMessage);
				break;
		}
	}
	
	/**
	 * Method to handle the start message.
	 * Start message should be formatted as follows:
	 * ProtocolMessages.GAME + ProtocolMessages.DELIMITER + board + ProtocolMessages.DELIMITER + 
	 * 			ProtocolMessages.BLACK / ProtocolMessages.WHITE
	 * 
	 * Separates the messages and sets the color and dimensions of the board.
	 * Prints a welcome message to the client and starts the GUI.
	 */
	
	public void startGame(String[] components) {
		
		/** 
		 * The second component of the message should be a string representation of the board.
		 * This contains only U, B and W. 
		 */
		board = components[1];
		int numberOfIntersections = components[1].length();
		boardDimension = (int) Math.sqrt(numberOfIntersections);
		for (int intersection = 0; intersection < board.length(); intersection++) {
			String locationStatus = Character.toString(board.charAt(intersection));
			if  (!(locationStatus.equals("W") || locationStatus.equals("B") 
															|| locationStatus.equals("U"))) {
				String errorMessage = messageGenerator.errorMessage("ProtocolException in start "
					+ "game message: only 'B', 'W' and 'U' expected in the string representation "
					+ "of the board, but " + board + " received.", version);
				serverHandler.sendToGame(errorMessage);
			}
		}
		
		/** 
		 * The third component of the message should be either 
		 * ProtocolMessages.WHITE or ProtocolMessages.BLACK. 
		 */
		String assignedColor = components[2];
		if (assignedColor.length() != 1) {
			String errorMessage = messageGenerator.errorMessage("ProtocolException in start game "
				+ "message: 'B' or 'W' expected as third command, but " + assignedColor + 
				" received.", version);
			serverHandler.sendToGame(errorMessage);
		}
		color = assignedColor.charAt(0);
		if (!(color == 'W' || color == 'B')) {
			String errorMessage = messageGenerator.errorMessage("ProtocolException in start game "
				+ "message: 'B' or 'W' expected as third command, but " + assignedColor 
				+ " received.", version);
			serverHandler.sendToGame(errorMessage);
		}
		
		/**
		 * Send start message to client containing the assigned color.
		 */
		String clientsColor = "";
		if (color == 'W') {
			clientsColor = "white";
		} else {
			clientsColor = "black";
		}
		clientTUI.showMessage("\nThe game has started! "
				+ "The board is " + boardDimension + " by " + boardDimension + ". "
				+ "\nYour color is " + clientsColor + ". Good luck!");
		
		/**
		 * Start the GUI.
		 */
		if (gogui == null) {
			gogui = new GoGUIIntegrator(true, true, boardDimension);
		}
		gogui.startGUI();
		gogui.setBoardSize(boardDimension);
	}
	
	/**
	 * Get a valid move from the player via the console.
	 * 
	 * Called upon receiving a turn message from the server, formatted like this.
	 * ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board + ProtocolMessages.DELIMITER 
	 * 			+ opponentsLastMove
	 * 
	 * @param components, an array of strings extracted from the server's message
	 */
	public void doMove(String[] components) {
		
		//set game state variable for printing correct message after submitting move
		boolean opponentPassed = false;
		
		//Get all components of the message
		//TODO could check whether components are correct (see eg startGame())
		if (components.length < 3) {
			String errorMessage = messageGenerator.errorMessage("Server response does not comply "
				+ "with the protocol! It did not send the board and the opponent's move as part "
				+ "of the turn message.", version);
			serverHandler.sendToGame(errorMessage);
		}
		board = components[1];
		String opponentsMove = components[2];
		
		//TODO add the score here!
		/** 
		 * Let the player know its his/her turn and what the opponent did. 
		 */
		if (opponentsMove.equals("null")) {
			clientTUI.showMessage("\nYou get the first move! " + 
					"Please check the GUI for the board size.");
		} else {
			if (opponentsMove.equals(Character.toString(ProtocolMessages.PASS))) {
				opponentPassed = true;
				clientTUI.showMessage("\nThe other player passed. " + 
						"If you pass as well, the game is over.");
			} else {
				int location = Integer.parseInt(opponentsMove);
				clientTUI.showMessage("\nThe other player placed a stone in location " + location +
					". Please check the GUI for the current board state.");
			}
		}
		
		/** 
		 * Show the current board state in the GUI and add board to previous boards. 
		 */
		showCurrentBoardState(board);
		prevBoards.add(board);
		
		/** 
		 * Keep asking the client for a move, until a valid move is given. 
		 */
		String move = "";
		boolean validInput = false;
		String moveMessage = "";
		
		while (!validInput) {
			boolean valid;
			
			move = clientTUI.getMove();
			
			//If the move is a pass or quit, send appropriate message and break out of loop.
			if (move.equals(Character.toString(ProtocolMessages.PASS))) {
				if (opponentPassed == true) {
					doublePass = true;
				}
				moveMessage = messageGenerator.moveMessage(move);
				serverHandler.sendToGame(moveMessage);
				return;
			} else if (move.equals(Character.toString(ProtocolMessages.QUIT))) {
				serverHandler.sendToGame(Character.toString(ProtocolMessages.QUIT));
				return;
			}
			
			valid = moveValidator.processMove(move, boardDimension, board, color, prevBoards);
			if  (!valid) {
				clientTUI.showMessage("Your move was invalid. "
							+ "Please try again.");
				continue; //continue from start of while loop (ask for new input)
			} 
			validInput = true;
		}
		/** Send move to the game. */
		moveMessage = messageGenerator.moveMessage(move);
		serverHandler.sendToGame(moveMessage);
	}
	
	/**
	 * Get result from the server and update the GUI.
	 * 
	 * Called upon receiving a turn message from the server, formatted like this.
	 * ProtocolMessages.RESULT + ProtocolMessages.DELIMITER + ProtocolMessages.VALID 
	 * 			+ ProtocolMessages.DELIMITER + board
	 * 
	 * 								or
	 * 
	 * ProtocolMessages.RESULT + ProtocolMessages.DELIMITER + ProtocolMessages.INVALID 
	 * 			+ optionally ProtocolMessages.DELIMITER + message
	 * 
	 * @param components, an array of strings extracted from the server's message
	 */
	public void getResult(String[] components) {
		
		//check whether the other components are included and if so, use them
		if (components.length < 2) {
			String errorMessage = messageGenerator.errorMessage("Server response does not "
				+ "comply with the protocol in the result message. It did not contain the "
				+ "validness of the move.", version);
			serverHandler.sendToGame(errorMessage);
		}
		String validity = components[1];
		//commands[2] can be a board (if valid result), a message or nothing (if invalid result)
		String boardOrMessage = (components.length > 1) ? components[2] : null;
		
		/**
		 * Communicate the result to the client
		 */
		if (Character.toString(ProtocolMessages.VALID).equals(validity)) {
			prevBoards.add(boardOrMessage);
			clientTUI.showMessage("Your move was valid. Check GUI for what the board looks like.");
			showCurrentBoardState(boardOrMessage);
			if (!doublePass) {
				clientTUI.showMessage("It's now the other player's turn. Please wait.");
			}
		} else {
			clientTUI.showMessage("Your move was invalid. You lose the game.");
			if (boardOrMessage != null) {
				clientTUI.showMessage("Message from the server: " + boardOrMessage);
			}
		}
	}
	
	/**
	 * Get an end game message from the server.
	 * Communicate the result to the player.
	 * 
	 * Called upon receiving a turn message from the server, formatted like this.
	 * ProtocolMessages.END + ProtocolMessages.DELIMITER + reasonEnd + ProtocolMessages.DELIMITER 
	 *  + winner + ProtocolMessages.DELIMITER + scoreBlack + ProtocolMessages.DELIMITER + scoreWhite
	 * 
	 * @param components, an array of strings extracted from the server's message
	 */
	public void endGame(String[] components) {
		
		gameEnded = true;
		
		if (components.length < 5) {
			String errorMessage = messageGenerator.errorMessage("Server response does not comply " +
						"with the protocol! It did not contain the necessary five components " +
						"in the endGame message.", version);
			serverHandler.sendToGame(errorMessage);
		}
		
		String reasonEnd = components[1];
		String winner = components[2];
		String scoreBlack = components[3];
		String scoreWhite = components[4];
		
		//reasonEnd is one character
		switch (reasonEnd.charAt(0)) {
			case ProtocolMessages.FINISHED:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("\nCongratulations, you won the game! Score black: " + 
							scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("\nToo bad, you lost the game! Score black: " + 
							scoreBlack + ", score white: " + scoreWhite + ".");
				}
				break;
			case ProtocolMessages.DISCONNECT:
				clientTUI.showMessage("\nThe other player disconnected, you win! Score black " +
							"was: " + scoreBlack + ", score white: " + scoreWhite + ".");
				break;
			case ProtocolMessages.CHEAT:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("\nYou won the game, the other player cheated. " +
							"Score black: " + scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("\nOh no, you lost the game because of an invalid move. " 
							+ "Score black: " + scoreBlack + ", score white: " + scoreWhite + ".");
				}
				break;
			case ProtocolMessages.QUIT:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("\nYou won the game, the other player quit. Score black:"
							+ scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("\nToo bad, you lost the game because you quit. " 
							+ "Score black: " + scoreBlack + ", score white: " + scoreWhite + ".");
				}
				break;
		}
		boolean reply = clientTUI.getBoolean("Do you want to play another game?");
		if (reply) {
			start();
		} else {
			serverHandler.closeConnection();
		}
	}
	
	public void showCurrentBoardState(String theBoard) {
		/** Show the current board to the player. */
		gogui.clearBoard();
		
		//Need to wait, otherwise it doesn't always fully clear the board before filling it again
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		for (int c = 0; c < boardDimension * boardDimension; c++) {
			char thisLocation = theBoard.charAt(c);
			if (thisLocation == ProtocolMessages.WHITE) {
				//location = x + y * boardDimension
				gogui.addStone(c % boardDimension, c / boardDimension, true);
			} else if (thisLocation == ProtocolMessages.BLACK) {
				gogui.addStone(c % boardDimension, c / boardDimension, false);
			}
		}
	}
}

