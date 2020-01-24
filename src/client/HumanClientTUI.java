package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.ProtocolMessages;

/** 
 * This class is responsible for getting user input from the console.
 *
 */

public class HumanClientTUI {
	
	/** Constructor, connected to the client that called the constructor. */
	public HumanClientTUI() {
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
	
	/**
	 * Prints the question and waits for a move (a String that parses to an int or 'pass').
	 * Returns the move as a String
	 * 
	 * @param question The question to show to the user
	 * @return The user input as boolean.
	 */
	public String getMove() {
		boolean validInput = false;
		int userInt = -1;
		String move = "";
		
		while (!validInput) {
			//get userInput about move
			String userInput = getString("Where do you want to place "
					+ "your next marker? (Type 'pass' to pass or 'quit' to quit.)");
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
			} else {
				try  {
					userInt = Integer.parseInt(userInput);
					move = Integer.toString(userInt);
					validInput = true;
				} catch (NumberFormatException e) {
					showMessage("Only an integer, 'pass' or 'quit' are accepted. You entered: " + 
								userInput + ". Please try again.");
				}
			}
		}
		return move;
	}
}
