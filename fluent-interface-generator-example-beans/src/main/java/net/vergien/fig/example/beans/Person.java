package net.vergien.fig.example.beans;

public class Person {
	private String firstName;
	private String lastName;
	private PhoneNumber[] phoneNumbers = new PhoneNumber[] {};

	public Person(String firstName, String lastName) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Person(String firstName, String lastName, PhoneNumber... phoneNumbers) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumbers = phoneNumbers;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public PhoneNumber[] getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(PhoneNumber... phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

}
