package game;

import java.util.*;

import protocol.ProtocolMessages;
import server.GoClientHandler;

public class Game {
	
	//save the game number, might need it later for a leader board
	private int gameNumber;

	/** Variable to keep track of whose turn it is. */
	private boolean firstPlayersTurn = true;
	
	/** Variable to keep track of a possible pass. */
	private boolean passed = false;
	
	/** Save the names of the players. */
	private String namePlayer1 = null; //name of player1
	private String namePlayer2 = null; //name of player2
	
	/** Save the colors of the players. Set to x as default. */
	private char colorPlayer1 = 'x';
	private char colorPlayer2 = 'x';
	
	/** Save access to clientHandlers of the competing player. */
	GoClientHandler goClientHandler1;
	GoClientHandler goClientHandler2;
	
	/** Variable to save whether two players were added. */
	private boolean complete = false;
	
	/** Variable to keep track whether game is done. */
	private boolean gameEnded = false;
	
	/** The board, represented as a string. */
	private String board;
	
	/** Board dimension (= length of board. */
	//TODO decide whether I want to determine that here or elsewhere
	//maybe in server via user input?
	private int boardDimension = 3;
	
	/** All previous boards, to check whether a move is valid. */
	private List<String> prevBoards = new ArrayList<String>();
	
	
	
	public Game(int number) {
		gameNumber = number;
	}
	
	//responds with a message that indicates the gameNumber and the number of players.
	public String addPlayer(String name, GoClientHandler goClientHandler, String wantedColor) {
		String message = "";
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
		return message;
	}
	
	public void startGame() {
		//TODO where do I want to decide on the board dimension?
		
		/** Create a string representation of the empty board. */
		//create char array of specified length
        char[] charArray = new char[boardDimension * boardDimension];
        //fill all elements with the specified char
        Arrays.fill(charArray, 'U');
        //create string from char array and return
        board = new String(charArray);

		
		//TODO check whether both players are still connected
		
		//send start game message to both players (via they clientHandler)
		//PROTOCOL.GAME + PROTOCOL.DELIMITER + bord + PROTOCOL.DELIMITER + color
		String startMessage1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer1;
		String startMessage2 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer2;
		goClientHandler1.sendMessageToClient(startMessage1);
		goClientHandler2.sendMessageToClient(startMessage2);
		
		//send turn message to black player to start the game
		if (colorPlayer1 == ProtocolMessages.BLACK) {
			firstPlayersTurn = true;
			doTurn();
		} else {
			firstPlayersTurn = false;
			doTurn();
		}
		
		//TODO now keep waiting for messages & send messages yourself.
		while (!gameEnded) {
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
			reply = goClientHandler1.sendAndReceiveMessage(turnMessage);
//			reply = goClientHandler1.getMessageFromClient();
		} else {
			reply = goClientHandler2.sendAndReceiveMessage(turnMessage);
//			reply = goClientHandler2.getMessageFromClient();
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
			firstPlayersTurn = true;;
		}
	}
	
	/**
	 * Process the move that was received.
	 * Move was already extracted from the message!
	 * (Move was formatted as follows: ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + move)
	 */
	
	public void processMove(String move) {
		boolean valid;
		
		if (move == Character.toString(ProtocolMessages.PASS)) {
			if (passed == true) {
				//second pass: the game is over
				endGame(ProtocolMessages.FINISHED);
			} else {
				//first pass: set 'passed' to true
				//as a pass is a valid more, validness is set to true
				//return message with the new board (= the old board)
				passed = true;
				char validness = ProtocolMessages.VALID;
				giveResult(validness);
			}
		} else {
			//if not passed, set passed to false
			passed = false;
			//check validity of the move
			//board will be changed if the move is valid.
			valid = checkValidity(move);
			//if move is not valid, end the game (other player will win)
			if (!valid) {
				char validness = ProtocolMessages.INVALID;
				giveResult(validness);
				endGame(ProtocolMessages.CHEAT);
			} else {
				char validness = ProtocolMessages.VALID;
				//determine whether stones need to be removed due to the move & update board
				determineNewBoard();
				giveResult(validness);
			}
		}
	}
	
	/**
	 * Checks whether a move is valid. If so, changes the board according to the move. 
	 * 
	 * It is invalid if:
	 * 1. the move cannot be parsed to an integer
	 * 2. the move is not within the board
	 * 3. the location is not currently empty
	 * 4. the move results in a board that was seen before
	 * 
	 * NB: 4 is now only checked before checking the result of the move (ie, checking whether
	 * stones need to be removed). Should I also do this afterwards? Check in rules!
	 * 
	 * @param move
	 * @return validness, a boolean that is true is the move is valid, otherwise false
	 */
	
	public boolean checkValidity(String move) {
		boolean validness;
		int location;
		
		//Check whether the move can be parsed to an integer
		try {
			location = Integer.parseInt(move);
		} catch (NumberFormatException e) {
			validness = false;
			return validness;
		}
		
		//Check if the move is within the board
		if (location < 0 || location >= boardDimension * boardDimension) {
			validness = false;
			return validness;
		}
		
		//Check whether the location is currently empty
		if (board.charAt(location) != ProtocolMessages.UNOCCUPIED) {
			validness = false;
			return validness;
		}
		
		//Set location to player's color
		String newBoard = "";
		if (firstPlayersTurn) {
			newBoard = board.substring(0, location) + colorPlayer1 + board.substring(location + 1);
		} else {
			newBoard = board.substring(0, location) + colorPlayer2 + board.substring(location + 1);
		}
		
		//add previous board to the array of previous boards
		prevBoards.add(board);
		//check whether the move results in a board that was seen before
		for (String aPrevBoard : prevBoards) {
			if (newBoard.equals(aPrevBoard)) {
				validness = false;
				return validness;
			}
		}
		
		//TODO should I also check this after possibly removing stones?
		
		//move is valid, set newBoard to current board and return true
		board = newBoard;
		validness = true;
		return validness;
	}
	
	/**
	 * Check whether stones need to be removed due to the move.
	 * 
	 * This consists of three subsequent methods
	 * 1. determine new board. This will loop through all locations on the board
	 * and will call:
	 * 2. checkAllNeighbors. gets all neighboring locations and for the locations 
	 * that have not yet been checked, it will call:
	 * 3. checkColor. checks whether the color is of the currently being looked-for
	 * color. If so, it is added to the group and the neighbors of this locations
	 * will be checked via a call to 2. If it is unoccupied, the boolean variable 
	 * surrounded for this group is set to false.
	 */
	
	//set a few variables that will be used in the following methods
	//to check the effect of a move
	private boolean surrounded;
	private char currentlyCheckedColor = 'x';
	
	//variable to keep track of places checked per group (cleared at end of group)
	private List<Integer> checkedPlaces;
	//variable to keep track of stones that are part of the currently checked group 
	//(cleared at end of group)
	private List<Integer> surroundedStones;
	//variable to keep track of this colored stones that have been checked (cleared
	//at the end of looking through this color
	private List<Integer> checkedStonesThisColor;
	
	public void determineNewBoard() {
		
		//first check the opponent's color, capturing of a group goes before suicide
		if (firstPlayersTurn) {
			currentlyCheckedColor = colorPlayer2;
		} else {
			currentlyCheckedColor = colorPlayer1;
		}
		
		checkedPlaces = new ArrayList<Integer>();
		surroundedStones = new ArrayList<Integer>();
		checkedStonesThisColor = new ArrayList<Integer>();
		
		//go from top left to bottom right to check for capture of a group of opponents
		for (int x = 0; x < boardDimension; x++) {
			for (int y = 0; y < boardDimension; y++) {
				//get the corresponding number of the string representation. 
				//only check it if it has not been checked as part of another group yet
				int numberInStringRepresentation = x  + y * boardDimension;
				if (checkedStonesThisColor.contains(numberInStringRepresentation)) {
					break;
				}
				checkedPlaces.add(numberInStringRepresentation);
				
				//check whether this location has a stone of the currently checked color
				if (board.charAt(numberInStringRepresentation) == currentlyCheckedColor) {
					//set surrounded to true (will be set to false if unoccupied neighbor is found)
					surrounded = true;
					surroundedStones.clear();
					checkedPlaces.clear();
					
					//check all surrounding stones
					checkAllNeighbors(numberInStringRepresentation);
				}
				
				//if surrounded is true at this point, the current group is surrounded
				//and each of its stones should be removed.
				if (surrounded == true) {
					for (int location : surroundedStones) {
						board = board.substring(0, location) + ProtocolMessages.UNOCCUPIED
								 + board.substring(location + 1);
					}
				}
			}
		}
		
		//go from new stone to check capture of a group of one's one color
		
		
		//TODO update board
		//TODO check again whether board is the same as previous boards
	}
	
	/**
	 * Check whether a stone is (part of a group that is) surrounded.
	 * 
	 * Go to all neighboring locations. If:
	 * - also of this color: add to group, go to all neighboring locations
	 * - of other color: go to next location
	 * - unoccupied: set surrounded to false, but keep looking to have all stones of a group.
	 * @param numberInStringRepresentation
	 * @return
	 */
	public void checkAllNeighbors(int numberInStringRepresentation) {
		//add this stone (= a stone of the currently checked color) to the list of stones of this 
		//group and the list of stones of this color that were checked this turn
		surroundedStones.add(numberInStringRepresentation);
		checkedStonesThisColor.add(numberInStringRepresentation);
		
		//check for all surrounding places whether they are unoccupied, of the current player
		//or also of the opponent. If they are also of the opponent: add to the current group
		int locationToTheLeft = numberInStringRepresentation - 1;
		int locationToTheRight = numberInStringRepresentation + 1;
		int locationAbove = numberInStringRepresentation - boardDimension;
		int locationBelow = numberInStringRepresentation + boardDimension;
		List<Integer> locationsToCheck = new ArrayList<Integer>();
		locationsToCheck.add(locationToTheLeft);
		locationsToCheck.add(locationToTheRight);
		locationsToCheck.add(locationAbove);
		locationsToCheck.add(locationBelow);
		
		
		//only check locations that are on the board
		for (int location : locationsToCheck) {
			if (location < 0 || location >= boardDimension * boardDimension) {
				break;
			}
			//only check locations if they haven't been checked before
			if (!checkedPlaces.contains(location)) {
				checkColor(location);
			}
		}
	}
	
	public void checkColor(int toBeCheckedLocation) {
		checkedPlaces.add(toBeCheckedLocation);
		
		//if the location is occupied by the currently checked color, it is added to the group
		if (board.charAt(toBeCheckedLocation) == currentlyCheckedColor) {
			surroundedStones.add(toBeCheckedLocation);
			checkAllNeighbors(toBeCheckedLocation);
		//if the location is unoccupied, surrounded is set to false
		} else if (board.charAt(toBeCheckedLocation) == ProtocolMessages.UNOCCUPIED) {
			surrounded = false;
			//don't break, keep looking until all connected opponent's stones 
			//have been added to 'checkedPlaces'
		} //if the location is of the other color, do nothing
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
