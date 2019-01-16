package socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Connection {

	private ServerSocket serverSocket;

	/**
	 * Defines port number of the server with default port number and creates a
	 * ServerSocket object
	 */
	public Server(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Establishes a socket connection with client
	 */
	@Override
	public void connect() {
		try {
			Socket socket = serverSocket.accept();
			super.setSocket(socket);
		} catch (IOException e) {
			System.err.println("Cannot connect with client");
		}
	}

}