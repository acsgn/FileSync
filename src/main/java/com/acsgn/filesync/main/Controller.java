package main;

import java.util.ArrayList;

public class Controller {
	
	private static Controller self;
	
	private ArrayList<Listener> listeners;
	private FileSync fileSync;
	
	private Controller() {
		listeners = new ArrayList<Listener>();
	}
	
	public void dispatchMessage(String message) {
		fileSync.executeMessage(message);
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

	public void setFileSync(FileSync fS) {
		fileSync = fS;
	}
	
}
