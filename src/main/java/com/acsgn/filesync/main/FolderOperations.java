package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
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
	private FilenameFilter fnf;
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
		fnf = (File dir, String name) -> !name.endsWith(".tmp");
	}

	public ArrayList<FileInfo> update() {
		return updateHelper(folder);
	}

	private ArrayList<FileInfo> updateHelper(File directory) {
		ArrayList<FileInfo> tmp = new ArrayList<>();
		for (File file : directory.listFiles(fnf)) {
			if (file.isFile()) {
				try {
					long hash = calcXXHash(file);
					String name = file.getAbsolutePath().substring(folder.getAbsolutePath().length() + 1);
					tmp.add(new FileInfo(name, file.length(), hash));
				} catch (IOException e) {
					continue;
				}
			} else
				tmp.addAll(updateHelper(file));
		}
		return tmp;
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
	public boolean hashCheck(String fileName, long hash) {
		try {
			return hash == calcXXHash(new File(getFilePath(fileName)));
		} catch (IOException e) {
			return false;
		}
	}

	public Checksum getChecksum() {
		return factory.newStreamingHash64(seed).asChecksum();
	}

	public void registerHash(long hash, String path) {
		File file = new File(path);
		SimpleEntry<Long, Long> pair = new SimpleEntry<Long, Long>(file.lastModified(), hash);
		calculatedHashes.put(file.getAbsolutePath(), pair);
	}

	private long calcXXHash(File file) throws IOException {
		if (calculatedHashes.containsKey(file.getAbsolutePath())) {
			SimpleEntry<Long, Long> pair = calculatedHashes.get(file.getAbsolutePath());
			if (pair.getKey() == file.lastModified())
				return pair.getValue();
		}
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
	}

}
