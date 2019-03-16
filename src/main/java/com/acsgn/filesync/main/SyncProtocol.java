package main;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import socket.CommandSocket;
import socket.Connection;
import socket.FileSocket;

public class SyncProtocol implements Runnable {

	private static final Type hT = new TypeToken<Hashtable<Long, FileInfo>>() {
	}.getType();
	private static final Type aL = new TypeToken<ArrayList<FileInfo>>() {
	}.getType();

	private FolderOperations fo;
	private Connection connection;
	private CommandSocket comSoc;
	private FileSocket fiSoc;
	private boolean isMaster;
	private Gson gson;

	private Hashtable<Long, FileInfo> fileHashInfoTable;
	private Hashtable<Long, FileInfo> oldFileHashInfoTable;
	private ArrayList<FileInfo> deletedFiles;
	private ArrayList<FileInfo> renamedFiles;

	/**
	 * Creates a runnable synchronization object that synchronize the system with
	 * other user(s)
	 * 
	 * @param connection       connectionSocket for sync operations
	 * @param folderOperations folderOperations for folder operations
	 * @param isMaster         Sets system as master or follower according to value
	 */
	public SyncProtocol(Connection c, FolderOperations folderOperations, boolean isMaster) {
		this.fo = folderOperations;
		this.isMaster = isMaster;
		this.connection = c;
		comSoc = connection.createCommandSocket();
		fiSoc = connection.createFileSocket();
		fileHashInfoTable = new Hashtable<Long, FileInfo>();
		oldFileHashInfoTable = new Hashtable<Long, FileInfo>();
		deletedFiles = new ArrayList<FileInfo>();
		renamedFiles = new ArrayList<FileInfo>();
		gson = new Gson();
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
			if (received.startsWith("TRANSMIT"))
				sendFile(received.replaceFirst("TRANSMIT", ""));
			else if (received.startsWith("DELETE"))
				deleteFiles(received.replaceFirst("DELETE", ""));
			else if (received.startsWith("RENAME"))
				renameFiles(received.replaceFirst("DELETE", ""));
			else if (received.startsWith("FILELIST")) {
				updateFolder(received);
				if (isMaster)
					close();
				else {
					sendRenamedList();
					sendDeletedList();
					sendFileList();
				}
			}
		} while (!received.equals("CLOSE"));
		if (!isMaster)
			close();
		connection.close();
		System.out.println(System.nanoTime() - time);
	}

	private void updateFolder(String received) {
		String json = received.replaceFirst("FILELIST", "");
		if (json.length() > 0) {
			ArrayList<FileInfo> filesToGet = detectFilesToGet(json);
			if (filesToGet != null) {
				for (FileInfo fI: filesToGet) {
					do {
						Controller.getInstance()
								.publishEvent("Transmit request for file " + fI.getName() + " sent.");
						sendFileRequest(fI.getName());
						receiveFile(fI);
					} while (!fo.hashCheck(fI.getName(), fI.getHash()));
					Controller.getInstance().publishEvent("Consistency check for " + fI.getName() + " passed");
				}
				updateFileList();
			}
		}
	}

	/**
	 * Sends the files' all informations to other user
	 */
	private void sendFileList() {
		String command = "FILELIST";
		if (!fileHashInfoTable.isEmpty()) {
			command += gson.toJson(fileHashInfoTable, hT);
		}
		comSoc.sendCommand(command);
	}

	/**
	 * Sends renamed files' SHA1 values and new names to the other user with RENAME
	 * command
	 */
	private void sendRenamedList() {
		if (!renamedFiles.isEmpty()) {
			String command = "RENAME";
			command += gson.toJson(renamedFiles, aL);
			comSoc.sendCommand(command);
		}
	}

	/**
	 * Sends deleted files' names to the other user with DELETE command
	 */
	private void sendDeletedList() {
		if (!deletedFiles.isEmpty()) {
			String command = "DELETE";
			command += gson.toJson(deletedFiles, aL);
			comSoc.sendCommand(command);
		}
	}

	/**
	 * Renames all the files in command with received new names
	 * 
	 * @param command RENAME command of the other user
	 */
	private void renameFiles(String json) {
		ArrayList<FileInfo> rF = gson.fromJson(json, aL);
		for (FileInfo fI : rF) {
			String oldName = fileHashInfoTable.get(fI.getHash()).getName();
			Controller.getInstance().publishEvent("File name changed from " + oldName + " to " + fI.getName());
			fo.renameFile(oldName, fI.getName());
		}
	}

	/**
	 * Deletes all the files in command
	 * 
	 * @param command DELETE command of the other user
	 */
	private void deleteFiles(String json) {
		ArrayList<FileInfo> dF = gson.fromJson(json, aL);
		for (FileInfo fI : dF) {
			Controller.getInstance().publishEvent("File named " + fI.getName() + " deleted.");
			fo.deleteFile(fI.getName());
		}
	}

	/**
	 * Sends a file request to the other user in order to receive the file
	 * 
	 * @param fileName The name of the requested file
	 */
	private void sendFileRequest(String fileName) {
		comSoc.sendCommand("TRANSMIT" + fileName);
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
	private void receiveFile(FileInfo fI) {
		fiSoc.receiveFile(fo.getFilePath(fI.getName()), fI.getLength(), fo);
	}

	/**
	 * Updates the list of files in the system for changes on folder
	 */
	private void updateFileList() {
		ArrayList<FileInfo> fileInfoList = fo.update();
		oldFileHashInfoTable.clear();
		deletedFiles.clear();
		renamedFiles.clear();
		oldFileHashInfoTable.putAll(fileHashInfoTable);
		fileHashInfoTable.clear();
		if (!fileInfoList.isEmpty()) {
			for (FileInfo tmp : fileInfoList) {
				fileHashInfoTable.put(tmp.getHash(), tmp);
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
	private ArrayList<FileInfo> detectFilesToGet(String json) {
		ArrayList<FileInfo> tmp = new ArrayList<>();
		Hashtable<Long, FileInfo> list = gson.fromJson(json, hT);
		for (FileInfo fI: list.values()) {
			if (!fileHashInfoTable.containsKey(fI.getHash()) && !oldFileHashInfoTable.containsKey(fI.getHash()))
				tmp.add(fI);
		}
		return tmp;
	}

	/**
	 * Detects deleted or renamed files in the system by comparing old and new file
	 * informations in maps
	 */
	private void detectDeletedAndRenamedFiles() {
		if (!oldFileHashInfoTable.isEmpty()) {
			if (fileHashInfoTable.isEmpty())
				oldFileHashInfoTable.values().forEach(fI -> deletedFiles.add(fI));
			else {
				ArrayList<Long> hashes = new ArrayList<Long>();
				fileHashInfoTable.keySet().forEach(str -> hashes.add(str));
				for (long hash : hashes) {
					if (oldFileHashInfoTable.containsKey(hash)) {
						String name = fileHashInfoTable.get(hash).getName();
						String oldName = oldFileHashInfoTable.get(hash).getName();
						if (!name.equals(oldName))
							renamedFiles.add(fileHashInfoTable.get(hash));
					}
				}
				hashes.forEach(hash -> oldFileHashInfoTable.remove(hash));
				for (FileInfo tmp : oldFileHashInfoTable.values()) {
					boolean deleted = true;
					for (long hash : hashes) {
						if (fileHashInfoTable.get(hash).getName().equals(tmp.getName()))
							deleted = false;
					}
					if (deleted)
						deletedFiles.add(tmp);
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
