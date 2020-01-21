package tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.*;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import client.GoClientHuman;
import exceptions.ExitProgram;
import exceptions.ProtocolException;
import exceptions.ServerUnavailableException;
import game.Game;
import protocol.ProtocolMessages;

/**
 * Possible things to test (maybe divide over tests).
 * 
 * StartGameTest
 * - starting a game
 * 
 * MoveCheckerTest
 * - giving an invalid move
 * - checking whether stones are removed
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

	Game testGame = new Game(1);
	
	private final static int BOARDDIMENSION = 5;
	private String board;
	
	//Create two clients
	GoClientHuman client1 = new GoClientHuman();
	GoClientHuman client2 = new GoClientHuman();
	
	@BeforeAll
	static public void setUpStream() {
	    System.setOut(new PrintStream(OUTCONTENT));
	}
	
	@Test
	void testServer() 
			throws ExitProgram, ServerUnavailableException, ProtocolException, IOException {
		
		// Create a string representation of the empty board.
        char[] charArray = new char[BOARDDIMENSION * BOARDDIMENSION];
        Arrays.fill(charArray, ProtocolMessages.UNOCCUPIED);
        board = new String(charArray);
        
        //
        char colorPlayer1 = ProtocolMessages.BLACK;
        char colorPlayer2 = ProtocolMessages.WHITE;
		
        //Make a String of commands for player 1 and set as inputStream for player1
		//String startCommando1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
		//		+ board + ProtocolMessages.DELIMITER + colorPlayer1;
		//but how do I know the new board?
		//String turnCommando1 = ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board;
		String message1 = ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + 3 + "\n";
		BufferedReader inPlayer1 = new BufferedReader(new StringReader(message1));
		StringWriter stringWriter1 = new StringWriter();
		BufferedWriter outPlayer1 = new BufferedWriter(stringWriter1);
		
		//Make a String of commands for player 2 and set as inputStream for player2
		String startCommando2 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + colorPlayer2;
		String message2 = startCommando2;
		BufferedReader inPlayer2 = new BufferedReader(new StringReader(message2));
		BufferedWriter outPlayer2 = new BufferedWriter(new StringWriter());
		
		//add players to game
		testGame.addPlayer("Player1", inPlayer1, outPlayer1, "black");
		testGame.addPlayer("Player2", inPlayer2, outPlayer2, "white");
		
		//
//		String msg = inPlayer1.readLine();
//		while (msg != null) {
//			testGame.sendMessageToClient(msg, outPlayer1);
//			String reply = outPlayer1.toString();
//			assertThat(outPlayer1.toString(), containsString("The game has started!"));
//			msg = inPlayer1.readLine();
//		}
		
		//TODO I still need to test whether the startGame sends the correct message
		testGame.startGame(); //dit werkt volgens mij niet meer
		assertThat(stringWriter1.toString(), containsString("G;UUUUUUUUUUUUUUUUUUUUUUUUU;B"));
		assertThat(stringWriter1.toString(), containsString("T;UUUUUUUUUUUUUUUUUUUUUUUUU"));
		assertThat(stringWriter1.toString(), containsString("R;V;UUUBUUUUUUUUUUUUUUUUUUUUU"));
		
		//Client prints a message to indicate the game has started
		
		//Test invalid move
		
		//Test valid move
		
		//Test several moves that result in capture of a group of stones
		
		OUTCONTENT.reset();
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}
}
