package com.capstone.mdfeventmanagementsystem.Student;

public class Student {
    private String idNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String yearLevel;
    private String section;
    private String role;
    private boolean isVerified;
    private String otp;

    // Empty Constructor for Firebase
    public Student() {
    }

    // Constructor with all parameters
    public Student(String idNumber, String firstName, String middleName, String lastName, String email,
                   String yearLevel, String section, String role, boolean isVerified, String otp) {
        this.idNumber = idNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.yearLevel = yearLevel;
        this.section = section;
        this.role = role;
        this.isVerified = isVerified;
        this.otp = otp;
    }

    // Getters
    public String getIdNumber() {
        return idNumber;
    }

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

    public String getSection() {
        return section;
    }

    public String getRole() {
        return role;
    }

    public String getOtp() {
        return otp;
    }

    // Setters
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setRole(String role) {
        this.role = role;
    }



    public void setOtp(String otp) {
        this.otp = otp;
    }

    // Getter for isVerified
    public boolean isVerified() {
        return isVerified;
    }

    // Setter for isVerified
    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

}
