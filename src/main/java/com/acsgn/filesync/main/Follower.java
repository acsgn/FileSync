package main;

import java.io.Closeable;
import java.io.IOException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import socket.Client;

public class Follower implements Runnable, Closeable {

	private ScheduledExecutorService ses;
	
	private Client client;
	private SyncProtocol sync;

	public Follower(FolderOperations fo, String IP, int port, int time) {
		client = new Client(IP, port);
		sync = new SyncProtocol(fo, false);
		ses = Executors.newSingleThreadScheduledExecutor();
		ses.scheduleWithFixedDelay(this, 0, time, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		try {
			sync.setConnection(client.connect());
			Controller.getInstance().publishEvent("Connected to the master.");
			sync.run();
		} catch (IOException e) {
			Controller.getInstance().publishEvent("Something happened when connecting master.");
		}
	}
	
	@Override
	public void close() throws IOException {
		ses.shutdown();
	}

}
