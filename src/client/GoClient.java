package client;

import java.io.IOException;
import java.net.InetAddress;

import exceptions.*;

public interface GoClient {
	/**
	 * Creates a connection to the server. Requests the IP and port to 
	 * connect to via the TUI.
	 * 
	 * The method continues to ask for an IP and port and attempts to connect 
	 * until a connection is established or until the client exits the program.
	 * 
	 * @throws ExitProgram if a connection is not established and the user 
	 * 				       indicates to want to exit the program.
	 * @ensures serverSock contains a valid socket connection to a server
	 */
	public void createConnection(InetAddress addr, int port) 
			throws IOException;
	
	/**
	 * Resets the serverSocket and In- and OutputStreams to null.
	 * 
	 * Always make sure to close current connections via shutdown() 
	 * before calling this method!
	 */
	public void clearConnection();
	
	/**
	 * Closes the connection by closing the In- and OutputStreams, as 
	 * well as the serverSocket.
	 */
	public void closeConnection();
	
	/**
	 * Send handshake to the server. Keep to the following protocol:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + requestedVersion + PROTOCOL.delimiter + naamClient 
	 * optionally these at the end: + PROTOCOL.delimiter + PROTOCOL.white/black
	 * 
	 * After sending, wait for response, which should be formatted as follows:
	 * PROTOCOL.handshake + PROTOCOL.delimiter + finalVersion (string) 
	 * optionally these at the end: PROTOCOL.delimiter + message (string)
	 * @throws ProtocolException 
	 * 
	 * 
	 */
	
	public void doHandshake() 
			throws ServerUnavailableException, ProtocolException;
	
	/**
	 * Reads and returns one line from the server.
	 * 
	 * @return the line sent by the server.
	 * @throws ServerUnavailableException if IO errors occur.
	 */
	public String readLineFromServer() 
			throws ServerUnavailableException;
	
	public void sendExit() 
			throws ServerUnavailableException;
}
