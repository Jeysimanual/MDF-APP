package com.capstone.mdfeventmanagementsystem.Student;

public class EventTicket {
    private String eventName;
    private String eventType;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String graceTime;
    private String eventSpan;
    private String venue;
    private String eventDescription;
    private String qrCodeUrl;
    private String ticketID;

    // Default constructor required for Firebase
    public EventTicket() {
    }

    // Constructor with parameters
    public EventTicket(String eventName, String eventType, String startDate, String endDate,
                       String startTime, String endTime, String graceTime, String eventSpan,
                       String venue, String eventDescription, String qrCodeUrl, String ticketID) {
        this.eventName = eventName;
        this.eventType = eventType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.graceTime = graceTime;
        this.eventSpan = eventSpan;
        this.venue = venue;
        this.eventDescription = eventDescription;
        this.qrCodeUrl = qrCodeUrl;
        this.ticketID = ticketID;
    }

    // Getters
    public String getEventName() {
        return eventName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getGraceTime() {
        return graceTime;
    }

    public String getEventSpan() {
        return eventSpan;
    }

    public String getVenue() {
        return venue;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public String getTicketID() {
        return ticketID;
    }
}
