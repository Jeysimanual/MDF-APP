package com.capstone.mdfeventmanagementsystem.Adapters;

public class Participant {
    private String id;
    private String name;
    private String section;
    private String timeIn;
    private String timeOut;
    private String status;
    private String timeIn24; // Original 24-hour format for calculations
    private String timeOut24; // Original 24-hour format for calculations
    private String ticketRef;  // Added this field properly

    public Participant() {
        // Empty constructor needed for Firestore
    }

    public Participant(String id, String name, String section, String timeIn, String timeOut, String status) {
        this.id = id;
        this.name = name;
        this.section = section;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.status = status;
        this.ticketRef = "";  // Initialize with empty string
    }

    // Constructor that includes ticketRef
    public Participant(String id, String name, String section, String timeIn, String timeOut, String status, String ticketRef) {
        this.id = id;
        this.name = name;
        this.section = section;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.status = status;
        this.ticketRef = ticketRef;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(String timeIn) {
        this.timeIn = timeIn;
    }

    public String getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(String timeOut) {
        this.timeOut = timeOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTicketRef(String ticketRef) {
        this.ticketRef = ticketRef;
    }

    public String getTicketRef() {
        return ticketRef;
    }

    public String getTimeIn24() {
        return timeIn24;
    }

    public void setTimeIn24(String timeIn24) {
        this.timeIn24 = timeIn24;
    }

    public String getTimeOut24() {
        return timeOut24;
    }

    public void setTimeOut24(String timeOut24) {
        this.timeOut24 = timeOut24;
    }
}