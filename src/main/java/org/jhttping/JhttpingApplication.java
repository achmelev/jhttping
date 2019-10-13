package org.jhttping;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JhttpingApplication implements CommandLineRunner {
	
	private static Logger log = LoggerFactory.getLogger(JhttpingApplication.class);
	private Socket socket = null;
	
	public static void main(String[] args) {
		SpringApplication.run(JhttpingApplication.class, args);
	}
	
	@Override
    public void run(String... args) {
        if (args.length == 0) {
        	log.error("Missing url argument");
        } else {
        	doPings(args[0]);
        }
    }
	
	private boolean isValidURI(String uriStr) {
	    try {
	      URI uri = new URI(uriStr);
	      return true;
	    }
	    catch (URISyntaxException e) {
	        return false;
	    }
	}
	
	private String createHttpRequestHead(String host, String uri,String method, List<Header> headers) {
		StringBuilder builder = new StringBuilder();
		builder.append(method+" "+uri+" HTTP/1.1\r\n");
		for (Header hd: headers) {
			appendHeader(hd, builder);
		}
		builder.append("\r\n");
		
		return builder.toString();
	}
	
	private void appendHeader(Header hd, StringBuilder builder) {
		builder.append(hd.getName()+": "+hd.getValue()+"\r\n");
	}
	
	private void doPings(String urlStr) {
		try {
			URL url = new URL(urlStr);
			String protocol = url.getProtocol();
			String host = url.getHost();
			int port = url.getPort();
			String path = url.getPath();
			String query = url.getQuery();
			if (path.length() == 0) {
				path = "/";
			}
			if (protocol.equals("http")) {
				InetAddress inetAdress = InetAddress.getByName(host);
				List<Header> headers = new ArrayList<Header>();
				headers.add(new Header("Host",host));
				headers.add(new Header("Connection","keep-alive"));
				String pathAndQuery =  path+((query == null)?"":"?"+query); 
				String requestHead = createHttpRequestHead(host,pathAndQuery, "GET", headers);
				if (port <= 0) {
					port = 80;
				}
				log.info("PING "+inetAdress .getHostAddress()+":"+port+"("+pathAndQuery+")");
				while (true) {
					ping(inetAdress,port,requestHead);
					Thread.currentThread().sleep(3000);
				}
			} else {
				log.error("Unsupported url protocol: "+protocol);
			}
			
			
		} catch (MalformedURLException e) {
			log.error("Malformed url: "+urlStr);
		} catch (Throwable e) {
			log.error("Failure",e);
		}
	}
	
	
	private void ping(InetAddress address, int port, String requestHead) {
		try {
			
			int headReadLimit = 4096;
			int bufSize = 1024; 
			
			int headerBytes = 0;
			int bodyBytes = 0;
			int totalBytes = 0;
			
			byte [] buf = new byte[bufSize]; 
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int headEndPos = -1;
			
			long t1 = System.currentTimeMillis();
			if (socket == null || socket.isClosed()) {
				socket = new Socket(address, port);
			}
			
			long connectTime = System.currentTimeMillis()-t1;
			
			long t2 = System.currentTimeMillis();
			byte [] requestHeadBytes = requestHead.getBytes(Charset.forName("ISO-8859-1"));
			socket.getOutputStream().write(requestHeadBytes);
			long writeTime = System.currentTimeMillis()-t2;
			
			BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
			input.mark(headReadLimit);
			
			long t3 = System.currentTimeMillis();
			int read = readNextPart(out, buf, input);
			long waitTime = System.currentTimeMillis()-t3;
			
			if (read > 0) {
				headEndPos = findEmptyLine(out.toByteArray());
			}
			while (headEndPos<0) {
				read = readNextPart(out, buf, input);
				if (read > 0) {
					headEndPos = findEmptyLine(out.toByteArray());
				}
			}
			
			//reset
			byte [] data = out.toByteArray();
			RequestHead head = new RequestHead(data, data.length, headEndPos+2);
			
			long t4 = System.currentTimeMillis();
			if (head.isChunked()) {
				bodyBytes = (int)readChunkedBody(input, headEndPos, bufSize);
			} else {
				bodyBytes= readBody(head, input, buf);
			}
			headerBytes = headEndPos+4;
			totalBytes = headerBytes+bodyBytes;
			
			long readTime = System.currentTimeMillis()-t4;
				
			//socket.close();
			log.info("connected to "+address.getHostName()+":"+port+" connect time = "+connectTime+", writeTime = "+writeTime+", waitTime = "+waitTime+", readTime = "+readTime+", totalTime = "+(connectTime+writeTime+waitTime+readTime)+", header size = "+headerBytes+", body size = "+bodyBytes+", total size = "+totalBytes+", response code = "+head.getResponseCode());
			
		} catch (IOException e) {
			throw new RuntimeException("Ping error",e);
		}
	}
	
	private int readBody(RequestHead head, InputStream input, byte [] buf) throws IOException {
		if (head.getContentLength() == 0) {
			return 0;
		}
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		if (head.getBody() != null) {
			body.write(head.getBody(), 0, head.getBody().length);
		}
		
		int read;
		if (head.getContentLength() < 0) {
		   	read = readNextPart(body, buf, input);
		   	while (read>=0) {
		   		read = readNextPart(body, buf, input);
		   	}
		} else {
			while (body.size() < head.getContentLength()) {
				read = readNextPart(body, buf, input);
			}	
		}	
		
		return body.size();
	}
	
	private long readChunkedBody(InputStream input, int headEndPos, int bufSize) throws IOException {
		input.reset();
		byte[] rereadBuf = new byte[headEndPos+4];
		int read = input.read(rereadBuf);
		if (read != headEndPos+4) {
			throw new IOException("reread failed!");
		}
		HttpTransportMetricsImpl metric = new HttpTransportMetricsImpl();
		SessionInputBufferImpl sessionInputBuffer = new SessionInputBufferImpl(metric, bufSize);
		sessionInputBuffer.bind(input);
		ChunkedInputStream cinput = new ChunkedInputStream(sessionInputBuffer);
		cinput.close();
		return metric.getBytesTransferred();
	}
	
	private int readNextPart(ByteArrayOutputStream out, byte[] buf, InputStream input) throws IOException{
		int read = input.read(buf);
		if (read > 0) {
			out.write(buf, 0, read);
		}	
		return read;
	}
	
	private int findEmptyLine(byte [] buf) {
		String str = new String(buf, Charset.forName("ISO-8859-1"));
		int index = str.indexOf("\r\n\r\n");
		return index;
	}
	
	private void dump(byte [] bytes, int length) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(bytes,0,length);
		log.info(new String(out.toByteArray()));
	}
	
	

}
