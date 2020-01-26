package tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.*;

import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import protocol.ProtocolMessages;
import ruleimplementations.MoveResultGenerator;
import server.Game;
import server.Handler;

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
	 * Test valid first turns (use edge cases 0 and 24). (Cannot test it without 
	 * the first turn: then I get an invalid move message that is not expected.)
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
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;0");
		handler1.sendMessageToClient("R;V;BUUUUUUUUUUUUUUUUUUUUUUUU");

		handler4.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;B");
		handler4.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler4.getReply()).andReturn("M;0");
		handler4.sendMessageToClient("R;V;BUUUUUUUUUUUUUUUUUUUUUUUU");
		
		EasyMock.replay(handler1, handler2, handler3, handler4);
		
		//act
		game1.startGame();
		game2.startGame();
		
		//assert
		EasyMock.verify(handler1, handler2, handler3, handler4);
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
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;-1");
		handler1.sendMessageToClient("R;I;Your move was invalid. You lose the game.");
		handler1.sendMessageToClient("E;C;W;0.0;0.0");
		handler2.sendMessageToClient("E;C;W;0.0;0.0");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.runGame();
		
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
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;3");
		handler1.sendMessageToClient("R;V;UUUBUUUUUUUUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UUUBUUUUUUUUUUUUUUUUUUUUU;3");
		EasyMock.expect(handler2.getReply()).andReturn("M;3");
		handler2.sendMessageToClient("R;I;Your move was invalid. You lose the game.");
		handler1.sendMessageToClient("E;C;B;0.0;0.0");
		handler2.sendMessageToClient("E;C;B;0.0;0.0");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.runGame();
		
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
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;g");
		handler1.sendMessageToClient("R;I;Your move was invalid. You lose the game.");
		handler1.sendMessageToClient("E;C;W;0.0;0.0");
		handler2.sendMessageToClient("E;C;W;0.0;0.0");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test whether two passes in a row lead to an end of game.
	 */
	@Test
	void twoPassesTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;P");
		handler1.sendMessageToClient("R;V;UUUUUUUUUUUUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;P");
		EasyMock.expect(handler2.getReply()).andReturn("M;P");
		handler2.sendMessageToClient("R;V;UUUUUUUUUUUUUUUUUUUUUUUUU");
		handler1.sendMessageToClient("E;F;B;0.0;0.0");
		handler2.sendMessageToClient("E;F;B;0.0;0.0");
		EasyMock.replay(handler1, handler2);
	
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test whether two passes not in a row do not lead to an end of game.
	 */
	
	@Test
	void twoNonConsecutivePassesTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;P");
		handler1.sendMessageToClient("R;V;UUUUUUUUUUUUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;P");
		EasyMock.expect(handler2.getReply()).andReturn("M;3");
		handler2.sendMessageToClient("R;V;UUUWUUUUUUUUUUUUUUUUUUUUU");
		handler1.sendMessageToClient("T;UUUWUUUUUUUUUUUUUUUUUUUUU;3");
		EasyMock.expect(handler1.getReply()).andReturn("M;4");
		handler1.sendMessageToClient("R;V;UUUWBUUUUUUUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UUUWBUUUUUUUUUUUUUUUUUUUU;4");
		EasyMock.expect(handler2.getReply()).andReturn("M;P");
		handler2.sendMessageToClient("R;V;UUUWBUUUUUUUUUUUUUUUUUUUU");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.startGame(); //first doTurn called in this method)
		game.doTurn();
		game.doTurn();
		game.doTurn();
		game.hasEnded();
		
		//assert
		EasyMock.verify(handler1, handler2);
		assertFalse(game.hasEnded());
	}

	/**
	 * Test whether quitting leads to an end of game.
	 */
	
	@Test
	void quitOnTurnTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;7");
		handler1.sendMessageToClient("R;V;UUUUUUUBUUUUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UUUUUUUBUUUUUUUUUUUUUUUUU;7");
		EasyMock.expect(handler2.getReply()).andReturn("Q");
		handler1.sendMessageToClient("E;Q;B;0.0;0.0");
		handler2.sendMessageToClient("E;Q;B;0.0;0.0");
		EasyMock.replay(handler1, handler2);
	
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test the ko rule. 
	 * 
	 * Explanation of the rule:
	 * U B U U U 	0  1  2  3  4
	 * B U B U U	5  6  7  8  9
	 * W U W U U	10 11 12 13 14 
	 * U W U U U	15 16 17 18 19
	 * U U U U U	20 21 22 23 24
	 * After this board, B may place a stone in 11 and then W may place a stone in 6 to 
	 * capture B. However, B may not subsequently place another stone in 11 to capture W 
	 * back again (repetition of previous board).
	 */
	
	@Test
	void koRuleTest() {
		
		//arrange
		Game game = new Game(1, "1.0");
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game.setClientHandlerPlayer1(handler1);
		game.setClientHandlerPlayer2(handler2);
		game.setColorPlayer1(ProtocolMessages.BLACK);
		game.setColorPlayer2(ProtocolMessages.WHITE);
		
		// --> set expectations
		handler2.sendMessageToClient("G;UUUUUUUUUUUUUUUUUUUUUUUUU;W");
		handler1.sendMessageToClient("T;UUUUUUUUUUUUUUUUUUUUUUUUU;null");
		EasyMock.expect(handler1.getReply()).andReturn("M;1");
		handler1.sendMessageToClient("R;V;UBUUUUUUUUUUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UBUUUUUUUUUUUUUUUUUUUUUUU;1");
		EasyMock.expect(handler2.getReply()).andReturn("M;10");
		handler2.sendMessageToClient("R;V;UBUUUUUUUUWUUUUUUUUUUUUUU");
		handler1.sendMessageToClient("T;UBUUUUUUUUWUUUUUUUUUUUUUU;10");
		EasyMock.expect(handler1.getReply()).andReturn("M;5");
		handler1.sendMessageToClient("R;V;UBUUUBUUUUWUUUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UBUUUBUUUUWUUUUUUUUUUUUUU;5");
		EasyMock.expect(handler2.getReply()).andReturn("M;12");
		handler2.sendMessageToClient("R;V;UBUUUBUUUUWUWUUUUUUUUUUUU");
		handler1.sendMessageToClient("T;UBUUUBUUUUWUWUUUUUUUUUUUU;12");
		EasyMock.expect(handler1.getReply()).andReturn("M;7");
		handler1.sendMessageToClient("R;V;UBUUUBUBUUWUWUUUUUUUUUUUU");
		handler2.sendMessageToClient("T;UBUUUBUBUUWUWUUUUUUUUUUUU;7");
		EasyMock.expect(handler2.getReply()).andReturn("M;16");
		handler2.sendMessageToClient("R;V;UBUUUBUBUUWUWUUUWUUUUUUUU");
		handler1.sendMessageToClient("T;UBUUUBUBUUWUWUUUWUUUUUUUU;16");
		EasyMock.expect(handler1.getReply()).andReturn("M;11");
		handler1.sendMessageToClient("R;V;UBUUUBUBUUWBWUUUWUUUUUUUU");
		handler2.sendMessageToClient("T;UBUUUBUBUUWBWUUUWUUUUUUUU;11");
		EasyMock.expect(handler2.getReply()).andReturn("M;6");
		handler2.sendMessageToClient("R;V;UBUUUBWBUUWUWUUUWUUUUUUUU");
		handler1.sendMessageToClient("T;UBUUUBWBUUWUWUUUWUUUUUUUU;6");
		EasyMock.expect(handler1.getReply()).andReturn("M;11");
		handler1.sendMessageToClient("R;I;Your move was invalid. You lose the game.");
		handler1.sendMessageToClient("E;C;W;0.0;0.0");
		handler2.sendMessageToClient("E;C;W;0.0;0.0");
		
		EasyMock.replay(handler1, handler2);
	
		//act
		game.runGame();
		
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
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}
}
