package game;

import server.GoClientHandler;

public class Game {
	
	//save the game number, might need it later for a leader board
	private int gameNumber;

	//TODO variable to keep track of who is black / white (or whose turn it is)
	
	private String player1 = null; //name of player1
	private String player2 = null; //name of player2
	private boolean complete = false; //checks if two players were added.
	
	/** The board, represented as a string. */
	private String board;
	
	/** All previous boards, to check whether a move is valid. */
	private String[] prevBoards;
	
	/** Save access to clientHandlers of the competing player. */
	GoClientHandler goClientHandler1;
	GoClientHandler goClientHandler2;
	
	public Game(int number) {
		gameNumber = number;
	}
	
	//responds with a message that indicates the gameNumber and the number of players.
	public String addPlayer(String name, GoClientHandler goClientHandler) {
		String message = "";
		if (player1 == null) {
			player1 = name;
			goClientHandler1 = goClientHandler;
			message = name + " was added to game " + gameNumber + " as the first player.";
		} else {
			player2 = name;
			goClientHandler2 = goClientHandler;
			complete = true;
			message = name + " was added to game " + gameNumber + 
					" as the second player. The game can start!";
		}
		return message;
	}
	
	public void startGame() {
		//send start game message to both players (via they clientHandler)
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
