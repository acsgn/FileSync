package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Hashtable;

import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;

public class FolderOperations {
	private File folder;
	private File usage;
	private Hashtable<String, SimpleEntry<Long, Integer>> calculatedHashes;
	private static final int seed = 9896;
	private static final XXHashFactory factory = XXHashFactory.fastestInstance();
	private static final StreamingXXHash32 hashing = factory.newStreamingHash32(seed);

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
		usage = new File(folderPath + "\\INUSE");
		try {
			usage.createNewFile();
		} catch (IOException e) {
			System.err.println("Cannot mark folder as in use");
		}
		usage.deleteOnExit();
		calculatedHashes = new Hashtable<String, SimpleEntry<Long, Integer>>();
	}

	/**
	 * Create a list of the files on the folder for sending and receiving purposes
	 * 
	 * @return ArrayList of files on the folder
	 */
	public ArrayList<String> fileList() {
		ArrayList<String> list = new ArrayList<String>();
		for (File tmp : folder.listFiles()) {
			if (tmp.getName().compareTo(usage.getName()) != 0)
				list.add(tmp.getName() + ":" + tmp.length() + ":" + calcXXHash(tmp));
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
		file.renameTo(new File(getFilePath(newName)));
	}

	/**
	 * Deletes The provided file
	 * 
	 * @param filename The name of file to be deleted
	 */
	public void deleteFile(String filename) {
		File file = new File(getFilePath(filename));
		file.delete();
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
		return Integer.parseInt(hash) == calcXXHash(new File(getFilePath(fileName)));
	}

	private int calcXXHash(File file) {
		if (calculatedHashes.containsKey(file.getName())) {
			SimpleEntry<Long, Integer> pair = calculatedHashes.get(file.getName());
			if (pair.getKey() == file.lastModified())
				return pair.getValue();
		}
		int result = 0;
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
			result = hashing.getValue();
			hashing.reset();
			SimpleEntry<Long, Integer> pair = new SimpleEntry<Long, Integer>(file.lastModified(), result);
			calculatedHashes.put(file.getName(), pair);
		} catch (FileNotFoundException e) {
			System.err.println("File not found wih name " + file.getName());
		} catch (IOException e) {
			System.err.println("I/O Exception");
		}
		return result;
	}

}
