package com.acsgn.filesync.main;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.acsgn.filesync.socket.Client;
import com.acsgn.filesync.socket.Connection;
import com.acsgn.filesync.socket.Server;

public class Main {

	private static final String DEFAULT_SERVER_ADDRESS = "localhost";
	private static final int DEFAULT_SYNC_SERVER_PORT = 12385;
	private static final int DEFAULT_DISCOVERY_SERVER_PORT = 42352;

	/**
	 * Start function of the system as master or follower and states time between
	 * according to arguments
	 * 
	 * @param args Arguments for system
	 */
	public static void main(String[] args) {

		int defaultTime = 30;

		if (args.length != 0 && args[0].compareToIgnoreCase("-m") == 0) {
			if (args.length == 3 && args[1].compareToIgnoreCase("-t") == 0) {
				defaultTime = Integer.parseInt(args[2]);
			}
			Connection dis = new Server(DEFAULT_DISCOVERY_SERVER_PORT);
			Connection sync = new Server(DEFAULT_SYNC_SERVER_PORT);
			FolderOperations fo = new FolderOperations(pathForDriveCloud());
			while (true) {
				dis.connect();
				dis.close();
				ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
				Sync syncS = new Sync(sync, fo, true);
				ses.scheduleWithFixedDelay(syncS, 0, defaultTime, TimeUnit.SECONDS);
			}

		} else if (args.length != 0 && args[0].compareToIgnoreCase("-f") == 0) {
			String serverAdress = DEFAULT_SERVER_ADDRESS;
			if (args.length == 3 && args[1].compareToIgnoreCase("-t") == 0) {
				defaultTime = Integer.parseInt(args[2]);
			} else if (args.length == 4 && args[2].compareToIgnoreCase("-t") == 0) {
				serverAdress = args[1];
				defaultTime = Integer.parseInt(args[3]);
			}
			Connection dis = new Client(serverAdress, DEFAULT_DISCOVERY_SERVER_PORT);
			Connection sync = new Client(serverAdress, DEFAULT_SYNC_SERVER_PORT);
			FolderOperations fo = new FolderOperations(pathForDriveCloud());
			dis.connect();
			dis.close();
			ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
			Sync syncC = new Sync(sync, fo, false);
			ses.scheduleWithFixedDelay(syncC, 0, defaultTime, TimeUnit.SECONDS);

		} else {
			System.out.println("Usage:");
			System.out.println("\t-m: Setup as master");
			System.out.println("\t-f: Setup as follower with default server address (localhost)");
			System.out.println("\t-f serverAdress: Setup as follower with given server address");
			System.out.println();
			System.out.println("\tOptional");
			System.out.println("\t\t -t time: Amount of time between synchronization as seconds");
			System.out.println("\t\t\t  Default is 30 seconds");
		}
	}

	/**
	 * Decide which folder to use for operations
	 * 
	 * @return Path of the folder
	 */
	private static String pathForDriveCloud() {
		String defaultPath = System.getProperty("user.home") + "\\Desktop\\DriveCloud";
		String path = defaultPath;
		File folder = new File(path);
		int count = 0;
		while (folder.exists()) {
			File usage = new File(path + "\\INUSE");
			if (usage.exists()) {
				count++;
				path = defaultPath + count;
				folder = new File(path);
				continue;
			}
			return path;
		}
		folder.mkdirs();
		return path;
	}

}
