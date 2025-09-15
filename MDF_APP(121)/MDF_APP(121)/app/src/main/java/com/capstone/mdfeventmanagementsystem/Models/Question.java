package com.capstone.mdfeventmanagementsystem.Models;

/**
 * Model class for an evaluation question
 */
public class Question {
    private String id;
    private String text;
    private String type; // "default", "likert", or "comment"
    private boolean required;

    /**
     * Constructor for Question
     *
     * @param id The unique identifier for the question
     * @param text The question text
     * @param type The question type ("default", "likert", or "comment")
     */
    public Question(String id, String text, String type) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.required = false;
    }

    /**
     * Get the question ID
     *
     * @return The question ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the question text
     *
     * @return The question text
     */
    public String getText() {
        return text;
    }

    /**
     * Get the question type
     *
     * @return The question type
     */
    public String getType() {
        return type;
    }

    /**
     * Check if the question is required
     *
     * @return True if the question is required, false otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Set whether the question is required
     *
     * @param required True if the question is required, false otherwise
     */
    public void setRequired(boolean required) {
        this.required = required;
    }
}