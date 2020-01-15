package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import protocol.ProtocolMessages;


public class GoClientHandler implements Runnable {
	/** The socket and In- and OutputStreams */
	private BufferedReader in;
	private BufferedWriter out;
	private Socket sock;
	
	/** The connected HotelServer */
	private GoServer srv;

	/** Name of this ClientHandler */
	private String name;

	/**
	 * Constructs a new HotelClientHandler. Opens the In- and OutputStreams.
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
	 * methods at the HotelServer. For example, when the message "i Name" 
	 * is received, the method doIn() of HotelServer should be called 
	 * and the output must be sent to the client. //how do I do this? or should the server do this?
	 * 
	 * If the received input is not valid, send an "Unknown Command" 
	 * message to the server. NB: this should be to the client!
	 * 
	 * @param msg command from client
	 * @throws IOException if an IO errors occur.
	 */
	
	private void handleCommand(String msg) throws IOException {
		String response = "";
		
		char command = msg.charAt(0);
		
		switch(command) {
			case ProtocolMessages.HANDSHAKE:
				//handshake of a client consists of: handshake + requestedVersion + nameClient + optionally the wantedColor
				String[] commands = msg.split(ProtocolMessages.DELIMITER);
				String wantedColor = (commands.length > 3) ? commands[3] : null; // if the wantedColor is available, get it, otherwise set null
				response = srv.doHandshake(commands[1], commands[2], wantedColor);
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
