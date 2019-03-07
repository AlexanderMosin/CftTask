package ru.cfttask.cfttask;

public class Response {
	protected String status;
	protected String message;
	protected String code;
	
	public void printResponse() {
		System.out.println("Значение поля status: " + this.status);
		System.out.println("Значение поля message: " + this.message);
		System.out.println("Значение поля code: " + this.code);
	}
	
}


