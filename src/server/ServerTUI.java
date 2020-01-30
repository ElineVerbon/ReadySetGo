package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;


public class ServerTUI {
	
	/** The PrintWriter to write messages to. */
	private PrintWriter console;

	// Constructor.
	public ServerTUI() {
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
	public int getPortNumber(String question) {
		showMessage(question);
		
		int minPortNumber = 1281;
		int maxPortNumber = 65535;
		
		boolean validInt = false;
		Integer userInt = 0;
		
		while (!validInt) {
			String userInput = readUserInput();
			
			try {
				userInt = Integer.parseInt(userInput);
	        } catch (NumberFormatException e) {
	            showMessage(userInput + " is not an integer. Please try again.");
	        }
			
			if (userInt < minPortNumber || userInt > maxPortNumber) {
				showMessage("The port number should be at least " + minPortNumber +
						" and at most " + maxPortNumber + ". Please try again.");
			} else {
				validInt = true;
			}
		}
		
		return userInt;
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
	        	showMessage(userInput + " is not an integer. Please try again.");
	        }
			
			if (userInt < minNumber) {
				showMessage("Your answer should be at least " + minNumber + ". Please try again.");
			} else {
				validInt = true;
			}
		}
		
		return userInt;
	}
	
	/**
	 * Prints the question and returns the input parsed to an double.
	 * If input cannot be parsed to a double, an error message is shown and user can try again.
	 * 
	 * @param question The question to show to the user
	 * @return The written Integer.
	 */
	public double getDouble(String question) {
		showMessage(question);
		
		boolean validDouble = false;
		double userDouble = 0;
		
		while (!validDouble) {
			String userInput = readUserInput();
			
			try {
				userDouble = Double.parseDouble(userInput);
	        } catch (NumberFormatException e) {
	        	showMessage(userInput + " is not an double. Please try again.");
	        }

			validDouble = true;
		}
		
		return userDouble;
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
				showMessage("sorry, this is not valid input, please enter yes or no");
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

