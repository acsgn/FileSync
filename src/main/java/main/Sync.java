package main;

import java.util.ArrayList;
import java.util.HashMap;

import socket.CommandSocket;
import socket.Connection;
import socket.FileSocket;

public class Sync implements Runnable {
	private FolderOperations fo;
	private Connection connection;
	private CommandSocket comSoc;
	private FileSocket fiSoc;
	private boolean isMaster;
	private HashMap<String, String> fileHashInfoMap = new HashMap<String, String>();
	private HashMap<String, String> oldFileHashInfoMap = new HashMap<String, String>();
	private ArrayList<String> deletedFiles = new ArrayList<String>();
	private ArrayList<String> renamedFiles = new ArrayList<String>();

	/**
	 * Creates a runnable synchronization object that synchronize the system with
	 * other user(s)
	 * 
	 * @param commandSocket    commandSocket for command operations
	 * @param fileSocket       fileSocket for file operations
	 * @param folderOperations folderOperations for folder operations
	 * @param isMaster         Sets system as master or follower according to value
	 */
	public Sync(Connection connection, FolderOperations folderOperations, boolean isMaster) {
		this.connection = connection;
		this.fo = folderOperations;
		this.isMaster = isMaster;
	}

	/**
	 * Starts and follows the synchronization protocol for DriveCloud
	 */
	public void run() {
		System.out.println("Sync started");
		connection.connect();
		comSoc = connection.createCommandSocket();
		fiSoc = connection.createFileSocket();
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
			if (command[0].compareTo("RETRANSMIT") == 0)
				sendFile(command[1]);
			else if (command[0].compareTo("DELETE") == 0) {
				if (command.length > 1)
					deleteFiles(command);
			} else if (command[0].compareTo("RENAME") == 0) {
				if (command.length > 1)
					renameFiles(command);
			} else if (command[0].compareTo("FILELIST") == 0) {
				if (command.length > 1) {
					String[] filesToGet = detectFilesToGet(received);
					if (filesToGet != null) {
						for (String fileInfo : filesToGet) {
							String[] info = fileInfo.split(":");
							sendFileRequest(info[0]);
							receiveFile(fileInfo);
							while (!hashControl(info[0], info[2])) {
								System.out.println("Retransmit request for file " + info[0]);
								sendFileRequest(info[0]);
								receiveFile(fileInfo);
							}
							System.out.println("Consistency check for " + info[0] + " passed");
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
			}
		} while (received.compareTo("CONSISTENCY_CHECK_PASSED") != 0);
		if (!isMaster)
			close();
		System.out.println("Sync completed");
		connection.close();
	}

	/**
	 * Sends the files' all informations to other user
	 */
	private void sendFileList() {
		String list = "";
		if (fileHashInfoMap.isEmpty())
			comSoc.sendCommand("FILELIST");
		else {
			for (String tmp : fileHashInfoMap.values()) {
				list += tmp + "/";
			}
			comSoc.sendCommand("FILELIST/" + list.substring(0, list.length() - 1));
		}
	}

	/**
	 * Sends renamed files' SHA1 values and new names to the other user with RENAME
	 * command
	 */
	private void sendRenamedList() {
		String list = "";
		if (renamedFiles.isEmpty())
			comSoc.sendCommand("RENAME");
		else {
			for (String tmp : renamedFiles) {
				list += tmp + "/";
			}
			comSoc.sendCommand("RENAME/" + list.substring(0, list.length() - 1));
		}
	}

	/**
	 * Sends deleted files' names to the other user with DELETE command
	 */
	private void sendDeletedList() {
		String list = "";
		if (deletedFiles.isEmpty())
			comSoc.sendCommand("DELETE");
		else {
			for (String tmp : deletedFiles) {
				list += tmp + "/";
			}
			comSoc.sendCommand("DELETE/" + list.substring(0, list.length() - 1));
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
			System.out.println("File name changed from " + oldName + " to " + infoArray[1]);
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
			System.out.println("File named " + command[i] + " deleted.");
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
	private void sendFile(String filename) {
		fiSoc.sendFile(fo.getFilePath(filename));
	}

	/**
	 * Receives the file related with the info provided
	 * 
	 * @param fileInfo The information of the file
	 */
	private void receiveFile(String fileInfo) {
		String[] fileInfoArray = fileInfo.split(":");
		fiSoc.receiveFile(fo.getFilePath(fileInfoArray[0]), Long.parseLong(fileInfoArray[1]));
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
		if (out.compareTo("") != 0) {
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
						if (name.compareTo(oldName) != 0)
							renamedFiles.add(hash + ":" + name);
					}
				}
				hashes.forEach(hash -> oldFileHashInfoMap.remove(hash));
				for (String tmp : oldFileHashInfoMap.values()) {
					String[] infoArray = tmp.split(":");
					boolean deleted = true;
					for (String hash : hashes) {
						if (fileHashInfoMap.get(hash).split(":")[0].compareTo(infoArray[0]) == 0)
							deleted = false;
					}
					if (deleted)
						deletedFiles.add(infoArray[0]);
				}
			}
		}
	}

	/**
	 * Check SHA1 value of the file as if it received without a loss
	 * 
	 * @param name Name of the received file
	 * @param hash Hash of the file that was received from the other user
	 * @return Result of hash checking
	 */
	private boolean hashControl(String name, String hash) {
		return fo.hashCheck(name, hash);
	}

	/**
	 * Closes the protocol
	 */
	private void close() {
		comSoc.sendCommand("CONSISTENCY_CHECK_PASSED");
	}

}
