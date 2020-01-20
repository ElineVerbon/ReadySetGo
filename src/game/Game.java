package game;

import java.util.Arrays;

import protocol.ProtocolMessages;
import server.GoClientHandler;

public class Game {
	
	//save the game number, might need it later for a leader board
	private int gameNumber;

	//TODO variable to keep track of who is black / white (or whose turn it is)
	
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
	
	/** The board, represented as a string. */
	private String board;
	
	/** Board dimension (= length of board. */
	//TODO decide whether I want to determine that here or elsewhere
	//maybe in server via user input?
	private int boardDimension = 3;
	
	/** All previous boards, to check whether a move is valid. */
	private String[] prevBoards;
	
	
	
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
		
		//send turn message to black player (
	}
	
	/** returns whether two players were added to this game. */
	public boolean getCompleteness() {
		return complete;
	}
	
	/** return identified (=number) of the game. */
	public int getNumber() {
		return gameNumber;
	}
	
	
}
