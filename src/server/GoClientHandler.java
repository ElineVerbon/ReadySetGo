package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import protocol.ProtocolMessages;
import game.*;


public class GoClientHandler implements Runnable {
	/** The socket and In- and OutputStreams. */
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	/** The connected HotelServer. */
	private GoServer srv;

	/** Name of this ClientHandler. */
	private String name;
	
	/** The game connected with this ClientHandler. */
	private Game thisClientsGame;
	
//	/** Communication version of this client-server combination) */
//	private String version;
	//TODO implement this to keep track of the protocol version. 
	//Not sure if this is necessary, depends on how the protocols differ

	/**
	 * Constructs a new GoClientHandler. Opens the In- and OutputStreams.
	 * 
	 * @param sock The socket of the server that connects to a certain client
	 * @param srv  The connected server
	 * @param name The name of this ClientHandler
	 */
	public GoClientHandler(Socket sock, GoServer srv, String name) {
		try {
			in = new BufferedReader(
					new InputStreamReader(sock.getInputStream()));
			out = new BufferedWriter(
					new OutputStreamWriter(sock.getOutputStream()));
			this.sock = sock;
			this.srv = srv;
			this.name = name;
		} catch (IOException e) {
			shutdown();
		}
	}

	/**
	 * Continuously listens to client input and forwards the input to the
	 * 'handleCommand(String)' method.
	 */
	public void run() {
		String msg;
		try {
			msg = in.readLine();
			while (msg != null) {
				System.out.println("> [" + name + "] Incoming: " + msg);
				handleCommand(msg);
				out.newLine();
				out.flush();
				msg = in.readLine();
			}
			shutdown();
		} catch (IOException e) {
			shutdown();
		}
	}

	/**
	 * Handles commands received from the client by calling the according 
	 * methods at the GoServer and sending the output to the client.
	 * 
	 * If the received input is not valid, send an "?" message to the client.
	 * 
	 * @param msg command from client
	 * @throws IOException if an IO errors occur.
	 */
	
	private void handleCommand(String msg) throws IOException {
		String response = "";
		
		char command = msg.charAt(0);
		
		switch (command) {
			case ProtocolMessages.HANDSHAKE:
				
				/** 
				 * Send the message of the client to the server formatted according to the protocol.
				 * The message of the client consists of: handshake + requestedVersion + nameClient 
				 * and optionally the wantedColor.
				 * The server will check the handshake.
				 * If correct, the clientHandler (this) will attach a game to this clientHandler.
				 */
				
				String[] commands = msg.split(ProtocolMessages.DELIMITER);
				String nameClient = commands[2];
				String wantedColor = (commands.length > 3) ? commands[3] : null; 
				
				//let the server check the handshake & decide the version number in doHandshake()
				//get 'ProtocolMessages.HANDSHAKE + ProtocolMessages.DELIMITER + usedVersion' back
				response = srv.doHandshake(commands[1], nameClient);
				
				//add the player to a game and get back the game to save in the client handler
				//this way methods can be called with the correct game instance
				thisClientsGame = srv.addClientToGame(nameClient);
				int gameNumber = thisClientsGame.getNumber();
				boolean gameComplete = thisClientsGame.getCompleteness();
				String message = "";
				
				if (gameComplete) {
					message = "Welcome " + nameClient + " to game " + gameNumber + ". " +
							"You are the second player, the game will start soon!"; 
				} else {
					message = "Welcome " + nameClient + " to game " + gameNumber + ". " +
							"You are the first player, please wait for the second player."; 
				}
				response += ProtocolMessages.DELIMITER + message;
				
				//TODO Problem: if the client rejects the received protocol message, it will have 
				//already been added to the game. Thus, the next player will wait indefinitely for
				//the second player. 
				
				//Send the response to the client
				//Response contains the handshake according to the protocol + the game info
				out.write(response);
				break;
				
			case ProtocolMessages.GAME:
				//TO DO, see above
				break;
			case ProtocolMessages.TURN:
				//TO DO, see above
				break;
			case ProtocolMessages.MOVE:
				//TO DO, see above
				break;
			case ProtocolMessages.RESULT:
				//TO DO, see above
				break;
			case ProtocolMessages.END:
				//TO DO, see above
				break;
			case ProtocolMessages.QUIT:
				//TO DO, see above
				break;
		}
	}

	/**
	 * Shut down the connection to this client by closing the socket and 
	 * the In- and OutputStreams.
	 */
	private void shutdown() {
		System.out.println("> [" + name + "] Shutting down.");
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
