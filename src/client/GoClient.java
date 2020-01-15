package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import exceptions.*;
import protocol.ProtocolMessages;

public class GoClient {
	
	private Socket sock;
	private BufferedReader in;
	private BufferedWriter out;
	private GoClientTUI clientTUI;
	private String wantedVersion; //set in the constructor
	private String usedVersion; //given back by server upon handshake

	/**
	 * Constructs a new GoClient. Initialises the TUI.
	 */
	public GoClient() {
		clientTUI = new GoClientTUI(this);
		wantedVersion = "1.0";
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new GoClient()).start();
	}
	
	/**
	 * Starts a new GoClient by creating a connection, followed by the 
	 * HELLO handshake as defined in the protocol. After a successful 
	 * connection and handshake, the tui is started. The tui asks for 
	 * user input and handles all further calls to methods of this class. 
	 * 
	 * When errors occur, or when the user terminates a server connection, the
	 * user is asked whether a new connection should be made.
	 */
	public void start() {
		boolean restart = false;
		boolean successfulConnection = false;
		
		do {
			//create a connection
			try {
				this.createConnection();
				successfulConnection = true;
				clientTUI.showMessage("You made a succesful connection!");
			} catch (ExitProgram e) {
				try {
					this.sendExit();
				} catch (ServerUnavailableException e1) {
					restart = clientTUI.getBoolean("The server is unavailable, do you want a new connection to be made?");
				}
			}
			
			//send HELLO handshake
			try {
				this.doHandshake();
			} catch (ServerUnavailableException | ProtocolException e) {
				restart = clientTUI.getBoolean("The server is unavailable or the protocol was not kept, do you want a new connection to be made?");
			}
		} while(restart);
		
		//start the view, that will take care of getting input from the user
		if(successfulConnection) {
			try {
				clientTUI.start();
			} catch (ServerUnavailableException e) {
				e.printStackTrace();
			}
		}
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
	public void createConnection() throws ExitProgram {
		clearConnection();
		
		InetAddress addr = clientTUI.getIp("To which IP address do you want to try to connect?");
		int port = clientTUI.getInt("On which port do you want to listen?");
		
		while (sock == null) {
			// try to open a Socket to the server
			try {
				clientTUI.showMessage("Attempting to connect to " + addr + ":" 
					+ port + "...");
				sock = new Socket(addr, port); //this is the socket to the server
				in = new BufferedReader(new InputStreamReader(
						sock.getInputStream())); //data from the server to this socket
				out = new BufferedWriter(new OutputStreamWriter(
						sock.getOutputStream())); //data from the client via this socket to the buffer 
			} catch (IOException e) {
				clientTUI.showMessage("ERROR: could not create a socket on " 
					+ addr + " and port " + port + ".");

				boolean userInput = clientTUI.getBoolean("Do you want to try again?");
				if(userInput) { 
					addr = clientTUI.getIp("To which IP address do you want to try to connect this time?");
					port = clientTUI.getInt("On which port do you want to listen this time?");
				}
				//Do you want to try again? (ask user, to be implemented)
				if(!userInput) {
					throw new ExitProgram("User indicated to exit.");
				}
			}
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
	
	/**
	 * Send handshake to the server according to the following protocol:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * After sending, wait for response, which should be formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
	 * optionally these at the end: PROTOCOL.delimiter + message (string)
	 * @throws ProtocolException 
	 * 
	 * 
	 */
	
	public void doHandshake() throws ServerUnavailableException, ProtocolException {
		String message = "";
		
		/** get name of client */
		boolean correctName = false;
		String nameClient = "";
		while(!correctName) {
			nameClient = clientTUI.getString("What name do you want to use?");
			if(nameClient.length() <= 10) {
				correctName = true;
			} else {
				clientTUI.showMessage("The maximum number of allowed characters (including spaces) is ten!");
			}
		}
		
		/** get black or white from the player */
		boolean correctColor = false;
		char wantedColor = '!';
		while(!correctColor) {
			String color = clientTUI.getString("Which color do you want to play with? white / black");
			if(color.equalsIgnoreCase("white")) {
				wantedColor = ProtocolMessages.WHITE;
				correctColor = true;
			} else if(color.equalsIgnoreCase("black")) { 
				wantedColor = ProtocolMessages.BLACK;
				correctColor = true;
			} else { 
				clientTUI.showMessage("Only 'black' and 'white' are allowed as answer.");
			}
		}
		
		message = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER + wantedVersion + ProtocolMessages.DELIMITER + nameClient 
				 + ProtocolMessages.DELIMITER + wantedColor;
		
		/** send handshake to server, read response */
		String line = "";
		try{
			out.write(message);
			out.newLine();
			out.flush();
			line = readLineFromServer();
		} catch (IOException e) {
			e.printStackTrace();
			throw new ServerUnavailableException("Could not read "
					+ "from server.");
		}
		
		/** 
		 * check whether server response complies with the protocol: 
		 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
		 * optionally these at the end: PROTOCOL.delimiter + message (string)
		 */ 
		
		//check whether the handshake message came first, if not: throw exception
		String[] serverResponse = line.split(ProtocolMessages.DELIMITER);
		if(line.charAt(0) != ProtocolMessages.HANDSHAKE) {
			throw new ProtocolException("Server response does not comply with the protocol!");
		}
		
		//get version of the communication procotol and print message(s)
		usedVersion = serverResponse[1];
		//get message if available
		if(serverResponse.length > 2) {
			clientTUI.showMessage(serverResponse[2]);
			//correct response was received, print hotel name (= String coming after the HELLO indicator
			clientTUI.showMessage("Previous message came from the server. In case it was not included in the message, communication will proceed according to version " + serverResponse[1] + "!");
		} else {
			//correct response was received, print hotel name (= String coming after the HELLO indicator
			clientTUI.showMessage("Welcome to the Go server, we will communicate according to version " + usedVersion + "!");
		}
	}
	
	/**
	 * Reads and returns one line from the server.
	 * 
	 * @return the line sent by the server.
	 * @throws ServerUnavailableException if IO errors occur.
	 */
	public String readLineFromServer() 
			throws ServerUnavailableException {
		if (in != null) {
			try {
				// Read and return answer from Server
				String answer = in.readLine();
				if (answer == null) {
					throw new ServerUnavailableException("Could not read "
							+ "from server.");
				}
				return answer;
			} catch (IOException e) {
				e.printStackTrace();
				throw new ServerUnavailableException("Could not read "
						+ "from server.");
			}
		} else {
			throw new ServerUnavailableException("Could not read "
					+ "from server.");
		}
	}
	
	public void sendExit() throws ServerUnavailableException {
		char toServer = ProtocolMessages.EXIT;
		try{
			out.write(toServer);
		} catch (IOException e) {
			throw new ServerUnavailableException("Could not read "
					+ "from server.");
		}
		
		closeConnection();
	}
}
