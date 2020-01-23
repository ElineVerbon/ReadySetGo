package protocol;

public class MessageGenerator {
	
	public String errorMessage(String versionNumber, String message) {
		
		String errorMessage = ProtocolMessages.ERROR + ProtocolMessages.DELIMITER + 
						versionNumber + ProtocolMessages.DELIMITER + message;
		return errorMessage;
		
	}
}
