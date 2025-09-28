package com.capstone.mdfeventmanagementsystem.Teacher;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Event {
    private String eventUID; // Added event UID
    private String eventName, eventDescription, venue, dateCreated, startDate, endDate, startTime, endTime;
    private String eventSpan, ticketType, graceTime, eventPhotoUrl, eventType, eventFor, status;

    // Updated date format to match "YYYY-MM-DD"
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.US);

    public Event() {
        // Default constructor for Firebase
    }

    public Event(String eventUID, String eventName, String eventDescription, String venue, String dateCreated,
                 String startDate, String endDate, String startTime, String endTime,
                 String eventSpan, String ticketType, String graceTime, String eventPhotoUrl,
                 String eventType, String eventFor, String status) {
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
        this.status = status;
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

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getEventSpan() {
        return eventSpan;
    }

    public void setEventSpan(String eventSpan) {
        this.eventSpan = eventSpan;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getGraceTime() {
        return graceTime;
    }

    public void setGraceTime(String graceTime) {
        this.graceTime = graceTime;
    }

    public String getEventPhotoUrl() {
        return eventPhotoUrl;
    }

    public void setEventPhotoUrl(String eventPhotoUrl) {
        this.eventPhotoUrl = eventPhotoUrl;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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