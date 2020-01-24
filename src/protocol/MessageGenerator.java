package protocol;

import client.HumanClientServerCommunicator;

public class MessageGenerator {
	
	HumanClientServerCommunicator serverHandler;
	
	public MessageGenerator(HumanClientServerCommunicator aServerHandler) {
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
