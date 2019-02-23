package socket;

import java.io.IOException;
import java.net.Socket;

public class Client {

	private String serverAddress;
	private int port;

	public Client(String serverAddress, int port) {
		this.serverAddress = serverAddress;
		this.port = port;
	}

	/**
	 * Establishes a socket connection to the server that is identified by the
	 * serverAddress
	 * 
	 * @throws IOException
	 */
	public Connection connect() throws IOException {
		return new Connection(new Socket(serverAddress, port));
	}

}