package com.capstone.mdfeventmanagementsystem.Student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.QuestionAdapter;
import com.capstone.mdfeventmanagementsystem.Models.Question;
import com.capstone.mdfeventmanagementsystem.Models.QuestionResponse;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Models.FeedbackMetadata;
import com.capstone.mdfeventmanagementsystem.Models.Question;
import com.capstone.mdfeventmanagementsystem.Models.QuestionResponse;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentEvaluation extends AppCompatActivity {

    private String eventId;
    private RecyclerView recyclerView;
    private QuestionAdapter questionAdapter;
    private List<Question> questionList;
    private ProgressBar loadingProgressBar;
    private TextView noQuestionsText;
    private LinearLayout contentLayout;
    private Button submitButton;
    private static final String TAG = "StudentEvaluation";
    private static final String RESPONSE_TAG = "responseTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_evaluation);

        // Initialize views
        recyclerView = findViewById(R.id.questionsRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        noQuestionsText = findViewById(R.id.loadingQuestionsText);
        contentLayout = findViewById(R.id.contentLayout);
        submitButton = findViewById(R.id.submitButton);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        questionList = new ArrayList<>();
        questionAdapter = new QuestionAdapter(questionList);
        recyclerView.setAdapter(questionAdapter);

        Log.d(RESPONSE_TAG, "onCreate: Setting up StudentEvaluation activity");

        // Get eventId from intent
        if (getIntent() != null && getIntent().hasExtra("eventUID")) {
            eventId = getIntent().getStringExtra("eventUID");
            Log.d(RESPONSE_TAG, "Received eventId: " + eventId);

            // Show loading indicator
            showLoading();

            // Load questions from Firebase
            loadQuestions();
        } else {
            Log.e(RESPONSE_TAG, "Event ID not found in intent");
            Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Submit button click listener
        submitButton.setOnClickListener(v -> {
            Log.d(RESPONSE_TAG, "Submit button clicked");
            submitEvaluation();
        });
    }

    private void loadQuestions() {
        DatabaseReference eventQuestionsRef = FirebaseDatabase.getInstance()
                .getReference("eventQuestions")
                .child(eventId);

        Log.d(RESPONSE_TAG, "Loading questions from path: " + eventQuestionsRef.toString());

        eventQuestionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(RESPONSE_TAG, "Questions data snapshot exists");

                    // Check if isSubmitted is true
                    Boolean isSubmitted = dataSnapshot.child("isSubmitted").getValue(Boolean.class);
                    Log.d(RESPONSE_TAG, "isSubmitted value: " + isSubmitted);

                    if (isSubmitted != null && isSubmitted) {
                        // isSubmitted is true, load and display questions
                        questionList.clear();

                        // Iterate through question data directly
                        for (DataSnapshot questionSnapshot : dataSnapshot.getChildren()) {
                            // Skip the isSubmitted field
                            if ("isSubmitted".equals(questionSnapshot.getKey())) {
                                continue;
                            }

                            String questionId = questionSnapshot.getKey();
                            String questionText = questionSnapshot.child("text").getValue(String.class);
                            String questionType = questionSnapshot.child("type").getValue(String.class);
                            Boolean isRequired = questionSnapshot.child("required").getValue(Boolean.class);

                            Log.d(RESPONSE_TAG, "Loading question: id=" + questionId +
                                    ", text=" + questionText +
                                    ", type=" + questionType +
                                    ", required=" + isRequired);

                            // Skip if essential data is missing
                            if (questionText == null || questionType == null) {
                                Log.e(RESPONSE_TAG, "Essential data missing for question: " + questionId);
                                continue;
                            }

                            Question question = new Question(questionId, questionText, questionType);
                            question.setRequired(isRequired != null && isRequired);

                            // Add to the list
                            questionList.add(question);
                        }

                        Log.d(RESPONSE_TAG, "Loaded " + questionList.size() + " questions");

                        if (questionList.isEmpty()) {
                            // No valid questions found
                            Log.w(RESPONSE_TAG, "No valid questions found");
                            showNoQuestions("No questions found for this evaluation.");
                        } else {
                            // Update the adapter
                            questionAdapter.notifyDataSetChanged();
                            // Show content
                            showContent();
                        }
                    } else {
                        // isSubmitted is false, show message
                        Log.w(RESPONSE_TAG, "Evaluation questions are not marked as submitted");
                        showNoQuestions("Evaluation questions are not available yet.");
                    }
                } else {
                    // eventQuestions node doesn't exist
                    Log.w(RESPONSE_TAG, "No evaluation has been created for this event");
                    showNoQuestions("No evaluation has been created for this event.");
                }

                // Hide loading indicator
                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(RESPONSE_TAG, "Firebase error loading questions: " + databaseError.getMessage());
                Toast.makeText(StudentEvaluation.this, "Error loading questions: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading();
                showNoQuestions("Failed to load evaluation questions.");
            }
        });
    }

    private void submitEvaluation() {
        // Get student ID from shared preferences
        String studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("studentID", null);
        String studentName = getSharedPreferences("UserSession", MODE_PRIVATE).getString("studentName", "Anonymous");

        Log.d(RESPONSE_TAG, "submitEvaluation: studentId=" + studentId + ", studentName=" + studentName);

        if (studentId == null) {
            Log.e(RESPONSE_TAG, "Student ID not found in shared preferences");
            Toast.makeText(this, "Student ID not found. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify question adapter is properly set
        if (questionAdapter == null) {
            Log.e(RESPONSE_TAG, "Question adapter is null");
            Toast.makeText(this, "Internal error: Question adapter not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if all required questions have answers
        List<QuestionResponse> responses = questionAdapter.getResponses();
        Log.d(RESPONSE_TAG, "Got " + responses.size() + " responses from adapter");

        // Debug each response
        for (int i = 0; i < responses.size(); i++) {
            QuestionResponse response = responses.get(i);
            Log.d(RESPONSE_TAG, "Response " + i + ": questionId=" + response.getQuestionId() +
                    ", answer=" + (response.getAnswer() == null ? "null" : "'" + response.getAnswer() + "'"));
        }

        boolean allRequiredAnswered = true;
        List<Integer> unansweredQuestions = new ArrayList<>();

        for (int i = 0; i < questionList.size(); i++) {
            Question question = questionList.get(i);
            QuestionResponse response = null;

            // Find matching response for this question
            for (QuestionResponse r : responses) {
                if (r.getQuestionId().equals(question.getId())) {
                    response = r;
                    break;
                }
            }

            Log.d(RESPONSE_TAG, "Checking question " + (i+1) + ": id=" + question.getId() +
                    ", required=" + question.isRequired() +
                    ", has response=" + (response != null) +
                    (response != null ? ", answer='" + response.getAnswer() + "'" : ""));

            if (question.isRequired() && (response == null || response.getAnswer() == null || response.getAnswer().isEmpty())) {
                allRequiredAnswered = false;
                unansweredQuestions.add(i + 1); // +1 for human-readable question numbers
                Log.w(RESPONSE_TAG, "Required question " + (i+1) + " is unanswered");
            }
        }

        if (!allRequiredAnswered) {
            String message = "Please answer required question" +
                    (unansweredQuestions.size() > 1 ? "s: " : ": ") +
                    android.text.TextUtils.join(", ", unansweredQuestions);
            Log.w(RESPONSE_TAG, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        // Show loading
        showLoading();
        submitButton.setEnabled(false);
        Log.d(RESPONSE_TAG, "All required questions answered, proceeding to save feedback");

        // Check if student has already submitted feedback for this event
        DatabaseReference eventFeedbackRef = FirebaseDatabase.getInstance()
                .getReference("eventFeedback")
                .child(eventId)
                .child(studentId);

        Log.d(RESPONSE_TAG, "Checking path for existing feedback: " + eventFeedbackRef.toString());

        eventFeedbackRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(RESPONSE_TAG, "Student has already submitted feedback for this event");
                } else {
                    Log.d(RESPONSE_TAG, "No existing feedback found for this student");
                }

                // Save feedback regardless of whether it exists or not
                saveFeedback(studentId, studentName, responses);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(RESPONSE_TAG, "Error checking existing feedback: " + databaseError.getMessage());
                Toast.makeText(StudentEvaluation.this, "Error submitting feedback: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading();
                submitButton.setEnabled(true);
            }
        });
    }

    private void saveFeedback(String studentId, String studentName, List<QuestionResponse> responses) {
        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateSubmitted = sdf.format(new Date());
        long timestamp = System.currentTimeMillis();

        Log.d(RESPONSE_TAG, "Starting to save feedback for student: " + studentId + " eventId: " + eventId);
        Log.d(RESPONSE_TAG, "Total responses to save: " + responses.size());

        // Create feedback metadata
        FeedbackMetadata metadata = new FeedbackMetadata(eventId, dateSubmitted, timestamp, studentName);
        Log.d(RESPONSE_TAG, "Created metadata: " + metadata.toString());

        // Create a separate write for metadata and responses
        // Strategy: Use updateChildren to ensure both nodes are written atomically

        DatabaseReference eventFeedbackRef = FirebaseDatabase.getInstance()
                .getReference("eventFeedback")
                .child(eventId)
                .child(studentId);

        // Create the metadata map
        Map<String, Object> metadataMap = metadata.toMap();

        // Create the responses map
        Map<String, Object> responsesMap = new HashMap<>();

        // Add all responses to the responses map
        for (QuestionResponse response : responses) {
            // Skip invalid responses
            if (response == null || response.getQuestionId() == null) {
                Log.e(RESPONSE_TAG, "Skipping invalid response");
                continue;
            }

            // Find corresponding question details
            String questionId = response.getQuestionId();
            String questionText = "Unknown Question";
            String questionType = "default";
            boolean required = false;

            for (Question q : questionList) {
                if (q.getId().equals(questionId)) {
                    questionText = q.getText();
                    questionType = q.getType();
                    required = q.isRequired();
                    break;
                }
            }

            // Log each response
            Log.d(RESPONSE_TAG, "Adding response for question: " + questionId +
                    ", text: " + questionText +
                    ", answer: " + response.getAnswer());

            // Create question response structure
            Map<String, Object> questionResponseMap = new HashMap<>();
            questionResponseMap.put("answer", response.getAnswer());
            questionResponseMap.put("questionText", questionText);
            questionResponseMap.put("questionType", questionType);
            questionResponseMap.put("required", required);

            // Add to responses map
            responsesMap.put(questionId, questionResponseMap);
        }

        // Create the combined update map with paths relative to the studentId node
        Map<String, Object> updates = new HashMap<>();
        updates.put("metadata", metadataMap);
        updates.put("responses", responsesMap);

        Log.d(RESPONSE_TAG, "About to write to Firebase with updateChildren");
        Log.d(RESPONSE_TAG, "Updates map contains keys: " + updates.keySet());
        Log.d(RESPONSE_TAG, "Responses map contains " + responsesMap.size() + " responses");

        // Use updateChildren to ensure atomic write
        eventFeedbackRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(RESPONSE_TAG, "Feedback saved successfully to eventFeedback node");

                    // Verify the save worked
                    eventFeedbackRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Log.d(RESPONSE_TAG, "Verification successful - data exists");

                                if (dataSnapshot.hasChild("responses")) {
                                    DataSnapshot responsesSnapshot = dataSnapshot.child("responses");
                                    Log.d(RESPONSE_TAG, "Responses node exists with " +
                                            responsesSnapshot.getChildrenCount() + " children");

                                    // Log the keys of all responses
                                    for (DataSnapshot responseSnapshot : responsesSnapshot.getChildren()) {
                                        Log.d(RESPONSE_TAG, "  Response found: " + responseSnapshot.getKey());
                                    }
                                } else {
                                    Log.e(RESPONSE_TAG, "Responses node missing after save!");
                                }
                            } else {
                                Log.e(RESPONSE_TAG, "Verification failed - data doesn't exist!");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(RESPONSE_TAG, "Verification failed: " + databaseError.getMessage());
                        }
                    });

                    // Now update student completion status in events
                    DatabaseReference eventStudentRef = FirebaseDatabase.getInstance()
                            .getReference("events")
                            .child(eventId)
                            .child("studentsFeedback")
                            .child(studentId);

                    Map<String, Object> completionUpdate = new HashMap<>();
                    completionUpdate.put("feedbackCompleted", true);
                    completionUpdate.put("feedbackTimestamp", timestamp);

                    eventStudentRef.updateChildren(completionUpdate)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(RESPONSE_TAG, "Student completion status updated successfully");
                                Toast.makeText(StudentEvaluation.this, "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                                hideLoading();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(RESPONSE_TAG, "Error updating event completion status: " + e.getMessage());
                                Toast.makeText(StudentEvaluation.this, "Feedback saved but event status update failed", Toast.LENGTH_SHORT).show();
                                hideLoading();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(RESPONSE_TAG, "Error saving feedback: " + e.getMessage());
                    Toast.makeText(StudentEvaluation.this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hideLoading();
                    submitButton.setEnabled(true);
                });
    }

    private void showLoading() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
        noQuestionsText.setVisibility(View.GONE);
        Log.d(RESPONSE_TAG, "Showing loading indicator");
    }

    private void hideLoading() {
        loadingProgressBar.setVisibility(View.GONE);
        Log.d(RESPONSE_TAG, "Hiding loading indicator");
    }

    private void showContent() {
        contentLayout.setVisibility(View.VISIBLE);
        noQuestionsText.setVisibility(View.GONE);
        Log.d(RESPONSE_TAG, "Showing content");
    }

    private void showNoQuestions(String message) {
        contentLayout.setVisibility(View.GONE);
        noQuestionsText.setVisibility(View.VISIBLE);
        noQuestionsText.setText(message);
        Log.d(RESPONSE_TAG, "Showing no questions message: " + message);
    }
}