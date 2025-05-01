package com.capstone.mdfeventmanagementsystem.Student;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import com.capstone.mdfeventmanagementsystem.Adapters.ResponseAdapter;
import com.capstone.mdfeventmanagementsystem.Models.Question;
import com.capstone.mdfeventmanagementsystem.Models.QuestionResponse;
import com.capstone.mdfeventmanagementsystem.R;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentResponse extends AppCompatActivity {

    private String eventId;
    private String studentId;
    private RecyclerView recyclerView;
    private ResponseAdapter responseAdapter;
    private List<QuestionResponse> responseList;
    private ProgressBar loadingProgressBar;
    private TextView noResponsesText;
    private TextView headerTextView;
    private static final String TAG = "StudentResponse";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_response);

        // Initialize views
        recyclerView = findViewById(R.id.responsesRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        noResponsesText = findViewById(R.id.noResponsesText);
        headerTextView = findViewById(R.id.responseHeaderText);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        responseList = new ArrayList<>();
        responseAdapter = new ResponseAdapter(responseList);
        recyclerView.setAdapter(responseAdapter);

        Log.d(TAG, "onCreate: Setting up StudentResponse activity");

        // Get eventId from intent
        eventId = getIntent().getStringExtra("eventUID");
        // Get studentId from intent
        studentId = getIntent().getStringExtra("studentID");

        Log.d(TAG, "Retrieved from intent - eventId: " + eventId + ", studentId: " + studentId);

        // Check if eventId is available
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Event ID not found in intent");
            Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If studentId is not provided in intent, use the current logged-in student
        if (studentId == null || studentId.isEmpty()) {
            studentId = getSharedPreferences("UserSession", MODE_PRIVATE).getString("studentID", null);
            Log.d(TAG, "Using logged-in studentId: " + studentId);

            // Check if we still don't have a valid studentId
            if (studentId == null || studentId.isEmpty()) {
                Log.e(TAG, "Student ID not found");
                Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        // Show loading indicator
        showLoading();

        // Load responses from Firebase
        loadResponses();

        // Set edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadResponses() {
        // Access the Firebase database at path: eventFeedback/{eventId}/{studentId}
        DatabaseReference feedbackRef = FirebaseDatabase.getInstance()
                .getReference("eventFeedback")
                .child(eventId)
                .child(studentId);

        Log.d(TAG, "Loading responses from path: eventFeedback/" + eventId + "/" + studentId);

        feedbackRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Found feedback data for student");
                    responseList.clear();

                    // Check if metadata exists
                    if (dataSnapshot.hasChild("metadata")) {
                        DataSnapshot metadataSnapshot = dataSnapshot.child("metadata");

                        // Get date submitted from metadata
                        String dateSubmitted = metadataSnapshot.child("dateSubmitted").getValue(String.class);

                        // Set header text with only submission date
                        String headerText = "";
                        if (dateSubmitted != null) {
                            headerText = "Submitted on " + dateSubmitted;
                        }
                        headerTextView.setText(headerText);
                        Log.d(TAG, "Setting header text: " + headerText);
                    } else {
                        Log.w(TAG, "No metadata found for this response");
                        headerTextView.setText("");
                    }

                    // Check if responses exist
                    if (dataSnapshot.hasChild("responses")) {
                        // Get responses
                        DataSnapshot responsesSnapshot = dataSnapshot.child("responses");
                        Log.d(TAG, "Found responses data with " + responsesSnapshot.getChildrenCount() + " questions");

                        for (DataSnapshot questionSnapshot : responsesSnapshot.getChildren()) {
                            String questionId = questionSnapshot.getKey();
                            String answer = questionSnapshot.child("answer").getValue(String.class);
                            String questionText = questionSnapshot.child("questionText").getValue(String.class);
                            String questionType = questionSnapshot.child("questionType").getValue(String.class);
                            Boolean required = questionSnapshot.child("required").getValue(Boolean.class);

                            Log.d(TAG, "Loading response: questionId=" + questionId +
                                    ", text=" + questionText +
                                    ", type=" + questionType +
                                    ", answer=" + answer);

                            // Skip if essential data is missing
                            if (questionText == null || questionType == null || answer == null) {
                                Log.e(TAG, "Essential data missing for question: " + questionId);
                                continue;
                            }

                            // Create question object for the response
                            Question question = new Question(questionId, questionText, questionType);
                            if (required != null) {
                                question.setRequired(required);
                            }

                            // Create response object
                            QuestionResponse response = new QuestionResponse(questionId, answer);
                            response.setQuestion(question);

                            // Add to the list
                            responseList.add(response);
                        }

                        Log.d(TAG, "Loaded " + responseList.size() + " responses");

                        if (responseList.isEmpty()) {
                            // No valid responses found
                            Log.w(TAG, "No valid responses found");
                            showNoResponses("No feedback responses found.");
                        } else {
                            // Update the adapter
                            responseAdapter.notifyDataSetChanged();
                            // Show content
                            showContent();
                        }
                    } else {
                        Log.w(TAG, "No responses node found");
                        showNoResponses("No feedback responses found for this student.");
                    }
                } else {
                    // No feedback data exists
                    Log.w(TAG, "No feedback data found for this student");
                    showNoResponses("This student has not submitted feedback for this event.");
                }

                // Hide loading indicator
                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase error loading responses: " + databaseError.getMessage());
                Toast.makeText(StudentResponse.this, "Error loading responses: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading();
                showNoResponses("Failed to load feedback responses.");
            }
        });
    }

    private void showLoading() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noResponsesText.setVisibility(View.GONE);
        Log.d(TAG, "Showing loading indicator");
    }

    private void hideLoading() {
        loadingProgressBar.setVisibility(View.GONE);
        Log.d(TAG, "Hiding loading indicator");
    }

    private void showContent() {
        recyclerView.setVisibility(View.VISIBLE);
        noResponsesText.setVisibility(View.GONE);
        Log.d(TAG, "Showing content");
    }

    private void showNoResponses(String message) {
        recyclerView.setVisibility(View.GONE);
        noResponsesText.setVisibility(View.VISIBLE);
        noResponsesText.setText(message);
        Log.d(TAG, "Showing no responses message: " + message);
    }
}