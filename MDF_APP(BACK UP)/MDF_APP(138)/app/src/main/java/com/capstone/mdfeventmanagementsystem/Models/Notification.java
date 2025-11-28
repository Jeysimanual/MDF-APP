package com.capstone.mdfeventmanagementsystem.Models;

public class Notification {
    private String type;
    private String title;
    private String body;
    private String eventId;
    private String description;
    private String startDateTime;
    private long timestamp;
    private boolean isRead = false;

    // Default constructor for Firebase
    public Notification() {}

    public Notification(String type, String title, String body, String eventId, String description, String startDateTime, long timestamp) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.eventId = eventId;
        this.description = description;
        this.startDateTime = startDateTime;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}