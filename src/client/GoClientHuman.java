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

public class GoClientHuman implements GoClient {
	
	//variables used to start a connection with the server
	private Socket sock;
	private BufferedReader in;
	private BufferedWriter out;
	private GoClientHumanTUI clientTUI;
	private String wantedVersion; //set in the constructor
	private String usedVersion; //given back by server upon handshake
	
	//variables to play a game
	private String color;
	private int boardDimension;

	/**
	 * Constructs a new GoClient. Initialises the TUI.
	 */
	public GoClientHuman() {
		clientTUI = new GoClientHumanTUI(this);
		wantedVersion = "1.0";
	}
	
	/**
	 * This method starts a new GoClient.
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		(new GoClientHuman()).start();
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
		boolean reconnect = false;
		boolean successfulConnection = false;
		boolean restart = false;
		boolean gameStarted = false;
		
		do {
			/** Create a connection. */
			try {
				this.createConnection();
				successfulConnection = true;
				clientTUI.showMessage("You made a succesful connection!");
			} catch (ExitProgram e) {
				try {
					this.sendExit();
				} catch (ServerUnavailableException e1) {
					reconnect = clientTUI.getBoolean("The server is unavailable, " +
							"do you want a new connection to be made?");
				}
			}
			
			//** Send HELLO handshake. */
			try {
				doHandshake();
			} catch (ServerUnavailableException | ProtocolException e) {
				reconnect = clientTUI.getBoolean("The server is unavailable or it did not " +
						"keep to the protocol, do you want a new connection to be made?");
			}
		} while (reconnect);
		
		//TODO if no connection, what should I do? Not continue, at least. Quit the program?
		//close connection? first send 'quit' (but there might be no connection)
		
		if (successfulConnection) {
			
			do {
				/** Wait for the start-game-message. */
				try {
					doStart();
					gameStarted = true;
				} catch (ServerUnavailableException | ProtocolException e) {
					restart = clientTUI.getBoolean("The server is unavailable or it did not " +
							"keep to the protocol, do you want to try again?");
				}
			} while (restart);
		
		}
		
		if (gameStarted) {
			try {
				playGame();
			} catch (ServerUnavailableException | ProtocolException e) {
				// TODO Auto-generated catch block
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
		
		/** 
		 * TODO: user the user input or test version to assign values to addr and port. 
		 * */
		// Get user input about where to connect & try connecting.
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
						sock.getOutputStream())); 
						//data from the client via this socket to the buffer 
			} catch (IOException e) {
				clientTUI.showMessage("ERROR: could not create a socket on " 
					+ addr + " and port " + port + ".");

				boolean userInput = clientTUI.getBoolean("Do you want to try again?");
				if (userInput) { 
					addr = clientTUI.getIp("To which IP address do you want to try " +
							"to connect this time?");
					port = clientTUI.getInt("On which port do you want to listen this time?");
				}
				//Do you want to try again? (ask user, to be implemented)
				if (!userInput) {
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
	
	/**.
	 * First get necessary information from the user via the console. 
	 * 
	 * Then send a handshake to the server according to the following protocol:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * After sending, wait for response, which should be formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
	 * optionally these at the end: PROTOCOL.delimiter + message (string)
	 * @throws ProtocolException 
	 */
	
	public void doHandshake() throws ServerUnavailableException, ProtocolException {
		
		/**
		 * Get all the necessary components of the handshake message via the console.
		 * Then paste them together according to the protocol. Send the message to the
		 * server and wait for a response.
		 */
		
		String message = "";
		
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
		
		//get 'black' or 'white' from the console
		boolean correctColor = false;
		char wantedColor = '!';
		while (!correctColor) {
			String userInput = clientTUI.getString("Which color do you want to play? Black/White");
			if (userInput.equalsIgnoreCase("white")) {
				wantedColor = ProtocolMessages.WHITE;
				correctColor = true;
			} else if (color.equalsIgnoreCase("black")) { 
				wantedColor = ProtocolMessages.BLACK;
				correctColor = true;
			} else { 
				clientTUI.showMessage("Only 'black' and 'white' are allowed as answer.");
			}
		}
		
		//assemble the handshake message that will be sent to the server.
		message = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER + wantedVersion + 
				ProtocolMessages.DELIMITER + nameClient + ProtocolMessages.DELIMITER + wantedColor;
		
		//send handshake message to the server, read the response.
		String line = "";
		try {
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
		 * Check whether server response complies with the protocol: 
		 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
		 * optionally these at the end: PROTOCOL.delimiter + message (string)
		 */ 
		
		//check whether the handshake character came first, if not: throw exception
		String[] serverResponse = line.split(ProtocolMessages.DELIMITER);
		if (line.charAt(0) != ProtocolMessages.HANDSHAKE) {
			throw new ProtocolException("Server response does not comply with the protocol!");
		}
		
		//get version of the communication procotol and print message(s)
		usedVersion = serverResponse[1]; //TODO check whether the version is valid?
		//get message if available
		if (serverResponse.length > 2) {
			//correct response was received, message was printed. Print this as well to be sure
			clientTUI.showMessage("You connected to a server. Communication will proceed " +
					"according to version " + usedVersion + ".\n" +
					"You received the following message from the server: ");
			clientTUI.showMessage(serverResponse[2]);
		} else {
			//correct response was received, print welcome string
			clientTUI.showMessage("You connected to a server. Communication will proceed " +
					"according to version " + usedVersion + ".\n");
		}
	}
	
	/**
	 * Method to handle the start message.
	 * Start message should be formatted as follows:
	 * PROTOCOL.GAME + PROTOCOL.DELIMITER + bord + PROTOCOL.DELIMITER + color
	 * 
	 * Separates the messages and sets the color and dimensions of the board.
	 */
	
	public void doStart() throws ServerUnavailableException, ProtocolException {
		//Wait for the start method & run the doStart method.
		String line = "";
		line = readLineFromServer();
		
		String[] commands = line.split(ProtocolMessages.DELIMITER);
		if (commands[0].length() != 1 || commands[0] != "G") {
			throw new ProtocolException("Server response does not comply with the start protocol!");
			//TODO send back '?' (because invalid command)
		}
		
		//set dimensions of board
		int numberOfPlaces = Integer.parseInt(commands[1]);
		boardDimension = (int) Math.sqrt(numberOfPlaces);
		
		//set assigned color
		color = commands[2];
		
	}
	
	/** 
	 * Method that waits for a message from the server and responds.
	 * It ends when 'Protocol.Messages.END' is received.
	 */
	public void playGame() throws ServerUnavailableException, ProtocolException {
		
		//Wait for a message from the server
		String line = "";
		line = readLineFromServer();
		
		//Split the message into parts
		String[] commands = line.split(ProtocolMessages.DELIMITER);
		if (commands[0].length() != 1) {
			throw new ProtocolException("Server response does not comply with the protocol! " + 
					"It did not send a char as the first part of its message.");
		}
		
		//check which kind of message is received
		char command = line.charAt(0);
		switch (command) {
			case 'T':
				doTurn(commands[1]);
				break;
			case 'R' :
				//TODO
				break;
			case 'E' :
				//TODO
				break;
			default :
				throw new ProtocolException("Server response does not comply with the protocol! " + 
						"It did not send a T, R or E as the first part of its message return.");
		}
		//wait for turn message from server
		
		//print board
		
		//get user input about wanted move
		
		//check move
		
		//send move to server (then start again to wait for turn message)
	}
	
	public void doTurn(String board) {
		//Incoming message is a String representation of the board
		for (int d = 0; d < boardDimension; d++) {
			clientTUI.showMessage(board.substring(d * boardDimension, 
					(d + 1) * boardDimension - 1));
		}
	}
	
	public void getResult(String[] commands) {
		//TODO
	}
	
	public void endGame(String[] commands) {
		//TODO
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
		try {
			out.write(toServer);
		} catch (IOException e) {
			throw new ServerUnavailableException("Could not read "
					+ "from server.");
		}
		
		closeConnection();
	}
}
