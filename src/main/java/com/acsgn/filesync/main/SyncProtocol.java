package main;

import java.util.ArrayList;
import java.util.HashMap;

import socket.CommandSocket;
import socket.Connection;
import socket.FileSocket;

public class SyncProtocol implements Runnable {

	private FolderOperations fo;
	private Connection connection;
	private CommandSocket comSoc;
	private FileSocket fiSoc;
	private boolean isMaster;

	private HashMap<String, String> fileHashInfoMap;
	private HashMap<String, String> oldFileHashInfoMap;
	private ArrayList<String> deletedFiles;
	private ArrayList<String> renamedFiles;

	/**
	 * Creates a runnable synchronization object that synchronize the system with
	 * other user(s)
	 * 
	 * @param connection       connectionSocket for sync operations
	 * @param folderOperations folderOperations for folder operations
	 * @param isMaster         Sets system as master or follower according to value
	 */
	public SyncProtocol(FolderOperations folderOperations, boolean isMaster) {
		this.fo = folderOperations;
		this.isMaster = isMaster;
		fileHashInfoMap = new HashMap<String, String>();
		oldFileHashInfoMap = new HashMap<String, String>();
		deletedFiles = new ArrayList<String>();
		renamedFiles = new ArrayList<String>();
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
		comSoc = connection.createCommandSocket();
		fiSoc = connection.createFileSocket();
	}

	/**
	 * Starts and follows the synchronization protocol for DriveCloud
	 */
	public void run() {
		long time = System.nanoTime();
		updateFileList();
		detectDeletedAndRenamedFiles();
		if (isMaster) {
			sendRenamedList();
			sendDeletedList();
			sendFileList();
		}
		String received;
		do {
			received = comSoc.receiveCommand();
			System.out.println(received);
			String[] command = received.split("/");
			switch (command[0]) {
			case "RETRANSMIT":
				sendFile(command[1]);
				break;
			case "DELETE":
				deleteFiles(command);
				break;
			case "RENAME":
				renameFiles(command);
				break;
			case "FILELIST":
				if (command.length > 1) {
					String[] filesToGet = detectFilesToGet(received);
					if (filesToGet != null) {
						for (String fileInfo : filesToGet) {
							String[] info = fileInfo.split(":");
							do {
								Controller.getInstance().publishEvent("Transmit request for file " + info[0] + " sent.");
								sendFileRequest(info[0]);
								receiveFile(fileInfo);
							}while (!fo.hashCheck(info[0], info[2]));
							Controller.getInstance().publishEvent("Consistency check for " + info[0] + " passed");
						}
						updateFileList();
					}
				}
				if (isMaster)
					close();
				else {
					sendRenamedList();
					sendDeletedList();
					sendFileList();
				}
				break;
			}
		} while (!received.equals("CLOSE"));
		if (!isMaster)
			close();
		connection.close();
		System.out.println(System.nanoTime() - time);
	}

	/**
	 * Sends the files' all informations to other user
	 */
	private void sendFileList() {
		String command = "FILELIST";
		if (!fileHashInfoMap.isEmpty())
			for (String tmp : fileHashInfoMap.values())
				command += "/" + tmp;
		comSoc.sendCommand(command);
	}

	/**
	 * Sends renamed files' SHA1 values and new names to the other user with RENAME
	 * command
	 */
	private void sendRenamedList() {
		if (!renamedFiles.isEmpty()) {
			String command = "RENAME";
			for (String tmp : renamedFiles)
				command += "/" + tmp;
			comSoc.sendCommand(command);
		}
	}

	/**
	 * Sends deleted files' names to the other user with DELETE command
	 */
	private void sendDeletedList() {
		if (!deletedFiles.isEmpty()) {
			String command = "DELETE";
			for (String tmp : deletedFiles)
				command += "/" + tmp;
			comSoc.sendCommand(command);
		}
	}

	/**
	 * Renames all the files in command with received new names
	 * 
	 * @param command RENAME command of the other user
	 */
	private void renameFiles(String[] command) {
		for (int i = 1; i < command.length; i++) {
			String[] infoArray = command[i].split(":");
			String oldName = fileHashInfoMap.get(infoArray[0]).split(":")[0];
			Controller.getInstance().publishEvent("File name changed from " + oldName + " to " + infoArray[1]);
			fo.renameFile(oldName, infoArray[1]);
		}
	}

	/**
	 * Deletes all the files in command
	 * 
	 * @param command DELETE command of the other user
	 */
	private void deleteFiles(String[] command) {
		for (int i = 1; i < command.length; i++) {
			Controller.getInstance().publishEvent("File named " + command[i] + " deleted.");
			fo.deleteFile(command[i]);
		}
	}

	/**
	 * Sends a file request to the other user in order to receive the file
	 * 
	 * @param fileName The name of the requested file
	 */
	private void sendFileRequest(String fileName) {
		comSoc.sendCommand("RETRANSMIT/" + fileName);
	}

	/**
	 * Sends the file to the other user
	 * 
	 * @param filename The name of the file that will be sent
	 */
	private void sendFile(String fileName) {
		Controller.getInstance().publishEvent("Sending " + fileName);
		fiSoc.sendFile(fo.getFilePath(fileName));
		Controller.getInstance().publishEvent(fileName + " sent.");
	}

	/**
	 * Receives the file related with the info provided
	 * 
	 * @param fileInfo The information of the file
	 */
	private void receiveFile(String fileInfo) {
		String[] fileInfoArray = fileInfo.split(":");
		fiSoc.receiveFile(fo.getFilePath(fileInfoArray[0]), Long.parseLong(fileInfoArray[1]), fo);
	}

	/**
	 * Updates the list of files in the system for changes on folder
	 */
	private void updateFileList() {
		ArrayList<String> fileInfoList = fo.fileList();
		oldFileHashInfoMap.clear();
		deletedFiles.clear();
		renamedFiles.clear();
		oldFileHashInfoMap.putAll(fileHashInfoMap);
		fileHashInfoMap.clear();
		if (!fileInfoList.isEmpty()) {
			for (String tmp : fileInfoList) {
				String[] infoArray = tmp.split(":");
				fileHashInfoMap.put(infoArray[2], tmp);
			}
		}
	}

	/**
	 * Detects the files that are not in the system by comparing SHA1 values of
	 * received file list with SHA1 values of flies in our system
	 * 
	 * @param command FILELIST command of the other user
	 * @return Files that will be requested from the other user
	 */
	private String[] detectFilesToGet(String command) {
		String out = "";
		String[] list = command.split("/");
		for (int i = 1; i < list.length; i++) {
			String[] infoArray = list[i].split(":");
			if (!fileHashInfoMap.containsKey(infoArray[2]) && !oldFileHashInfoMap.containsKey(infoArray[2]))
				out += list[i] + "/";
		}
		if (!out.equals("")) {
			return out.substring(0, out.length() - 1).split("/");
		}
		return null;
	}

	/**
	 * Detects deleted or renamed files in the system by comparing old and new file
	 * informations in maps
	 */
	private void detectDeletedAndRenamedFiles() {
		if (!oldFileHashInfoMap.isEmpty()) {
			if (fileHashInfoMap.isEmpty())
				oldFileHashInfoMap.values().forEach(str -> deletedFiles.add(str.split(":")[0]));
			else {
				ArrayList<String> hashes = new ArrayList<String>();
				fileHashInfoMap.keySet().forEach(str -> hashes.add(str));
				for (String hash : hashes) {
					if (oldFileHashInfoMap.containsKey(hash)) {
						String name = fileHashInfoMap.get(hash).split(":")[0];
						String oldName = oldFileHashInfoMap.get(hash).split(":")[0];
						if (!name.equals(oldName))
							renamedFiles.add(hash + ":" + name);
					}
				}
				hashes.forEach(hash -> oldFileHashInfoMap.remove(hash));
				for (String tmp : oldFileHashInfoMap.values()) {
					String[] infoArray = tmp.split(":");
					boolean deleted = true;
					for (String hash : hashes) {
						if (fileHashInfoMap.get(hash).split(":")[0].equals(infoArray[0]))
							deleted = false;
					}
					if (deleted)
						deletedFiles.add(infoArray[0]);
				}
			}
		}
	}

	/**
	 * Closes the protocol
	 */
	private void close() {
		comSoc.sendCommand("CLOSE");
	}

}
