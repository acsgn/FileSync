package com.acsgn.filesync.socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FileSocket {

	private static final int BUFFER_SIZE = 32768;
	private InputStream is;
	private OutputStream os;


	public FileSocket(Socket s) {
		try {
			is = s.getInputStream();
			os = s.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sends a file over the socket connection
	 * 
	 * @param path Path of the file that will be send
	 */
	public void sendFile(String path) {
		try {
			File file = new File(path);
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			System.out.println("Sending " + file.getName() + " (" + file.length() + " bytes)");
			while ((bytesRead = fis.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			fis.close();
			System.out.println(file.getName() + " sent.");
		} catch (Exception e) {
			System.err.println("Exception on sending file");
		}
	}

	/**
	 * Receives a file over the socket connection fileSize is used to determine if
	 * system received every byte
	 * 
	 * @param path     Path of the file that will be received
	 * @param fileSize Size of the file that will be received
	 */
	public void receiveFile(String path, long fileSize) {
		try {
			File file = new File(path);
			byte[] buffer = new byte[BUFFER_SIZE];
			FileOutputStream fos = new FileOutputStream(file);
			int bytesRead;
			long current = 0;
			while (current < fileSize) {
				bytesRead = is.read(buffer);
				fos.write(buffer, 0, bytesRead);
				current += bytesRead;
			}
			fos.close();
			System.out.println(file.getName() + " downloaded (" + current + " bytes read)");
		} catch (IOException e) {
			System.err.println("Couldn't receive file");
		}
	}

}
