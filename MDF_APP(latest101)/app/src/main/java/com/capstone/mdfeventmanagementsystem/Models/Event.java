package com.capstone.mdfeventmanagementsystem.Models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class Event implements Serializable {

    @Exclude
    private String eventId;
    private String eventName;
    private String venue;
    private String startDate;
    private String startTime;
    private String endDate;
    private String endTime;
    private String dateCreated;
    private String status; // "pending", "approved", "rejected"
    private String createdBy; // User ID of the creator

    // Empty constructor required for Firebase
    public Event() {
    }

    public Event(String eventName, String venue, String startDate, String startTime,
                 String endDate, String endTime, String dateCreated, String status, String createdBy) {
        this.eventName = eventName;
        this.venue = venue;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.dateCreated = dateCreated;
        this.status = status;
        this.createdBy = createdBy;
    }

    @Exclude
    public String getEventId() {
        return eventId;
    }

    @Exclude
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}