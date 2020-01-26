package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import protocol.MessageGenerator;
import protocol.ProtocolMessages;

/**
 * The GoClientHandler handles all communication between the server and the client.
 */

public class GoClientHandler implements Runnable, Handler {
	
	/** The In- and OutputStreams to communicate with the client. */
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	/** The connected Server. */
	private GoServer srv;

	/** Name of the connected client. */
	private String clientName;
	
	/** The game connected with this ClientHandler. */
	private Game thisClientsGame;
	
	/** Communication version of this client-server combination). */
	//TODO this is not implemented neatly yet (not set anywhere yet)
	private String version;
	
	private MessageGenerator messageGenerator = new MessageGenerator();

	/**
	 * Constructs a new GoClientHandler. Opens the In- and OutputStreams.
	 * 
	 * @param sock The socket of the server that connects to a certain client
	 * @param srv  The connected server
	 * @param name The name of this ClientHandler
	 */
	
	public GoClientHandler(Socket sock, GoServer srv) {
		try {
			in = new BufferedReader(
					new InputStreamReader(sock.getInputStream()));
			out = new BufferedWriter(
					new OutputStreamWriter(sock.getOutputStream()));
			this.sock = sock;
			this.srv = srv;
		} catch (IOException e) {
			shutdown();
		}
	}

	/**
	 * This method is called from the server immediately upon construction of the handler. It 
	 * listens for a handshake message. Once it receives this message, it instructs the server 
	 * to return the handshake and add the client as a player to a game.
	 * If it is the second player added to the game, the game will start and be run from the Game
	 * object. The Game controls the flow of information, which is transmitted via the handler.
	 */
	public void run() {
		String msg;
		
		try {
			msg = in.readLine();
			if (msg.charAt(0) == ProtocolMessages.HANDSHAKE) {
				doHandshakeAndAddToGame(msg);
				
			} else {
				String errorMessage = messageGenerator.errorMessage("The client did not comply "
						+ "with the protocol: a handshake message was expected, but " + msg + 
						" was received.", version);
				sendMessageToClient(errorMessage);
			}
			
			if (thisClientsGame.hasTwoPlayers()) {
				thisClientsGame.runGame();
			}
		} catch (IOException e) {
			shutdown();
		}
	}
	
	/**
	 * Check handshake message from the client. Should follow this protocol:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * Upon receiving a handshake message from the client, send handshake command to the server. 
	 * The server will check the handshake, construct the appropriate reply and add the client 
	 * to a game. The reply + information about the game are send back to the client.
	 */
	
	private void doHandshakeAndAddToGame(String msg) {
		
		/**
		 * Split the handshake message from client into the components.
		 */
		String[] commands = msg.split(ProtocolMessages.DELIMITER);
		
		String command = commands[0];
		if (command.length() != 1) {
			String errorMessage = messageGenerator.errorMessage("Client did not keep to the "
					+ "handshake protocol. Excepted 'H' as 1st component of the message, received " 
					+ command + ".", version);
			sendMessageToClient(errorMessage);
		}
		String requestedVersion = commands[1];
		clientName = commands[2];
		String wantedColor = (commands.length > 3) ? commands[3] : null; 
		
		/**
		 * Send message to the server and record the returning handshake message.
		 * In addition, let server add the client to a game
		 */
		
		String handshakeResponse = srv.doHandshake(requestedVersion, clientName);
		
		thisClientsGame = srv.addClientToGame(clientName, wantedColor, this);
		String gameMessage = "";
		if (thisClientsGame.hasTwoPlayers()) {
			gameMessage = " You have been added to game " + thisClientsGame.getGameNumber() + ". " +
					"You are the second player, the game will start soon!"; 
		} else {
			gameMessage = " You have been added to game " + thisClientsGame.getGameNumber() + ". " +
					"You are the first player, please wait for the second player."; 
		}
		
		//Send the server's handshake message + game info to the client.
		String message = handshakeResponse + gameMessage;
		try {
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send start game message to the first connected client when the second player it to be added
	 * to the same game to check whether the first player did not disconnect.
	 * @param board
	 * @param color
	 * @throws IOException, used to check whether client is still connected
	 */
	public void startGameMessageInTwoParts(String board, char color) throws IOException {
		//Check whether player1 has disconnected by sending the start message in two parts (if
		//disconnected, the second flush will give an IO exception)
		String startMessage1part1 = ProtocolMessages.GAME + ProtocolMessages.DELIMITER;
		String startMessage1part2 = board + ProtocolMessages.DELIMITER + color;
		try {
			out.write(startMessage1part1);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Need to wait, otherwise it does not go into the exception
		try {
			TimeUnit.SECONDS.sleep(1); //TODO try with shorter time step
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		out.write(startMessage1part2);
		out.newLine();
		out.flush();
	}
	
	/**
	 * Send a message from to the client.
	 */
	public void sendMessageToClient(String msg) {
		try {
			out.write(msg);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
	}
	
	/**
	 * Get a message from client.
	 */
	public String getReply() {
		String reply = "";
		try {
			reply = in.readLine();
		} catch (IOException e) {
			//TODO auto-generated
			e.printStackTrace();
		}
		return reply;
	}
	
	/**
	 * Shut down the connection to this client by closing the socket and 
	 * the In- and OutputStreams.
	 */
	private void shutdown() {
		System.out.println("> Handler of client " + clientName + " is shutting down.");
		try {
			in.close();
			out.close();
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		srv.removeClient(this);
	}
}
