package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import exceptions.ExitProgram;
import exceptions.ProtocolException;
import exceptions.ServerUnavailableException;
import protocol.MessageGenerator;
import protocol.ProtocolMessages;

/**
 * Handles the communication between a given player and the server it connected to.
 */

public class ServerHandler {
	
	// The socket and In- and OutputStreams.
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	// Version information
	private String wantedVersion;
	private String usedVersion;
	
	// The TUI of the connected client.
	private ClientTUI clientTUI;
	
	//The message generator
	private MessageGenerator messageGenerator;
	
	//Game states
	private boolean successfulConnection;
	private boolean successfulHandshake;
	
	/**
	 * Constructor.
	 */
	public ServerHandler(ClientTUI givenClientTUI) {
		clientTUI = givenClientTUI;
		wantedVersion = "1.0";
		messageGenerator = new MessageGenerator();
	}
	
	public String getVersion() {
		return usedVersion;
	}
	
	public boolean getSuccessfulConnection() {
		return successfulConnection;
	}
	
	public boolean getSuccessfulHandshake() {
		return successfulHandshake;
	}
	
	/**
	 * Creates a connection to a server with the user-defined IP and port number. 
	 * 
	 * The method continues to ask for an IP and port and attempts to connect until either 
	 * a connection is established or the user indicates he/she doesn't want to try anymore.
	 * 
	 * @throws ExitProgram if a connection is not established and the user 
	 * 				       indicates to want to exit the program.
	 * @ensures serverSock contains a valid socket connection to a server
	 */
	
	public void createConnectionWithUserInput() {
		boolean reconnect = true;
		
		sock = null; //to enable a new game to be started after an end game
		
		while (reconnect) {
			while (sock == null) {
				InetAddress addr = clientTUI.getIp("To which IP address do you want to connect?");
				int port = clientTUI.getInt("On which port do you want to listen?");
				
				try {
					createConnection(addr, port);
					return;
				} catch (IOException e) {
					clientTUI.showMessage("ERROR: could not create a socket on " 
						+ addr + " and port " + port + ".");

					boolean userInput = clientTUI.getBoolean("Do you want to try again?");
					if (!userInput) {
						clientTUI.showMessage("Okay, we will shut down!");
					}
				}
			}
		}
		if (!successfulConnection) {
			clientTUI.showMessage("Sorry, no connection could be established. " +
					"Hope to see you again in the future!");
		}
	}
	
	/**
	 * Creates a connection to the server with the given IP and port.
	 * 
	 * @throws IO Exception if the connection cannot be made 
	 * @ensures serverSock contains a valid socket connection to a server
	 */
	public void createConnection(InetAddress addr, int port) throws IOException {
		successfulConnection = false;
		
		clearConnection();
			
		clientTUI.showMessage("Attempting to connect to " + addr + ":" 
			+ port + "...");
		sock = new Socket(addr, port); //this is the socket to the server
		in = new BufferedReader(new InputStreamReader(
				sock.getInputStream())); //data from the server to this socket
		out = new BufferedWriter(new OutputStreamWriter(
				sock.getOutputStream())); 
		clientTUI.showMessage("You made a succesful connection!");
		successfulConnection = true;
	}
	
	/**
	 * Send a handshake message, based on user input, to the server and wait for the reply.
	 */
	
	public void doHandshakeWithUserInput() {
		
		boolean correctName = false;
		String nameClient = "";
		while (!correctName) {
			nameClient = clientTUI.getString("What name do you want to use?");
			if (nameClient.length() <= 10) {
				correctName = true;
			} else {
				clientTUI.showMessage("The maximum number of allowed characters " +
						"(including spaces) is ten!");
			}
		}
		
		boolean correctColor = false;
		char wantedColor = '!';
		while (!correctColor) {
			String userInput = clientTUI.getString("Which color do you want to play? Black/White");
			if (userInput.equalsIgnoreCase("white")) {
				wantedColor = ProtocolMessages.WHITE;
				correctColor = true;
			} else if (userInput.equalsIgnoreCase("black")) { 
				wantedColor = ProtocolMessages.BLACK;
				correctColor = true;
			} else { 
				clientTUI.showMessage("Only 'black' and 'white' are allowed as answer.");
			}
		}

		doHandshake(nameClient, wantedColor);
	}
	
	/**
	 * Send a handshake to the server containing the client's name and the wanted color.
	 * 
	 * After sending, wait for response, which should be formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
	 * optionally these at the end: PROTOCOL.delimiter + message (string)
	 * 
	 * @throws ProtocolException 
	 */
	
	public void doHandshake(String nameClient, char wantedColor) {
		
		sendToGame(messageGenerator.clientHandshakeMessage(wantedVersion, nameClient, wantedColor));
		
		String line = "";
		line = readLineFromServer();
		if (line == null) {
			return;
		}
		
		String[] serverResponse = line.split(ProtocolMessages.DELIMITER);
		if (line.charAt(0) != ProtocolMessages.HANDSHAKE) {
			clientTUI.showMessage("The server did not keep to the protocol during the handshake.");
		}
		
		usedVersion = serverResponse[1]; 
		if (serverResponse.length > 2) {
			clientTUI.showMessage("You connected to a server. Communication will proceed " +
					"according to version " + usedVersion + ".\n" +
					"You received the following message from the server: ");
			clientTUI.showMessage(serverResponse[2]);
		} else {
			clientTUI.showMessage("You connected to a server. Communication will proceed " +
					"according to version " + usedVersion + ".\n");
		}
		successfulHandshake = true;
	}
	
	/**
	 * Reads and returns one line from the server.
	 * 
	 * @return the line sent by the server.
	 * @throws ServerUnavailableException if IO errors occur.
	 */
	public String readLineFromServer() {
		String answer = null;
		
		if (in != null) {
			try {
				answer = in.readLine();
				
				if (answer == null) {
					clientTUI.showMessage("\nServer disconnected. The connection will be closed.");
					closeConnection();
				}
			} catch (IOException e) {
				clientTUI.showMessage("\nServer cannot be reached. The connection will be closed.");
				closeConnection();
			}
		} else {
			clientTUI.showMessage("Could not read from server.");
		}
		return answer;
	}
	
	/**
	 * Sends one line to the server.
	 * 
	 * @return the line sent by the server.
	 * @throws ServerUnavailableException if IO errors occur.
	 */
	public void sendToGame(String message) {
		try {
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			clientTUI.showMessage("Sorry, the server cannot be reached!");
			clientTUI.showMessage("We will close the connection.");
			closeConnection();
		}
	}
	
	/**
	 * Resets the serverSocket and In- and OutputStreams to null.
	 */
	public void clearConnection() {
		sock = null;
		in = null;
		out = null;
	}
	
	/**
	 * Closes the socket.
	 */
	public void closeConnection() {
		clientTUI.showMessage("Closing the connection...");
		try {
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientTUI.showMessage("Connection has been closed, hope to see you again someday!");
	}
}
