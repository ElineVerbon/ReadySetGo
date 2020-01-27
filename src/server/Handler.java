package server;

import java.net.SocketTimeoutException;

public interface Handler {
	public void sendMessageToClient(String msg);
	
	public String getReply() throws SocketTimeoutException;
}
