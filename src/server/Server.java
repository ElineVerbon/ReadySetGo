package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import exceptions.ExitProgram;
import protocol.MessageGenerator;
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
	
	/** 
	 * Variables for the board size and waiting time of the games hosted by this server. 
	 */
	int boardDimension;
	int waitTime;
	
	/** Available versions of this server. */
	private List<String> availableVersions = new ArrayList<String>();
	private String usedVersion;
	
	private MessageGenerator messageGenerator = new MessageGenerator();

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
	 * Sets up the server. Gets some user input for game variables and sets up a ServerSocket
	 * on a user-defined port.
	 * If the socket cannot be created, the user can decide whether to try again, 
	 * after which a new port is entered, or to stop, resulting in an ExitProgram exception.
	 * 
	 * @throws ExitProgram if a connection can not be created on the given 
	 *                     port and the user decides to exit the program.
	 * @ensures a serverSocket is opened.
	 */
	public void setup() throws ExitProgram {
		tui.showMessage("Welcome to the Server hosting Go! Starting...");
		
		ssock = null;
		while (ssock == null) {
			tui.showMessage("To start, please answer some questions to initialize the server.\n");
			
			boardDimension = tui.getInt("Please enter a positive integer to set the board"
					+ "size of the games that you will host. \n(Minimum is 5.)", 5);
			
			waitTime = tui.getInt("Please enter a positive integer to set the wait time in "
					+ "milliseconds between receiving a reply and sending the result \nto the "
					+ "client. This can allow for following the game play on the GUI in case "
					+ "of very quick stone placements or small boards, \nbut it can also slow "
					+ "down the game. (Minimum is 0. 100 is plenty.)", 0);
			
			int port = tui.getPortNumber("Please enter the number of the server port " +
					"that you want to listen on.");
			
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
	 * Create a socket with given port.
	 * 
	 * @param port, an integer between 1281 and 65535
	 * @throws IOException
	 */
	public void createSocket(int port) throws IOException {
		tui.showMessage("Attempting to open a socket on port " + port + "...");
		ssock = new ServerSocket(port, 0);
		tui.showMessage("Socket opened, waiting for a client.");
	}
	
	/**
	 * Continuously creates connections with clients via the server's ServerSocket.
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
	 * Send a handshake response to the client.
	 */
	
	public String doHandshake(String requestedVersion, String nameClient) {
		
		usedVersion = "1.0";
		for (String version : availableVersions) {
			if (version.equals(requestedVersion)) {
				usedVersion = requestedVersion;
				break;
			}
		}
		return messageGenerator.serverHandshakeMessage(nameClient, usedVersion);
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
	 * 
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
		
		Game aGame = new Game(nextGameNo, usedVersion, boardDimension, waitTime);
		
		nextGameNo++;
		
		games.add(aGame);
		
		return aGame;
	}
}
