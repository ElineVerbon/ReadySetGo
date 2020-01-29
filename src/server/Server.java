package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import exceptions.ExitProgram;
import protocol.ProtocolMessages;

/**
 * Server for playing GO
 * 
 * The server listens for clients continuously. When a client connects, the 
 * server starts a clientHandler to handle interaction with the client.
 * It will then add the client to a game instance. Once two clients are in a game,
 * the game is started and control is given to the game instance.
 */


public class Server implements Runnable {
	
	/** The Socket of this GoServer (is a serverSocket!). */
	private ServerSocket ssock;

	/** List of GoClientHandlers, one for each connected client. */
	private List<ClientHandler> clients;
	
	/** Next client number, increasing for every new connection. */
	private int nextClientNo;
	
	/** List of Games, one for each two connected clients. */
	private List<Game> games;
	
	/** Next game number, increasing for every new connection. */
	private int nextGameNo;
	
	/** User-defined variables for the board size and komi of the games hosted by this server. */
	int boardDimension;
	double komi;
	
	/** Available versions of this server. */
	private List<String> availableVersions = new ArrayList<String>();
	private String usedVersion;

	/**  
	 * The TUI of this GoServer.
	 * Required to ask for port number and board size.
	 */
	private ServerTUI tui;
	
	// ------------------ Main --------------------------

	/** 
	 * Start a new GoServer.
	 * A GoServer is constructed, a serverSocket set up and then its run() 
	 * method is called in a new thread to continuously listen for new clients. 
	 */
	public static void main(String[] args) {
		Server server = new Server();
		System.out.println("Welcome to the Server hosting Go! Starting...");
		
		try {
			server.setup();
		} catch (ExitProgram e1) {
			return;
		}
		
		new Thread(server).start();
	}
	
	/**
	 * Constructor of a GoServer.
	 */
	
	public Server() {
		clients = new ArrayList<>();
		nextClientNo = 1;
		games = new ArrayList<Game>();
		nextGameNo = 1;
		tui = new ServerTUI();
		
		availableVersions.add("0.01");
	}
	
	/**
	 * Sets up a ServerSocket on a user-defined port.
	 * If the socket cannot be created, the user can decide whether to try again, 
	 * after which a new port is entered, or to stop, resulting in an ExitProgram exception.
	 * 
	 * @throws ExitProgram if a connection can not be created on the given 
	 *                     port and the user decides to exit the program.
	 * @ensures a serverSocket is opened.
	 */
	public void setup() throws ExitProgram {
		ssock = null;
		while (ssock == null) {
			tui.showMessage("To start, please answer some questions to initialize the server.\n");
			boardDimension = tui.getInt("Please enter a positive integer to set the board"
					+ "size of the games that you will host. (Minimum is 3.)", 3);
			
			komi = tui.getDouble("Please enter a double (eg 0.5) to set the komi (= penalty"
					+ " for black) for the games that you will host.");
			
			int port = tui.getInt("Please enter the number of the server port " +
					"that you want to listen on.", 1281);
			
			// try to open a new ServerSocket
			try {
				createSocket(port);
			} catch (IOException e) {
				tui.showMessage("ERROR: could not create a socket on port " + port + ".");

				if (!tui.getBoolean("Do you want to try again? (yes/no)")) {
					throw new ExitProgram("User indicated to exit the "
							+ "program.");
				}
			}
		}
	}
	
	/**
	 * Create a socket with given IP and port (also useful for testing purposes).
	 * Is called in setup()
	 * 
	 * @param addr
	 * @param port
	 * @throws IOException
	 */
	public void createSocket(int port) throws IOException {
		tui.showMessage("Attempting to open a socket on port " + port + "...");
		ssock = new ServerSocket(port, 0);
		tui.showMessage("Socket opened, waiting for a client.");
	}
	
	/**
	 * Create connections with clients via the server's ServerSocket.
	 * 
	 * The ServerSocket listens for new clients, makes a clientHandler 
	 * for each connected client and starts the clientHandler in a new 
	 * thread (so the ServerSocket can continue listening for new clients).
	 */
	
	public void run() {
		boolean openNewSocket = true;
		while (openNewSocket) {
			try {
				Socket sock = ssock.accept();
				tui.showMessage("Client number " + nextClientNo + " just connected!");
				
				ClientHandler handler = new ClientHandler(sock, this);
				new Thread(handler).start();
				
				clients.add(handler);
				nextClientNo++;
			} catch (IOException e) {
				tui.showMessage("A server IO error occurred: " 
						+ e.getMessage() + " The server will shut down.");
				openNewSocket = false;
			}
		}
	}
	
	/**
	 * Read handshake from a client. If correct: set usedVersion,
	 * add client to a game & send a response
	 * 
	 * Handshake from the client is correct if it is formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * Response string should be formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
	 * optionally these at the end: PROTOCOL.delimiter + message (string)
	 */
	
	public String doHandshake(String requestedVersion, String nameClient) {
		String response;
		
		/**
		 * check if requested version is available, if so:
		 * it will be used for communication with this client, if not:
		 * version 1.0 will be used. 
		 */
		usedVersion = "1.0";
		for (String version : availableVersions) {
			if (version.equals(requestedVersion)) {
				usedVersion = requestedVersion;
				break;
			}
		}
		String message = "Welcome " + nameClient + " to the GO server! " +
				"Communication will proceed via version " + usedVersion + ".";
		
		response = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER 
				+ usedVersion + ProtocolMessages.DELIMITER + message;
		
		return response;
	}
	
	/**
	 * Add a client to a game.
	 * If no games started yet or all games have already started, start a new game.
	 * 
	 * Otherwise, add the player to an existing game. When second player connects, 
	 * try to send startGame message to the first player to check
	 * whether he/she didn't disconnect while waiting for the second player.
	 */
	
	public synchronized Game addClientToGame(
				String nameClient, String wantedColor, ClientHandler thisClientsHandler) {
		
		//if no games yet or the last game has already started, make a new game, add the client
		if (games.isEmpty() || games.get(games.size() - 1).getStarted()) {
			Game newGame = setupGoGame();
			addClientAsPlayer1(nameClient, wantedColor, newGame, thisClientsHandler);
			tui.showMessage(nameClient + " was added to game number " + newGame.getGameNumber() +
					 " as the first player.");
			return newGame;
			
		} else {
			
			//if there is a not-yet-started game, check whether the first player is still connected
			Game lastGame = games.get(games.size() - 1);
			ClientHandler player1goClientHandler = (ClientHandler) 
															lastGame.getClientHandlerPlayer1();
			try {
				player1goClientHandler.startGameMessageInTwoParts(lastGame.getBoard(), 
															lastGame.getColorPlayer1());
			} catch (IOException e) {
				//if not connected anymore: set current client as the first player in the game
				removeClient(player1goClientHandler);
				addClientAsPlayer1(nameClient, wantedColor, lastGame, thisClientsHandler);
				tui.showMessage("Player 1 disconnected, " + nameClient + " was added to game " + 
										lastGame.getGameNumber() + " as the first player.");
				return lastGame;
			}
			
			//otherwise, set current client as the second player in the game
			addClientAsPlayer2(nameClient, lastGame, thisClientsHandler);
			tui.showMessage(nameClient + " was added to game " + lastGame.getGameNumber() + 
					" as the second player. The game can start!");
			return lastGame;
		}
	}
	
	/**
	 * Removes a clientHandler from the client list.
	 * @requires client != null
	 */
	public void removeClient(ClientHandler client) {
		this.clients.remove(client);
	}
	
	/** 
	 * Adds a first player to a game. The player gets the color that he / she requested.
	 * If no color requested, the player will get BLACK.
	 * 
	 * @param name, the name of the player
	 * @param wantedColor, the color requested by the player (null if not specified by the player)
	 * @param game, the game that the client is added to
	 */
	
	public void addClientAsPlayer1(String nameClient, String wantedColor, Game game, 
														ClientHandler thisClientsHandler) {
		game.setNamePlayer1(nameClient);
		game.setClientHandlerPlayer1(thisClientsHandler);
		
		//if no provided wanted color (or wanted color not of length 1), give black
		if (wantedColor == null || wantedColor.length() != 1) {
			game.setColorPlayer1(ProtocolMessages.BLACK);
		} else {
			if (wantedColor.charAt(0) == ProtocolMessages.WHITE) {
				game.setColorPlayer1(ProtocolMessages.WHITE);
			} else { 
				game.setColorPlayer1(ProtocolMessages.BLACK);
			}
		}
	}
	
	/** 
	 * Adds a second player to a game. 
	 * He/she will get the other color from player 1.
	 * 
	 * @param nameClient, the name of the player
	 * @param game, the game that the client is added to
	 */
	public void addClientAsPlayer2(String nameClient, Game game, 
														ClientHandler thisClientsHandler) {
		game.setNamePlayer2(nameClient);
		game.setClientHandlerPlayer2(thisClientsHandler);
		
		//give player 2 the other color than player 1
		if (game.getColorPlayer1() == ProtocolMessages.BLACK) {
			game.setColorPlayer2(ProtocolMessages.WHITE);
		} else {
			game.setColorPlayer2(ProtocolMessages.BLACK);
		}
		game.setTwoPlayers(true);
	}
	
	/**
	 * Start a new Game.
	 * 
	 * @return the newly created game.
	 */
	public Game setupGoGame() {
		
		Game aGame = new Game(nextGameNo, usedVersion, boardDimension, komi);
		
		//increase next game number by one
		nextGameNo++;
		
		//add game to list of games
		games.add(aGame);
		
		return aGame;
	}
}