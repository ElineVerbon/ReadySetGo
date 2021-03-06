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

import java.net.SocketTimeoutException;

/**
 * The GoClientHandler handles all communication between the server and the client.
 */

public class ClientHandler implements Runnable, Handler {
	
	/** The In- and OutputStreams to communicate with the client. */
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	/** The connected Server. */
	private Server srv;

	/** Name of the connected client. */
	private String clientName;
	
	/** The game connected with this ClientHandler. */
	private Game thisClientsGame;
	
	/** Communication version of this client-server combination). */
	private String version;
	
	private MessageGenerator messageGenerator = new MessageGenerator();

	/**
	 * Constructs a new GoClientHandler. Opens the In- and OutputStreams.
	 * 
	 * @param sock The socket of the server that connects to a certain client
	 * @param srv  The connected server
	 */
	
	public ClientHandler(Socket sock, Server srv) {
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
	 * Method that listens for a handshake message from the client. Once it receives this message, 
	 * it instructs the server to return the handshake and to add the client as a player to a game.
	 * If it is the second player added to that game, the Game object is told to start the game.
	 */
	public void run() {
		String msg;
		
		try {
			msg = in.readLine();
			if (msg.charAt(0) == ProtocolMessages.HANDSHAKE) {
				doHandshakeAndAddToGame(msg);
			} else {
				sendMessageToClient(messageGenerator.errorMessage("The client did not comply "
						+ "with the protocol: a handshake message was expected, but " + msg + 
						" was received.", version));
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
		
		//Check the components of the client's message
		String[] commands = msg.split(ProtocolMessages.DELIMITER);
		
		String command = commands[0];
		if (command.length() != 1) {
			sendMessageToClient(messageGenerator.errorMessage("Client did not keep to the "
					+ "handshake protocol. Excepted 'H' as 1st component of the message, received " 
					+ command + ".", version));
			return;
		}
		if (commands.length < 3) {
			sendMessageToClient(messageGenerator.errorMessage("Client did not keep to the "
					+ "handshake protocol. Excepted both version and name as components of the "
					+ "message, received " + command + ".", version));
			return;
		}
		String requestedVersion = commands[1];
		clientName = commands[2];
		String wantedColor = (commands.length > 3) ? commands[3] : null; 
		
		// Get a handshake message from the server & instruct the server to add client to a game.
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
		
		sendMessageToClient(message);
		
	}

	/**
	 * Send start game message to the first connected client when the second player it to be added
	 * to the same game to check whether the first player did not disconnect.
	 * 
	 * @param board
	 * @param color
	 * @throws IOException when the client is no longer connected
	 */
	public void startGameMessageInTwoParts(String board, char color) throws IOException {
		String startMessage1part1 = messageGenerator.startGameMessagePart1();
		String startMessage1part2 = messageGenerator.startGameMessagePart2(board, color);

		try {
			out.write(startMessage1part1);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Need to wait, otherwise it does not go into the exception
		try {
			TimeUnit.SECONDS.sleep(1);
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
			shutdown();
		}
	}
	
	/**
	 * Get a message from client. Client has 1 minute to reply
	 */
	public String getReply() throws SocketTimeoutException {
		String reply = "";
		
		try {
			sock.setSoTimeout(60000);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
																	sock.getInputStream()));
			reply = bufferedReader.readLine();
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
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
