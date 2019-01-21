package main;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import socket.Client;
import socket.Connection;
import socket.Server;

public class FileSync implements Runnable {

	private static final int DEFAULT_SYNC_SERVER_PORT = 9896;
	private static final int DEFAULT_DISCOVERY_SERVER_PORT = 9698;

	private FolderOperations fo;
	private int time;

	public FileSync() {
	
	}
	
	private void follower(String IP) {
		Connection dis = new Client(IP, DEFAULT_DISCOVERY_SERVER_PORT);
		Connection sync = new Client(IP, DEFAULT_SYNC_SERVER_PORT);
		dis.connect();
		int time = Integer.parseInt(dis.createCommandSocket().receiveCommand().split("/")[1]);
		dis.close();
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
		SyncProtocol syncC = new SyncProtocol(sync, fo, false);
		ses.scheduleWithFixedDelay(syncC, 0, time, TimeUnit.SECONDS);
	}

	public void executeMessage(String message) {
		String[] parsed = message.split("/");
		switch (parsed[0]) {
		case "FOLDER":
			fo = new FolderOperations(parsed[1]);
			break;
		case "MASTER":
			time = Integer.parseInt(parsed[1]);
			new Thread(this).start();
			break;
		case "FOLLOWER":
			follower(parsed[1]);
			break;
		case "STOP":
			break;
		}

	}

	@Override
	public void run() {
		Connection dis = new Server(DEFAULT_DISCOVERY_SERVER_PORT);
		Connection sync = new Server(DEFAULT_SYNC_SERVER_PORT);
		while (true) {
			dis.connect();
			dis.createCommandSocket().sendCommand("TIME/"+time);
			dis.close();
			ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
			SyncProtocol syncP = new SyncProtocol(sync, fo, true);
			ses.scheduleWithFixedDelay(syncP, 0, time, TimeUnit.SECONDS);
		}
	}

}
