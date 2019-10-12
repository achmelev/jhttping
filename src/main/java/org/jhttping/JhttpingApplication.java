package org.jhttping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
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
	
	private String createHttpRequest(String host, String uri,String method, List<Header> headers) {
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
				String request = createHttpRequest(host, path, "GET", headers);
				if (port <= 0) {
					port = 80;
				}
				log.info("PING "+inetAdress .getHostAddress()+":"+port+"("+path+")");
				while (true) {
					ping(inetAdress,port,request);
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
	
	private void ping(InetAddress address, int port, String request) {
		try {
			
			byte [] buf = new byte[1024]; 
			long t1 = System.currentTimeMillis();
			Socket socket = new Socket(address, port);
			long connectTime = System.currentTimeMillis()-t1;
			long t2 = System.currentTimeMillis();
			dump(request.getBytes(), request.getBytes().length);
			socket.getOutputStream().write(request.getBytes());
			long writeTime = System.currentTimeMillis()-t2;
			long t3 = System.currentTimeMillis();
			int read = socket.getInputStream().read(buf);
			long waitTime = System.currentTimeMillis()-t3;
			socket.close();
			log.info("connected to "+address.getHostName()+":"+port+" connect time = "+connectTime+", writeTime = "+writeTime+", waitTime = "+waitTime+", totalTime = "+(connectTime+writeTime+waitTime));
			
		} catch (IOException e) {
			throw new RuntimeException("Ping error",e);
		}
	}
	
	

}
