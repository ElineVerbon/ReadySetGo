package protocol;

import client.ServerHandlerForHumanClient;

public class MessageGenerator {
	
	ServerHandlerForHumanClient serverHandler;
	
	public MessageGenerator(ServerHandlerForHumanClient aServerHandler) {
		serverHandler = aServerHandler;
	}
	
	public void errorMessage(String message) {
		String errorMessage = ProtocolMessages.ERROR + ProtocolMessages.DELIMITER + 
						serverHandler.getVersion() + ProtocolMessages.DELIMITER + message;
		messageSender(errorMessage);
	}
	
	public void moveMessage(String move) {
		
		String moveMessage = ProtocolMessages.MOVE + ProtocolMessages.DELIMITER + move;
		messageSender(moveMessage);
	}
	
	public void messageSender(String message) {
		serverHandler.sendToGame(message);
	}
}
