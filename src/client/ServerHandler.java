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
import protocol.ProtocolMessages;

public class ServerHandler {
	/** The socket and In- and OutputStreams. */
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	private String wantedVersion; //set in the constructor
	private String usedVersion; //given back by server upon handshake
	
	/** The connected human client. */
	ClientTUI clientTUI;
	
	public ServerHandler(ClientTUI givenClientTUI) {
		clientTUI = givenClientTUI;
		wantedVersion = "1.0";
	}
	
	public String getVersion() {
		return usedVersion;
	}
	
	/**
	 * Creates a connection to the server. Requests the IP and port to 
	 * connect to via the TUI.
	 * 
	 * The method continues to ask for an IP and port and attempts to connect 
	 * until a connection is established or until the user indicates to exit 
	 * the program.
	 * 
	 * @throws ExitProgram if a connection is not established and the user 
	 * 				       indicates to want to exit the program.
	 * @ensures serverSock contains a valid socket connection to a server
	 */
	
	public void createConnectionWithUserInput() {
		//variable to allow to try to connect a second time if not successful
		boolean reconnect = true;
		boolean successfulConnection = false;
		
		sock = null; //to enable restarts
		
		while (reconnect) {
			while (sock == null) {
				//Get user input about where to connect & try connecting.
				InetAddress addr = clientTUI.getIp("To which IP address do you want to connect?");
				int port = clientTUI.getInt("On which port do you want to listen?");
				
				// try to open a Socket to the server
				try {
					createConnection(addr, port);
					//successful connection, return
					successfulConnection = true;
					return;
				} catch (IOException e) {
					clientTUI.showMessage("ERROR: could not create a socket on " 
						+ addr + " and port " + port + ".");

					boolean userInput = clientTUI.getBoolean("Do you want to try again?");
					//If user doesn't want to try again, shut down, otherwise, loop will start again
					if (!userInput) {
						clientTUI.showMessage("Okay, we will shut down!");
						closeConnection();
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
	 * 
	 * @ensures serverSock contains a valid socket connection to a server
	 */
	public void createConnection(InetAddress addr, int port) throws IOException {
		clearConnection();
			
		clientTUI.showMessage("Attempting to connect to " + addr + ":" 
			+ port + "...");
		sock = new Socket(addr, port); //this is the socket to the server
		in = new BufferedReader(new InputStreamReader(
				sock.getInputStream())); //data from the server to this socket
		out = new BufferedWriter(new OutputStreamWriter(
				sock.getOutputStream())); 
		clientTUI.showMessage("You made a succesful connection!");
	}
	
	/**
	 * Get all the necessary components of the handshake message via the console.
	 * Then paste them together according to the protocol. Send the message to the
	 * server and wait for a response.
	 */
	
	public void doHandshakeWithUserInput() {
		
		//get name of client
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
		
		//get user color preference from the console
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

		//perform the handshake
		doHandshake(nameClient, wantedColor);
	}
	
	/**
	 * Send a handshake to the server. Follow this protocol:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * After sending, wait for response, which should be formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
	 * optionally these at the end: PROTOCOL.delimiter + message (string)
	 * @throws ProtocolException 
	 */
	
	public void doHandshake(String nameClient, char wantedColor) {
		
		//assemble the handshake message that will be sent to the server.
		String message = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER + wantedVersion + 
				ProtocolMessages.DELIMITER + nameClient + ProtocolMessages.DELIMITER + wantedColor;
		
		//send handshake message to the server, read the response.
		String line = "";
		try {
			out.write(message);
			out.newLine();
			out.flush();
			line = readLineFromServer();
			if (line == null) {
				return;
			}
		} catch (IOException e) {
			clientTUI.showMessage("The server cannot be reached anymore for the handshake.");
			//TODO what to do when the server cannot be reached anymore? Try again? 
			//Close connection? Check other SUE in other places, handle same way)
		}
		
		/** 
		 * Check whether server response complies with the protocol: 
		 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
		 * optionally these at the end: PROTOCOL.delimiter + message (string)
		 */ 
		
		//check whether the handshake character came first, if not: throw exception
		String[] serverResponse = line.split(ProtocolMessages.DELIMITER);
		if (line.charAt(0) != ProtocolMessages.HANDSHAKE) {
			clientTUI.showMessage("The server did not keep to the protocol during the handshake.");
		}
		
		//get version of the communication protocol and print message(s)
		usedVersion = serverResponse[1]; //TODO check whether the version is valid?
		//get message if available
		if (serverResponse.length > 2) {
			//correct response was received. Print own message
			clientTUI.showMessage("You connected to a server. Communication will proceed " +
					"according to version " + usedVersion + ".\n" +
					"You received the following message from the server: ");
			//print server message
			clientTUI.showMessage(serverResponse[2]);
		} else {
			//correct response was received. Print own message (no message received).
			clientTUI.showMessage("You connected to a server. Communication will proceed " +
					"according to version " + usedVersion + ".\n");
		}
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
				// Read and return answer from Server
				answer = in.readLine();
				
				if (answer == null) {
					clientTUI.showMessage("\nServer disconnected. The connection will be closed.");
					closeConnection();
				}
				return answer;
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
			//TODO shut down?
		}
	}
	
	/**
	 * Resets the serverSocket and In- and OutputStreams to null.
	 * 
	 * Always make sure to close current connections via shutdown() 
	 * before calling this method!
	 */
	public void clearConnection() {
		sock = null;
		in = null;
		out = null;
	}
	
	/**
	 * Closes the connection by closing the In- and OutputStreams, as 
	 * well as the serverSocket.
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
