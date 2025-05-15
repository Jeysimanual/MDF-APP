package com.capstone.mdfeventmanagementsystem.Student;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Event {
    private String eventUID; // Added event UID
    private String eventName, eventDescription, venue, dateCreated, startDate, endDate, startTime, endTime;
    private String eventSpan, ticketType, graceTime, eventPhotoUrl, eventType, eventFor;

    // Updated date format to match "YYYY-MM-DD"
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.US);

    public Event() {
        // Default constructor for Firebase
    }

    public Event(String eventUID, String eventName, String eventDescription, String venue, String dateCreated,
                 String startDate, String endDate, String startTime, String endTime,
                 String eventSpan, String ticketType, String graceTime, String eventPhotoUrl,
                 String eventType, String eventFor) {
        this.eventUID = eventUID;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.venue = venue;
        this.dateCreated = dateCreated;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventSpan = eventSpan;
        this.ticketType = ticketType;
        this.graceTime = graceTime;
        this.eventPhotoUrl = eventPhotoUrl;
        this.eventType = eventType;
        this.eventFor = eventFor;
    }

    public String getEventUID() {
        return eventUID;
    }

    public void setEventUID(String eventUID) {
        this.eventUID = eventUID;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public String getVenue() {
        return venue;
    }

    public String getDateCreated() {
        return dateCreated;
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

    public String getEventSpan() {
        return eventSpan;
    }

    public String getTicketType() {
        return ticketType;
    }

    public String getGraceTime() {
        return graceTime;
    }

    public String getEventPhotoUrl() {
        return eventPhotoUrl;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventFor() {
        return eventFor;
    }

    public void setEventFor(String eventFor) {
        this.eventFor = eventFor;
    }

    // Extracts day of the month
    public String getDayOfMonth() {
        try {
            LocalDate date = LocalDate.parse(startDate, INPUT_FORMATTER);
            return String.valueOf(date.getDayOfMonth());
        } catch (Exception e) {
            e.printStackTrace();
            return "--";
        }
    }

    // Extracts month name
    public String getMonthShort() {
        try {
            LocalDate date = LocalDate.parse(startDate, INPUT_FORMATTER);
            return date.format(MONTH_FORMATTER);
        } catch (Exception e) {
            e.printStackTrace();
            return "---";
        }
    }
}
