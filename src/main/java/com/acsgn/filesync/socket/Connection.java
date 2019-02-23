package socket;

import java.io.IOException;
import java.net.Socket;

public class Connection {

	private Socket socket;

	public Connection(Socket socket){
		this.socket = socket;
	}

	public CommandSocket createCommandSocket() {
		return new CommandSocket(socket);
	}

	public FileSocket createFileSocket() {
		return new FileSocket(socket);
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
