package socket;

import java.io.IOException;
import java.net.Socket;

public class Client extends Connection {

	private String serverAddress;
	private int port;

	public Client(String serverAddress, int port) {
		this.serverAddress = serverAddress;
		this.port = port;
	}

	/**
	 * Establishes a socket connection to the server that is identified by the
	 * serverAddress
	 */
	@Override
	public void connect() {
		try {
			Socket socket = new Socket(serverAddress, port);
			System.out.println(socket.toString());
			super.setSocket(socket);
		} catch (IOException e) {
			System.err.println("No server has been found on " + serverAddress);
		}
	}

}