package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

import com.nedap.go.gui.GoGUIIntegrator;

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
	private char color;
	private int boardDimension;
	private GoGUIIntegrator gogui;
	private List<String> prevBoards = new ArrayList<String>();
	//TODO add boards!

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
	 * 1) Creates a connection with a server.
	 * 2) Sends a handshake as defined in the protocol. 
	 * 3) Waits for the start-game message, start playing
	 * While game did not end {
	 * 4) Waits for turn message, returns move
	 * 5) Waits for result message, prints results
	 * }
	 * 
	 * When errors occur, or when the user terminates a server connection, the
	 * user is asked whether a new connection should be made.
	 */
	public void start() {
		//TODO Handle incoming messages starting with '?'
		
		//set boolean variables to keep track whether connections and signals worked
		boolean successfulConnection = false;
		boolean handshake = false;
		
		/** 
		 * Try to create a connection to a server. 
		 */
		successfulConnection = createConnectionWithUserInput();
		//If no connection, return (this will stop the thread?)
		if (!successfulConnection) {
			clientTUI.showMessage("Sorry, no connection could be established. " +
					"Hope to see you again in the future!");
			return;
		}
		
		/** 
		 * Send HELLO handshake. 
		 */
		try {
			doHandshakeWithUserInput();
			handshake = true;
		} catch (ProtocolException e) {
			clientTUI.showMessage("The server did not keep to the protocol during the handshake.");
			//TODO what to do when the protocol is not kept? Quit? (='return;')
		} catch (ServerUnavailableException e) {
			clientTUI.showMessage("The server cannot be reached anymore for the handshake.");
			//TODO what to do when the server cannot be reached anymore? Try again? 
			//Close connection? Check other SUE in other places, handle same way)
		}
		
		if (!handshake) {
			//TODO what to do (see questions above)
			return;
		}
		
		/** 
		 * Wait for start game message. 
		 * Upon reception of the message, set variables & let user know via console. 
		 */
		String message = "";
		try {
			message = readLineFromServer();
		} catch (ServerUnavailableException e) {
			clientTUI.showMessage("Could not read "
					+ "from server.");
			return;
			//TODO decide whether I want to quit here or whether I want to do something else.
		}
		//TODO handle message starting with '?'
		
		//When receiving the start game message, 
		if (message.charAt(0) == 'G') {
			try {
				startGame(message);
			} catch (ProtocolException e) {
				//TODO print specific error message as shown in startGame
				clientTUI.showMessage(e.getMessage());
				//TODO what to do when the protocol is not kept?
			} catch (ServerUnavailableException e) {
				clientTUI.showMessage("The server could not be reached for game start.");
				//TODO what to do when the server cannot be reached anymore? Try again? 
				//Close connection? Check other SUE in other places, handle same way)
			}
		} 
		
		try {
			playGame();
		} catch (ServerUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//TODO end game?
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
	
	public boolean createConnectionWithUserInput() {
		//variable to allow to try to connect a second time if not successful
		boolean reconnect = true;
		boolean successfulConnection = false;
		
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
					return successfulConnection;
				} catch (IOException e) {
					clientTUI.showMessage("ERROR: could not create a socket on " 
						+ addr + " and port " + port + ".");

					boolean userInput = clientTUI.getBoolean("Do you want to try again?");
					//If user doesn't want to try again, shut down, otherwise, loop will start again
					if (!userInput) {
						try {
							clientTUI.showMessage("Okay, we will shut down!");
							this.sendExit();
						} catch (ServerUnavailableException e1) {
							reconnect = clientTUI.getBoolean("The server is unavailable, " +
									"do you want a new connection to be made?");
						}
					}
				}
			}
		}
		//If there is a successful connection, you won't get here, return false
		return false;
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
	 * Get all the necessary components of the handshake message via the console.
	 * Then paste them together according to the protocol. Send the message to the
	 * server and wait for a response.
	 */
	
	public void doHandshakeWithUserInput() throws ServerUnavailableException, ProtocolException {
		
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
	
	public void doHandshake(String nameClient, char wantedColor) 
			throws ServerUnavailableException, ProtocolException {
		
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
	 * Method to handle the start message.
	 * Start message should be formatted as follows:
	 * PROTOCOL.GAME + PROTOCOL.DELIMITER + bord + PROTOCOL.DELIMITER + 
	 * 			ProtocolMessage.BLACK / ProtocolMessage.WHITE
	 * 
	 * Separates the messages and sets the color and dimensions of the board.
	 * Print a welcome message to the client.
	 */
	
	public void startGame(String message) throws ServerUnavailableException, ProtocolException {
		
		String[] commands = message.split(ProtocolMessages.DELIMITER);
		
		/** The first component of the message should be the command ProtocolMessages.GAME. */
		if (commands[0].length() != 1 || !commands[0].equals("G")) {
			throw new ProtocolException("ProtocolException: "
					+ "'G' expected as first command, but " + commands[0] + " received.");
			//TODO send back '?' (because invalid command)
		}
		
		/** The second component of the message should be a string representation of the board. */
		String board = commands[1];
		int numberOfPlaces = commands[1].length();
		boardDimension = (int) Math.sqrt(numberOfPlaces);
		
		/** 
		 * The third component of the message should be either 
		 * ProtocolMessages.WHITE or ProtocolMessages.BLACK. 
		 */
		String assignedColor = commands[2];
		if (assignedColor.length() != 1) {
			clientTUI.showMessage(Integer.toString(assignedColor.length()));
			throw new ProtocolException("ProtocolException: "
					+ "'B' or 'W' expected as third command, but " + commands[2] + " received.");
		}
		color = assignedColor.charAt(0);
		if (!(color == 'W' || color == 'B')) {
			throw new ProtocolException("ProtocolException: "
					+ "'B' or 'W' expected as third command, but " + commands[2] + " received.");
		}
		
		String clientsColor = "";
		if (color == 'W') {
			clientsColor = "white";
		} else {
			clientsColor = "black";
		}
		clientTUI.showMessage("The game has started! "
				+ "The board is " + boardDimension + " by " + boardDimension + ". "
				+ "\nYour color is " + clientsColor + ". Good luck!");
	}
	
	/** 
	 * Method that waits for a message from the server and responds.
	 * It can receive a turn message, an end message or a ?.
	 * (Or a result message, but that is handled in doTurn().)
	 * 
	 * It ends when 'Protocol.Messages.END' is received.
	 */
	public void playGame() throws ServerUnavailableException, ProtocolException {
		boolean gameEnded = false;
		
		while (!gameEnded) {
			//Wait for a message from the server
			String line = "";
			line = readLineFromServer();
			
			//Split the message into parts
			String[] commands = line.split(ProtocolMessages.DELIMITER);
			//if the first component is not of length 1, server does not comply with the protocol
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
					//TODO what to do if there aren't two other components
					getResult(commands[1], commands[2]);
					break;
				case 'E' :
					//TODO
					gameEnded = true;
					break;
				case '?':
					//TODO
					break;
				default :
					throw new ProtocolException("Server did not stick to the response protocol! " + 
							"It did not send a T, R, E or ? as the first part of its message.");
			}
		}
		//wait for turn message from server
		
		//print board
		
		//get user input about wanted move
		
		//check move
		
		//send move to server (then start again to wait for turn message)
	}
	
	/**
	 * Decide on a move, based on the board representation.
	 * 
	 * @param board
	 */
	public void doTurn(String board) {
		String move = "";
		String message;
		
		//add the board to the list of previous boards
		prevBoards.add(board);
		
		//Show the board to the client
		//TODO this should be a GUI
		clientTUI.showMessage("\nIt's you turn! The board currently looks like this:");
		for (int d = 0; d < boardDimension; d++) {
			clientTUI.showMessage(board.substring(d * boardDimension, 
					(d + 1) * boardDimension));
		}
		
		boolean passing = clientTUI.getBoolean("\nDo you want to pass?");
		if (passing) {
			move = Character.toString(ProtocolMessages.PASS);
		} else {
			move = Integer.toString(clientTUI.getInt("Where do you want to place "
					+ "your next marker?"));
			checkValid(move);
		}
		message = ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + move;
		sendToGame(message);
	}
	
	/**
	 * Check whether a suggested move is valid.
	 * 1) It does not result in a board that has been seen before
	 * 2) The suggested place is in between 0 & board.length-1
	 * 3) The suggested space is currently Unoccupied
	 */
	//checks whether a move is valid
	public void checkValid(String move) {
		Integer.parseInt(move);
		//check w
	}
	
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
	
	public void getResult(String validChar, String board) {
		//add the board to the list of previous boards
		prevBoards.add(board);
		
		//communicate result to the client
		if (Character.toString(ProtocolMessages.VALID).equals(validChar)) {
			clientTUI.showMessage("Your move was valid. The board now looks like this: ");
			for (int d = 0; d < boardDimension; d++) {
				clientTUI.showMessage(board.substring(d * boardDimension, 
						(d + 1) * boardDimension));
			}
			clientTUI.showMessage("It's now the other players turn. Please wait.");
		} else {
			clientTUI.showMessage("Your move was invalid. You lose the game.");
			//TODO add parameter to indicate why the game ended.
		}
		
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
