package tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.not;

import java.io.*;
import java.net.InetAddress;

import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import exceptions.ExitProgram;
import protocol.ProtocolMessages;
import ruleimplementations.MoveResultGenerator;
import server.Game;
import server.GoClientHandler;
import server.GoServer;
import server.Handler;
import ss.week7.hotel.server.HotelServer;

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
 * -quitOnTurnTest tests whether the game ends correctly when a user types 'quit' during turn
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
	 * Test game start: 
	 * - second player's handler's startGame message is called 
	 * (first player's handler is called in the addPlayer call of the second)
	 * - black player gets first turn.
	 * 
	 * @throws FileNotFoundException 
	 */
	
	@Test
	void startGameTest() {
		
		//arrange
		Game game1 = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game1.setClientHandlerPlayer1(handler1);
		game1.setClientHandlerPlayer2(handler2);
		game1.setColorPlayer1(ProtocolMessages.BLACK);
		game1.setColorPlayer2(ProtocolMessages.WHITE);
		
		Game game2 = new Game(2, "1.0");
		Handler handler3 = EasyMock.createMock(Handler.class);
		Handler handler4 = EasyMock.createMock(Handler.class);
		game2.setClientHandlerPlayer1(handler3);
		game2.setClientHandlerPlayer2(handler4);
		game2.setColorPlayer1(ProtocolMessages.WHITE);
		game2.setColorPlayer2(ProtocolMessages.BLACK);
		
		// --> set expectations
		handler2.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'W');
		EasyMock.expect(handler1.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null)).andReturn("");
		handler4.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'B');
		EasyMock.expect(handler4.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null)).andReturn("");
		EasyMock.replay(handler1, handler2, handler3, handler4);
		
		//act
		game1.startGame();
		game2.startGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test valid first turns (use edge cases 0 and 24).
	 */
	
	@Test
	void validFirstTurnTest() {
		
		//arrange
		Game game1 = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game1.setClientHandlerPlayer1(handler1);
		game1.setClientHandlerPlayer2(handler2);
		game1.setColorPlayer1(ProtocolMessages.BLACK);
		game1.setColorPlayer2(ProtocolMessages.WHITE);
		
		Game game2 = new Game(2, "1.0");
		Handler handler3 = EasyMock.createMock(Handler.class);
		Handler handler4 = EasyMock.createMock(Handler.class);
		game2.setClientHandlerPlayer1(handler3);
		game2.setClientHandlerPlayer2(handler4);
		game2.setColorPlayer1(ProtocolMessages.WHITE);
		game2.setColorPlayer2(ProtocolMessages.BLACK);
		
		// --> set expectations
		handler2.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'W');
		EasyMock.expect(handler1.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null)).andReturn("M;0");
		handler1.giveResultMessage(true, "BUUUUUUUUUUUUUUUUUUUUUUUU");
		handler4.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'B');
		EasyMock.expect(handler4.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null))
																			.andReturn("M;24");
		handler4.giveResultMessage(true, "UUUUUUUUUUUUUUUUUUUUUUUUB");
		EasyMock.replay(handler1, handler2, handler3, handler4);
		
		//act
		game1.startGame();
		game2.startGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/** Test outside-of-board invalid first turn. */
	
	@Test
	void outsideOfBoardInvalidTurnTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'W');
		EasyMock.expect(handler1.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null)).
																		andReturn("M;-1");
		handler1.giveResultMessage(false, "Your move was invalid. You lose the game.");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.startGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/** Test occupied-location invalid turn. */
	@Test
	void occupiedLocationInvalidTurnTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'W');
		EasyMock.expect(handler1.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null)).
																		andReturn("M;3");
		handler1.giveResultMessage(true, "UUUBUUUUUUUUUUUUUUUUUUUUU");
		EasyMock.expect(handler2.doTurnMessage("UUUBUUUUUUUUUUUUUUUUUUUUU", "3")).
																		andReturn("M;3");
		handler2.giveResultMessage(false, "Your move was invalid. You lose the game.");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.startGame();
		game.doTurn();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/** Test non-integer invalid turn. */
	@Test
	void nonIntegerInvalidTurnTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.startGameMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", 'W');
		EasyMock.expect(handler1.doTurnMessage("UUUUUUUUUUUUUUUUUUUUUUUUU", null)).
																		andReturn("M;g");
		handler1.giveResultMessage(false, "Your move was invalid. You lose the game.");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.startGame();
		game.doTurn();
		
		//assert
		EasyMock.verify(handler1, handler2);
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
	void removeStonesTest() {
		MoveResultGenerator moveResult = new MoveResultGenerator();
		String oldBoard;
		String expectedNewBoard;
		String newBoard;
		
		/** 
		 * Remove a corner. 
		 * 
		 * WBUUU	 UBUUU
		 * BUUUU	 BUUUU
		 * UUUUU --> UUUUU
		 * UUUUU	 UUUUU
		 * UUUUU	 UUUUU
		 */
		oldBoard = "WBUUUBUUUUUUUUUUUUUUUUUUU";
		expectedNewBoard = "UBUUUBUUUUUUUUUUUUUUUUUUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove other color corner. 
		 * 
		 * BWUUU	 UWUUU
		 * WUUUU	 WUUUU
		 * UUUUU --> UUUUU
		 * UUUUU	 UUUUU
		 * UUUUU	 UUUUU
		 */
		oldBoard = "BWUUUWUUUUUUUUUUUUUUUUUUU";
		expectedNewBoard = "UWUUUWUUUUUUUUUUUUUUUUUUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove bigger corner. 
		 * 
		 * UUUUU	 UUUUU
		 * UUUUU	 UUUUU
		 * UUUUU --> UUUUU
		 * UUUWW	 UUUWW
		 * UUWBB	 UUWUU
		 */
		oldBoard = "UUUUUUUUUUUUUUUUUUWWUUWBB";
		expectedNewBoard = "UUUUUUUUUUUUUUUUUUWWUUWUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove a block of 2 by 2 on the side. 
		 * 
		 * UBWWB	 UBUUB
		 * UBWWB --> UBUUB
		 * UUBBU	 UUBBU
		 * WWUUU	 WWUUU
		 * UUUUU	 UUUUU
		 */
		oldBoard = "UBWWBUBWWBUUBBUWWUUUUUUUU";
		expectedNewBoard = "UBUUBUBUUBUUBBUWWUUUUUUUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove a circle in the middle. 
		 * 
		 * UUUUUU		UUUUUU
		 * UBBBBU		UBBBBU
		 * BWWWWB --->  BUUUUB
		 * BWWWWB		BUUUUB
		 * UBBBBU		UBBBBU
		 * UUUUUU		UUUUUU
		 */
		oldBoard = "UUUUUUUBBBBUBWWWWBBWWWWBUBBBBUUUUUUU";
		expectedNewBoard = "UUUUUUUBBBBUBUUUUBBUUUUBUBBBBUUUUUUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Do not remove a circle in the middle (one liberty). 
		 * 
		 * UUUUUU		UUUUUU
		 * UBBBBU		UBBBBU
		 * BWWWWU --->  BUUUUU
		 * BWWWWB		BUUUUB
		 * UBBBBU		UBBBBU
		 * UUUUUU		UUUUUU
		 */
		oldBoard = "UUUUUUUBBBBUBWWWWUBWWWWBUBBBBUUUUUUU";
		expectedNewBoard = "UUUUUUUBBBBUBUUUUUBUUUUBUBBBBUUUUUUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertFalse(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove a donut. 
		 * 
		 * UBBBBU		UBBBBU
		 * BWWWWB		BUUUUB
		 * BWWWWB --->  BUUUUB
		 * BWBBWB		BUBBUB
		 * BWWWWB		BUUUUB
		 * UBBBBU		UBBBBU
		 */
		oldBoard = "UBBBBUBWWWWBBWWWWBBWBBWBBWWWWBUBBBBU";
		expectedNewBoard = "UBBBBUBUUUUBBUUUUBBUBBUBBUUUUBUBBBBU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Do not remove a donut. 
		 * 
		 * UBBBBU		UBBBBU
		 * BWWWWB		BUUUUB
		 * BWWWWB --->  BUUUUB
		 * BWUUWB		BUUUUB
		 * BWWWWB		BUUUUB
		 * UBBBBU		UBBBBU
		 */
		oldBoard = "UBBBBUBWWWWBBWWWWBBWUUWBBWWWWBUBBBBU";
		expectedNewBoard = "UBBBBUBUUUUBBUUUUBBUUUUBBUUUUBUBBBBU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertFalse(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove two pieces. 
		 * 
		 * UBBUU	 UBBUU
		 * BWWBU	 BUUBU
		 * UBBUU --> UBBUU
		 * UUUWW	 UUUWW
		 * UUWBB	 UUWUU
		 */
		oldBoard = "UBBUUBWWBUUBBUUUUUWWUUWBB";
		expectedNewBoard = "UBBUUBUUBUUBBUUUUUWWUUWUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove a two forms close to each other. 
		 * 
		 * UUBBU		UUBBU
		 * UBWWB --->   UBUUB
		 * UUBBU		UUBBU
		 * UUUWW		UUUWW
		 * UUWBB		UUWUU
		 */
		oldBoard = "UUBBUUBWWBUUBBUUUUWWUUWBB";
		expectedNewBoard = "UUBBUUBUUBUUBBUUUUWWUUWUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove two forms close to each other. 
		 * 
		 * UUBBU		UUBBU
		 * UBWWB --->   UBUUB
		 * UUBBU		UUBBU
		 * UBUWW		UBUWW
		 * UUWBB		UUWUU
		 */
		oldBoard = "UUBBUUBWWBUUBBUUUUWWUUWBB";
		expectedNewBoard = "UUBBUUBUUBUUBBUUUUWWUUWUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove a strange form + an extra corner of the same color. 
		 * 
		 * BWWBBU		BUUBBU
		 * BWBBWB		BUBBUB
		 * BWWWWB --->  BUUUUB
		 * UBWBBU		UBUBBU
		 * UUBUBB		UUBUBB
		 * UUUBWW		UUUBUU
		 */
		oldBoard = "BWWBBUBWBBWBBWWWWBUBWBBUUUBUBBUUUBWW";
		expectedNewBoard = "BUUBBUBUBBUBBUUUUBUBUBBUUUBUBBUUUBUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove a strange form + an extra corner of the other color. 
		 * 
		 * BWWBBU		BUUBBU
		 * BWBBWB		BUBBUB
		 * BWWWWB --->  BUUUUB
		 * UBWBBU		UBUBBU
		 * UUBUWW		UUBUWW
		 * UUUWBB		UUUWUU
		 */
		oldBoard = "BWWBBUBWBBWBBWWWWBUBWBBUUUBUWWUUUWBB";
		expectedNewBoard = "BUUBBUBUBBUBBUUUUBUBUBBUUUBUWWUUUWUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
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
		Game testGame = new Game(2, "1.0");
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("src/tests/resources/repetitionBoardTest_Player1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("src/tests/resources/repetitionBoardTest_Player2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame();
		//I have 8 lines of commands (4 for player1, 4 for player2)
		//first command is used in startGame, so 7 more to go.
		for (int x = 0; x < 6; x++) {
			testGame.doTurn();
		}
		testGame.doTurn();
		//The last board seen by player 1 (W in 6 is removed, B in 18 not yet removed)
		assertThat(stringWriter1.toString(), containsString("R;V;UBUUUBUBUUUBUUUUUUUUUWWUU"));
		//The last board seen by player 2 (placing a W in 6 is invalid)
		assertThat(stringWriter2.toString(), containsString("R;I"));
		
		//TODO end game (and then I can use playGame instead of doTurgn 13 times)

		OUTCONTENT.reset();
	}
	
	/**
	 * End game tests.
	 */
	
	/**
	 * Test whether two passes in a row lead to and end of game.
	 * And that two passes not in a row do not lead to an end of game.
	 * 
	 * @throws FileNotFoundException
	 */
	@Test
	void twoPassesTest() throws FileNotFoundException {
		Game testGame = new Game(2, "1.0");
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("src/tests/resources/twoPassesTest_MovesPlayer1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("src/tests/resources/twoPassesTest_MovesPlayer2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); //first turn player1 (P)
		assertThat(stringWriter1.toString(), containsString("R;V;UUUUUUUUUUUUUUUUUUUUUUUUU"));
		
		testGame.doTurn(); //first turn player2 (6)
		assertThat(stringWriter2.toString(), containsString("R;V;UUUUUUWUUUUUUUUUUUUUUUUUU"));
		
		testGame.doTurn(); //second turn player1 (3)
		assertThat(stringWriter1.toString(), containsString("R;V;UUUBUUWUUUUUUUUUUUUUUUUUU"));
		
		testGame.doTurn(); //second turn player2 (P) - second pass, but not consecutive
		assertThat(stringWriter2.toString(), containsString("R;V;UUUBUUWUUUUUUUUUUUUUUUUUU"));
		
		testGame.doTurn(); //third turn player1 (P) - second consecutive pass
		assertThat(stringWriter1.toString(), containsString("R;V;UUUBUUWUUUUUUUUUUUUUUUUUU"));
		testGame.endGame();
		//TODO change once I can calculate score!
		assertThat(stringWriter1.toString(), containsString("E;F;B;0;0"));

		OUTCONTENT.reset();
	}
	
	@Test
	void quitOnTurnTest() throws FileNotFoundException {
		Game testGame = new Game(2, "1.0");
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		BufferedReader inPlayer1 = new BufferedReader(
				new FileReader("src/tests/resources/quitOnTurnPlayer2Test_MovesPlayer1.txt"));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		BufferedReader inPlayer2 = new BufferedReader(
				new FileReader("src/tests/resources/quitOnTurnPlayer2Test_MovesPlayer2.txt"));
		StringWriter stringWriter2 = new StringWriter();
		BufferedWriter outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); //first turn player1 (3)
		assertThat(stringWriter1.toString(), containsString("R;V;UUUBUUUUUUUUUUUUUUUUUUUUU"));
		
		testGame.doTurn(); //first turn player2 (Q)
		assertThat(stringWriter2.toString(), not(containsString("R;V;UUUBUUUUUUUUUUUUUUUUUUUUU")));
		testGame.endGame();
		assertThat(stringWriter2.toString(), containsString("E;Q;B;0;0")); //black wins

		OUTCONTENT.reset();
		
		testGame = new Game(2, "1.0");
		
        //Use a file with a command per line to represent the moves of player1 as the bufferedReader
		inPlayer1 = new BufferedReader(
				new FileReader("src/tests/resources/quitOnTurnPlayer1Test_MovesPlayer1.txt"));
		stringWriter1 = new StringWriter();
		outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Use a file with a command per line to represent the moves of player2 as the bufferedReader
		inPlayer2 = new BufferedReader(
				new FileReader("src/tests/resources/quitOnTurnPlayer1Test_MovesPlayer2.txt"));
		stringWriter2 = new StringWriter();
		outPlayer2 = new BufferedWriter(stringWriter2);
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		testGame.startGame(); //first turn player1 (3)
		assertThat(stringWriter1.toString(), not(containsString("R")));
		
		testGame.endGame();
		assertThat(stringWriter2.toString(), containsString("E;Q;W;0;0")); //white wins

		OUTCONTENT.reset();
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}
}
