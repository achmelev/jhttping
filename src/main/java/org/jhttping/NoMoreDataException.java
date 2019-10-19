package org.jhttping;

import java.io.IOException;

public class NoMoreDataException extends IOException {

	public NoMoreDataException() {
		super("connection closed by remote host");
		// TODO Auto-generated constructor stub
	}
}
