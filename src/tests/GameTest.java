package tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.net.SocketTimeoutException;

import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import protocol.ProtocolMessages;
import ruleimplementations.BoardUpdater;
import ruleimplementations.ScoreCalculator;
import server.Game;
import server.Handler;

/**
 * This class will test whether the Game sends messages correctly and responds correctly 
 * to the responses of the players.
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
	 * (first player's handler is called in the addPlayer call when the second player
	 * is added to the game)
	 * - black player gets first turn.
	 * 
	 * Test valid first turns (use edge cases 0 and 24). (Cannot test it without 
	 * the first turn: then I get an invalid move message that is not expected.)
	 * @throws SocketTimeoutException 
	 */
	
	@Test
	void startGameTest() throws SocketTimeoutException {
		
		//arrange
		Game game1 = new Game(1, "1.0", 5, 100);
		Handler handler1 = EasyMock.createMock(Handler.class);
		Handler handler2 = EasyMock.createMock(Handler.class);
		game1.setClientHandlerPlayer1(handler1);
		game1.setClientHandlerPlayer2(handler2);
		game1.setColorPlayer1(ProtocolMessages.BLACK);
		game1.setColorPlayer2(ProtocolMessages.WHITE);
		
		Game game2 = new Game(2, "1.0", 5, 100);
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
	
	/** Test outside-of-board invalid first turn. 
	 * @throws SocketTimeoutException */
	
	@Test
	void outsideOfBoardInvalidTurnTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		handler1.sendMessageToClient("E;C;W;-0.5;0.0");
		handler2.sendMessageToClient("E;C;W;-0.5;0.0");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/** Test occupied-location invalid turn. 
	 * @throws SocketTimeoutException */
	
	@Test
	void occupiedLocationInvalidTurnTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		handler1.sendMessageToClient("E;C;B;24.5;0.0");
		handler2.sendMessageToClient("E;C;B;24.5;0.0");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/** Test non-integer invalid turn. 
	 * @throws SocketTimeoutException */
	
	@Test
	void nonIntegerInvalidTurnTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		handler1.sendMessageToClient("E;C;W;-0.5;0.0");
		handler2.sendMessageToClient("E;C;W;-0.5;0.0");
		EasyMock.replay(handler1, handler2);
		
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test whether two passes in a row lead to an end of game.
	 * @throws SocketTimeoutException 
	 */
	
	@Test
	void twoPassesTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		handler1.sendMessageToClient("E;F;W;-0.5;0.0");
		handler2.sendMessageToClient("E;F;W;-0.5;0.0");
		EasyMock.replay(handler1, handler2);
	
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test whether two passes not in a row do not lead to an end of game.
	 * @throws SocketTimeoutException 
	 */
	
	@Test
	void twoNonConsecutivePassesTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		game.processReply();
		game.doTurn();
		game.processReply();
		game.doTurn();
		game.processReply();
		game.hasEnded();
		
		//assert
		EasyMock.verify(handler1, handler2);
//		assertFalse(game.hasEnded());
	}

	/**
	 * Test whether quitting leads to an end of game.
	 * @throws SocketTimeoutException 
	 */
	
	@Test
	void quitOnTurnTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		handler1.sendMessageToClient("E;Q;B;24.5;0.0");
		handler2.sendMessageToClient("E;Q;B;24.5;0.0");
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
	 * 
	 * End result: B has one captured area of size 1 (location 0) + 3 stones - 0.5 komi
	 * W has one captured area of size 1 (11) + 4 stones
	 * @throws SocketTimeoutException 
	 */
	
	@Test
	void koRuleTest() throws SocketTimeoutException {
		
		//arrange
		Game game = new Game(1, "1.0", 5, 100);
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
		handler1.sendMessageToClient("E;C;W;3.5;5.0");
		handler2.sendMessageToClient("E;C;W;3.5;5.0");
		
		EasyMock.replay(handler1, handler2);
	
		//act
		game.runGame();
		
		//assert
		EasyMock.verify(handler1, handler2);
	}
	
	/**
	 * Test whether the correct stones are removed from the board.
	 */
	
	@Test
	void removeStonesTest() {
		BoardUpdater moveResult = new BoardUpdater();
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
		
		/** 
		 * Remove a stone from the left side of the board. 
		 * 
		 * BUUU
		 * WBUU
		 * BUUU
		 * WUUU		
		 * 
		 */
		oldBoard = "BUUUWBUUBUUUWUUU";
		expectedNewBoard = "BUUUUBUUBUUUWUUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
		
		/** 
		 * Remove several stones from the left and right sides of the board. 
		 * 
		 * BUUUBW
		 * WBUBWW
		 * BUUUBW
		 * WUUBWW
		 * WUBWWW
		 * UUUBWW		
		 * 
		 */
		oldBoard = "BUUUBWWBUBWWBUUUBWWUUBWWWUBWWWUUUBWW";
		expectedNewBoard =  "BUUUBUUBUBUUBUUUBUWUUBUUWUBUUUUUUBUU";
		newBoard = moveResult.determineNewBoard(oldBoard, ProtocolMessages.BLACK);
		assertTrue(newBoard.equals(expectedNewBoard));
	}
	
	@Test
	void calculateScoresTest() {
		ScoreCalculator scoreCalculator = new ScoreCalculator();
		
		/** Empty board. */
		scoreCalculator.calculateScores("UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == -0.5);
		assertTrue(scoreCalculator.getScoreWhite() == 0.0);
		
		/** Boards with one stone. */
		scoreCalculator.calculateScores("WUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == -0.5);
		assertTrue(scoreCalculator.getScoreWhite() == 36.0);
		
		scoreCalculator.calculateScores("UUUUUUUUUUUUUUUUUUUUUUBUUUUUUUUUUUUU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == 35.5);
		assertTrue(scoreCalculator.getScoreWhite() == 0.0);
		
		/** Filled board. */
		scoreCalculator.calculateScores("WWWWWWWWWWWWWWWWWWBBBBBBBBBBBBBBBBBB", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == 17.5);
		assertTrue(scoreCalculator.getScoreWhite() == 18.0);
		
		/**
		 * Area's for both B and W.
		 * 
		 * BUUBBU
		 * BUBBUB
		 * BUUUUB
		 * UBUBBU
		 * UUBUWW
		 * UUUWUU
		 */
		scoreCalculator.calculateScores("BUUBBUBUBBUBBUUUUBUBUBBUUUBUWWUUUWUU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == 22.5);
		assertTrue(scoreCalculator.getScoreWhite() == 5.0);
		
		/**
		 * Only an surrounded area for B.
		 * 
		 * UUUUUU
		 * BUUUUB
		 * UBBBBU
		 * UUUUUU
		 * UUUUUU
		 * UUUWWU
		 */
		scoreCalculator.calculateScores("UUUUUUBUUUUBUBBBBUUUUUUUUUUUUUUUUWWU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == 15.5);
		assertTrue(scoreCalculator.getScoreWhite() == 2.0);
		
		/**
		 * No surrounded areas.
		 * 
		 * UWUUUU
		 * BUUUUB
		 * UBBBBU
		 * UUUUUU
		 * UUUUUU
		 * UUUWWU
		 */
		scoreCalculator.calculateScores("UWUUUUBUUUUBUBBBBUUUUUUUUUUUUUUUUWWU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == 5.5);
		assertTrue(scoreCalculator.getScoreWhite() == 3.0);
		
		/** 
		 * Surrounded areas on the left and right sides of the board. 
		 * 
		 * BUUUBU
		 * UBUBUU
		 * BUUUBU
		 * WUUBUU
		 * WUBUUU
		 * UUUBUU		
		 * 
		 */
		scoreCalculator.calculateScores("BUUUBUUBUBUUBUUUBUWUUBUUWUBUUUUUUBUU", 0.5);
		assertTrue(scoreCalculator.getScoreBlack() == 20.5);
		assertTrue(scoreCalculator.getScoreWhite() == 2.0);
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}
}
