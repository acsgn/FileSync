package main;

import java.util.ArrayList;

public class Controller {
	
	private static Controller self;
	private ArrayList<Listener> listeners;
	
	private Controller() {
		listeners = new ArrayList<Listener>();
	}
	
	public void dispatchMessage(String message) {
		//.executeMessage(message);
	}

	public void addListener(Listener lis) {
		listeners.add(lis);
	}
	
	public void publishGameEvent(String message) {
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
