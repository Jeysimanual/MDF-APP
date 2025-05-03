package com.capstone.mdfeventmanagementsystem.Student;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StudentDashboardInside extends AppCompatActivity {

    private static final String TAG = "TestApp";

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime;
    private ImageView eventImage;
    private Button registerButton, ticketButton, evalButton;

    private FirebaseAuth mAuth;
    private String eventUID; // Store event UID as a variable
    private String studentID; // Store the student ID retrieved from SharedPreferences
    private String studentYearLevel; // Store student's year level

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard_inside);

        // Initialize UI elements
        initializeViews();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Retrieve studentID from UserSession SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        studentID = sharedPreferences.getString("studentID", null);
        studentYearLevel = sharedPreferences.getString("yearLevel", null);

        if (studentID == null || studentID.isEmpty()) {
            Toast.makeText(this, "Student ID not found! Please log in again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No studentID found in SharedPreferences!");
            // Redirect back to login
            Intent intent = new Intent(StudentDashboardInside.this, StudentLogin.class);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "Using studentID from SharedPreferences: " + studentID);
        Log.d(TAG, "Using yearLevel from SharedPreferences: " + studentYearLevel);

        // Get event details from intent and display them
        displayEventDetails();

        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        venue = findViewById(R.id.venue);
        eventSpan = findViewById(R.id.eventSpan);
        graceTime = findViewById(R.id.graceTime);
        eventImage = findViewById(R.id.eventPhotoUrl);
        registerButton = findViewById(R.id.registerButton);
        ticketButton = findViewById(R.id.ticketButton);
        evalButton = findViewById(R.id.evalButton);
    }

    private void displayEventDetails() {
        Intent intent = getIntent();
        eventUID = intent.getStringExtra("eventUID");

        // Set text values from intent
        eventName.setText(intent.getStringExtra("eventName"));
        eventDescription.setText(intent.getStringExtra("eventDescription"));
        startDate.setText(intent.getStringExtra("startDate"));
        endDate.setText(intent.getStringExtra("endDate"));
        startTime.setText(intent.getStringExtra("startTime"));
        endTime.setText(intent.getStringExtra("endTime"));
        venue.setText(intent.getStringExtra("venue"));
        eventSpan.setText(intent.getStringExtra("eventSpan"));
        graceTime.setText(intent.getStringExtra("graceTime"));

        // Display the event type and event for
        TextView eventType = findViewById(R.id.eventType);
        TextView eventFor = findViewById(R.id.eventFor);
        eventType.setText(intent.getStringExtra("eventType"));
        eventFor.setText(intent.getStringExtra("eventFor"));

        Log.d(TAG, "Received Event UID: " + eventUID);

        // Load event image using Glide
        String eventPhotoUrl = intent.getStringExtra("eventPhotoUrl");
        if (eventPhotoUrl != null && !eventPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(eventPhotoUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(eventImage);
        } else {
            eventImage.setImageResource(R.drawable.placeholder_image);
        }

        // Check if the event is for the correct year level
        String eventForValue = intent.getStringExtra("eventFor");
        Log.d(TAG, "Intent eventFor: " + eventForValue);
        Log.d(TAG, "SharedPreferences yearLevel: " + studentYearLevel);

        if (studentYearLevel != null && eventForValue != null) {
            String normalizedEventFor = eventForValue.toLowerCase().replaceAll("[-\\s]", "").trim();
            String normalizedYearLevel = studentYearLevel.toLowerCase().replaceAll("[-\\s]", "").trim();

            if (!normalizedEventFor.equals("all") && !normalizedEventFor.equals(normalizedYearLevel)) {
                Log.d(TAG, "eventFor does not match yearLevel. Showing warning.");
                registerButton.setEnabled(false);
                registerButton.setAlpha(0.5f); // Dim the button
                Toast.makeText(this, "This event is not for your year level.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "eventFor matches yearLevel or is 'All'. Button allowed.");
            }
        }
    }

    private void setupButtonListeners() {
        // Register button click
        registerButton.setOnClickListener(v -> registerForEvent());

        // Ticket button click
        ticketButton.setOnClickListener(v -> {
            Intent ticketIntent = new Intent(StudentDashboardInside.this, StudentTickets.class);
            ticketIntent.putExtra("eventUID", eventUID);
            startActivity(ticketIntent);
        });

        // Evaluation button click
        // Evaluation button click
        evalButton.setOnClickListener(v -> {
            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
            DatabaseReference eventQuestionsRef = FirebaseDatabase.getInstance().getReference("eventQuestions").child(eventUID);

            // First check if student has already submitted feedback
            eventRef.child("studentsFeedback").child(studentID).get().addOnCompleteListener(feedbackTask -> {
                boolean hasSubmittedFeedback = feedbackTask.isSuccessful() && feedbackTask.getResult().exists();

                if (hasSubmittedFeedback) {
                    // Student already submitted feedback, go to StudentResponse
                    Intent viewResponseIntent = new Intent(StudentDashboardInside.this, StudentResponse.class);
                    viewResponseIntent.putExtra("eventUID", eventUID);
                    startActivity(viewResponseIntent);
                } else {
                    // Student hasn't submitted feedback, check if questions are available
                    eventQuestionsRef.child("isSubmitted").get().addOnCompleteListener(questionsTask -> {
                        boolean questionsAvailable = questionsTask.isSuccessful() &&
                                questionsTask.getResult().exists() &&
                                questionsTask.getResult().getValue(Boolean.class) == Boolean.TRUE;

                        if (questionsAvailable) {
                            // Questions are available but student hasn't submitted, go to StudentEvaluation
                            Intent evalIntent = new Intent(StudentDashboardInside.this, StudentEvaluation.class);
                            evalIntent.putExtra("eventUID", eventUID);
                            startActivity(evalIntent);
                        } else {
                            // No questions available yet
                            Toast.makeText(StudentDashboardInside.this,
                                    "Evaluation questions not yet available", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkRegistrationStatus(); // Automatically check registration status when the activity starts
    }

    /**
     * Master method to check registration status and update UI accordingly
     */
    private void checkRegistrationStatus() {
        if (eventUID == null || eventUID.isEmpty()) {
            Log.e(TAG, "Event UID is missing");
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        eventRef.get().addOnCompleteListener(eventTask -> {
            if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                DataSnapshot eventSnapshot = eventTask.getResult();

                String eventFor = eventSnapshot.child("eventFor").getValue(String.class);
                Boolean registrationAllowed = eventSnapshot.child("registrationAllowed").getValue(Boolean.class);
                String status = eventSnapshot.child("status").getValue(String.class);
                String currentEventVersion = eventSnapshot.child("version").getValue(String.class);

                // First check if student is eligible for this event
                if (!isStudentEligible(eventFor, studentYearLevel)) {
                    // Student is not eligible - hide all buttons
                    hideAllButtons();
                    Toast.makeText(this, "You are not eligible for this event.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Student is not eligible for this event. Hiding all buttons.");
                    return;
                }

                // Check if event is expired
                boolean isExpired = "expired".equalsIgnoreCase(status);
                if (isExpired) {
                    registerButton.setVisibility(View.GONE);
                    ticketButton.setVisibility(View.GONE);
                    checkEvaluationStatus();
                    return;
                }

                // Check if registration is allowed
                if (registrationAllowed == null || !registrationAllowed) {
                    hideAllButtons();
                    Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Registration is not allowed. Hiding all buttons.");
                    return;
                }

                // Check if student has a ticket
                ticketRef.get().addOnCompleteListener(ticketTask -> {
                    if (ticketTask.isSuccessful()) {
                        boolean hasTicket = ticketTask.getResult().exists();

                        if (hasTicket) {
                            String studentTicketVersion = ticketTask.getResult().child("version").getValue(String.class);

                            if (currentEventVersion != null && !currentEventVersion.equals(studentTicketVersion)) {
                                // Version mismatch — invalidate ticket
                                ticketRef.removeValue().addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Old ticket version (" + studentTicketVersion + ") invalidated. Current version is " + currentEventVersion);
                                    Toast.makeText(this, "Event updated. Please re-register.", Toast.LENGTH_SHORT).show();
                                    updateButtonsForNonRegistered();
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to remove outdated ticket: " + e.getMessage());
                                });
                            } else {
                                // Valid ticket
                                Log.d(TAG, "Ticket is valid for current version: " + currentEventVersion);
                                updateButtonsForRegistered();
                            }
                        } else {
                            // No ticket
                            updateButtonsForNonRegistered();
                        }
                    } else {
                        Log.e(TAG, "Error checking ticket: " +
                                (ticketTask.getException() != null ? ticketTask.getException().getMessage() : "Unknown error"));
                    }
                });
            } else {
                Log.e(TAG, "Error fetching event data: " +
                        (eventTask.getException() != null ? eventTask.getException().getMessage() : "Unknown error"));
            }
        });
    }

    /**
     * Check if the student is eligible for the event based on year level
     */
    private boolean isStudentEligible(String eventFor, String studentYearLevel) {
        if (eventFor == null || studentYearLevel == null) {
            Log.d(TAG, "Eligibility check failed: one or both values are null");
            return false;
        }

        String normalizedEventFor = eventFor.toLowerCase().replaceAll("[-\\s]", "").trim();
        String normalizedYearLevel = studentYearLevel.toLowerCase().replaceAll("[-\\s]", "").trim();

        Log.d(TAG, "Normalized eventFor: " + normalizedEventFor);
        Log.d(TAG, "Normalized studentYearLevel: " + normalizedYearLevel);

        boolean isEligible = normalizedEventFor.equals("all") || normalizedEventFor.equals(normalizedYearLevel);
        Log.d(TAG, "Eligibility result: " + isEligible);

        return isEligible;
    }

    /**
     * Update buttons for registered students
     */
    private void updateButtonsForRegistered() {
        registerButton.setVisibility(View.GONE);
        ticketButton.setVisibility(View.VISIBLE);
        evalButton.setVisibility(View.GONE);
        Log.d(TAG, "Student is registered. Showing ticket button.");
    }

    /**
     * Update buttons for non-registered students
     */
    private void updateButtonsForNonRegistered() {
        registerButton.setVisibility(View.VISIBLE);
        ticketButton.setVisibility(View.GONE);
        evalButton.setVisibility(View.GONE);
        Log.d(TAG, "Student is not registered. Showing register button.");
    }

    /**
     * Hide all buttons
     */
    private void hideAllButtons() {
        registerButton.setVisibility(View.GONE);
        ticketButton.setVisibility(View.GONE);
        evalButton.setVisibility(View.GONE);
    }

    /**
     * Register the current student for the event
     */
    private void registerForEvent() {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference("students").child(studentID).child("tickets");

        eventRef.child("registrationAllowed").get().addOnCompleteListener(eventTask -> {
            if (eventTask.isSuccessful()) {
                Boolean registrationAllowed = eventTask.getResult().getValue(Boolean.class);

                if (registrationAllowed == null || !registrationAllowed) {
                    hideAllButtons();
                    Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
                    return;
                }

                ticketsRef.get().addOnCompleteListener(ticketTask -> {
                    if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                        for (DataSnapshot ticketSnapshot : ticketTask.getResult().getChildren()) {
                            if (ticketSnapshot.getKey().equals(eventUID)) {
                                Toast.makeText(this, "You are already registered!", Toast.LENGTH_SHORT).show();
                                updateButtonsForRegistered();
                                return;
                            }
                        }
                    }

                    eventRef.child("eventFor").get().addOnCompleteListener(eventTaskFor -> {
                        if (eventTaskFor.isSuccessful() && eventTaskFor.getResult().exists()) {
                            String eventFor = eventTaskFor.getResult().getValue(String.class);

                            if (isStudentEligible(eventFor, studentYearLevel)) {
                                proceedWithRegistration();
                            } else {
                                hideAllButtons();
                                Toast.makeText(this, "You are not eligible for this event.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Error fetching event details.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });

            } else {
                Toast.makeText(this, "Error checking registration status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Check evaluation status and update UI accordingly
     */
    /**
     * Check evaluation status and update UI accordingly
     */
    /**
     * Check evaluation status and update UI accordingly
     */
    private void checkEvaluationStatus() {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        DatabaseReference eventQuestionsRef = FirebaseDatabase.getInstance().getReference("eventQuestions").child(eventUID);
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        // Check if student is registered for this event first
        ticketRef.get().addOnCompleteListener(ticketTask -> {
            boolean isRegistered = ticketTask.isSuccessful() && ticketTask.getResult().exists();

            if (!isRegistered) {
                // Student is not registered, show a message that they didn't register
                evalButton.setVisibility(View.VISIBLE);
                evalButton.setText("Not Registered");
                evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                evalButton.setEnabled(false); // Disable the button since it's just informational
                Log.d(TAG, "Student not registered for this event. Showing 'Not Registered' message.");
                return;
            }

            // Student is registered, now check feedback and evaluation status
            eventRef.get().addOnCompleteListener(eventTask -> {
                if (eventTask.isSuccessful()) {
                    DataSnapshot eventSnapshot = eventTask.getResult();

                    // Check if user has already submitted feedback
                    boolean feedbackSubmitted;
                    if (eventSnapshot.child("studentsFeedback").exists()) {
                        feedbackSubmitted = eventSnapshot.child("studentsFeedback").hasChild(studentID);
                    } else {
                        feedbackSubmitted = false;
                    }

                    // Now check the isSubmitted flag in eventQuestions
                    eventQuestionsRef.get().addOnCompleteListener(questionsTask -> {
                        if (questionsTask.isSuccessful()) {
                            DataSnapshot questionsSnapshot = questionsTask.getResult();
                            Boolean isSubmitted = questionsSnapshot.child("isSubmitted").getValue(Boolean.class);

                            evalButton.setVisibility(View.VISIBLE);

                            if (feedbackSubmitted) {
                                // User has submitted feedback, show "View Response" button
                                evalButton.setText("View Response");
                                evalButton.setBackgroundColor(getResources().getColor(R.color.green));
                                Log.d(TAG, "User has submitted feedback. Showing View Response button.");
                            } else if (isSubmitted != null && isSubmitted) {
                                // Event has questions ready for evaluation but user hasn't submitted
                                evalButton.setText("ANSWER EVALUATION");
                                evalButton.setBackgroundColor(getResources().getColor(R.color.bg_green));
                                Log.d(TAG, "Event has ended. Evaluation not submitted yet. Showing Evaluate button.");
                            } else {
                                // Event has ended but evaluation is not available or not submitted
                                evalButton.setText("Event Ended");
                                evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                                evalButton.setEnabled(false); // Disable the button since it's just informational
                                Log.d(TAG, "Event has ended. No evaluation available. Showing red Event Ended indicator.");
                            }
                        } else {
                            Log.e(TAG, "Error checking eventQuestions: " +
                                    (questionsTask.getException() != null ? questionsTask.getException().getMessage() : "Unknown error"));
                            // Handle the error state - show a disabled button
                            evalButton.setVisibility(View.VISIBLE);
                            evalButton.setText("Event Ended");
                            evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                            evalButton.setEnabled(false);
                        }
                    });
                } else {
                    Log.e(TAG, "Error checking event data: " +
                            (eventTask.getException() != null ? eventTask.getException().getMessage() : "Unknown error"));
                }
            });
        });
    }

    /**
     * Proceed with actual registration after all checks
     */
    private void proceedWithRegistration() {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        ticketRef.get().addOnCompleteListener(ticketTask -> {
            if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                Toast.makeText(this, "You are already registered for this event!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Student already registered for event: " + eventUID);
                updateButtonsForRegistered();
            } else {
                eventRef.get().addOnCompleteListener(eventTask -> {
                    if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                        DataSnapshot eventSnapshot = eventTask.getResult();
                        String eventSpanValue = eventSnapshot.child("eventSpan").getValue(String.class);
                        String startDateValue = eventSnapshot.child("startDate").getValue(String.class);
                        String endDateValue = eventSnapshot.child("endDate").getValue(String.class);
                        String eventVersion = eventSnapshot.child("version").getValue(String.class);

                        long currentTimeMillis = System.currentTimeMillis();
                        String formattedTimestamp = getCurrentTimestamp();

                        HashMap<String, Object> ticketData = new HashMap<>();
                        ticketData.put("registeredAt", formattedTimestamp);
                        ticketData.put("timestampMillis", currentTimeMillis);
                        ticketData.put("version", eventVersion != null ? eventVersion : "v1");

                        boolean isMultiDay = "multi-day".equals(eventSpanValue);
                        if (isMultiDay && startDateValue != null && endDateValue != null) {
                            Log.d(TAG, "Creating attendance days for multi-day event");
                            HashMap<String, Object> attendanceDays = createAttendanceDaysForDateRange(startDateValue, endDateValue);
                            ticketData.put("attendanceDays", attendanceDays);
                        } else {
                            HashMap<String, Object> attendanceDays = new HashMap<>();
                            HashMap<String, Object> dayData = new HashMap<>();
                            dayData.put("date", startDateValue);
                            dayData.put("status", "N/A");
                            dayData.put("attendance", "pending"); // Add attendance field for single-day event
                            attendanceDays.put("day_1", dayData);
                            ticketData.put("attendanceDays", attendanceDays);
                        }

                        ticketRef.setValue(ticketData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Event registered at " + formattedTimestamp);
                                    generateAndUploadQRCode();
                                    updateButtonsForRegistered();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error registering event: " + e.getMessage());
                                });
                    } else {
                        Toast.makeText(this, "Failed to fetch event details.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching event details: " +
                                (eventTask.getException() != null ? eventTask.getException().getMessage() : "Unknown error"));
                    }
                });
            }
        });
    }

    /**
     * Creates attendance day entries for each day in the given date range.
     * Each day will have the "pending" status initially.
     */
    private HashMap<String, Object> createAttendanceDaysForDateRange(String startDateStr, String endDateStr) {
        HashMap<String, Object> attendanceDays = new HashMap<>();

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);

            if (startDate == null || endDate == null) {
                Log.e(TAG, "Invalid date format for start or end date");
                return attendanceDays;
            }

            // Calculate number of days between start and end date (inclusive)
            long diffInMillis = endDate.getTime() - startDate.getTime();
            long dayCount = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS) + 1;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            // Create an entry for each day in the range with date, status, and attendance
            for (int i = 1; i <= dayCount; i++) {
                String currentDateStr = dateFormat.format(calendar.getTime());

                // Create a HashMap for each day containing date, status, and attendance
                HashMap<String, Object> dayData = new HashMap<>();
                dayData.put("date", currentDateStr);
                dayData.put("status", "N/A"); // Initialize status as pending for each day
                dayData.put("attendance", "pending"); // Initialize attendance as pending for each day

                // Add this day's data to the attendanceDays map
                attendanceDays.put("day_" + i, dayData);

                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            Log.d(TAG, "Created attendance days with status and attendance: " + attendanceDays.toString());
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates: " + e.getMessage());
        }

        return attendanceDays;
    }

    /**
     * Generate QR code for the ticket and upload to Firebase
     */
    private void generateAndUploadQRCode() {
        QrCodeGenerator.generateQRCodeWithEventAndStudentInfo(this, eventUID, new QrCodeGenerator.OnQRCodeGeneratedListener() {
            @Override
            public void onQRCodeGenerated(Bitmap qrCodeBitmap) {
                Log.d(TAG, "QR Code successfully generated.");
            }

            @Override
            public void onQRCodeUploaded(String downloadUrl, String ticketID) {
                // Reference to the student's ticket entry
                DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                        .getReference("students")
                        .child(studentID)
                        .child("tickets")
                        .child(eventUID);

                // Add ticketID and QR code URL (without overwriting attendanceDays if present)
                HashMap<String, Object> ticketData = new HashMap<>();
                ticketData.put("ticketID", ticketID); // Save the ticket ID
                ticketData.put("qrCodeUrl", downloadUrl);

                ticketRef.updateChildren(ticketData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(StudentDashboardInside.this, "QR Code saved!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "QR Code URL saved in Firebase: " + downloadUrl);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(StudentDashboardInside.this, "Failed to save QR Code URL!", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error saving QR Code URL: " + e.getMessage());
                        });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(StudentDashboardInside.this, "QR Code Generation Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "QR Code Generation Error: " + errorMessage);
            }
        });
    }

    /**
     * Returns the current timestamp in the format MM/dd/yyyy hh:mm a (e.g., 03/14/2025 5:00 AM)
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }
}