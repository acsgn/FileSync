package main;

public class FileInfo {
	
	private String name;
	private long length;
	private long hash;
	
	public FileInfo(String name, long length, long hash) {
		this.name = name;
		this.length = length;
		this.hash = hash;
	}
	
	public String getName() {
		return name;
	}

	public long getLength() {
		return length;
	}

	public long getHash() {
		return hash;
	}

	@Override
	public String toString() {
		return name+length+hash;
	}

}
