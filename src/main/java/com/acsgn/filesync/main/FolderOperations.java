package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.zip.Checksum;

import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

public class FolderOperations {

	private static final int seed = 9896;
	private static final XXHashFactory factory = XXHashFactory.fastestInstance();
	private StreamingXXHash64 hashing;
	
	private File folder;
	private Hashtable<String, SimpleEntry<Long, Long>> calculatedHashes;

	/**
	 * Creates a folderOperations object to perform file operations such as getting
	 * list of files in the folder, deleting a file etc. Also creates a INUSE file
	 * that deletes itself on exit to lock the folder for other instances of
	 * DriveCloud
	 * 
	 * @param folderPath Path of the folder that will be used on operations
	 */
	public FolderOperations(String folderPath) {
		folder = new File(folderPath);
		hashing = factory.newStreamingHash64(seed);
		calculatedHashes = new Hashtable<String, SimpleEntry<Long, Long>>();
	}

	/**
	 * Create a list of the files on the folder for sending and receiving purposes
	 * 
	 * @return ArrayList of files on the folder
	 */
	public ArrayList<String> fileList() {
		return fileListHelper(folder);
	}

	private ArrayList<String> fileListHelper(File directory) {
		ArrayList<String> list = new ArrayList<String>();
		for (File file : directory.listFiles()) {
			if (file.isFile())
				list.add(file.getName() + ":" + file.length() + ":" + calcXXHash(file));
			else
				for (String fileString : fileListHelper(file))
					list.add(file.getName() + "\\" + fileString);
		}
		return list;
	}

	/**
	 * Changes the name of the file
	 * 
	 * @param oldName The old name that will be changed
	 * @param newName The new name that will be used
	 */
	public void renameFile(String oldName, String newName) {
		File file = new File(getFilePath(oldName));
		File newFile = new File(getFilePath(newName));
		file.renameTo(newFile);
		calculatedHashes.put(newFile.getAbsolutePath(), calculatedHashes.remove(file.getAbsolutePath()));
	}

	/**
	 * Deletes The provided file
	 * 
	 * @param filename The name of file to be deleted
	 */
	public void deleteFile(String filename) {
		File file = new File(getFilePath(filename));
		file.delete();
		calculatedHashes.remove(file.getAbsolutePath());
	}

	/**
	 * Gets the path to the file with the given name
	 * 
	 * @param name The name of the file
	 * @return The path of the file
	 */
	public String getFilePath(String name) {
		return folder.getPath() + "/" + name;
	}

	/**
	 * Checks hash of the file that received from other user.
	 * 
	 * @param fileName Name of the file that will be checked
	 * @param hash     Hash of the file that was sent from other user
	 * @return Result of hash checking
	 */
	public boolean hashCheck(String fileName, String hash) {
		return Long.parseLong(hash) == calcXXHash(new File(getFilePath(fileName)));
	}

	public Checksum getChecksum() {
		return factory.newStreamingHash64(seed).asChecksum();
	}

	public void registerHash(long hash,String path) {
		File file = new File(path);
		SimpleEntry<Long, Long> pair = new SimpleEntry<Long, Long>(file.lastModified(), hash);
		calculatedHashes.put(file.getAbsolutePath(), pair);
	}

	private long calcXXHash(File file) {
		if (calculatedHashes.containsKey(file.getAbsolutePath())) {
			SimpleEntry<Long, Long> pair = calculatedHashes.get(file.getAbsolutePath());
			if (pair.getKey() == file.lastModified())
				return pair.getValue();
		}
		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[16384];
			int bytesRead;
			do {
				bytesRead = fis.read(buffer);
				if (bytesRead > 0)
					hashing.update(buffer, 0, bytesRead);
			} while (bytesRead != -1);
			fis.close();
			long result = hashing.getValue();
			hashing.reset();
			SimpleEntry<Long, Long> pair = new SimpleEntry<Long, Long>(file.lastModified(), result);
			calculatedHashes.put(file.getAbsolutePath(), pair);
			return result;
		} catch (IOException e) {
			System.err.println("File not found wih name " + file.getName());
			return 0;
		} 
	}

}
