package socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CommandSocket {

	private BufferedReader is;
	private PrintWriter os;

	/**
	 * Creates a commandSocket object for file operations
	 * 
	 * @param socket A connection socket
	 * @param inputStream Input stream for command operations
	 * @param outputStream Output stream for command operations
	 */
	public CommandSocket(Socket socket) {
		try {
			this.is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.os = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends commands to the client.
	 * 
	 * @param command Command that will be sent
	 */
	public void sendCommand(String command) {
		os.println(command);
	}

	/**
	 * Receives command from the client.
	 * 
	 * @return Command
	 */
	public String receiveCommand() {
		try {
			return is.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
