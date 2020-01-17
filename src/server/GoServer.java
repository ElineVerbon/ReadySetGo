package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import exceptions.ExitProgram;
import game.Game;
import protocol.ProtocolMessages;

/**
 * Server for playing GO.
 * 
 * The server listens for clients continuously. When a client connects, the 
 * server starts a clientHandler to handle interaction with the client.
 * It will then add the client to a game instance. Once two clients are in a game,
 * the game is started and control is given to the game instance.
 * 
 * What happens when the game is over? 
 * 
 * Not yet supported:
 * ClientHandler can ask the client whether they want to do another game against the same player
 * Clients should be removed from the list once a game is over?
 */

public class GoServer implements Runnable {
	
	/** The Socket of this GoServer (is a serverSocket!). */
	private ServerSocket ssock;

	/** List of GoClientHandlers, one for each connected client. */
	private List<GoClientHandler> clients;
	
	/** Next client number, increasing for every new connection. */
	private int nextClientNo;
	
	/** List of Games, one for each two connected clients. */
	private List<Game> games;
	
	/** Next game number, increasing for every new connection. */
	private int nextGameNo;
	
	/** Available versions of this server. */
	private List<String> availableVersions = new ArrayList<String>();

	/**  
	 * The TUI of this GoServer.
	 * Needed to ask for IP address and port number
	 */
	private GoServerTUI tui;
	
	// ------------------ Main --------------------------

	/** 
	 * Start a new GoServer.
	 * A GoServer is constructed and then its run() method is called
	 * in a new thread to listen for new clients. 
	 */
	public static void main(String[] args) {
		GoServer server = new GoServer();
		System.out.println("Welcome to the GoServer! Starting...");
		
		/**
		 * Set up a ServerSocket.
		 * First, get user input for IP and port
		 * Then create the socket
		 */
		try {
			server.setup();
		} catch (ExitProgram e1) {
			// If setup() throws an ExitProgram exception, stop the program.
			return;
		}
		
		/**
		 * Start listening for connections in a separate thread
		 */
		new Thread(server).start();
	}
	
	/**
	 * Constructor of a GoServer.
	 */
	
	public GoServer() {
		clients = new ArrayList<>();
		nextClientNo = 1;
		games = new ArrayList<Game>();
		nextGameNo = 1;
		tui = new GoServerTUI();
		
		//add all known versions
		availableVersions.add("0.01");
	}
	
	/**
	 * Create connections with clients.
	 * The ServerSocket listens for new clients, makes a clientHandler 
	 * for each connected client and starts the clientHandler in a new 
	 * thread (so the ServerSocket can continue listening for new clients).
	 */
	
	public void run() {
		try {
			Socket sock = ssock.accept();
			tui.showMessage("Client number " + nextClientNo + " just connected!");
			
			//construct a client handler to handle the communication with the client
			//and start this in a new thread
			GoClientHandler handler = 
					new GoClientHandler(sock, this, "Client " 
							+ String.format("%02d", nextClientNo));
			new Thread(handler).start();
			
			//add the handler to the list of handlers
			clients.add(handler);
			nextClientNo++;
		} catch (IOException e) {
			System.out.println("A server IO error occurred: " 
					+ e.getMessage());
			System.out.println("The server will shut down.");
		}
	}

	/**
	 * Sets up a ServerSocket at localhost on a user-defined port.
	 * 
	 * The user of the server is asked to input a port and an IP address, 
	 * after which a socket is attempted to be opened. If the attempt succeeds, 
	 * the method ends. If the attempt fails, the user can decide to try again, 
	 * after which an ExitProgram exception is thrown or a new port is entered.
	 * 
	 * @throws ExitProgram if a connection can not be created on the given 
	 *                     port and the user decides to exit the program.
	 * @ensures a serverSocket is opened.
	 */
	public void setup() throws ExitProgram {
		ssock = null;
		while (ssock == null) {
			InetAddress addr = tui.getIp("Please enter your IP address.");
			int port = tui.getInt("Please enter the number of the server port " +
					"that you want to listen on.", 1281);
			
			// try to open a new ServerSocket
			try {
				createSocket(addr, port);
			} catch (IOException e) {
				tui.showMessage("ERROR: could not create a socket on "
						+ addr.toString() + " and port " + port + ".");

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
	public void createSocket(InetAddress addr, int port) throws IOException {
		tui.showMessage("Attempting to open a socket at " + addr.toString() +
				"on port " + port + "...");
		ssock = new ServerSocket(port, 0, addr);
		tui.showMessage("Socket opened, waiting for a client.");
	}
	
	/**
	 * Removes a clientHandler from the client list.
	 * @requires client != null
	 */
	public void removeClient(GoClientHandler client) {
		this.clients.remove(client);
	}
	
	/**
	 * Read handshake from a client. If correct: set usedVersion,
	 * add client to a game & send a response
	 * 
	 * Handshake from the client is correct if it is formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 */
	
	public String doHandshake(String requestedVersion, String nameClient) {
		/**
		 * Response string should be formatted as follows:
		 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
		 * optionally these at the end: PROTOCOL.delimiter + message (string)
		 */
		
		String response;
		
		/**
		 * check if requestedVersion is available, if so:
		 * the requestedVersion becomes the used version, if not:
		 * version 1.0 is the used version. 
		 */
		String usedVersion = "";
		
		//check if required version is available
		boolean versionAvailable = false;
		for (String version : availableVersions) {
			if (version.equals(requestedVersion)) {
				versionAvailable = true;
				break;
			}
		}
		
		if (versionAvailable) {
			usedVersion = requestedVersion;
		} else {
			usedVersion = "1.0";
		}
		
		response = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER 
				+ usedVersion;
		return response;
	}
	
	public Game addClientToGame(String nameClient, GoClientHandler goClientHandler) {
		/**
		 * add the player to an existing game, or (if no game available with one player)
		 * start a new game with this player as first player
		 */
		String message;
		int gameNumber;
		//save the game the player is added to
		Game game;
		
		//if there are no games yet, make a new game, add the client
		if (games.isEmpty()) {
			gameNumber = 1;
			game = setupGoGame();
			tui.showMessage(game.addPlayer(nameClient, goClientHandler));
			return game;
		} else {
			//check whether last game is already full
			//if not full, add player to this game
			Game lastGame = games.get(games.size() - 1);
			if (!lastGame.getCompleteness()) {
				gameNumber = games.size();
				game = lastGame;
				tui.showMessage(lastGame.addPlayer(nameClient, goClientHandler));
				return game;
			//otherwise, start a new game
			} else {
				gameNumber = nextGameNo;
				game = setupGoGame();
				tui.showMessage(game.addPlayer(nameClient, goClientHandler));
				return game;
			}
		}
	}
	
	public Game setupGoGame() {
		Game aGame = new Game(nextGameNo);
		
		//let server user know what's happening
		String name = "Game " 
				+ String.format("%02d", nextGameNo);
		
		//increase next game number by one
		nextGameNo++;
		
		//add game to list of games
		games.add(aGame);
		
		return aGame;
	}
}
