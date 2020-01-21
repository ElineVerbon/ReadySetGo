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
	private String handlerName;
	
	/** Name of the attached client. */
	private String clientName;
	
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
			this.handlerName = name;
		} catch (IOException e) {
			shutdown();
		}
	}

	/**
	 * Listens for handshake message & lets the client start playing.
	 */
	public void run() {
		String msg;
		try {
			msg = in.readLine();
			//TODO check whether it is not a quit or '?' message 
					//Those are the only other possible methods at this point
			
			//send handshake to server, send response of server to the client
			//client will not respond, so can add client to player as well.
			if (msg.charAt(0) == ProtocolMessages.HANDSHAKE) {
				//Don't use the returned game here, only in the tests
				doHandshakeAndAddToGame(msg);
			}
			
			//if this was the second player added to the game, start the game
			//TODO From here, the flow of the game is handled by the Game instance
			//server is not involved anymore, will only get back at the end!
			if (thisClientsGame.getCompleteness()) {
				thisClientsGame.runGame();
			}
			
			//TODO shutdown client when necessary
//			shutdown();
		} catch (IOException e) {
			shutdown();
		}
	}
	
	/**
	 * Check handshake message from the client. Should follow this protocol:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * Upon receiving a handshake message from the client, send handshake
	 * command to the server. Get the response. Add the client to a game.
	 * Send response + message about game back to client.
	 * 
	 * Return the Game for testing purposes!
	 */
	
	private void doHandshakeAndAddToGame(String msg) {
		String message = "";
		String handshakeResponse = "";
		String addToGameResponse = "";
		
		//break message into pieces
		String[] commands = msg.split(ProtocolMessages.DELIMITER);
		
		String command = commands[0];
		if (command.length() != 1) {
			//TODO does not keep to the protocol!
		}
		String requestedVersion = commands[1];
		clientName = commands[2];
		String wantedColor = (commands.length > 3) ? commands[3] : null; 
		
		//let server check handshake and get the response
		handshakeResponse = doHandshake(requestedVersion, clientName);
		addToGameResponse = addToGame(wantedColor);
		
		message = handshakeResponse + addToGameResponse;
		//Send the response to the client
		try {
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		return thisClientsGame;
	}
	
	/** 
	 * Get handshake message of the client, send handshake command to server
	 * Send response back to client. (No response expected)
	 * 
	 * @return a String formatted according to handshake format,
	 * with information about the server communication & the game.
	 */
	private String doHandshake(String command, String name) {
		
		//let the server check the handshake and return a message
		String response = srv.doHandshake(command, name);
		
		return response;
	}
	
	/**
	 * Add client to a game.
	 * 
	 * @return a string with game number and player number
	 */
	
	private String addToGame(String wantedColor) {
		
		//let the server add the client to a game
		thisClientsGame = srv.addClientToGame(clientName, in, out, wantedColor);
		int gameNumber = thisClientsGame.getNumber();
		boolean gameComplete = thisClientsGame.getCompleteness();
		String message = "";
		
		//send appropriate message to the client (cannot be send by server,
		//server is returning the game so the clientHandler can organize the 
		//client - game communication
		if (gameComplete) {
			message = " You have been added to game " + gameNumber + ". " +
					"You are the second player, the game will start soon!"; 
		} else {
			message = " You have been added to game " + gameNumber + ". " +
					"You are the first player, please wait for the second player."; 
		}
		
		return message;
	}

	/**
	 * If correct, the clientHandler (this) will attach a game to this clientHandler.
	 *
	 */
	
	
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
			//made a separate thing for the handshake
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
		System.out.println("> [" + handlerName + "] Shutting down.");
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
