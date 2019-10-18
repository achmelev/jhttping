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

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JhttpingApplication implements CommandLineRunner {
	
	private static Logger log = LoggerFactory.getLogger("jhttping");
	private Socket socket = null;
	
	@Value("${interval}")
	private int pingInterval;
	@Value("${bufsize}")
	private int bufSize;
	@Value("${headreadlimit}")
	private int headReadLimit;
	@Value("${count}")
	private int maxCount;
	@Value("${agent}")
	private String agent;
	@Value("${version}")
	private String version;
	@Value("${method}")
	private String method;
	
	
	private static Options options;
	
	@Value("${url}")
	private String urlStr;
		
	public static void main(String[] args) {
		options = createOpts();
		if (args.length == 0) {
			usage();
		} else {
			CommandLineParser parser = new DefaultParser();
			// parse the command line arguments
		    try {
				CommandLine line = parser.parse( options, args );
				SpringApplication.run(JhttpingApplication.class, convertToSpringArgs(line));
			} catch (Throwable e) {
				log.error("Unexpected exception",e);
				System.exit(1);
			} finally {
				System.exit(0);
			}
		}
	}
	
	private static String [] convertToSpringArgs(CommandLine line) {
		ArrayList<String> args = new ArrayList<String>();
		for (Option o: options.getOptions()) {
			if (line.hasOption(o.getOpt())) {
				if (!o.hasArg()) {
					args.add("--"+o.getLongOpt()+"=true");
				} else {
					args.add("--"+o.getLongOpt()+"="+line.getOptionValue(o.getOpt()));
				}
			}
		}
		return  args.toArray(new String[args.size()]);
		
	}
	
	private static Options createOpts() {
		Options opts = new Options();
		opts.addOption("g", "url", true,"This selects the url to probe. E.g.: http://localhost/");
		opts.addOption("c", "count", true,"How many probes to send before exiting.");
		opts.addOption("i", "interval", true,"How many seconds to sleep between every probe sent.");
		opts.addOption("m", "method", true,"HTTP method to use. Allowed values: get, post, head. Default is get");
		opts.addOption("I", "agent", true,"User-Agent to send to the server.(instead of 'JHTTPing <version>')");
		opts.addOption("b", "bufsize", true,"Read buffer size to use. (in bytes, default is 8192)");
		return opts;
	}
	
	private static void usage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "java -jar jhttping-<version> [options]", options );
		System.exit(0);
	}
	
	
	@Override
    public void run(String... args) {
		
        if (log.isDebugEnabled()) {
        	log.debug("Config values interval="+pingInterval+", bufsize="+bufSize+", headreadlimit="+headReadLimit+", count = "+maxCount+", version="+version+", method="+method);
        }
        doPings();
        
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
	
	private List<Header> createHeaders(URL url) {
		String host = url.getHost();
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("Host",host));
		headers.add(new Header("Connection","keep-alive"));
		if (StringUtils.isEmpty(agent)) {
			headers.add(new Header("User-Agent","JHTTPing "+version));
		} else {
			headers.add(new Header("User-Agent",agent));
		}
		return headers;
	}
	
	private void doPings() {
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
			if (protocol.equals("http") || protocol.equals("https")) {
				method = method.trim().toUpperCase();
				if (!(method.equals("GET") || method.equals("POST") || method.equals("HEAD"))) {
					throw new IllegalArgumentException("Method "+method+" not allowed");
				}
				InetAddress inetAdress = InetAddress.getByName(host);
				String pathAndQuery =  path+((query == null)?"":"?"+query); 
				String requestHead = createHttpRequestHead(host,pathAndQuery, method, createHeaders(url));
				if (port <= 0) {
					if (protocol.equals("http")) {
						port = 80;
					} else {
						port = 443;
					}
				} 
				log.info("PING "+inetAdress.getHostAddress()+":"+port+"("+pathAndQuery+")");
				int counter = 0;
				while (maxCount<=0 || (counter<maxCount)) {
					ping(host, inetAdress,port,requestHead, protocol.equals("https"));
					Thread.currentThread().sleep(pingInterval*1000);
					counter++;
				}
			} else {
				log.error("Unsupported url protocol: "+protocol);
			}
			
			
		} catch (MalformedURLException e) {
			log.error("Malformed url: "+urlStr);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
		} catch (Throwable e) {
			log.error("Failure",e);
		}
	}
	
	
	private void ping(String host, InetAddress address, int port, String requestHead, boolean ssl) {
		try {
			
			int headerBytes = 0;
			int bodyBytes = 0;
			int totalBytes = 0;
			
			byte [] buf = new byte[bufSize]; 
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int headEndPos = -1;
			
			long t1 = System.currentTimeMillis();
			if (socket == null || socket.isClosed() ) {
				connect(host, address, port, ssl);
			}
			
			long connectTime = System.currentTimeMillis()-t1;
			
			long t2 = System.currentTimeMillis();
			byte [] requestHeadBytes = requestHead.getBytes(Charset.forName("ISO-8859-1"));
			if (log.isDebugEnabled()) {
				dump(requestHeadBytes, requestHeadBytes.length);
			}
			socket.getOutputStream().write(requestHeadBytes);
			long writeTime = System.currentTimeMillis()-t2;
			
			BufferedInputStream input = new BufferedInputStream(socket.getInputStream(), bufSize);
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
			log.info("connected to "+address.getHostName()+":"+port+" connect time = "+connectTime+", write time = "+writeTime+", wait time = "+waitTime+", read time = "+readTime+", total time = "+(connectTime+writeTime+waitTime+readTime)+", header size = "+headerBytes+", body size = "+bodyBytes+", total size = "+totalBytes+", status code = "+head.getResponseCode());
			
		} catch (IOException e) {
			log.error(e.getMessage());
			socket = null;
		}
	}
	
	private void connect(String hostName, InetAddress address, int port, boolean ssl) throws IOException {
		if (!ssl) {
			socket = new Socket(address, port);
		} else {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslSocket = (SSLSocket)sslsocketfactory.createSocket(address, port);
			SSLParameters params = sslSocket.getSSLParameters();
			List sniHostNames = new ArrayList(1);
			sniHostNames.add(new SNIHostName(hostName));
			params.setServerNames(sniHostNames);
			sslSocket.setSSLParameters(params);
			sslSocket.startHandshake();
			socket = sslSocket;
			
		}
	}
	
	private int readBody(RequestHead head, InputStream input, byte [] buf) throws IOException {
		if (head.getContentLength() == 0 || method.equals("HEAD")) {
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
