package com.capstone.mdfeventmanagementsystem;

public class EventTicket {
    private String eventName;
    private String eventType;
    private String startDate;
    private String startTime;
    private String venue;
    private String qrCodeUrl;
    private String ticketID;

    // Default constructor required for Firebase
    public EventTicket() {
    }

    // Constructor with parameters
    public EventTicket(String eventName, String eventType, String startDate,
                       String startTime, String venue, String qrCodeUrl, String ticketID) {
        this.eventName = eventName;
        this.eventType = eventType;
        this.startDate = startDate;
        this.startTime = startTime;
        this.venue = venue;
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

    public String getStartTime() {
        return startTime;
    }

    public String getVenue() {
        return venue;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public String getTicketID() {
        return ticketID;
    }
}
