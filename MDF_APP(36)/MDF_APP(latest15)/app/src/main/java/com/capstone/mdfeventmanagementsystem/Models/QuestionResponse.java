package com.capstone.mdfeventmanagementsystem.Models;

import android.util.Log;

/**
 * Represents a response to a question in an evaluation.
 */
public class QuestionResponse {

    private String questionId;
    private String answer;
    private Question question; // Reference to the Question object
    private static final String RESPONSE_TAG = "responseTest";

    /**
     * Constructor for a question response.
     *
     * @param questionId The unique ID of the question being answered
     * @param answer The user's answer to the question
     */
    public QuestionResponse(String questionId, String answer) {
        this.questionId = questionId;
        this.answer = answer;
        Log.d(RESPONSE_TAG, "Created QuestionResponse: id=" + questionId + ", answer=" + answer);
    }

    /**
     * Gets the ID of the question this response is for.
     *
     * @return The question ID
     */
    public String getQuestionId() {
        return questionId;
    }

    /**
     * Sets the ID of the question this response is for.
     *
     * @param questionId The question ID
     */
    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    /**
     * Gets the user's answer to the question.
     *
     * @return The answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Sets the user's answer to the question.
     *
     * @param answer The answer
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Gets the Question object associated with this response.
     *
     * @return The Question object
     */
    public Question getQuestion() {
        return question;
    }

    /**
     * Sets the Question object associated with this response.
     *
     * @param question The Question object
     */
    public void setQuestion(Question question) {
        this.question = question;
    }

    @Override
    public String toString() {
        return "QuestionResponse{" +
                "questionId='" + questionId + '\'' +
                ", answer='" + answer + '\'' +
                ", question=" + (question != null ? question.toString() : "null") +
                '}';
    }
}