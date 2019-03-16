package main;

import java.io.Closeable;
import java.io.IOException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import socket.Client;
import socket.Connection;

public class Follower implements Runnable, Closeable {

	private ScheduledExecutorService ses;
	
	private Client client;
	private FolderOperations fo;

	public Follower(FolderOperations fo, String IP, int port, int time) {
		client = new Client(IP, port);
		this.fo = fo;
		ses = Executors.newSingleThreadScheduledExecutor();
		ses.scheduleWithFixedDelay(this, 0, time, TimeUnit.SECONDS);
	}

	public void run() {
		try {
			Connection c = client.connect();
			Controller.getInstance().publishEvent("Connected to the master.");
			SyncProtocol sync = new SyncProtocol(c,fo, false);
			sync.run();
		} catch (IOException e) {
			Controller.getInstance().publishEvent("Something happened when connecting master.");
		}
	}
	
	public void close() throws IOException {
		ses.shutdown();
	}

}
