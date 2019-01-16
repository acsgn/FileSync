package com.acsgn.filesync.socket;

import java.io.IOException;
import java.net.Socket;

public abstract class Connection {

	private Socket socket;

	public abstract void connect();

	public CommandSocket createCommandSocket() {
		return new CommandSocket(socket);
	}

	public FileSocket createFileSocket() {
		return new FileSocket(socket);
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
