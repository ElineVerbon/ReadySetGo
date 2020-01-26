package server;

public interface Handler {
	public void sendMessageToClient(String msg);
	
	public String getReply();
}
