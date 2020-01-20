package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import exceptions.*;
import protocol.ProtocolMessages;


public class GoClientHumanTUI {
	private GoClientHuman goHumanClient;
//	private boolean active = true; //don't know what I would use this for
	
	/** Constructor, connected to the client that called the constructor. */
	public GoClientHumanTUI(GoClientHuman goClient) {
		this.goHumanClient = goClient;
	}
	
	/**
	 * Asks for user input continuously and handles communication accordingly using
	 * the 'handleUserInput(String input)' method.
	 * 
	 * If an ExitProgram exception is thrown, stop asking for input, send an exit
	 * message to the server according to the protocol and close the connection.
	 * 
	 * @throws ServerUnavailableException in case of IO exceptions.
	 */
	
//	public void start() throws ServerUnavailableException {
//		Scanner scanner = new Scanner(System.in);
//		
//		while (active) {
//			String userInput = getString("Which move would you like to do?");
//			try {
//				handleUserInput(userInput);
//			} catch (ExitProgram e) {
//				active = false;
//				goHumanClient.sendExit();
//			}
//			if (userInput.equals("x")) { 
//				active = false; 
//				//TODO kijken of dit mooier opgelost kan worden
//			} 
//		}
//		scanner.close();
//	}
	
	/**
	 * Split the user input on a space and handle it accordingly. 
	 * - If the input is valid, take the corresponding action (for example, 
	 *   when "i Name" is called, send a checkIn request for Name) 
	 * - If the input is invalid, show a message to the user and print the help menu.
	 * 
	 * @param input The user input.
	 * @throws ExitProgram               	When the user has indicated to exit the
	 *                                    	program.
	 * @throws ServerUnavailableException 	if an IO error occurs in taking the
	 *                                    	corresponding actions.
	 */
	public void handleUserInput(String msg) throws ExitProgram, ServerUnavailableException {
		//if there is no input
		//TODO
		
		char command = msg.charAt(0);
		switch (command) {
			case ProtocolMessages.HANDSHAKE:
				showMessage("Handshake has already been exchanged, "
						+ "please use another command ('M' or 'Q').");
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
			default: 
		}
	}
	
	/**
	 * Writes the given message to standard output.
	 * 
	 * @param msg the message to write to the standard output.
	 */
	public void showMessage(String message) {
		System.out.println(message);
	}

	/**
	 * Ask the user to input a valid IP. If it is not valid, show a message and ask
	 * again.
	 * 
	 * @return a valid IP
	 */
	public InetAddress getIp(String message) {
		showMessage(message);
		boolean validIP = false;
		InetAddress inetAddress = null;
		
		while (!validIP) {
			System.out.println("Please enter a valid IP, numbers divided by points.");
			String userInput = "";
			
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			
			try {
				userInput = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				inetAddress = InetAddress.getByName(userInput);
				validIP = true;
			} catch (UnknownHostException e) {
				System.out.println("Sorry, this is not a valid IP. ");
			}
		}
		return inetAddress;
	}

	/**
	 * Prints the question and asks the user to input a String.
	 * 
	 * @param question The question to show to the user
	 * @return The user input as a String
	 */
	public String getString(String question) {
		System.out.println(question);
		String userInput = "";
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			userInput = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return userInput;
	}

	/**
	 * Prints the question and asks the user to input an Integer.
	 * 
	 * @param question The question to show to the user
	 * @return The written Integer.
	 */
	public int getInt(String question) {
		System.out.println(question);
		boolean validInt = false;
		String userInput = "";
		Integer userInt = 0;
		
		while (!validInt) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			try {
				userInput = in.readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			try {
				userInt = Integer.parseInt(userInput);
				validInt = true;
	        } catch (NumberFormatException e) {
	            System.out.println("ERROR: " + userInput
	            		           + " is not an integer.");
	            System.out.println("Please try again.");
	        }
		}
		
		return userInt;
	}

	/**
	 * Prints the question and asks the user for a yes/no answer.
	 * 
	 * @param question The question to show to the user
	 * @return The user input as boolean.
	 */
	public boolean getBoolean(String question) {
		System.out.println(question);
		boolean validInput = false;
		boolean userBoolean = false;
		
		while (!validInput) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String userInput = "";
			try {
				userInput = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (userInput.equalsIgnoreCase("yes")) { 
				userBoolean = true; validInput = true;
			} else if (userInput.equalsIgnoreCase("no")) { 
				userBoolean = false; validInput = true;
			} else { 
				System.out.println("Sorry, this is not valid input, please enter yes or no");
			}
		}
		
		return userBoolean;
	}
}
