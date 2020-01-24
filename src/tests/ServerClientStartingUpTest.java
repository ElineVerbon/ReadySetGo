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

public class ServerClientStartingUpTest {
	private final static ByteArrayOutputStream OUTCONTENT = new ByteArrayOutputStream();
	private final static PrintStream ORIGINALOUT = System.out;

	@BeforeAll
	static public void setUpStream() {
	    System.setOut(new PrintStream(OUTCONTENT));
	}
	
	/**
	 * Test creating a connection and doing a handshake. 
	 * After two players have done this, the game can start.
	 * 
	 * Note: cannot test with wrong input, because that is caught by the TUI (not user here)
	 */
	@Test
	void testServer() 
			throws ExitProgram, ServerUnavailableException, ProtocolException, IOException {
		
		HumanClientTUI humanClientTUI1 = new HumanClientTUI();
		HumanClientServerCommunicator humanClientServerCommunicator1 = 
				 					new HumanClientServerCommunicator(humanClientTUI1);
		HumanClientTUI humanClientTUI2 = new HumanClientTUI();
		HumanClientServerCommunicator humanClientServerCommunicator2 = 
				 					new HumanClientServerCommunicator(humanClientTUI2);
		
		/** Preparation: start server with local host and port 8888 and let it listen for clients.*/
		GoServer testServer = new GoServer();
		InetAddress addr = InetAddress.getLocalHost();
		int port = 8888;
		testServer.createSocket(addr, port);
		new Thread(testServer).start();
		
		/**
		 * Test creating a connection. 
		 */
		humanClientServerCommunicator1.createConnection(addr, port);
		//Client indicates it made a successful connection
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		OUTCONTENT.reset();
		
		
		humanClientServerCommunicator1.doHandshake("Eline", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Client also prints the message from the server: a welcome & to which game it was added
		assertThat(OUTCONTENT.toString(), containsString("Welcome"));
		assertThat(OUTCONTENT.toString(), containsString("You have been added"));
		//Server prints that a client was added to a game (game cannot start yet)
		assertThat(OUTCONTENT.toString(), containsString(" was added to game "));
		assertThat(OUTCONTENT.toString(), not(containsString("The game can start!")));
		OUTCONTENT.reset();
		
		humanClientServerCommunicator2.createConnection(addr, port);
		//Client indicates it made a successful connection
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		OUTCONTENT.reset();
		
		humanClientServerCommunicator2.doHandshake("Joep", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Client also prints the message from the server: a welcome & to which game it was added
		assertThat(OUTCONTENT.toString(), containsString("Welcome"));
		assertThat(OUTCONTENT.toString(), containsString("You have been added"));
		//Server prints that a client was added to a game & that the game can start
		assertThat(OUTCONTENT.toString(), containsString(" was added to game "));
		assertThat(OUTCONTENT.toString(), containsString("The game can start!"));
		OUTCONTENT.reset();
		
		//TODO close connections?
	}
	
	/**
	 * Test adding one player to a game, disconnecting, then adding two players.
	 * Game should only start after the third player connected.
	 */
	@Test
	void testDisconnectBeforeStartGame() 
			throws ExitProgram, ServerUnavailableException, ProtocolException, IOException {
		HumanClientTUI humanClientTUI3 = new HumanClientTUI();
		HumanClientServerCommunicator humanClientServerCommunicator3 = 
				 					new HumanClientServerCommunicator(humanClientTUI3);
		HumanClientTUI humanClientTUI4 = new HumanClientTUI();
		HumanClientServerCommunicator humanClientServerCommunicator4 = 
				 					new HumanClientServerCommunicator(humanClientTUI4);
		HumanClientTUI humanClientTUI5 = new HumanClientTUI();
		HumanClientServerCommunicator humanClientServerCommunicator5 = 
				 					new HumanClientServerCommunicator(humanClientTUI5);
		
		/** Preparation: start server with local host and port 8888 and let it listen for clients.*/
		GoServer testServer = new GoServer();
		InetAddress addr = InetAddress.getLocalHost();
		int port = 8889;
		testServer.createSocket(addr, port);
		new Thread(testServer).start();
		
		//Connect and disconnect first client
		humanClientServerCommunicator3.createConnection(addr, port);
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		
		humanClientServerCommunicator3.doHandshake("Eline", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Server prints does not yet print that the game can start
		assertThat(OUTCONTENT.toString(), not(containsString("The game can start!")));
		humanClientServerCommunicator3.closeConnection();
		OUTCONTENT.reset();
		
		//Connect second client
		humanClientServerCommunicator4.createConnection(addr, port);
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		
		humanClientServerCommunicator4.doHandshake("Eline", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Server prints does not yet print that the game can start
		assertThat(OUTCONTENT.toString(), not(containsString("The game can start!")));
		
		//Connect third client
		humanClientServerCommunicator5.createConnection(addr, port);
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		
		humanClientServerCommunicator5.doHandshake("Eline", 'B');
		//Client prints a message to indicate which communication version will be used
		assertThat(OUTCONTENT.toString(), containsString("Communication will proceed according"));
		//Server prints does not yet print that the game can start
		assertThat(OUTCONTENT.toString(), containsString("The game can start!"));
		
		//TODO close connection?
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}

}
