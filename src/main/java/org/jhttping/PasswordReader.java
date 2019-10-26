package org.jhttping;

import java.util.Scanner;

public class PasswordReader {
	
	private Scanner scan = new Scanner(System.in);
	
	public String readPassword() {
		System.out.print("Password: ");
		String result = scan.nextLine();
		return result;
	}

}
