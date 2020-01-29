package tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import client.ClientTUI;
import client.ServerHandler;
import client.Smart1ComputerPlayer;
import exceptions.ExitProgram;
import exceptions.ProtocolException;
import exceptions.ServerUnavailableException;
import server.Server;

/**
 * This test tests the computer player
 */

public class ClientTest {
	private final static ByteArrayOutputStream OUTCONTENT = new ByteArrayOutputStream();
	private final static PrintStream ORIGINALOUT = System.out;

	@BeforeAll
	static public void setUpStream() {
	    System.setOut(new PrintStream(OUTCONTENT));
	}
	
	/**
	 * Test creating a connection and doing a handshake. 
	 */
	@Test
	void testComputerPlayer() 
			throws ExitProgram, ServerUnavailableException, ProtocolException, IOException {
		
		Smart1ComputerPlayer computerPlayer = new Smart1ComputerPlayer();
		
		
		/** Preparation: start server with local host and port 8888 and let it listen for clients.*/
		Server testServer = new Server();
		InetAddress addr = InetAddress.getLocalHost();
		int port = 8888;
		testServer.createSocket(port);
		new Thread(testServer).start();
		
		/**
		 * Test creating a connection. 
		 */
		computerPlayer.start();
		
		//TODO close connections?
	}
}
