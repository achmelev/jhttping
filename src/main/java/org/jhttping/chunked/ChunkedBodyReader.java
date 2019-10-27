package org.jhttping.chunked;

import java.io.InputStream;

public class ChunkedBodyReader {
	
	private byte [] buf;
	private InputStream source;
	
	public ChunkedBodyReader(InputStream source, int bufSize) {
		buf = new byte[bufSize];
		this.source = source;
	}
	
	public int read() {
		return 0;
	}

}
