package server;

public interface Handler {

	String doTurnMessage(String board, String opponentsMove);

	void endGameMessage(char reasonGameEnd, char winner, String string, String string2);

	void errorMessage(String string);

	void giveResultMessage(boolean valid, String message);

	void startGameMessage(String board, char colorPlayer2);

}
