package org.jhttping.chunked;

import java.io.IOException;
import java.io.InputStream;

import org.jhttping.JhttpingApplication;
import org.jhttping.NoMoreDataException;

public class ChunkedBodyReader {
	
	private byte [] buf;
	private InputStream source;
	private FastStringBuffer fBuf = new FastStringBuffer();
	
	
	private final static FastString CRLF = new FastString("\r\n"); 
	
	private int chunkCounter  = 0;
	private int bodySize = 0;
	
	
	public ChunkedBodyReader(InputStream source, int bufSize, byte [] body) {
		buf = new byte[bufSize];
		this.source = source;
		if (body != null && body.length > 0) {
			fBuf.expand(new FastString(body));
		}
	}
	
	public void readChunkedBody() throws IOException {
		int chunkSize = readNextChunk();
		
		while (chunkSize > 0) {
			chunkCounter++;
			bodySize+=chunkSize;
			chunkSize = readNextChunk();
		}
	}
	
	private int readNextChunk() throws IOException {
		int size = readChunkSize();
		int toRead = size+2;
		if (toRead < fBuf.size()) {
			fBuf.contract(toRead);
		} else if (toRead == fBuf.size()) {
			fBuf.clear();
		} else {
			toRead-=fBuf.size();
			fBuf.clear();
			int lastRead = 0;
			while (toRead > 0) {
				lastRead = readNextPart(false);
				toRead-=lastRead;
			}
			if (toRead<0) {
				int offset = lastRead+toRead;
				int count = -toRead;
				fBuf.expand(new FastString(buf, offset, count));
			}
		}

		return size;
	}
	
	private int readChunkSize() throws IOException {
		int fromIndex = 0;
		int index = fBuf.indexOf(CRLF, fromIndex);
		while (index <0) {
			fromIndex = Math.max(fBuf.size()-1,0);
			readNextPart(true);
			index = fBuf.indexOf(CRLF, fromIndex);
		}
		FastString sizeStr = fBuf.substring(0, index);
		fBuf.contract(index+2);
		try {
			int result = Integer.parseInt(sizeStr.toString(), 16);
			return result;
		} catch (NumberFormatException e) {
			throw new IOException(e.getMessage());
		}
	}
	
	private int readNextPart(boolean expandBuf) throws IOException{
		int read = source.read(buf);
		if (read > 0) {
			JhttpingApplication.dumpServerData(buf, read);
		}
		if (read > 0 && expandBuf) {
			fBuf.expand(new FastString(buf, 0, read));
		}	
		if (read < 0) {
			throw new NoMoreDataException();
		}
		return read;
	}

	public int getChunkCounter() {
		return chunkCounter;
	}

	public int getBodySize() {
		return bodySize;
	}

	

}
