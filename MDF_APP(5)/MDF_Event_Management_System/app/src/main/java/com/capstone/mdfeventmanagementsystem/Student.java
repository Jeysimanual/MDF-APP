package com.capstone.mdfeventmanagementsystem;

public class Student {
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String yearLevel;
    private String section; // Re-added section field
    private String role;
    private boolean isVerified;
    private String otp; // Store hashed OTP instead of plain integer

    // Constructor with all parameters
    public Student(String firstName, String middleName, String lastName, String email, String yearLevel, String section, String role, boolean isVerified, String otp) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.yearLevel = yearLevel;
        this.section = section; // Re-added section field
        this.role = role;
        this.isVerified = isVerified;
        this.otp = otp; // Store hashed OTP
    }

    // Empty Constructor for Firebase
    public Student() {
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public String getSection() { // Getter for section
        return section;
    }

    public String getRole() {
        return role;
    }

    public boolean getIsVerified() {
        return isVerified;
    }

    public String getOtp() { // Return hashed OTP
        return otp;
    }

    // Setters
    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public void setOtp(String otp) { // Store hashed OTP
        this.otp = otp;
    }

    public void setSection(String section) { // Setter for section
        this.section = section;
    }
}
