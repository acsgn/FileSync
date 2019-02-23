package socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

	private static final int timeout = 1000;
	private ServerSocket serverSocket;

	/**
	 * Defines port number of the server with default port number and creates a
	 * ServerSocket object
	 */
	public Server(int port) {
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(timeout);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Establishes a socket connection with client
	 * 
	 * @throws IOException
	 */
	public Connection connect() throws IOException {
		Socket socket = serverSocket.accept();
		return new Connection(socket);
	}
	
	public void close() {
		try {
			serverSocket.close();
			finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}