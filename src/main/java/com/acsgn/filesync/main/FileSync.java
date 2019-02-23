package main;

import gui.GUI;

public class FileSync {

	public static void main(String[] args) {
		Controller.getInstance().addListener(new GUI());
	}

}
