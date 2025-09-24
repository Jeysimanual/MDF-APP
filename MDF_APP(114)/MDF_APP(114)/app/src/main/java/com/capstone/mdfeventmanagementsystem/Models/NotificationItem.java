package com.capstone.mdfeventmanagementsystem.Models;

public class NotificationItem {
    private String eventName;
    private String eventDescription;
    private String eventPhotoUrl;
    private String dateCreated;
    private String venue;
    private String time;
    private boolean isRead;
    private String eventId;
    private long timestamp;

    public NotificationItem(String eventName, String eventDescription, String eventPhotoUrl, String dateCreated, String venue, String time, boolean isRead, String eventId, long timestamp) {
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventPhotoUrl = eventPhotoUrl;
        this.dateCreated = dateCreated;
        this.venue = venue;
        this.time = time;
        this.isRead = isRead;
        this.eventId = eventId;
        this.timestamp = timestamp;
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

    public String getEventPhotoUrl() {
        return eventPhotoUrl;
    }

    public void setEventPhotoUrl(String eventPhotoUrl) {
        this.eventPhotoUrl = eventPhotoUrl;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}