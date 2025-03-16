package com.capstone.mdfeventmanagementsystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Event {
    private String eventUID; // Added event UID
    private String eventName, eventDescription, venue, dateCreated, startDate, endDate, startTime, endTime;
    private String eventSpan, ticketType, ticketActivationTime, eventPhotoUrl;

    // Updated date format to match "YYYY-MM-DD"
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.US);

    public Event() {
        // Default constructor for Firebase
    }

    public Event(String eventUID, String eventName, String eventDescription, String venue, String dateCreated,
                 String startDate, String endDate, String startTime, String endTime,
                 String eventSpan, String ticketType, String ticketActivationTime, String eventPhotoUrl) {
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
        this.ticketActivationTime = ticketActivationTime;
        this.eventPhotoUrl = eventPhotoUrl;
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

    public String getTicketActivationTime() {
        return ticketActivationTime;
    }

    public String getEventPhotoUrl() {
        return eventPhotoUrl;
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
