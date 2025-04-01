package com.capstone.mdfeventmanagementsystem;

public class Teacher {
    private String contact_number;
    private String email;
    private String firstname;
    private String lastname;
    private String role;
    private String section;
    private boolean verified;
    private String year_level_advisor;

    // Default constructor required for Firebase
    public Teacher() {
        // Required empty constructor
    }

    // Constructor with parameters
    public Teacher(String contact_number, String email, String firstname, String lastname,
                   String role, String section, boolean verified, String year_level_advisor) {
        this.contact_number = contact_number;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.role = role;
        this.section = section;
        this.verified = verified;
        this.year_level_advisor = year_level_advisor;
    }

    // Getters and Setters
    public String getContact_number() {
        return contact_number;
    }

    public void setContact_number(String contact_number) {
        this.contact_number = contact_number;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getYear_level_advisor() {
        return year_level_advisor;
    }

    public void setYear_level_advisor(String year_level_advisor) {
        this.year_level_advisor = year_level_advisor;
    }
}