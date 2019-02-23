package main;

import java.io.Closeable;
import java.io.IOException;

import java.net.SocketTimeoutException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import socket.Server;

public class Master implements Runnable, Closeable {

	private volatile boolean close = false;

	private ExecutorService cTP;

	private Server server;
	private SyncProtocol sync;

	public Master(FolderOperations fo, int port) {
		server = new Server(port);
		sync = new SyncProtocol(fo, true);
		cTP = Executors.newCachedThreadPool();
	}

	@Override
	public void run() {
		while (true) {
			try {
				sync.setConnection(server.connect());
				Controller.getInstance().publishEvent("Connected to a follower.");
				cTP.execute(sync);
			} catch (SocketTimeoutException e) {
				if (close) {
					server.close();
					break;
				}
			} catch (IOException e) {
				Controller.getInstance().publishEvent("Something happened on master.");
			}
		}
	}

	@Override
	public void close() throws IOException {
		close = true;
	}

}
