package protocol;

/**
 * Protocol for NU5 GO game, version: ‘1.0’.
 */
public class ProtocolMessages {

	// Version of this protocol.
	public static final String VERSION = "1.0";
	
	// Delimiter used to separate arguments sent over the network.
	public static final String DELIMITER = ";";
	
	// Signal to indicate unknown message.
	public static final char ERROR = '?'; 

	//Commands to indicate game flow (first messages in a protocol message)
	public static final char HANDSHAKE = 'H'; 
		//From client and back again from the server to check the protocol
	public static final char GAME = 'G'; //From server, to indicate the start of the game
	public static final char TURN = 'T'; //From server, to indicate to a player that it's its turn
	public static final char MOVE = 'M'; //From player, to indicate that a move is corresponded
	public static final char RESULT = 'R'; //From server, to indicate the result of the move
	public static final char END = 'E'; //From server, to indicate end of game
	public static final char QUIT = 'Q'; //From player to server, to indicate (s)he wants to quit.
	
	
	//Constants for clean information transfer
	public static final char WHITE = 'W'; 
		//From either player (to indicate preferred color) or server (to indicate assigned color)
		//also used in the string representation of the board
	public static final char BLACK = 'B'; 
		//From either player (to indicate preferred color) or server (to indicate assigned color)
		//also used in the string representation of the board
	public static final char UNOCCUPIED = 'U'; 
		//used in the string representation representation of the board
	public static final char PASS = 'P'; //used to indicate a pass move
	public static final char VALID = 'V'; 
		//From server, to indicate that a move is valid (always sent after 'R;')
	public static final char INVALID = 'I'; 
		//From server, to indicate that a move is invalid (always sent after 'R;')
	public static final char FINISHED = 'F'; 
		//From server, indicates normal end of game (after double pass)
	public static final char DISCONNECT = 'D'; 
		//From server, indicates that other player disconnected (= end of game)
	public static final char CHEAT = 'C'; //From server, to players (the non-cheating player wins!)
	public static final char EXIT = 'X'; //From server, indicates that other player quit
}