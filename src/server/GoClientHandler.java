package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import protocol.ProtocolMessages;


public class GoClientHandler implements Runnable, Handler {
	
	/** The socket and In- and OutputStreams. */
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	/** The connected HotelServer. */
	private GoServer srv;

	
	/** Name of the attached client. */
	private String clientName;
	
	/** The game connected with this ClientHandler. */
	private Game thisClientsGame;
	
	/** Communication version of this client-server combination). */
	//TODO this is not implemented neatly yet (not set anywhere yet)
	private String version;

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
//	public GoClientHandler(GoServer server, BufferedReader givenIn, 
//										BufferedWriter givenOut) {
//		in = givenIn;
//		out = givenOut;
//		srv = server;
//	}

	/**
	 * Listens for handshake message & lets the client start playing.
	 */
	public void run() {
		String msg;
		try {
			msg = in.readLine();
			
			//call server's handshake method
			if (msg.charAt(0) == ProtocolMessages.HANDSHAKE) {
				//Don't use the returned game here, only in the tests
				doHandshakeAndAddToGame(msg);
			}
			
			//if this was the second player added to the game, start the game
			if (thisClientsGame.getTwoPlayers()) {
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
		
		/**
		 * Get handshake message from client and split into pieces.
		 */
		String[] commands = msg.split(ProtocolMessages.DELIMITER);
		
		String command = commands[0];
		if (command.length() != 1) {
			errorMessage("Client did not keep to the handshake protocol. Excepted 'H' as first "
					+ "component of the message, received " + command + ".");
		}
		String requestedVersion = commands[1];
		clientName = commands[2];
		String wantedColor = (commands.length > 3) ? commands[3] : null; 
		
		/**
		 * Send message to the server and record the returning handshake message.
		 * In addition, let server add the client to a game
		 * 
		 * Send returning handshake message + game info to the client.
		 */
		
		String handshakeResponse = srv.doHandshake(requestedVersion, clientName);
		
		thisClientsGame = srv.addClientToGame(clientName, wantedColor, this);
		String gameMessage = "";
		if (thisClientsGame.getTwoPlayers()) {
			gameMessage = " You have been added to game " + thisClientsGame.getGameNumber() + ". " +
					"You are the second player, the game will start soon!"; 
		} else {
			gameMessage = " You have been added to game " + thisClientsGame.getGameNumber() + ". " +
					"You are the first player, please wait for the second player."; 
		}
		
		//Send the response to the client
		String message = handshakeResponse + gameMessage;
		try {
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Methods to send messages formatted according to the protocol to the client. 
	 */
	
	public void errorMessage(String message) {
		String errorMessage = ProtocolMessages.ERROR + ProtocolMessages.DELIMITER + 
						version + ProtocolMessages.DELIMITER + message;
		sendMessageToClient(errorMessage);
	}
	
	public void startGameMessage(String board, char color) {
		String startMessage = ProtocolMessages.GAME + ProtocolMessages.DELIMITER
				+ board + ProtocolMessages.DELIMITER + color;
		sendMessageToClient(startMessage);
	}
	
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
	
	public String doTurnMessage(String board, String opponentsMove) {
		String turnMessage = ProtocolMessages.TURN + ProtocolMessages.DELIMITER + board + 
				ProtocolMessages.DELIMITER + opponentsMove;
		sendMessageToClient(turnMessage);
		
		String reply = getReply();
		return reply;
	}
	
	public void giveResultMessage(boolean valid, String msg) {
		String resultMessage = "";
		
		if (valid) {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ ProtocolMessages.VALID + ProtocolMessages.DELIMITER + msg;
		} else {
			resultMessage = ProtocolMessages.RESULT + ProtocolMessages.DELIMITER
					+ ProtocolMessages.INVALID + ProtocolMessages.DELIMITER + msg;
		}
		sendMessageToClient(resultMessage);
	}
	
	public void endGameMessage(char reasonGameEnd, char winner, 
									String scoreBlack, String scoreWhite) {
		String endOfGameMessage = ProtocolMessages.END + ProtocolMessages.DELIMITER + reasonGameEnd
				+ ProtocolMessages.DELIMITER + winner + ProtocolMessages.DELIMITER + 
				scoreBlack + ProtocolMessages.DELIMITER + 
				scoreWhite;
		sendMessageToClient(endOfGameMessage);
		
	}
	
	/**
	 * Send message from game to client.
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
	 * Send message to and get message from client.
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
