package com.capstone.mdfeventmanagementsystem.Models;

import java.io.Serializable;

public class Event implements Serializable {
    private String eventId;
    private String eventName;
    private String description;
    private String venue;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String dateCreated;
    private String status;
    private String photoUrl;
    private String eventType;
    private String eventFor;
    private String eventSpan;
    private String graceTime;
    private String rejectionReason;
    private String userId;
    private String targetParticipant; // Added targetParticipant field as String

    // Empty constructor needed for Firebase
    public Event() {
    }

    // Constructor with parameters
    public Event(String eventId, String eventName, String description, String venue,
                 String startDate, String endDate, String startTime, String endTime,
                 String dateCreated, String status, String photoUrl, String eventType,
                 String eventFor, String eventSpan, String graceTime, String targetParticipant) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.description = description;
        this.venue = venue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.dateCreated = dateCreated;
        this.status = status;
        this.photoUrl = photoUrl;
        this.eventType = eventType;
        this.eventFor = eventFor;
        this.eventSpan = eventSpan;
        this.graceTime = graceTime;
        this.targetParticipant = targetParticipant; // Initialize targetParticipant
    }

    // Getters and Setters with type conversion handling
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    // Handle possible Long value for startDate
    public void setStartDate(Object startDate) {
        if (startDate instanceof Long) {
            this.startDate = String.valueOf(startDate);
        } else if (startDate instanceof String) {
            this.startDate = (String) startDate;
        }
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    // Handle possible Long value for endDate
    public void setEndDate(Object endDate) {
        if (endDate instanceof Long) {
            this.endDate = String.valueOf(endDate);
        } else if (endDate instanceof String) {
            this.endDate = (String) endDate;
        }
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    // Handle possible Long value for startTime
    public void setStartTime(Object startTime) {
        if (startTime instanceof Long) {
            this.startTime = String.valueOf(startTime);
        } else if (startTime instanceof String) {
            this.startTime = (String) startTime;
        }
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    // Handle possible Long value for endTime
    public void setEndTime(Object endTime) {
        if (endTime instanceof Long) {
            this.endTime = String.valueOf(endTime);
        } else if (endTime instanceof String) {
            this.endTime = (String) endTime;
        }
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    // Handle possible Long value for dateCreated
    public void setDateCreated(Object dateCreated) {
        if (dateCreated instanceof Long) {
            this.dateCreated = String.valueOf(dateCreated);
        } else if (dateCreated instanceof String) {
            this.dateCreated = (String) dateCreated;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
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

    public String getEventSpan() {
        return eventSpan;
    }

    public void setEventSpan(String eventSpan) {
        this.eventSpan = eventSpan;
    }

    // Handle possible Long value for eventSpan
    public void setEventSpan(Object eventSpan) {
        if (eventSpan instanceof Long) {
            this.eventSpan = String.valueOf(eventSpan);
        } else if (eventSpan instanceof String) {
            this.eventSpan = (String) eventSpan;
        }
    }

    public String getGraceTime() {
        return graceTime;
    }

    public void setGraceTime(String graceTime) {
        this.graceTime = graceTime;
    }

    // Handle possible Long value for graceTime
    public void setGraceTime(Object graceTime) {
        if (graceTime instanceof Long) {
            this.graceTime = String.valueOf(graceTime);
        } else if (graceTime instanceof String) {
            this.graceTime = (String) graceTime;
        }
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getter and Setter for targetParticipant
    public String getTargetParticipant() {
        return targetParticipant;
    }

    public void setTargetParticipant(String targetParticipant) {
        this.targetParticipant = targetParticipant;
    }

    // Handle possible Integer or String value for targetParticipant
    public void setTargetParticipant(Object targetParticipant) {
        if (targetParticipant instanceof Integer) {
            this.targetParticipant = String.valueOf(targetParticipant);
        } else if (targetParticipant instanceof String) {
            this.targetParticipant = (String) targetParticipant;
        }
    }
}