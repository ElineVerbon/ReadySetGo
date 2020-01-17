package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class GoServerTUI {
	
	/** The PrintWriter to write messages to. */
	private PrintWriter console;

	/**
	 * Constructs a new HotelServerTUI. Initializes the console.
	 */
	public GoServerTUI() {
		console = new PrintWriter(System.out, true);
	}

	/**
	 * Prints the question and asks the user to input a String.
	 * 
	 * @param question The question to show to the user
	 * @return The user input as a String
	 */
	public String getString(String question) {
		showMessage(question);
		
		String userInput = readUserInput();
		
		return userInput;
	}

	/**
	 * Prints the question and returns the input parsed to an integer.
	 * If input cannot be parsed to an integer, an error message is shown and user can try again.
	 * 
	 * @param question The question to show to the user
	 * @return The written Integer.
	 */
	public int getInt(String question, int minNumber) {
		showMessage(question);
		
		boolean validInt = false;
		Integer userInt = 0;
		
		while (!validInt) {
			String userInput = readUserInput();
			
			try {
				userInt = Integer.parseInt(userInput);
	        } catch (NumberFormatException e) {
	            System.out.println(userInput
	            		           + " is not an integer. Please try again.");
	        }
			
			if (userInt < minNumber) {
				System.out.println("The number should be at least " + minNumber +
						"Please try again.");
			} else {
				validInt = true;
			}
		}
		
		return userInt;
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
	 * Prints the question and returns the input as a boolean.
	 * If input cannot be translated to a boolean, an error message is shown and user can try again.
	 * 
	 * @param question The question to show to the user
	 * @return The user input as boolean.
	 */
	public boolean getBoolean(String question) {
		showMessage(question);
		boolean validInput = false;
		boolean userBoolean = false;
		
		while (!validInput) {
			String userInput = readUserInput();
			
			if (userInput.equalsIgnoreCase("yes")) { 
				userBoolean = true; validInput = true;
			} else if (userInput.equalsIgnoreCase("no")) { 
				userBoolean = false; validInput = true;
			} else { 
				System.out.println("sorry, this is not valid input, please enter yes or no");
			}
		}
		
		return userBoolean;
	}
	
	/**
	 * Writes a given message to standard output.
	 * 
	 * @param message the message to write to the standard output.
	 */
	public void showMessage(String message) {
		console.println(message);
	}
	
	/**
	 * Reads user input (is always a String).
	 */
	public String readUserInput() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String userInput = "";
		try {
			userInput = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userInput;
	}

}

