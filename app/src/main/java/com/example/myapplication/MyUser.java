package com.example.myapplication;


public class MyUser {
    private String fName;
    private String lName;
    private String email;  // Add email field

    // Default constructor (required for Firestore deserialization)
    public MyUser() {
        // Empty constructor required for Firestore
    }

    // Constructor with email, first name, and last name
    public MyUser(String email, String firstName, String lastName) {
        this.email = email;
        this.fName = firstName;
        this.lName = lastName;
    }

    // Getter and Setter methods for firstName, lastName, and email
    public String getFirstName() {
        return fName;
    }

    public void setFirstName(String firstName) {
        this.fName = firstName;
    }

    public String getLastName() {
        return lName;
    }

    public void setLastName(String lastName) {
        this.lName = lastName;
    }

    public String getEmail() {  // Getter for email
        return email;
    }

    public void setEmail(String email) {  // Setter for email
        this.email = email;
    }

    // Override toString method for better readability
    @Override
    public String toString() {
        return "MyUser{" +
                "fName='" + fName + '\'' +
                ", lName='" + lName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
