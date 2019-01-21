package main;

import gui.GUI;

public class Main {

	public static void main(String[] args) {

		FileSync fS = new FileSync();
		Controller.getInstance().setFileSync(fS);
		GUI gui = new GUI();
		Controller.getInstance().addListener(gui);

	}

}
