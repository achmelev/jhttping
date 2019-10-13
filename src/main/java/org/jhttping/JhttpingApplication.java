package org.jhttping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

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
			if (protocol.equals("http")) {
				InetAddress inetAdress = InetAddress.getByName(host);
				List<Header> headers = new ArrayList<Header>();
				headers.add(new Header("Host",host));
				headers.add(new Header("Connection","keep-alive"));
				String requestHead = createHttpRequestHead(host, path, "GET", headers);
				if (port <= 0) {
					port = 80;
				}
				log.info("PING "+inetAdress .getHostAddress()+":"+port+"("+path+")");
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
	
	private void dump(byte [] bytes, int length) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(bytes,0,length);
		log.info(new String(out.toByteArray()));
	}
	
	private void ping(InetAddress address, int port, String requestHead) {
		try {
			
			byte [] buf = new byte[1024]; 
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
			
			long t3 = System.currentTimeMillis();
			int read = readNextPart(out, buf, socket);
			long waitTime = System.currentTimeMillis()-t3;
			
			if (read > 0) {
				headEndPos = findEmptyLine(out.toByteArray());
			}
			while (headEndPos<0) {
				read = socket.getInputStream().read(buf);
				read = readNextPart(out, buf, socket);
				if (read > 0) {
					headEndPos = findEmptyLine(out.toByteArray());
				}
			}
			byte [] data = out.toByteArray();
			dump(data, data.length);
			
			RequestHead head = new RequestHead(data, data.length, headEndPos+2);
			if (head.isChunked()) {
				throw new RuntimeException("Chunked responses not supported yet!");
			}
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			if (head.getBody() != null) {
				body.write(head.getBody(), 0, head.getBody().length);
			}
			
			long t4 = System.currentTimeMillis();
			if (head.getContentLength() < 0) {
			   	read = readNextPart(out, buf, socket);
			   	while (read>=0) {
			   		read = readNextPart(out, buf, socket);
			   	}
			} else {
				while (body.size() < head.getContentLength()) {
					readNextPart(body, buf, socket);
				}
			}	
			long readTime = System.currentTimeMillis()-t4;
				
			//socket.close();
			log.info("connected to "+address.getHostName()+":"+port+" connect time = "+connectTime+", writeTime = "+writeTime+", waitTime = "+waitTime+",readTime = "+readTime+",totalTime = "+(connectTime+writeTime+waitTime+readTime)+", response code = "+head.getResponseCode());
			
		} catch (IOException e) {
			throw new RuntimeException("Ping error",e);
		}
	}
	
	private int readNextPart(ByteArrayOutputStream out, byte[] buf, Socket socket) throws IOException{
		int read = socket.getInputStream().read(buf);
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
	
	

}
