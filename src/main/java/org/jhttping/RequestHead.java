package org.jhttping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

public class RequestHead {
	
	private int responseCode = -1;
	private int contentLengthValue = -1;
	private boolean chunked = false;
	private boolean malformed = false;
	private byte[] body = null;
	private boolean connectionClosed = false; 
	
	private static LineParser statusLineParser = new BasicLineParser();
	
	public RequestHead(byte[] data, int len, int headBoundary) {
		CharArrayBuffer cb = new CharArrayBuffer(headBoundary);
		cb.append(data, 0, headBoundary);
		ParserCursor cursor = new ParserCursor(0, cb.length());
		if (getNextLineEnd(cursor, cb)>0) {
			try {
				StatusLine sl = statusLineParser.parseStatusLine(cb, cursor);
				responseCode = sl.getStatusCode();
				if (responseCode > 0) {
					BufferedReader reader = new BufferedReader(new StringReader(cb.substring(getNextLineEnd(cursor, cb)+1, cb.length())));
					String headerLine = reader.readLine();
					while (headerLine != null) {
						if (headerLine.indexOf(':')>0) {
							String name = headerLine.substring(0,headerLine.indexOf(':')).trim().toLowerCase();
							String value = headerLine.substring(headerLine.indexOf(':')+1,headerLine.length()).trim();
							if (name.equals("content-length")) {
								contentLengthValue = Integer.parseInt(value);
							} else if (name.endsWith("transfer-encoding")) {
								chunked = value.equals("chunked");
							} else if (name.endsWith("connection")) {
								connectionClosed = value.equals("close");
							}
						}
						headerLine = reader.readLine();
					}	
					
				} else {
					malformed = true;
				}
			} catch (ParseException|NumberFormatException|IOException e) {
				malformed = true;
			} 
		} else {
			malformed = true;
		}
		
		if (!malformed) {
			body = new byte[len-headBoundary-2];
			if (body.length > 0) {
				System.arraycopy(data, headBoundary+2, body, 0, body.length);
			}
		}
		
	}
	
	private int getNextLineEnd(ParserCursor cursor, CharArrayBuffer cb) {
		if (cursor.atEnd()) {
			return -1;
		} else {
			return cb.indexOf('\n',cursor.getLowerBound(),cursor.getUpperBound());
		}
	}

	public boolean isMalformed() {
		return malformed;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public byte[] getBody() {
		return body;
	}
	
	public int getContentLength() {
		if ((responseCode >=100 && responseCode<=199) || responseCode == 204 || responseCode == 304) {
			return 0;
		} else if (contentLengthValue >=0) {
			return contentLengthValue;
		} else {
			return -1;
		}
	}

	public boolean isChunked() {
		return chunked;
	}

	public boolean isConnectionClosed() {
		return connectionClosed;
	}
	
	

}
