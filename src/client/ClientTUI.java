package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.ProtocolMessages;

/** 
 * This class is responsible for getting user input from the console.
 */

public class ClientTUI {
	
	/** 
	 * Constructor. 
	 */
	public ClientTUI() {
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
	 * Ask the user for a valid IP. If it is invalid, ask again.
	 * 
	 * @param question, a String representing the question to show to the user
	 * @return a user-defined valid IP
	 */
	public InetAddress getIp(String message) {
		showMessage(message);
		boolean validIP = false;
		InetAddress inetAddress = null;
		
		while (!validIP) {
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
				showMessage("Sorry, this is not a valid IP. Please enter a valid IP, "
						+ "numbers divided by points (or 'localhost').");
			}
		}
		return inetAddress;
	}

	/**
	 * Prints the question and asks the user to input a String.
	 * 
	 * @param question, a String representing the question to show to the user
	 * @return a user-defined String
	 */
	public String getString(String question) {
		showMessage(question);
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
	 * @param question, a String representing the question to show to the user
	 * @return a user-defined integer.
	 */
	public int getInt(String question) {
		showMessage(question);
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
	        	showMessage("ERROR: " + userInput
	            		           + " is not an integer.");
	        	showMessage("Please try again.");
	        }
		}
		
		return userInt;
	}

	/**
	 * Prints the question and asks the user for a yes/no answer.
	 * 
	 * @param question, a String representing the question to show to the user
	 * @return a user-defined boolean.
	 */
	public boolean getBoolean(String question) {
		showMessage(question);
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
				showMessage("Sorry, this is not valid input, please enter yes or no");
			}
		}
		
		return userBoolean;
	}
	
	/**
	 * Prints the question and waits for a move (a String that parses to an int or 'pass').
	 * 
	 * @param question The question to show to the user
	 * @return a user-defined String representing the move
	 */
	public String getMove() {
		boolean validInput = false;
		int userInt = -1;
		String move = "";
		
		while (!validInput) {
			String userInput = getString("Where do you want to place "
					+ "your next marker? (Type 'pass' to pass, 'quit' to quit "
					+ "or 'hint' to get a hint.)");
			if  (userInput.equalsIgnoreCase("pass")) {
				if (getBoolean("Are you sure you want to pass? (yes/no)")) {
					move = Character.toString(ProtocolMessages.PASS);
					validInput = true;
				}
			} else  if (userInput.equalsIgnoreCase("quit")) {
				if (getBoolean("Are you sure you want to quit? (yes/no)")) {
					move = Character.toString(ProtocolMessages.QUIT);
					validInput = true;
				}
			} else if (userInput.equalsIgnoreCase("hint")) {
				move = Character.toString('N');
				validInput = true;
			} else {
				try  {
					userInt = Integer.parseInt(userInput);
					move = Integer.toString(userInt);
					validInput = true;
				} catch (NumberFormatException e) {
					showMessage("Only an integer, 'pass', 'quit' and 'hint' are accepted. "
							+ "You entered: " + userInput + ". Please try again.");
				}
			}
		}
		return move;
	}
}
