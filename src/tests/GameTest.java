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
 * Possible things to test (maybe divide over tests), responses to:
 * 
 * StartGameTest
 * - starting a game
 * 
 * MoveCheckerTest
 * - invalid move
 * - removal of stones
 * 
 * EndGameTest
 * - getting the score
 * - deciding on the winner
 * - ending a game by two passes
 * - ending a game by losing connection
 * - ending a game by invalid move
 * - ending a game by quitting
 * 
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
	void testStartGame() throws FileNotFoundException  {
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
	void testRemoveStones() throws FileNotFoundException {
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

		OUTCONTENT.reset();
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}
}
