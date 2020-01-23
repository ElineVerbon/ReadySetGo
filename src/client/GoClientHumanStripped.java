package client;

import java.io.IOException;
import java.util.*;

import com.nedap.go.gui.GoGUIIntegrator;

import exceptions.*;
import movechecker.MoveResult;
import movechecker.MoveValidator;
import protocol.*;

/**
 * Class that allows someone play GO by using the console to input moves
 * and checking the status of the game on a GUI. 
 *
 * User needs to now the IP address of the server and the port on which the server
 * is listening to start the game.
 */

public class GoClientHumanStripped {
	
	//variables used to start a connection with the server
	private GoClientHumanStrippedTUI clientTUI;
	private ServerHandlerForHumanClient serverHandler;
	private MessageGenerator messageGenerator;
	
	//variables to play a game
	private char color;
	private int boardDimension;
	private GoGUIIntegrator gogui;
	
	/** The board and all previous boards, represented as strings. */
	private String board;
	private List<String> prevBoards = new ArrayList<String>();
	
	/** Set classes to check move results. */
	private MoveValidator moveValidator = new MoveValidator();
	private MoveResult moveResult = new MoveResult();
	
	/** Variables to keep track of game states. */
	boolean gameEnded = false;
	boolean misunderstood = false;

	/**
	 * Constructs a new GoClient. Initializes the TUI.
	 * Does not initialize the GUI, as board size has to be known.
	 */
	public GoClientHumanStripped() {
		clientTUI = new GoClientHumanStrippedTUI(this);
		serverHandler = new ServerHandlerForHumanClient();
		messageGenerator = new MessageGenerator();
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new GoClientHuman()).start();
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
		//TODO Handle incoming messages starting with '?'
		
		/** Create a connection and do handshake. */
		serverHandler.startServerConnection(clientTUI);
		
		/** 
		 * Wait for start game message. 
		 * Upon reception of the message, set variables & let user know via console. 
		 * 
		 * Ecxeptions are handled in the method
		 */
		String message = serverHandler.readLineFromServer();

		
		/**
		 * Handle commands received.
		 * 
		 * No need to keep to certain order, that's the task of the server.
		 */
		handleServerMessage(message);
		
		//TODO handle message starting with '?'
		
		//When receiving the start game message, 
		if (message.charAt(0) == 'G') {
			try {
				startGame(message);
			} catch (ProtocolException e) {
				//TODO print specific error message as shown in startGame
				clientTUI.showMessage(e.getMessage());
				//TODO what to do when the protocol is not kept?
			} catch (ServerUnavailableException e) {
				clientTUI.showMessage("The server could not be reached for game start.");
				//TODO what to do when the server cannot be reached anymore? Try again? 
				//Close connection? Check other SUE in other places, handle same way)
			}
		} 
		
		try {
			playGame();
		} catch (ServerUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	/**
	 * Handles messages received from the server.
	 * @param message
	 */
	private void handleServerMessage(String message) {
		String[] components = message.split(ProtocolMessages.DELIMITER);
		
		//First part of a server message should one character
		if (components[0].length() != 1) {
			sendErrorMessage("Protocol error: First component of the message is not a single character.");
		}
		
		char command = components[0].charAt(0);
		switch (command) {
			case ProtocolMessages.ERROR:
				if (misunderstood) {
					clientTUI.showMessage("Server did not understand again. We cannot communicate. "
							+ "We will send 'quit' to end the game. Sorry!");
					serverHandler.sendToGame(Character.toString(ProtocolMessages.QUIT));
				}
				clientTUI.showMessage("Server did not understand the message. Please try again.");
				//TODO go to doTurn?
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
				sendErrorMessage("Protocol error: First component of the message is not a single character.");
				break;
		}
	}
	
	/**
	 * Method to handle the start message.
	 * Start message should be formatted as follows:
	 * PROTOCOL.GAME + PROTOCOL.DELIMITER + bord + PROTOCOL.DELIMITER + 
	 * 			ProtocolMessage.BLACK / ProtocolMessage.WHITE
	 * 
	 * Separates the messages and sets the color and dimensions of the board.
	 * Print a welcome message to the client.
	 */
	
	public void startGame(String[] components) {
		
		/** The second component of the message should be a string representation of the board. */
		board = components[1];
		int numberOfPlaces = components[1].length();
		boardDimension = (int) Math.sqrt(numberOfPlaces);
		
		/** 
		 * The third component of the message should be either 
		 * ProtocolMessages.WHITE or ProtocolMessages.BLACK. 
		 */
		String assignedColor = components[2];
		if (assignedColor.length() != 1) {
			clientTUI.showMessage(Integer.toString(assignedColor.length()));
			throw new ProtocolException("ProtocolException: "
					+ "'B' or 'W' expected as third command, but " + components[2] + " received.");
		}
		color = assignedColor.charAt(0);
		if (!(color == 'W' || color == 'B')) {
			throw new ProtocolException("ProtocolException: "
					+ "'B' or 'W' expected as third command, but " + components[2] + " received.");
		}
		
		String clientsColor = "";
		if (color == 'W') {
			clientsColor = "white";
		} else {
			clientsColor = "black";
		}
		clientTUI.showMessage("The game has started! "
				+ "The board is " + boardDimension + " by " + boardDimension + ". "
				+ "\nYour color is " + clientsColor + ". Good luck!");
		
		//start the GUI
		gogui = new GoGUIIntegrator(true, true, boardDimension);
		gogui.startGUI();
		gogui.setBoardSize(boardDimension);
	}
	
	/** 
	 * Method that waits for a message from the server and responds.
	 * It can receive a turn message, an end message or a ?.
	 * (Or a result message, but that is handled in doTurn().)
	 * 
	 * It ends when 'Protocol.Messages.END' is received.
	 * 
	 * Called at the end of the start() method of this client.
	 */
	public void playGame() throws ServerUnavailableException, ProtocolException {
		
		while (!gameEnded) {
			//Wait for a message from the server
			String line = "";
			line = readLineFromServer();
			
			//Split the message into parts
			String[] commands = line.split(ProtocolMessages.DELIMITER);
			//if the first component is not of length 1, server does not comply with the protocol
			if (commands[0].length() != 1) {
				throw new ProtocolException("Server response does not comply with the protocol! " + 
						"It did not send a char as the first part of its message.");
			}
			
			//check which kind of message is received
			char command = line.charAt(0);
			switch (command) {
				case 'T':
					if (commands.length < 3) {
						throw new ProtocolException("Server response does not comply with " +
								"the protocol! It did not send the board and the opponent's move " +
								"as part of it's turn message.");
					}
					String opponentsMove = commands[2];
					doMove(commands[1], opponentsMove);
					break;
				case 'R' :
					//check whether the other components are included and if so, use them
					if (commands.length < 2) {
						throw new ProtocolException("Server response does not comply with " +
								"the protocol! It did not contain at least one component after " +
								"the 'R' in the result message.");
					}
					String validity = commands[1];
					//commands[2] can be a board, a message or nothing
					String message = (commands.length > 1) ? commands[2] : null;
					
					getResult(validity, message);
					break;
				case 'E' :
					if (commands.length < 5) {
						throw new ProtocolException("Server response does not comply with " +
								"the protocol! It did not contain the necessary five components " +
								"in the endGame message.");
					}
					gameEnded = true; //break out of this loop, TODO not sure if necessary
					endGame(commands[1], commands[2], commands[3], commands[4]); //end the game
					return;
				case '?':
					//TODO
					break;
				default :
					throw new ProtocolException("Server did not stick to the response protocol! " + 
							"It did not send a T, R, E or ? as the first part of its message.");
			}
		}
		closeConnection();
	}
	
	/**
	 * Get a valid move from the user.
	 * 
	 * @param board
	 */
	public void doMove(String boardBeforeMove, String opponentsMove) {
		board = boardBeforeMove;
		
		//TODO add the score here!
		/** Let the player know its his/her turn and what the opponent did. */
		if (opponentsMove.equals("null")) {
			clientTUI.showMessage("\nYou get the first move! " + 
					"Please check the GUI for the board size.");
		} else {
			if (opponentsMove.equals(Character.toString(ProtocolMessages.PASS))) {
				clientTUI.showMessage("\nThe other player passed." + 
						"If you pass as well, the game is over.");
			} else {
				int location = Integer.parseInt(opponentsMove);
				clientTUI.showMessage("\nThe other player placed a stone in location " + location +
					". Please check the GUI for the current board state.");
			}
			
		}
		
		/** Show the current board to the player. */
		gogui.clearBoard();
		for (int c = 0; c < boardDimension * boardDimension; c++) {
			char thisLocation = board.charAt(c);
			if (thisLocation == ProtocolMessages.WHITE) {
				//location = x + y * boardDimension
				gogui.addStone(c % boardDimension, c / boardDimension, true);
			} else if (thisLocation == ProtocolMessages.BLACK) {
				gogui.addStone(c % boardDimension, c / boardDimension, false);
			}
		}
		
		/** Ask the client for a move, keep asking until a valid move is given. */
		String move = "";
		String message;
		boolean validInput = false;
		int location = 0;
		
		//add the board to the list of previous boards
		prevBoards.add(board);
		
		//Ask the client for a move until a valid move is given
		while (!validInput) {
			boolean valid;
			
			move = clientTUI.getMove();
			
			/** Check whether the player passed, if so, break out of loop (no move to check 
			 * for validity) and send return message. 
			 */
			if (move.equals(Character.toString(ProtocolMessages.PASS))) {
				break;
			} else if (move.equals(Character.toString(ProtocolMessages.QUIT))) {
				sendToGame(Character.toString(ProtocolMessages.QUIT));
				gameEnded = true;
				return;
			}
			
			/** 
			 * Check whether move is an integer within the board of an UNOCCUPIED location, 
			 * let user try again if this is not the case. 
			 */
			valid = moveValidator.checkValidityBeforeRemoving(move, boardDimension, board);
			if  (!valid) {
				clientTUI.showMessage("You move cannot be parsed to an Integer, is not within "
						+ "the board, or the location is already taken. Please try again.");
				continue; //breaks out of this iteration of while loop and starts over.
			} 
			
			/**
			 * Add stone to the board, remove captured stones and check whether this results 
			 * in a previously seen board. If so, let user try again.
			 */
			
			location = Integer.parseInt(move);
			
			//determine what the board looks like after removing stones
			board = board.substring(0, location) + color + board.substring(location + 1);
			board = moveResult.determineNewBoard(board, color);
			
			//check whether the new board is not the same as a previous board
			valid = moveValidator.checkValidityAfterRemoving(board, prevBoards);
			if  (!valid) {
				clientTUI.showMessage("Your move results in a board that has been seen before. "
							+ "Please try again.");
				//set board back to previous board, break out of iteration and try again
				board = boardBeforeMove;
				continue; 
			} 
			//Do not add the board to the previous boards, for security reasons, only the board
			//given by the server are added. Thus, it will be added when returned by the server
			//in the result message.
			validInput = true;
		}
		/** Send move to the game. */
		message = ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + move; 
		sendToGame(message);
	}
	
	public void getResult(String validChar, String message) {
		
		//communicate result to the client
		if (Character.toString(ProtocolMessages.VALID).equals(validChar)) {
			//move was valid: the message contains the board.
			prevBoards.add(message);
			clientTUI.showMessage("Your move was valid. Check GUI for what the board looks like.");
			gogui.clearBoard();
			//go through all chars in the board string
			for (int c = 0; c < boardDimension * boardDimension; c++) {
				char thisLocation = board.charAt(c);
				if (thisLocation == ProtocolMessages.WHITE) {
					//location = x + y * boardDimension
					gogui.addStone(c % boardDimension, c / boardDimension, true);
				} else if (thisLocation == ProtocolMessages.BLACK) {
					gogui.addStone(c % boardDimension, c / boardDimension, false);
				}
			}
			clientTUI.showMessage("It's now the other player's turn. Please wait.");
		} else {
			clientTUI.showMessage("Your move was invalid. You lose the game.");
			//show message if added by the server
			if (message != null) {
				clientTUI.showMessage("Message from the server: " + message);
			}
		}
	}
	
	public void endGame(String reasonEnd, String winner, String scoreBlack, String scoreWhite) {
		//reasonEnd is one character
		switch (reasonEnd.charAt(0)) {
			case ProtocolMessages.FINISHED:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("Congratulations, you won the game! Score black: " + 
							scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("Too bad, you lost the game! Score black: " + 
							scoreBlack + ", score white: " + scoreWhite + ".");
				}
				break;
			case ProtocolMessages.DISCONNECT:
				clientTUI.showMessage("The other player disconnected, you win! " + "Score black " +
							"was: " + scoreBlack + ", score white: " + scoreWhite + ".");
				break;
			case ProtocolMessages.CHEAT:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("You won the game, the other player cheated. Score black:"
							+ scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("Too bad, you lost the game because of an invalid move. " 
							+ "Score black: " + scoreBlack + ", score white: " + scoreWhite + ".");
				}
				break;
			case ProtocolMessages.QUIT:
				if (winner.equals(Character.toString(color))) {
					clientTUI.showMessage("You won the game, the other player quit. Score black:"
							+ scoreBlack + ", score white: " + scoreWhite + ".");
				} else {
					clientTUI.showMessage("Too bad, you lost the game because you quit. " 
							+ "Score black: " + scoreBlack + ", score white: " + scoreWhite + ".");
				}
				break;
		}
		closeConnection();
	}
	
	public void sendErrorMessage(String message) {
		String formattedMessage = messageGenerator.errorMessage(serverHandler.getVersion(), 
																						message);
		serverHandler.sendToGame(formattedMessage;
	}
	
	public void sendExit() throws ServerUnavailableException {
		char toServer = ProtocolMessages.EXIT;
		try {
			out.write(toServer);
		} catch (IOException e) {
			throw new ServerUnavailableException("Could not read "
					+ "from server.");
		}
		
		closeConnection();
	}
}

