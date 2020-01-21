package tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import org.junit.jupiter.api.*;

import client.*;
import exceptions.*;
import server.*;

public class ServerClientStartingIUpTest {
	private final static ByteArrayOutputStream OUTCONTENT = new ByteArrayOutputStream();
	private final static PrintStream ORIGINALOUT = System.out;

	//Create two clients
	GoClientHuman client1 = new GoClientHuman();
	GoClientHuman client2 = new GoClientHuman();
	GoClientHuman client3 = new GoClientHuman();
	GoClientHuman client4 = new GoClientHuman();
	
	@BeforeAll
	static public void setUpStream() {
	    System.setOut(new PrintStream(OUTCONTENT));
	}
	
	@Test
	void testServer() 
			throws ExitProgram, ServerUnavailableException, ProtocolException, IOException {
		
		/** Preparation: start server with local host and port 8888 and let it listen for clients.*/
		GoServer testServer = new GoServer();
		InetAddress addr = InetAddress.getLocalHost();
		int port = 8888;
		testServer.createSocket(addr, port);
		new Thread(testServer).start();
		
		
		/**
		 * Test creating a connection. 
		 */
		client1.createConnection(addr, port);
		//Client indicates it made a successful connection
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		OUTCONTENT.reset();
		
		/**
		 * Test the handshake coming from the client.
		 * Player will be added to a game in this step.
		 * (As no is message received from the player after it gets the returning handshake)
		 * 
		 * Note: cannot test with wrong input, because that is caught by the TUI (not user here)
		 */
		client1.doHandshake("Eline", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Client also prints the message from the server: a welcome & to which game it was added
		assertThat(OUTCONTENT.toString(), containsString("Welcome"));
		assertThat(OUTCONTENT.toString(), containsString("You have been added"));
		//Server prints that a client was added to a game (game cannot start yet)
		assertThat(OUTCONTENT.toString(), containsString(" was added to game "));
		assertThat(OUTCONTENT.toString(), not(containsString("The game can start!")));
		OUTCONTENT.reset();
		
		/**
		 * Test creating a second connection. 
		 */
		client2.createConnection(addr, port);
		//Client indicates it made a successful connection
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		OUTCONTENT.reset();
		
		/**
		 * Add another client via the client doHandshake and test whether the game begins
		 */
		client2.doHandshake("Joep", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Client also prints the message from the server: a welcome & to which game it was added
		assertThat(OUTCONTENT.toString(), containsString("Welcome"));
		assertThat(OUTCONTENT.toString(), containsString("You have been added"));
		//Server prints that a client was added to a game & that the game can start
		assertThat(OUTCONTENT.toString(), containsString(" was added to game "));
		assertThat(OUTCONTENT.toString(), containsString("The game can start!"));
		OUTCONTENT.reset();
		
		/**
		 * TODO: test ending a game (if I want to do that via the server)
		 */
		// Exit the program
//		client.sendExit();
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}

}
