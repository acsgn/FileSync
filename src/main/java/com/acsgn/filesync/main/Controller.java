package main;

import java.io.Closeable;
import java.io.IOException;

import java.util.ArrayList;

public class Controller {

	private static final int DEFAULT_SYNC_PORT = 12345;

	private static Controller self;

	private ArrayList<Listener> listeners;

	private FolderOperations fo;
	private Closeable sync;

	private Controller() {
		listeners = new ArrayList<Listener>();
	}

	public void dispatchMessage(String message) {
		String[] parsed = message.split("/");
		switch (parsed[0]) {
		case "FOLDER":
			fo = new FolderOperations(parsed[1]);
			break;
		case "MASTER":
			Master master = new Master(fo, DEFAULT_SYNC_PORT);
			new Thread(master, "Master").start();
			sync = master;
			publishEvent("Started as Master.");
			break;
		case "FOLLOWER":
			sync = new Follower(fo, parsed[1], DEFAULT_SYNC_PORT, Integer.parseInt(parsed[2]));
			publishEvent("Started as Follower.");
			break;
		case "STOP":
			try {
				sync.close();
				publishEvent("Closed.");
			} catch (IOException e) {
				publishEvent("Something happened when closing.");
			}
			break;
		}
	}

	public void addListener(Listener lis) {
		listeners.add(lis);
	}

	public void publishEvent(String message) {
		for (Listener l : listeners) {
			l.onEvent(message);
		}
	}

	public static synchronized Controller getInstance() {
		if (self == null)
			self = new Controller();
		return self;
	}

}
