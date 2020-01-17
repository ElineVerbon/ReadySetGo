package tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
//import static org.hamcrest.CoreMatchers.not;
import org.junit.jupiter.api.*;

import client.*;
import exceptions.*;
import game.*;
import server.*;

public class ServerClientTest {
	private final static ByteArrayOutputStream OUTCONTENT = new ByteArrayOutputStream();
	private final static PrintStream ORIGINALOUT = System.out;

	//Create a client
	GoClientHuman client = new GoClientHuman();
	
	@BeforeAll
	static public void setUpStream() {
	    System.setOut(new PrintStream(OUTCONTENT));
	}
	
	@Test
	void testServer() 
			throws ExitProgram, ServerUnavailableException, ProtocolException, IOException {
		//Test with the server on local host and port 8888
		InetAddress addr = InetAddress.getLocalHost();
		int port = 8888;
		
		//Start server with local host and port 8888 and let it listen for client
		GoServer testServer = new GoServer();
		testServer.createSocket(addr, port);
		new Thread(testServer).start();
		
		//wait to ensure that server has started
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create a connection. Expected: server indicates a new client has connected.
		client.createConnection(addr, port);
		assertThat(OUTCONTENT.toString(), 
				containsString("You made a succesful connection!"));
		OUTCONTENT.reset();
		
//		// Do the HELLO handshake. Expect a welcome message.
//		client.doHandshake();
//		assertThat(OUTCONTENT.toString(), containsString(
//			"Welcome to the Hotel booking system of U Parkhotel!"));
//		OUTCONTENT.reset();
//		
//		// Check in a guest
//		client.doIn(GUEST1);
//		assertThat(OUTCONTENT.toString(), containsString(
//			"> CheckIn successful, you room number is "));
//		OUTCONTENT.reset();
//		
//		// TRY TO Activate safe of GUEST1 (fails)
//		client.doAct(GUEST1, "");
//		assertThat(OUTCONTENT.toString(), containsString(
//				"> Parameter is wrong (password is required)."));
//		OUTCONTENT.reset();
//		
//		// Retrieve room information of GUEST1
//		client.doRoom(GUEST1);
//		assertThat(OUTCONTENT.toString(), containsString("> Guest ... is in room number ..."));
//		OUTCONTENT.reset();
//		
//		// Retrieve room information of a non existing guest
//		client.doRoom(FAKE_GUEST);
//		assertThat(outContent.toString(), containsString(
//					"> Received: r;" + FAKE_GUEST + System.lineSeparator()));
//		outContent.reset();
//		
//		// Check in a second guest
//		client.doIn(GUEST2);
//		assertThat(OUTCONTENT.toString(), containsString(
//			"> CheckIn successful, you room number is "));
//		OUTCONTENT.reset();
//		
//		// Retrieve the state of the hotel
//		client.doPrint();
//		assertThat(OUTCONTENT.toString(), containsString("> The state of hotel ... is:"));
//		OUTCONTENT.reset();
//		
//		// Get the bill of guest 1 for a certain number of nights
//		client.doBill(GUEST1, NIGHTS);
//		assertThat(OUTCONTENT.toString(), containsString("> The bill of guest ... is:"));
//		OUTCONTENT.reset();
//		
//		//Expect a local error message when no integer is given (i.e. no message sent to the server)
//		client.doBill(GUEST1, INVALID_NIGHTS);
//		assertThat(OUTCONTENT.toString(), containsString(
//			"ERROR: " + INVALID_NIGHTS + " is not an integer"));
//		OUTCONTENT.reset();
//		
//		// Check out GUEST1
//		client.doOut(GUEST1);
//		assertThat(OUTCONTENT.toString(), containsString("> CheckOut successful."));
//		OUTCONTENT.reset();
//		
//		// Exit the program
//		client.sendExit();
	}
	
	@AfterAll
	static void restoreStream() {
	    System.setOut(ORIGINALOUT);
	}

}
