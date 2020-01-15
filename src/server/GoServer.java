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
 * Server for playing GO
 * 
 * Intended Functionality: should listen for clients
 * once two client have connected, starts a game while continuing
 * to listen for further clients and setting up new games
 */

public class GoServer implements Runnable {
	
	/** The Socket of this GoServer (is a serverSocket!) */
	private ServerSocket ssock;

	/** List of GoClientHandlers, one for each connected client */
	private List<GoClientHandler> clients;
	
	/** Next client number, increasing for every new connection */
	private int next_client_no;
	
	/** List of Games, one for each two connected clients */
	private List<Game> games;
	
	/** Next game number, increasing for every new connection */
	private int next_game_no;
	
	/** Available versions of this server */
	private List<String> availableVersions = new ArrayList<String>();

	/** The TUI of this GoServer */
	private GoServerTUI tui;
	
	// ------------------ Main --------------------------

	/** Start a new GoServer */
	public static void main(String[] args) {
		GoServer server = new GoServer();
		System.out.println("Welcome to the GoServer! Starting...");
		new Thread(server).start();
	}
	
	/**
	 * Constructor of a GoServer
	 */
	
	public GoServer() {
		clients = new ArrayList<>();
		next_client_no = 1;
		games = new ArrayList<Game>();
		next_game_no = 1;
		tui = new GoServerTUI();
		
		//add all known versions
		availableVersions.add("0.01");
	}
	
	/**
	 * Opens a new socket by calling 'setup()' and starts a new
	 * GoClientHandler for every connecting client.
	 * 
	 * If 'setup()' throws a ExitProgram exception, stop the program. 
	 * In case of any other errors, ask the user whether the setup should be 
	 * ran again to open a new socket.
	 */
	public void run() {
		boolean openNewSocket = true;
		while (openNewSocket) {
			try {
				// Sets up the hotel application
				setup();

				while (true) {
					Socket sock = ssock.accept();
					String name = "Client " 
							+ String.format("%02d", next_client_no++);
					tui.showMessage("New client [" + name + "] connected!");
					
					//construct a client handler to handle the communication with the client
					//and start this in a new thread
					GoClientHandler handler = 
							new GoClientHandler(sock, this, name);
					new Thread(handler).start();
					
					//add the handler to the list of handlers
					clients.add(handler);
					
					//check whether there are N%2=0 clients connected + with same handshake, if so, start game for the latest two clients
				}

			} catch (ExitProgram e1) {
				// If setup() throws an ExitProgram exception, 
				// stop the program.
				openNewSocket = false;
			} catch (IOException e) {
				System.out.println("A server IO error occurred: " 
						+ e.getMessage());

				if (!tui.getBoolean("Do you want to open a new socket?")) {
					openNewSocket = false;
				}
			}
		}
		tui.showMessage("Hope to see you again later to let more people play GO!");
	}

	/**
	 * Sets up a new Game using 'setupGame()' and opens a new 
	 * ServerSocket at localhost on a user-defined port.
	 * 
	 * The user is asked to input a port, after which a socket is attempted 
	 * to be opened. If the attempt succeeds, the method ends, If the 
	 * attempt fails, the user decides to try again, after which an 
	 * ExitProgram exception is thrown or a new port is entered.
	 * 
	 * @throws ExitProgram if a connection can not be created on the given 
	 *                     port and the user decides to exit the program.
	 * @ensures a serverSocket is opened.
	 */
	public void setup() throws ExitProgram {
		ssock = null;
		while (ssock == null) {
			int port = tui.getInt("Please enter the server port.");
			
			// try to open a new ServerSocket
			try {
				tui.showMessage("Attempting to open a socket at 127.0.0.1 "
						+ "on port " + port + "...");
				ssock = new ServerSocket(port, 0, 
						InetAddress.getByName("127.0.0.1"));
				tui.showMessage("Server started at port " + port);
			} catch (IOException e) {
				tui.showMessage("ERROR: could not create a socket on "
						+ "127.0.0.1" + " and port " + port + ".");

				if (!tui.getBoolean("Do you want to try again? (yes/no)")) {
					throw new ExitProgram("User indicated to exit the "
							+ "program.");
				}
			}
		}
	}
	
	/**
	 * Removes a clientHandler from the client list.
	 * @requires client != null
	 */
	public void removeClient(GoClientHandler client) {
		this.clients.remove(client);
	}
	
	/**
	 * Read handshake from a client, if correct: set usedVersion,
	 * add client to a game & send a response
	 * 
	 * Handshake from the client is correct if it is formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 */
	
	public String doHandshake(String requestedVersion, String nameClient, String wantedColor) {
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
		for(String version : availableVersions) {
			if(version.equals(requestedVersion)) {
				versionAvailable = true;
				break;
			}
		}
		
		if(versionAvailable) {
			usedVersion = requestedVersion;
		} else {
			usedVersion = "1.0";
		}
		
		/**
		 * add the player to an existing game, or (if no game available with one player)
		 * start a new game with this player as first player
		 */
		String message;
		int gameNumber;
		
		if(games.isEmpty()) {
			gameNumber = games.size()-1;
			Game game = setupGoGame();
			game.addPlayer(nameClient);
		} else {
			//check whether last game is already full
			//if not full, add player to this game
			Game lastGame = games.get(games.size()-1);
			if(!lastGame.getCompleteness()) {
				gameNumber = games.size();
				lastGame.addPlayer(nameClient);
			//otherwise, start a new game
			} else {
				gameNumber = next_game_no;
				Game game = setupGoGame();
				game.addPlayer(nameClient);
			}
		}
		
		message = "Welcome " + nameClient + " to game number " + gameNumber + " on the GoServer!";
		
		//TODO use wanted color!
		
		response = ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER + usedVersion + ProtocolMessages.DELIMITER + message;
		return response;
	}
	
	public Game setupGoGame() {
		Game aGame = new Game(next_game_no);
		
		//let server user know what's happening
		String name = "Game " 
				+ String.format("%02d", next_game_no);
		tui.showMessage("New game [" + name + "] was made, waiting for second player");
		
		//increase next game number by one
		next_game_no++;
		
		games.add(aGame);
		
		return aGame;
	}
}
