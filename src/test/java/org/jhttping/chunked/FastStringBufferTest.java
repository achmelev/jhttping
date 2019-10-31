package org.jhttping.chunked;

import junit.framework.Assert;

import org.junit.Test;

public class FastStringBufferTest {
	
	@Test
	public void test1() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		Assert.assertEquals(buf.toFastString(), new FastString("1234567890ABC"));
		buf.contract(1);
		Assert.assertEquals(buf.toFastString(), new FastString("234567890ABC"));
		Assert.assertEquals("234567890ABC".length(), buf.size());
		
		buf.contract(4);
		Assert.assertEquals(buf.toFastString(), new FastString("67890ABC"));
		Assert.assertEquals("67890ABC".length(), buf.size());
		
		buf.contract(6);
		Assert.assertEquals(buf.toFastString(), new FastString("BC"));
		Assert.assertEquals("BC".length(), buf.size());
	}
	
	@Test
	public void test2() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		Assert.assertEquals(buf.toFastString(), new FastString("1234567890ABC"));
		buf.contract(1);
		Assert.assertEquals(buf.toFastString(), new FastString("234567890ABC"));
		Assert.assertEquals("234567890ABC".length(), buf.size());
		
		buf.contract(12);
		Assert.assertEquals(0, buf.size());
		
	}
	
	@Test
	public void test3() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		buf.contract(13);
		Assert.assertEquals(0, buf.size());
		
	}
	
	@Test
	public void charAtTest() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		Assert.assertEquals(0x31, buf.charAt(0));
		Assert.assertEquals(0x32, buf.charAt(1));
		Assert.assertEquals(0x35, buf.charAt(4));
		Assert.assertEquals(0x37, buf.charAt(6));
		Assert.assertEquals(0x30, buf.charAt(9));
		Assert.assertEquals(0x43, buf.charAt(12));
		
		buf.contract(1);
		
		Assert.assertEquals(0x32, buf.charAt(0));
		Assert.assertEquals(0x35, buf.charAt(3));
		Assert.assertEquals(0x37, buf.charAt(5));
		Assert.assertEquals(0x30, buf.charAt(8));
		Assert.assertEquals(0x43, buf.charAt(11));
	}
	
	@Test
	public void testIndexOf() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		Assert.assertEquals(0, buf.indexOf(new FastString("12"), 0));
		Assert.assertEquals(-1, buf.indexOf(new FastString("12"), 1));
		Assert.assertEquals(2, buf.indexOf(new FastString("345"), 1));
		Assert.assertEquals(10, buf.indexOf(new FastString("ABC"), 4));
		
		buf = new FastStringBuffer();
		buf.expand(new FastString("AB3"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		Assert.assertEquals(10, buf.indexOf(new FastString("ABC"), 4));
		
		buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4517"));
		buf.expand(new FastString("34"));
		buf.expand(new FastString("56AB"));
		
		Assert.assertEquals(7, buf.indexOf(new FastString("3456"), 1));
		
		buf = new FastStringBuffer();
		Assert.assertEquals(-1, buf.indexOf(new FastString("3456"), 0));
		
	}
	
	@Test
	public void substringTest() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.expand(new FastString("123"));
		buf.expand(new FastString("4567"));
		buf.expand(new FastString("89"));
		buf.expand(new FastString("0ABC"));
		
		Assert.assertEquals(new FastString("123"), buf.substring(0, 3));
		Assert.assertEquals(new FastString("123456789"), buf.substring(0, 9));
		Assert.assertEquals(new FastString("456789"), buf.substring(3, 9));
		Assert.assertEquals(new FastString("4567890ABC"), buf.substring(3, 13));
		Assert.assertEquals(new FastString("BC"), buf.substring(11, 13));
		
		
	}
	
	

}
