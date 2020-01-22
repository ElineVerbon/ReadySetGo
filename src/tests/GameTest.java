package tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import game.Game;

/**
 * This class will test whether the Game responds correctly to certain responses of players.
 * Each test comes with two files, specifying the moves of player1 and player2, respectively.
 * These are used as inputStreams for the game.
 * 
 * Implemented tests:
 * -startGameTest tests whether G, T and R messages are correctly sent to the players.
 * -removeStonesTest tests whether the board is updated correctly according to removal of stones.
 * -wrongMovesTest tests whether move is outside of board, not an int or the location already taken
 * -repetitionBoardsTest tests whether repetition of boards is found correctly
 * 
 * TODO:
 * -getScoreTest
 * -endGameTests test whether game is ended cleanly and correct winner is found
 * - getting the score
 * - deciding on the winner
 * - ending a game by two passes
 * - ending a game by losing connection
 * - ending a game by invalid move
 * - ending a game by quitting
 * 
 * @author eline.verbon
 *
 */

public class GameTest {
	private final static ByteArrayOutputStream OUTCONTENT = new ByteArrayOutputStream();
	private final static PrintStream ORIGINALOUT = System.out;
	
	@BeforeAll
	static public void setUpStream() {
	    System.setOut(new PrintStream(OUTCONTENT));
	}
	
	/**
	 * Test game start.
	 * 
	 * Give one move for player 1. Except messages starting with 
	 * G, T and R.
	 * 
	 * @throws FileNotFoundException 
	 */
	@Test
	void startGameTest() throws FileNotFoundException  {
		Game testGame = new Game(1);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/startGameTest_MovesPlayer1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/startGameTest_MovesPlayer2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		/**
		 * Test start game.
		 * This will 
		 */
		testGame.startGame();
		assertThat(stringWriter1.toString(), containsString("G;UUUUUUUUUUUUUUUUUUUUUUUUU;B"));
		assertThat(stringWriter1.toString(), containsString("T;UUUUUUUUUUUUUUUUUUUUUUUUU"));
		assertThat(stringWriter1.toString(), containsString("R;V;UUUBUUUUUUUUUUUUUUUUUUUUU"));
		
		//TODO end game
		
		OUTCONTENT.reset();
	}
	
	/**
	 * Test whether wrong moves are recognized as invalid.
	 * TODO: check whether game is ended & correct player is called the winner
	 * 
	 * Wrong moves are: outside of the board, a location that is not empty or 
	 * something that cannot be parsed to an integer.
	 * 
	 * @throws FileNotFoundException 
	 */
	
	@Test
	void wrongMovesTest() throws FileNotFoundException {
		
		/** 
		 * Test move outside of board player1. 
		 */
		Game testGame = new Game(3);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_Player1outsideBoardPlayer1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_Player1outsideBoardPlayer2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); //player1 makes a wrong move in the first turn
		
		//Player 1 gets the message that the move is invalid
		assertThat(stringWriter1.toString(), containsString("R;I"));
		
		//TODO end game (and then I can use playGame instead of doTurn a certain amount of times)

		OUTCONTENT.reset();
		
		/** 
		 * Test move outside of board player2. 
		 */
		testGame = new Game(4);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_Player2outsideBoardPlayer1.txt"));
		stringWriter1 = new StringWriter();
		outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_Player2outsideBoardPlayer2.txt"));
		stringWriter2 = new StringWriter();
		outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); 
		testGame.doTurn(); //player2 makes a wrong move in the first turn
		
		//Player 2 gets the message that the move is invalid
		assertThat(stringWriter2.toString(), containsString("R;I"));
		
		//TODO end game (and then I can use playGame instead of doTurn a certain amount of times)

		OUTCONTENT.reset();
		
		/** 
		 * Test move to a spot already taken (by player2). 
		 */
		testGame = new Game(4);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_spotAlreadyTakenPlayer1.txt"));
		stringWriter1 = new StringWriter();
		outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_spotAlreadyTakenPlayer2.txt"));
		stringWriter2 = new StringWriter();
		outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); 
		testGame.doTurn(); //player2 makes a wrong move in the first turn
		
		//Player 1 gets the message that the move is invalid
		assertThat(stringWriter2.toString(), containsString("R;I"));
		
		//TODO end game (and then I can use playGame instead of doTurn a certain amount of times)

		OUTCONTENT.reset();
		
		/** 
		 * Test move that is not an integer - and also not pass (by player2). 
		 */
		testGame = new Game(4);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_notIntegerPlayer1.txt"));
		stringWriter1 = new StringWriter();
		outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/wrongMovesTest_notIntegerPlayer2.txt"));
		stringWriter2 = new StringWriter();
		outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); 
		testGame.doTurn(); //player2 makes a wrong move in the first turn
		
		//Player 2 gets the message that the move is invalid
		assertThat(stringWriter2.toString(), containsString("R;I"));
		
		//TODO end game (and then I can use playGame instead of doTurn a certain amount of times)

		OUTCONTENT.reset();
	}
	
	/**
	 * Test whether the correct stones are removed from the board.
	 * 
	 * Moves are given as lines in two separate files.
	 * Last boards are checked
	 * 
	 * @throws FileNotFoundException 
	 */
	
	@Test
	void removeStonesTest() throws FileNotFoundException {
		Game testGame = new Game(2);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/removeStonesTest_MovesPlayer1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/removeStonesTest_MovesPlayer2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame();
		//I have 14 lines of commands (7 for player1, 7 for player2)
		//first command is used in startGame, so 13 more to go.
		for (int x = 0; x < 13; x++) {
			testGame.doTurn();
		}
		//The last board seen by player 1 (W in 6 and 7 is removed, B in 18 not yet removed)
		assertThat(stringWriter1.toString(), containsString("R;V;UBBUUBUUBUUBBWUUUWBWWUUUU"));
		//The last board seen by player 2 (B in 18 is not also removed)
		assertThat(stringWriter2.toString(), containsString("R;V;UBBUUBUUBUUBBWUUUWUWWUUWU"));
		
		//TODO end game (and then I can use playGame instead of doTurgn 13 times)

		OUTCONTENT.reset();
	}
	
	/**
	 * Test whether a repetition of the board (by player2) is caught as invalid.
	 * 
	 * Moves are given as lines in two separate files.
	 * Result message is checked
	 * 
	 * @throws FileNotFoundException 
	 */
	
	@Test
	void repetitionBoardTest() throws FileNotFoundException {
		Game testGame = new Game(2);
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/repetitionBoardTest_Player1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("/Users/eline.verbon/workspaces/proeftuin/FinalAssignment1"
						+ "/src/tests/resources/repetitionBoardTest_Player2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame();
		//I have 8 lines of commands (4 for player1, 4 for player2)
		//first command is used in startGame, so 13 more to go.
		for (int x = 0; x < 7; x++) {
			testGame.doTurn();
		}
		//The last board seen by player 1 (W in 6 and 7 is removed, B in 18 not yet removed)
		assertThat(stringWriter1.toString(), containsString("R;V;UBUUUBUBUUUBUUUUUUUUUWWUU"));
		//The last board seen by player 2 (B in 18 is not also removed)
		assertThat(stringWriter2.toString(), containsString("R;I"));
		
		//TODO end game (and then I can use playGame instead of doTurgn 13 times)

		OUTCONTENT.reset();
	}
	
	/**
	 * End game tests.
	 */
	
	/**
	 * Test whether two passes in a row lead to and end of game.
	 * (And that two passes not in a row do not lead to an end of game.
	 * 
	 * @throws FileNotFoundException
	 */
	@Test
	void twoPassesTest() throws FileNotFoundException {
		
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}
}
