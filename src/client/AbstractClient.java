package client;

import java.util.ArrayList;
import java.util.List;
import com.nedap.go.gui.GoGUIIntegrator;

import protocol.MessageGenerator;
import protocol.ProtocolMessages;

/**
 * This class contains all methods that are shared between the different players.
 * All implemented players extend this class.
 */

public abstract class AbstractClient implements Client {

	//variables used to start a connection with the server
	protected ClientTUI clientTUI;
	protected ServerHandler serverHandler;
	private MessageGenerator messageGenerator;
	
	//variables to play a game
	private char color;
	private int boardDimension;
	protected GoGUIIntegrator gogui;
	private String version;
	public static final double KOMI = 0.5;
	
	/** The board and all previous boards, represented as strings. */
	private List<String> prevBoards;
	
	/** Variables to keep track of game states. */
	private boolean gameEnded;
	private boolean doublePass;
	private boolean misunderstood;
	
	private String prevServerMessage;

	public AbstractClient() {
		clientTUI = new ClientTUI();
		serverHandler = new ServerHandler(clientTUI);
		messageGenerator = new MessageGenerator();
	}
	
	/**
	 * Start method.
	 * 
	 * Creates a connection with a server and sends a handshake. Then waits for server input
	 * and handles it until the game ends.
	 */
	public void start() {
		
		gameEnded = false;
		doublePass = false;
		misunderstood = false;
		prevBoards = new ArrayList<String>();
		
		serverHandler.createConnectionWithUserInput();
		if (!serverHandler.getSuccessfulConnection()) {
			serverHandler.closeConnection();
			return;
		}
		doHandshake();
		if (!serverHandler.getSuccessfulHandshake()) {
			serverHandler.closeConnection();
			return;
		}
		
		version = serverHandler.getVersion();
		if (version == null) { 
			return; //server has disconnected
		} 
		
		/** Play the game. */
		while (!gameEnded) {
			String message = serverHandler.readLineFromServer();
			if (message == null) {
				return; //server has disconnected
			}
			handleServerMessage(message);
		}
	}
	
	/**
	 * Do handshake, implementation depends on the client type.
	 */
	public abstract void doHandshake();
	
	/**
	 * Handles messages received from the server.
	 * 
	 * Checks whether all necessary components are there and sends them to the correct player method
	 * .
	 * @param message, a String received from the server.
	 */
	public void handleServerMessage(String message) {
		String[] components = message.split(ProtocolMessages.DELIMITER);
		
		//First part of a server message should one character
		if (components[0].length() != 1) {
			serverHandler.sendToGame(messageGenerator.errorMessage("Protocol error: " +
					"First component of the message is not a single character.", version));
		}
		
		char command = components[0].charAt(0);
		switch (command) {
			case ProtocolMessages.ERROR:
				//if an error message is received twice in a row:
				if (misunderstood) {
					clientTUI.showMessage("Server did not understand for the second time. We cannot"
							+ " communicate. We will send 'quit' to end the game. Sorry!");
					serverHandler.sendToGame(Character.toString(ProtocolMessages.QUIT));
				} else {
					misunderstood = true;
				}
				clientTUI.showMessage("Server did not understand the message. Let's try again.");
				handleServerMessage(prevServerMessage); //whether this works depends on server
				break;
				
			case ProtocolMessages.GAME:
				misunderstood = false;
				
				if (components.length < 3) {
					serverHandler.sendToGame(messageGenerator.errorMessage("Server response does " +
						"not comply with the protocol in the start game message. It did not " +
						"contain the board and your color.", version));
				} 
				String board = components[1];
				String assignedColor = components[2];
				
				startGame(board, assignedColor);
				break;
				
			case ProtocolMessages.TURN:
				misunderstood = false;
				
				if (components.length < 3) {
					serverHandler.sendToGame(messageGenerator.errorMessage("Server response does " +
						"not comply with the protocol in the turn message. It did not contain " +
						"the board and the opponent's move.", version));
				}
				board = components[1];
				String opponentsMove = components[2];
				
				doMove(board, opponentsMove);
				break;
				
			case ProtocolMessages.RESULT:
				misunderstood = false;
				
				if (components.length < 2) {
					serverHandler.sendToGame(messageGenerator.errorMessage("Server response does " +
						"not comply with the protocol in the result message. It did not contain " +
						"the validness of the move.", version));
				}
				String validity = components[1];
				String boardOrMessage = (components.length > 1) ? components[2] : null;
				
				getResult(validity, boardOrMessage, doublePass);
				break;
				
			case ProtocolMessages.END:
				misunderstood = false;
				
				if (components.length < 5) {
					serverHandler.sendToGame(messageGenerator.errorMessage("Server response does " +
						"not comply with the protocol in the end game message. It did not contain" +
						" the necessary five components in the endGame message.", version));
				}
				
				String reasonEnd = components[1];
				String winner = components[2];
				String scoreBlack = components[3];
				String scoreWhite = components[4];
				
				endGame(reasonEnd, winner, scoreBlack, scoreWhite);
				break;
				
			default:
				serverHandler.sendToGame(messageGenerator.errorMessage("Server response does " +
					"not comply with the protocol: the first component of the last server message" +
					" (" + message + ") is not a known command.", version));
				break;
		}
		prevServerMessage = message;
	}
	
	/**
	 * Check the components start message, let client know the game has started and start the GUI.
	 */
	
	public void startGame(String board, String assignedColor) {
		
		//add board, else 2nd player doesn't save the empty board. 1st player will save it twice
		prevBoards.add(board);
		
		// Verify that the board contains only U, B and W. 
		int numberOfIntersections = board.length();
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
		 * Verify that the assigned color is either ProtocolMessages.WHITE or .BLACK.
		 */
		
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
		 * Show start message to client containing the assigned color.
		 */
		String clientsColor = "";
		if (color == 'W') {
			clientsColor = "white";
		} else {
			clientsColor = "black";
		}
		clientTUI.showMessage("The game has started! "
				+ "The board is " + boardDimension + " by " + boardDimension + ". "
				+ "Your color is " + clientsColor + ". Good luck!");
		
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
	 * Show the current board state in the GUI and ask the client for a move. 
	 * Send appropriate message with the move to the server.
	 */
	public void doMove(String board, String opponentsMove) {
		showCurrentBoardState(board);
		prevBoards.add(board);
		
		String move = getMove(opponentsMove, boardDimension, board, color, prevBoards);
		
		boolean opponentPassed = opponentsMove.equals(Character.toString(ProtocolMessages.PASS));
		if (move.equals(Character.toString(ProtocolMessages.PASS))) {
			if (opponentPassed == true) {
				doublePass = true;
			}
		} else if (move.equals(Character.toString(ProtocolMessages.QUIT))) {
			serverHandler.sendToGame(Character.toString(ProtocolMessages.QUIT));
			return;
		}
		
		/** Send move to the game. */
		serverHandler.sendToGame(messageGenerator.moveMessage(move));
	}
	
	/**
	 * Get a valid move, should be implemented in all subclasses.
	 * 
	 * @return
	 */
	public abstract String getMove(String theOpponentsMove, int theBoardDimension, 
										String theBoard, char theColor, List<String> thePrevBoards);
	
	/**
	 * 
	 * @param validity, boolean that is true if the move is valid, otherwise false
	 * @param boardOrMessage, a String containing the board (if valid move) or 
	 * 				(if invalid move) null or a message
	 * @param theDoublePass, boolean indicating whether there were were two subsequent passes
	 */
	public void getResult(String validity, String boardOrMessage, boolean theDoublePass) {
		
		if (Character.toString(ProtocolMessages.VALID).equals(validity)) {
			showCurrentBoardState(boardOrMessage);
			prevBoards.add(boardOrMessage);
		} else {
			clientTUI.showMessage("Your move was invalid. You lose the game.");
			if (boardOrMessage != null) {
				clientTUI.showMessage("Message from the server: " + boardOrMessage);
			}
		}
	}
	
	/**
	 * Communicate the end game and the result to the player.
	 * 
	 * @param reasonEnd, a one-character string indicating the reason for the end of game
	 * @param winner, a one-character string indicating who won the game
	 */
	public void endGame(String reasonEnd, String winner, String scoreBlack, String scoreWhite) {
		
		gameEnded = true;
		
		switch (reasonEnd.charAt(0)) {
			case ProtocolMessages.FINISHED:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("\nCongratulations, you won the game! Score black: " + 
							scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("\nToo bad, you lost the game after two consecutive pas" +
						"ses! Score black: " + scoreBlack + ", score white: " + scoreWhite + ".");
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
		
		//A wait step here prevents not fully clearing the board before filling it again
		//However, it also results in not being able to follow the computer player games
//		try {
//			TimeUnit.SECONDS.sleep(1);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
		
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
