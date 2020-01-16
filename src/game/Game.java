package game;

public class Game {
	
	//save the game number, might need it later for a leader board
	private int gameNumber;

	private String player1 = null; //name of player1
	private String player2 = null; //name of player2
	private boolean complete = false; //checks if two players were added.
	
	/** The board, represented as a string. */
	private String board;
	
	/** All previous boards, to check whether a move is valid. */
	private String[] prevBoards;
	
	public Game(int number) {
		gameNumber = number;
	}
	
	public void addPlayer(String name) {
		if (player1 == null) {
			player1 = name;
		} else {
			player2 = name;
			complete = true;
		}
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
