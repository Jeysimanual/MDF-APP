package com.capstone.mdfeventmanagementsystem.Models;

/**
 * Model class for storing metadata about feedback submissions
 */
public class FeedbackMetadata {
    private String eventId;
    private String dateSubmitted;
    private long timestamp;
    private String studentName;

    /**
     * Constructor for FeedbackMetadata
     *
     * @param eventId The ID of the event being evaluated
     * @param dateSubmitted The date and time when feedback was submitted (formatted string)
     * @param timestamp The timestamp when feedback was submitted (milliseconds)
     * @param studentName The name of the student submitting feedback
     */
    public FeedbackMetadata(String eventId, String dateSubmitted, long timestamp, String studentName) {
        this.eventId = eventId;
        this.dateSubmitted = dateSubmitted;
        this.timestamp = timestamp;
        this.studentName = studentName;
    }

    /**
     * Convert metadata to a Map for Firebase storage
     *
     * @return Map containing metadata fields
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("eventId", eventId);
        map.put("dateSubmitted", dateSubmitted);
        map.put("timestamp", timestamp);
        map.put("studentName", studentName);
        return map;
    }

    // Getters

    public String getEventId() {
        return eventId;
    }

    public String getDateSubmitted() {
        return dateSubmitted;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStudentName() {
        return studentName;
    }
}