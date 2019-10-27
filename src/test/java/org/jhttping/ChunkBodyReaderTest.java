package org.jhttping;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jhttping.chunked.ChunkedBodyReader;
import org.junit.Assert;
import org.junit.Test;

public class ChunkBodyReaderTest {
	
	@Test
	public void test1() {
		StringBuilder builder = new StringBuilder();
		builder.append("4\r\n");
		builder.append("Wiki\r\n");
		builder.append("5\r\n");
		builder.append("pedia\r\n");
		builder.append("E\r\n");
		builder.append("in\r\n");
		builder.append("\r\n");
		builder.append(" chunks.\r\n");
		builder.append("0\r\n");
		builder.append("\r\n");
		
		ByteArrayInputStream source = new ByteArrayInputStream(builder.toString().getBytes());
		ChunkedBodyReader reader = new ChunkedBodyReader(source, 5, null);
		boolean noException = true;
		try {
			reader.readChunkedBody();
		} catch (IOException e) {
			e.printStackTrace();
			noException = false;
		} finally {
			Assert.assertTrue(noException);
			Assert.assertEquals(3, reader.getChunkCounter());
			Assert.assertEquals(23, reader.getBodySize());
		}
		
	}
	
	@Test
	public void test2() {
		StringBuilder builder = new StringBuilder();
		builder.append("4\r\n");
		builder.append("Wiki\r\n");
		builder.append("5\r\n");
		builder.append("pedia\r\n");
		builder.append("E\r\n");
		builder.append("in\r\n");
		builder.append("\r\n");
		builder.append(" chunks.\r\n");
		builder.append("0\r\n");
		builder.append("\r\n");
		
		ByteArrayInputStream source = new ByteArrayInputStream(builder.toString().getBytes());
		ChunkedBodyReader reader = new ChunkedBodyReader(source, 100, null);
		boolean noException = true;
		try {
			reader.readChunkedBody();
		} catch (IOException e) {
			e.printStackTrace();
			noException = false;
		} finally {
			Assert.assertTrue(noException);
			Assert.assertEquals(3, reader.getChunkCounter());
			Assert.assertEquals(23, reader.getBodySize());
		}
		
	}
	
	@Test
	public void test3() {
		StringBuilder builder = new StringBuilder();
		builder.append("Wiki\r\n");
		builder.append("5\r\n");
		builder.append("pedia\r\n");
		builder.append("E\r\n");
		builder.append("in\r\n");
		builder.append("\r\n");
		builder.append(" chunks.\r\n");
		builder.append("0\r\n");
		builder.append("\r\n");
		
		ByteArrayInputStream source = new ByteArrayInputStream(builder.toString().getBytes());
		ChunkedBodyReader reader = new ChunkedBodyReader(source, 5, "4\r\n".getBytes());
		boolean noException = true;
		try {
			reader.readChunkedBody();
		} catch (IOException e) {
			e.printStackTrace();
			noException = false;
		} finally {
			Assert.assertTrue(noException);
			Assert.assertEquals(3, reader.getChunkCounter());
			Assert.assertEquals(23, reader.getBodySize());
		}
		
	}

}
