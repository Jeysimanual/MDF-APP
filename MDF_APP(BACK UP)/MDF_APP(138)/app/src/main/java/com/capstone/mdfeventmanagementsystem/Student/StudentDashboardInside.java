package com.capstone.mdfeventmanagementsystem.Student;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StudentDashboardInside extends BaseActivity {

    private static final String TAG = "TestApp";

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime;
    private ImageView eventImage;
    private TextView textViewCheckInTime, textViewCheckOutTime, textViewCurrentDay, textViewStatus;
    private CardView attendanceStatusCard;
    private Button registerButton, ticketButton, evalButton, responseButton;
    private ProgressBar registerProgressBar, ticketProgressBar, evalProgressBar, responseProgressBar; // Added ProgressBars
    private Object targetParticipant = null;
    private TextView textViewParticipantCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean qrCodeGenerationFailed = false; // Track QR code generation status

    private FirebaseAuth mAuth;
    private String eventUID;
    private String studentID;
    private String studentYearLevel;
    private String eventPhotoUrl;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard_inside);

        initializeViews();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        attendanceStatusCard.setVisibility(View.GONE);

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        studentID = sharedPreferences.getString("studentID", null);
        studentYearLevel = sharedPreferences.getString("yearLevel", null);

        if (studentID == null || studentID.isEmpty()) {
            Toast.makeText(this, "Student ID not found! Please log in again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No studentID found in SharedPreferences!");
            Intent intent = new Intent(StudentDashboardInside.this, StudentLogin.class);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "Using studentID from SharedPreferences: " + studentID);
        Log.d(TAG, "Using yearLevel from SharedPreferences: " + studentYearLevel);

        // Retrieve eventUID from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("eventUID")) {
            eventUID = intent.getStringExtra("eventUID");
            Log.d(TAG, "Retrieved eventUID from intent: " + eventUID);
        } else {
            Log.e(TAG, "Intent is null or missing eventUID extra");
            eventUID = null;
        }

        // Call displayEventDetails to fetch and display event data
        Log.d(TAG, "Calling displayEventDetails with eventUID: " + eventUID);
        displayEventDetails();

        // **DELAY logging until eventUID is set**
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (eventUID != null) {
                logRegistrationStatus(eventUID);
            }
        }, 500);

        setupButtonListeners();
    }

    private void initializeViews() {
        // Existing code
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
        responseButton = findViewById(R.id.responseButton);
        registerProgressBar = findViewById(R.id.registerProgressBar); // Initialize ProgressBars
        ticketProgressBar = findViewById(R.id.ticketProgressBar);
        evalProgressBar = findViewById(R.id.evalProgressBar);
        responseProgressBar = findViewById(R.id.responseProgressBar);
        textViewParticipantCount = findViewById(R.id.textViewParticipantCount);
        attendanceStatusCard = findViewById(R.id.attendanceStatusCard);
        textViewCheckInTime = findViewById(R.id.textViewCheckInTime);
        textViewCheckOutTime = findViewById(R.id.textViewCheckOutTime);
        textViewCurrentDay = findViewById(R.id.textViewCurrentDay);
        textViewStatus = findViewById(R.id.textViewStatus);
        ImageView backButton = findViewById(R.id.back_button);
        TextView eventType = findViewById(R.id.eventType);
        TextView eventFor = findViewById(R.id.eventFor);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        backButton.setOnClickListener(v -> finish());

        // Add OnClickListener for eventImage to show popup
        eventImage.setOnClickListener(v -> showImagePopup(eventPhotoUrl)); // Use class variable

        // Set up SwipeRefreshLayout listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh data by calling existing methods
            displayEventDetails();
            checkRegistrationStatus();
            getTargetParticipant(eventUID);
            // Retry QR code generation if it previously failed
            if (qrCodeGenerationFailed) {
                Log.d(TAG, "Retrying QR code generation due to previous failure");
                DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                        .getReference("students")
                        .child(studentID)
                        .child("tickets")
                        .child(eventUID);
                ticketRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        generateAndUploadQRCode();
                    }
                });
            }
            // Stop the refreshing animation
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void showImagePopup(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "No image available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Dialog
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        // Find the ImageView in the dialog layout
        ImageView fullScreenImageView = dialog.findViewById(R.id.fullScreenImageView);

        // Load the image using Glide
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(fullScreenImageView);

        // Dismiss the dialog when the image is clicked
        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }

    private void displayEventDetails() {
        Log.d(TAG, "displayEventDetails called with eventUID: " + eventUID);
        if (eventUID == null || eventUID.isEmpty()) {
            Log.e(TAG, "Event ID is missing or empty");
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Try fetching from archive_events first
        DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventUID);
        Log.d(TAG, "Fetching event from archive_events: " + archiveRef.toString());
        archiveRef.get().addOnCompleteListener(archiveTask -> {
            if (archiveTask.isSuccessful()) {
                DataSnapshot archiveSnapshot = archiveTask.getResult();
                if (archiveSnapshot.exists()) {
                    Log.d(TAG, "Event found in archive_events for eventUID: " + eventUID);
                    populateEventDetails(archiveSnapshot);
                } else {
                    Log.w(TAG, "Event not found in archive_events for eventUID: " + eventUID);
                    // Try events as fallback
                    DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
                    Log.d(TAG, "Fetching event from events: " + eventRef.toString());
                    eventRef.get().addOnCompleteListener(eventTask -> {
                        if (eventTask.isSuccessful()) {
                            DataSnapshot eventSnapshot = eventTask.getResult();
                            if (eventSnapshot.exists()) {
                                Log.d(TAG, "Event found in events for eventUID: " + eventUID);
                                populateEventDetails(eventSnapshot);
                            } else {
                                Log.e(TAG, "Event not found in events for eventUID: " + eventUID);
                                Toast.makeText(this, "Event not found!", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } else {
                            Log.e(TAG, "Failed to fetch from events for eventUID: " + eventUID, eventTask.getException());
                            Toast.makeText(this, "Error fetching event details!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Failed to fetch from archive_events for eventUID: " + eventUID, archiveTask.getException());
                // Try events as fallback
                DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
                Log.d(TAG, "Fetching event from events (fallback): " + eventRef.toString());
                eventRef.get().addOnCompleteListener(eventTask -> {
                    if (eventTask.isSuccessful()) {
                        DataSnapshot eventSnapshot = eventTask.getResult();
                        if (eventSnapshot.exists()) {
                            Log.d(TAG, "Event found in events (fallback) for eventUID: " + eventUID);
                            populateEventDetails(eventSnapshot);
                        } else {
                            Log.e(TAG, "Event not found in events (fallback) for eventUID: " + eventUID);
                            Toast.makeText(this, "Event not found!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch from events (fallback) for eventUID: " + eventUID, eventTask.getException());
                        Toast.makeText(this, "Error fetching event details!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        });
    }

    private void populateEventDetails(DataSnapshot eventSnapshot) {
        Log.d(TAG, "populateEventDetails called for eventUID: " + eventSnapshot.getKey());

        // Extract event details
        String eventNameValue = eventSnapshot.child("eventName").getValue(String.class);
        String eventDescriptionValue = eventSnapshot.child("eventDescription").getValue(String.class);
        String startDateValue = eventSnapshot.child("startDate").getValue(String.class);
        String endDateValue = eventSnapshot.child("endDate").getValue(String.class);
        String startTimeValue = eventSnapshot.child("startTime").getValue(String.class);
        String endTimeValue = eventSnapshot.child("endTime").getValue(String.class);
        String venueValue = eventSnapshot.child("venue").getValue(String.class);
        String eventSpanValue = eventSnapshot.child("eventSpan").getValue(String.class);
        String graceTimeValue = eventSnapshot.child("graceTime").getValue(String.class);
        String eventTypeValue = eventSnapshot.child("eventType").getValue(String.class);
        String eventForValue = eventSnapshot.child("eventFor").getValue(String.class);
        eventPhotoUrl = eventSnapshot.child("eventPhotoUrl").getValue(String.class);

        // Log extracted values
        Log.d(TAG, "Event details - eventName: " + eventNameValue);
        Log.d(TAG, "Event details - eventDescription: " + eventDescriptionValue);
        Log.d(TAG, "Event details - startDate: " + startDateValue);
        Log.d(TAG, "Event details - endDate: " + endDateValue);
        Log.d(TAG, "Event details - startTime: " + startTimeValue);
        Log.d(TAG, "Event details - endTime: " + endTimeValue);
        Log.d(TAG, "Event details - venue: " + venueValue);
        Log.d(TAG, "Event details - eventSpan: " + eventSpanValue);
        Log.d(TAG, "Event details - graceTime: " + graceTimeValue);
        Log.d(TAG, "Event details - eventType: " + eventTypeValue);
        Log.d(TAG, "Event details - eventFor: " + eventForValue);
        Log.d(TAG, "Event details - eventPhotoUrl: " + eventPhotoUrl);

        // Set UI elements
        eventName.setText(eventNameValue != null ? eventNameValue : "--");
        eventDescription.setText(eventDescriptionValue != null ? eventDescriptionValue : "--");
        startDate.setText(formatDate(startDateValue));
        endDate.setText(formatDate(endDateValue));
        startTime.setText(formatTo12HourTime(startTimeValue));
        endTime.setText(formatTo12HourTime(endTimeValue));
        venue.setText(venueValue != null ? venueValue : "--");
        eventSpan.setText(eventSpanValue != null ? eventSpanValue : "--");
        if (graceTimeValue != null && !graceTimeValue.isEmpty()) {
            if (!"none".equalsIgnoreCase(graceTimeValue) && graceTimeValue.matches("\\d+")) {
                graceTime.setText(graceTimeValue + " minutes");
            } else {
                graceTime.setText(graceTimeValue);
            }
        } else {
            graceTime.setText("--");
        }

        TextView eventType = findViewById(R.id.eventType);
        TextView eventFor = findViewById(R.id.eventFor);
        eventType.setText(eventTypeValue != null ? eventTypeValue : "--");
        if (eventForValue != null && !eventForValue.isEmpty()) {
            String[] grades = eventForValue.split(",");
            StringBuilder newText = new StringBuilder();
            for (int i = 0; i < grades.length; i++) {
                newText.append(grades[i].trim());
                if (i < grades.length - 1) {
                    newText.append(", ");
                    if ((i + 1) % 2 == 0) {
                        newText.append("\n");
                    }
                }
            }
            eventFor.setText(newText.toString());
        } else {
            eventFor.setText("--");
        }
        setEventTypeStyle(eventType, null);

        // Load event image
        if (eventPhotoUrl != null && !eventPhotoUrl.isEmpty()) {
            Log.d(TAG, "Loading event image from URL: " + eventPhotoUrl);
            Glide.with(this)
                    .load(eventPhotoUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(eventImage);
        } else {
            Log.w(TAG, "No eventPhotoUrl provided, using placeholder image");
            eventImage.setImageResource(R.drawable.placeholder_image);
        }

        // Check eligibility
        if (studentYearLevel != null && eventForValue != null) {
            if (!isStudentEligible(eventForValue, studentYearLevel)) {
                Log.d(TAG, "Student yearLevel (" + studentYearLevel + ") does not match eventFor (" + eventForValue + "). Disabling register button.");
                registerButton.setEnabled(false);
                registerButton.setAlpha(0.5f);
                Toast.makeText(this, "This event is not for your year level.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Student yearLevel (" + studentYearLevel + ") matches eventFor (" + eventForValue + ") or eventFor is 'All'. Register button enabled.");
            }
        } else {
            Log.w(TAG, "studentYearLevel or eventForValue is null (studentYearLevel: " + studentYearLevel + ", eventForValue: " + eventForValue + "). Disabling register button.");
            registerButton.setEnabled(false);
            registerButton.setAlpha(0.5f);
            Toast.makeText(this, "Unable to verify eligibility.", Toast.LENGTH_LONG).show();
        }

        // Fetch target participant and ticket count
        Log.d(TAG, "Fetching target participant and ticket count for eventUID: " + eventUID);
        getTargetParticipant(eventUID);
    }

    private void setupButtonListeners() {
        registerButton.setOnClickListener(v -> {
            showRegisterLoading();
            registerForEvent();
        });

        ticketButton.setOnClickListener(v -> {
            showTicketLoading();
            Intent ticketIntent = new Intent(StudentDashboardInside.this, StudentTickets.class);
            ticketIntent.putExtra("eventUID", eventUID);
            startActivity(ticketIntent);
            hideTicketLoading();
        });

        evalButton.setOnClickListener(v -> {
            showEvalLoading();
            // Determine the node based on event status
            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
            eventRef.get().addOnCompleteListener(eventTask -> {
                String node = "events";
                if (!eventTask.isSuccessful() || !eventTask.getResult().exists()) {
                    node = "archive_events";
                }
                DatabaseReference finalEventRef = FirebaseDatabase.getInstance().getReference(node).child(eventUID);
                DatabaseReference eventQuestionsRef = FirebaseDatabase.getInstance().getReference("eventQuestions").child(eventUID);

                finalEventRef.child("studentsFeedback").child(studentID).get().addOnCompleteListener(feedbackTask -> {
                    boolean hasSubmittedFeedback = feedbackTask.isSuccessful() && feedbackTask.getResult().exists();

                    if (hasSubmittedFeedback) {
                        Intent viewResponseIntent = new Intent(StudentDashboardInside.this, StudentResponse.class);
                        viewResponseIntent.putExtra("eventUID", eventUID);
                        startActivity(viewResponseIntent);
                        hideEvalLoading();
                    } else {
                        eventQuestionsRef.child("isSubmitted").get().addOnCompleteListener(questionsTask -> {
                            boolean questionsAvailable = questionsTask.isSuccessful() &&
                                    questionsTask.getResult().exists() &&
                                    questionsTask.getResult().getValue(Boolean.class) == Boolean.TRUE;

                            if (questionsAvailable) {
                                Intent evalIntent = new Intent(StudentDashboardInside.this, StudentEvaluation.class);
                                evalIntent.putExtra("eventUID", eventUID);
                                startActivity(evalIntent);
                            } else {
                                Toast.makeText(StudentDashboardInside.this,
                                        "Evaluation questions not yet available", Toast.LENGTH_SHORT).show();
                            }
                            hideEvalLoading();
                        });
                    }
                });
            });
        });

        responseButton.setOnClickListener(v -> {
            showResponseLoading();
            Intent viewResponseIntent = new Intent(StudentDashboardInside.this, StudentResponse.class);
            viewResponseIntent.putExtra("eventUID", eventUID);
            startActivity(viewResponseIntent);
            hideResponseLoading();
        });
    }

    // ProgressBar show/hide methods
    private void showRegisterLoading() {
        registerProgressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        Log.d(TAG, "Showing register button loading");
    }

    private void hideRegisterLoading() {
        registerProgressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);
        registerButton.setText("Register");
        Log.d(TAG, "Hiding register button loading");
    }

    private void showTicketLoading() {
        ticketProgressBar.setVisibility(View.VISIBLE);
        ticketButton.setEnabled(false);
        ticketButton.setText("Loading...");
        Log.d(TAG, "Showing ticket button loading");
    }

    private void hideTicketLoading() {
        ticketProgressBar.setVisibility(View.GONE);
        ticketButton.setEnabled(true);
        ticketButton.setText("See Ticket");
        Log.d(TAG, "Hiding ticket button loading");
    }

    private void showEvalLoading() {
        evalProgressBar.setVisibility(View.VISIBLE);
        evalButton.setEnabled(false);
        evalButton.setText("Loading...");
        Log.d(TAG, "Showing eval button loading");
    }

    private void hideEvalLoading() {
        evalProgressBar.setVisibility(View.GONE);
        evalButton.setEnabled(true);
        evalButton.setText("Answer Evaluation");
        Log.d(TAG, "Hiding eval button loading");
    }

    private void showResponseLoading() {
        responseProgressBar.setVisibility(View.VISIBLE);
        responseButton.setEnabled(false);
        responseButton.setText("Loading...");
        Log.d(TAG, "Showing response button loading");
    }

    private void hideResponseLoading() {
        responseProgressBar.setVisibility(View.GONE);
        responseButton.setEnabled(true);
        responseButton.setText("See Response");
        Log.d(TAG, "Hiding response button loading");
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

        // First try the "events" node
        final DatabaseReference[] eventRef = {FirebaseDatabase.getInstance().getReference("events").child(eventUID)};
        eventRef[0].get().addOnCompleteListener(eventTask -> {
            if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                DataSnapshot eventSnapshot = eventTask.getResult();
                handleEventData(eventSnapshot, "events");
            } else {
                // If not found in "events", try "archive_events"
                eventRef[0] = FirebaseDatabase.getInstance().getReference("archive_events").child(eventUID);
                eventRef[0].get().addOnCompleteListener(archiveTask -> {
                    if (archiveTask.isSuccessful() && archiveTask.getResult().exists()) {
                        DataSnapshot eventSnapshot = archiveTask.getResult();
                        handleEventData(eventSnapshot, "archive_events");
                    } else {
                        Log.e(TAG, "Error fetching event data: Event not found in events or archive_events");
                    }
                });
            }
        });
    }

    private void handleEventData(DataSnapshot eventSnapshot, String node) {
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        String eventFor = eventSnapshot.child("eventFor").getValue(String.class);
        Boolean registrationAllowed = eventSnapshot.child("registrationAllowed").getValue(Boolean.class);
        String status = eventSnapshot.child("status").getValue(String.class);
        String currentEventVersion = eventSnapshot.child("version").getValue(String.class);

        // **CRITICAL LOGGING - ADD THIS**
        Log.d(TAG, "=== handleEventData DEBUG ===");
        Log.d(TAG, "registrationAllowed from Firebase: " + registrationAllowed);
        Log.d(TAG, "node: " + node);
        Log.d(TAG, "targetParticipant: " + targetParticipant);
        Log.d(TAG, "event status: " + status);

        // Check if student is eligible for this event
        if (!isStudentEligible(eventFor, studentYearLevel)) {
            hideAllButtons();
            attendanceStatusCard.setVisibility(View.GONE);
            Toast.makeText(this, "You are not eligible for this event.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Student is not eligible for this event. Hiding all buttons and attendance card.");
            return;
        }

        // Check if event is expired
        boolean isExpired = "expired".equalsIgnoreCase(status);
        if (isExpired) {
            registerButton.setVisibility(View.GONE);
            ticketButton.setVisibility(View.GONE);
            attendanceStatusCard.setVisibility(View.GONE);
            checkEvaluationStatus(node);
            Log.d(TAG, "Event is expired. Hiding registration buttons.");
            return;
        }

        // Check ticket count to determine if event is full
        studentsRef.get().addOnCompleteListener(studentsTask -> {
            if (studentsTask.isSuccessful()) {
                final int[] ticketCount = {0};
                String eventKey = eventUID;
                if (eventUID.contains("/")) {
                    String[] parts = eventUID.split("/");
                    eventKey = parts[parts.length - 1];
                }

                for (DataSnapshot studentSnapshot : studentsTask.getResult().getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            if (eventKey.equals(ticketKey) ||
                                    (ticketSnapshot.hasChild("eventUID") &&
                                            (eventUID.equals(ticketSnapshot.child("eventUID").getValue(String.class)) ||
                                                    eventKey.equals(ticketSnapshot.child("eventUID").getValue(String.class))))) {
                                ticketCount[0]++;
                            }
                        }
                    }
                }

                // **CRITICAL LOGGING - ADD THIS**
                Log.d(TAG, "=== TICKET COUNT DEBUG ===");
                Log.d(TAG, "Final ticket count: " + ticketCount[0]);
                Log.d(TAG, "registrationAllowed: " + registrationAllowed);
                Log.d(TAG, "targetParticipant value: " + targetParticipant);
                Log.d(TAG, "targetParticipant type: " + (targetParticipant != null ? targetParticipant.getClass().getSimpleName() : "null"));

                // Check if student has a ticket
                ticketRef.get().addOnCompleteListener(ticketTask -> {
                    if (ticketTask.isSuccessful()) {
                        boolean hasTicket = ticketTask.getResult().exists();

                        if (hasTicket) {
                            // Student is registered - existing logic stays the same
                            String studentTicketVersion = ticketTask.getResult().child("version").getValue(String.class);
                            if (currentEventVersion != null && !currentEventVersion.equals(studentTicketVersion)) {
                                ticketRef.removeValue().addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Old ticket version (" + studentTicketVersion + ") invalidated. Current version is " + currentEventVersion);
                                    Toast.makeText(this, "Event updated. Please re-register.", Toast.LENGTH_SHORT).show();
                                    updateButtonsForNonRegistered();
                                    attendanceStatusCard.setVisibility(View.GONE);
                                    hideRegisterLoading();
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to remove outdated ticket: " + e.getMessage());
                                    hideRegisterLoading();
                                });
                            } else {
                                Log.d(TAG, "Student is registered. Showing ticket button and attendance card.");
                                ticketButton.setText("See Ticket");
                                ticketButton.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_green));
                                ticketButton.setEnabled(true);
                                updateButtonsForRegistered();
                                attendanceStatusCard.setVisibility(View.VISIBLE);
                                fetchAttendanceData();
                            }
                        } else {
                            // **STUDENT NOT REGISTERED - CRITICAL LOGIC**
                            Log.d(TAG, "Student is NOT registered. Evaluating registration status...");

                            // **FIXED LOGIC: Only hide buttons if registrationAllowed is explicitly FALSE**
                            if (registrationAllowed != null && !registrationAllowed) {
                                Log.d(TAG, "registrationAllowed is FALSE from Firebase. Hiding registration UI.");

                                // Check if it's full (for better user message)
                                if (targetParticipant instanceof String) {
                                    try {
                                        int targetValue = Integer.parseInt((String) targetParticipant);
                                        if (ticketCount[0] >= targetValue) {
                                            Log.d(TAG, "Event is FULL (count: " + ticketCount[0] + " >= " + targetValue + ")");
                                            registerButton.setVisibility(View.GONE);
                                            ticketButton.setVisibility(View.VISIBLE);
                                            ticketButton.setText("Event Full");
                                            ticketButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                                            ticketButton.setEnabled(false);
                                            evalButton.setVisibility(View.GONE);
                                            responseButton.setVisibility(View.GONE);
                                            attendanceStatusCard.setVisibility(View.GONE);
                                            Toast.makeText(this, "The event has reached its participant limit.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.d(TAG, "Registration closed manually (not full). Hiding all buttons.");
                                            registerButton.setVisibility(View.GONE);
                                            ticketButton.setVisibility(View.GONE);
                                            evalButton.setVisibility(View.GONE);
                                            responseButton.setVisibility(View.GONE);
                                            attendanceStatusCard.setVisibility(View.GONE);
                                            Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Invalid targetParticipant format, treating as manually closed", e);
                                        registerButton.setVisibility(View.GONE);
                                        ticketButton.setVisibility(View.GONE);
                                        evalButton.setVisibility(View.GONE);
                                        responseButton.setVisibility(View.GONE);
                                        attendanceStatusCard.setVisibility(View.GONE);
                                        Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // No target participant limit, just show closed message
                                    registerButton.setVisibility(View.GONE);
                                    ticketButton.setVisibility(View.GONE);
                                    evalButton.setVisibility(View.GONE);
                                    responseButton.setVisibility(View.GONE);
                                    attendanceStatusCard.setVisibility(View.GONE);
                                    Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // **REGISTRATION IS OPEN - SHOW REGISTER BUTTON**
                                Log.d(TAG, "Registration is OPEN. Showing register button.");
                                updateButtonsForNonRegistered();
                                // Keep ticket button hidden for non-registered users
                                ticketButton.setVisibility(View.GONE);
                                evalButton.setVisibility(View.GONE);
                                responseButton.setVisibility(View.GONE);
                                attendanceStatusCard.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error checking ticket: " + (ticketTask.getException() != null ? ticketTask.getException().getMessage() : "Unknown error"));
                    }
                });
            } else {
                Log.e(TAG, "Error fetching ticket count: " + (studentsTask.getException() != null ? studentsTask.getException().getMessage() : "Unknown error"));
            }
        });
    }

    private void getTicketCount(String eventId) {
        if (eventId == null || eventId.isEmpty() || textViewParticipantCount == null) {
            Log.e(TAG, "Cannot fetch tickets: eventId is null/empty or view not found");
            if (textViewParticipantCount != null) {
                textViewParticipantCount.setText("0");
            }
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        final int[] ticketCount = {0};

        studentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ticketCount[0] = 0;

                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();

                            if (eventKey.equals(ticketKey)) {
                                ticketCount[0]++;
                                continue;
                            }

                            if (ticketSnapshot.hasChild("eventUID")) {
                                String ticketEventUID = ticketSnapshot.child("eventUID").getValue(String.class);
                                if (eventId.equals(ticketEventUID) || eventKey.equals(ticketEventUID)) {
                                    ticketCount[0]++;
                                }
                            }
                        }
                    }
                }

                // Update UI only - NO AUTO-CLOSE LOGIC HERE
                String displayText;
                if ("none".equalsIgnoreCase(String.valueOf(targetParticipant))) {
                    displayText = String.valueOf(ticketCount[0]);
                } else if (targetParticipant instanceof String) {
                    try {
                        int targetParticipantValue = Integer.parseInt((String) targetParticipant);
                        displayText = ticketCount[0] + "/" + targetParticipantValue;
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid targetParticipant format: " + targetParticipant, e);
                        displayText = String.valueOf(ticketCount[0]);
                    }
                } else {
                    displayText = String.valueOf(ticketCount[0]);
                }
                textViewParticipantCount.setText(displayText);
                Log.d(TAG, "Final ticket count: " + ticketCount[0] + ", targetParticipant: " + targetParticipant);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching tickets: " + error.getMessage());
                if (textViewParticipantCount != null) {
                    textViewParticipantCount.setText("0");
                }
            }
        });
    }

    private void getCurrentTicketCount(String eventId, TicketCountCallback callback) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int ticketCount = 0;
                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                for (DataSnapshot studentSnapshot : task.getResult().getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            if (eventKey.equals(ticketKey) ||
                                    (ticketSnapshot.hasChild("eventUID") &&
                                            (eventId.equals(ticketSnapshot.child("eventUID").getValue(String.class)) ||
                                                    eventKey.equals(ticketSnapshot.child("eventUID").getValue(String.class))))) {
                                ticketCount++;
                            }
                        }
                    }
                }
                Log.d(TAG, "Current ticket count for " + eventId + ": " + ticketCount);
                callback.onCountRetrieved(ticketCount);
            } else {
                Log.e(TAG, "Error fetching ticket count: " + task.getException());
                callback.onCountRetrieved(-1); // Error indicator
            }
        });
    }

    // Callback interface
    interface TicketCountCallback {
        void onCountRetrieved(int count);
    }

    private void closeRegistrationIfNeeded(String eventId, int currentCount) {
        if (targetParticipant instanceof String) {
            try {
                int targetParticipantValue = Integer.parseInt((String) targetParticipant);
                Log.d(TAG, "Checking registration limit: currentCount=" + currentCount + ", target=" + targetParticipantValue);

                if (currentCount >= targetParticipantValue) {
                    Log.d(TAG, "LIMIT REACHED! Attempting to close registration: " + currentCount + "/" + targetParticipantValue);

                    // Try events first
                    DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
                    eventRef.child("registrationAllowed").setValue(false)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "SUCCESS: Registration closed in 'events' - limit reached: " + currentCount + "/" + targetParticipantValue);
                                    Toast.makeText(this, "Registration closed - participant limit reached!", Toast.LENGTH_LONG).show();
                                } else {
                                    Log.e(TAG, "FAILED to close registration in 'events': " + task.getException());
                                    // Try archive_events as fallback
                                    DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventId);
                                    archiveRef.child("registrationAllowed").setValue(false)
                                            .addOnCompleteListener(archiveTask -> {
                                                if (archiveTask.isSuccessful()) {
                                                    Log.d(TAG, "SUCCESS: Registration closed in 'archive_events' - limit reached");
                                                } else {
                                                    Log.e(TAG, "FAILED to close registration in both locations: " + archiveTask.getException());
                                                }
                                            });
                                }
                            });
                } else {
                    Log.d(TAG, "Limit NOT reached: " + currentCount + " < " + targetParticipantValue + ". Registration remains open.");
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid targetParticipant format for auto-close: " + targetParticipant, e);
            }
        } else {
            Log.d(TAG, "No targetParticipant limit set. Registration remains open.");
        }
    }

    /**
     * Fetch and display attendance data
     */
    private void fetchAttendanceData() {
        if (eventUID == null || studentID == null) {
            Log.e(TAG, "Event UID or Student ID is missing for attendance check");
            updateAttendanceStatusUI("Not registered", "Not registered", "N/A", "Not Registered", R.color.red);
            return;
        }

        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        ticketRef.get().addOnCompleteListener(ticketTask -> {
            if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                DataSnapshot ticketSnapshot = ticketTask.getResult();

                // Default values
                String checkInTime = "--:--";
                String checkOutTime = "--:--";
                String currentDay = "Day 1";
                String status = "Pending";
                int statusColor = R.color.gray;

                if (ticketSnapshot.child("attendanceDays").exists()) {
                    DataSnapshot attendanceDaysSnapshot = ticketSnapshot.child("attendanceDays");

                    // Find the current day or most recently active day
                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    DataSnapshot activeDaySnapshot = null;
                    String activeDayKey = null;

                    // First, try to find today's date in attendance days
                    for (DataSnapshot daySnapshot : attendanceDaysSnapshot.getChildren()) {
                        String dayDate = daySnapshot.child("date").getValue(String.class);
                        if (currentDate.equals(dayDate)) {
                            activeDaySnapshot = daySnapshot;
                            activeDayKey = daySnapshot.getKey();
                            break;
                        }
                    }

                    // If today's date not found, look for the most recent past day
                    if (activeDaySnapshot == null) {
                        Date today = new Date();
                        Date closestDate = null;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                        for (DataSnapshot daySnapshot : attendanceDaysSnapshot.getChildren()) {
                            String dayDate = daySnapshot.child("date").getValue(String.class);
                            if (dayDate != null) {
                                try {
                                    Date snapshotDate = sdf.parse(dayDate);
                                    if (snapshotDate.before(today) &&
                                            (closestDate == null || snapshotDate.after(closestDate))) {
                                        closestDate = snapshotDate;
                                        activeDaySnapshot = daySnapshot;
                                        activeDayKey = daySnapshot.getKey();
                                    }
                                } catch (ParseException e) {
                                    Log.e(TAG, "Error parsing date: " + dayDate, e);
                                }
                            }
                        }
                    }

                    // Fallback to first day
                    if (activeDaySnapshot == null && attendanceDaysSnapshot.getChildren().iterator().hasNext()) {
                        activeDaySnapshot = attendanceDaysSnapshot.getChildren().iterator().next();
                        activeDayKey = activeDaySnapshot.getKey();
                    }

                    if (activeDayKey != null) {
                        currentDay = activeDayKey.replace("_", " ").toUpperCase().charAt(0) +
                                activeDayKey.replace("_", " ").substring(1);
                    }

                    // **GET RAW TIMES DIRECTLY FROM DATABASE**
                    // Get raw "in" time exactly as stored
                    checkInTime = activeDaySnapshot.child("in").getValue(String.class);
                    if (checkInTime == null) {
                        checkInTime = "--:--"; // Default if null
                    }

                    // Get raw "out" time exactly as stored
                    checkOutTime = activeDaySnapshot.child("out").getValue(String.class);
                    if (checkOutTime == null) {
                        checkOutTime = "--:--"; // Default if null
                    }

                    Log.d(TAG, "Raw check-in time from DB: '" + checkInTime + "'");
                    Log.d(TAG, "Raw check-out time from DB: '" + checkOutTime + "'");

                    // Determine attendance status using raw times
                    if (checkInTime != null && !checkInTime.equals("--:--") &&
                            (checkOutTime == null || checkOutTime.equals("--:--"))) {
                        status = "Ongoing";
                        statusColor = R.color.yellow;
                    } else {
                        // Fallback to database attendance status
                        String attendanceStatus = activeDaySnapshot.child("attendance").getValue(String.class);
                        if (attendanceStatus != null) {
                            switch (attendanceStatus.toLowerCase()) {
                                case "present":
                                    status = "Present";
                                    statusColor = R.color.green;
                                    break;
                                case "late":
                                    status = "Late";
                                    statusColor = R.color.yellow;
                                    break;
                                case "absent":
                                    status = "Absent";
                                    statusColor = R.color.red;
                                    break;
                                default:
                                    status = "Pending";
                                    statusColor = R.color.gray;
                                    break;
                            }
                        }
                    }
                }

                // Update UI with RAW times
                updateAttendanceStatusUI(checkInTime, checkOutTime, currentDay, status, statusColor);

            } else {
                updateAttendanceStatusUI("Not registered", "Not registered", "N/A", "Not Registered", R.color.red);
            }
        });
    }

    /**
     * Format date string to "MMM dd, yyyy" (e.g., "Apr 18, 2025")
     */

    private String formatTo12HourTime(String timeString) {
        if (timeString == null || timeString.equals("--:--") || timeString.isEmpty()) {
            return "--:--";
        }

        try {
            // Parse the input time
            SimpleDateFormat inputFormat;
            if (timeString.contains(":")) {
                if (timeString.split(":").length == 3) {
                    // Format: HH:mm:ss
                    inputFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
                } else {
                    // Format: HH:mm
                    inputFormat = new SimpleDateFormat("HH:mm", Locale.US);
                }
            } else {
                // Unknown format, return as is
                return timeString;
            }

            // Convert to Date
            Date time = inputFormat.parse(timeString);
            if (time == null) {
                return timeString;
            }

            // Format to 12-hour with AM/PM
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.US);
            return outputFormat.format(time);
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting time: " + timeString, e);
            return timeString; // Return original on error
        }
    }
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "--";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inputFormat.parse(dateString);
            if (date == null) return dateString;
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting date: " + dateString, e);
            return dateString;
        }
    }

    /**
     * Set dynamic background color for eventType based on event type
     */
    private void setEventTypeStyle(TextView eventType, ImageView badgeIcon) {
        if (eventType == null) return;
        String eventTypeString = eventType.getText().toString().toLowerCase();
        if (eventTypeString == null || eventTypeString.isEmpty()) return;

        int colorRes = R.color.event_other;
        if ("off-campus activity".equals(eventTypeString)) {
            colorRes = R.color.event_off_campus;
        } else if ("seminar".equals(eventTypeString)) {
            colorRes = R.color.event_seminar;
        } else if ("sports event".equals(eventTypeString)) {
            colorRes = R.color.event_sports;
        }

        // Get the background drawable and apply dynamic color
        Drawable background = ContextCompat.getDrawable(this, R.drawable.event_type_background);
        if (background != null) {
            background = background.mutate(); // Ensure a unique instance
            int baseColor = ContextCompat.getColor(this, colorRes);
            background.setColorFilter(baseColor, PorterDuff.Mode.SRC_IN);
            eventType.setBackground(background);

            // Calculate a darker shade (reduce brightness by 30%)
            float[] hsv = new float[3];
            Color.colorToHSV(baseColor, hsv);
            hsv[2] *= 0.7f; // Reduce value (brightness) by 30%
            int darkerColor = Color.HSVToColor(hsv);
            eventType.setTextColor(darkerColor);
        }
    }

    /**
     * Update the UI elements of the attendance status card
     */
    private void updateAttendanceStatusUI(String checkInTime, String checkOutTime, String currentDay,
                                          String status, int statusColorResId) {
        textViewCheckInTime.setText(checkInTime);      // Now shows raw DB value
        textViewCheckOutTime.setText(checkOutTime);    // Now shows raw DB value
        textViewCurrentDay.setText(currentDay);
        textViewStatus.setText(status);
        textViewStatus.setTextColor(ContextCompat.getColor(this, statusColorResId));
    }

    /**
     * Check if the student is eligible for the event based on year level
     */
    private boolean isStudentEligible(String eventFor, String studentYearLevel) {
        if (eventFor == null || studentYearLevel == null) {
            Log.d(TAG, "Eligibility check failed: eventFor or studentYearLevel is null (eventFor: " + eventFor + ", studentYearLevel: " + studentYearLevel + ")");
            return false;
        }

        // Log raw inputs
        Log.d(TAG, "Checking eligibility - Raw eventFor: " + eventFor + ", Raw studentYearLevel: " + studentYearLevel);

        // Normalize: lowercase, remove spaces, keep hyphens
        String normalizedYearLevel = studentYearLevel.toLowerCase().replaceAll("\\s+", "").trim();
        String normalizedEventFor = eventFor.toLowerCase().replaceAll("\\s+", "").trim();

        Log.d(TAG, "Normalized studentYearLevel: " + normalizedYearLevel);
        Log.d(TAG, "Normalized eventFor: " + normalizedEventFor);

        // Check if eventFor is "all"
        if (normalizedEventFor.equals("all")) {
            Log.d(TAG, "Event is for all year levels. Student is eligible.");
            return true;
        }

        // Split eventFor by commas
        String[] eventForLevels = normalizedEventFor.split(",");
        Log.d(TAG, "EventFor levels: " + Arrays.toString(eventForLevels));

        // Check if studentYearLevel is purely numeric
        boolean isNumericYearLevel = normalizedYearLevel.matches("\\d+");

        for (String level : eventForLevels) {
            String normalizedLevel = level.trim();
            Log.d(TAG, "Checking eventFor level: " + normalizedLevel);

            // Existing matching: exact match or hyphen-ignored match
            if (normalizedLevel.equals(normalizedYearLevel) ||
                    normalizedLevel.replace("-", "").equals(normalizedYearLevel.replace("-", ""))) {
                Log.d(TAG, "Student yearLevel matches eventFor level (exact or hyphen-ignored): " + normalizedLevel);
                return true;
            }

            // New numeric matching: extract numeric part from eventFor level
            if (isNumericYearLevel) {
                // Extract numeric part from normalizedLevel (e.g., "grade-7" -> "7")
                String numericPart = normalizedLevel.replaceAll("[^0-9]", "");
                if (numericPart.equals(normalizedYearLevel)) {
                    Log.d(TAG, "Student yearLevel matches numeric part of eventFor level: " + normalizedLevel + " -> " + numericPart);
                    return true;
                }
            }
        }

        Log.d(TAG, "No match found for studentYearLevel in eventFor. Student is not eligible.");
        return false;
    }

    /**
     * Update buttons for registered students
     */
    private void updateButtonsForRegistered() {
        registerButton.setVisibility(View.GONE);
        ticketButton.setVisibility(View.VISIBLE);
        evalButton.setVisibility(View.GONE);
        responseButton.setVisibility(View.GONE);
        // Show attendance card for registered students
        attendanceStatusCard.setVisibility(View.VISIBLE);
        Log.d(TAG, "Student is registered. Showing ticket button and attendance card.");
    }

    /**
     * Update buttons for non-registered students
     */
    private void updateButtonsForNonRegistered() {
        registerButton.setVisibility(View.VISIBLE);
        ticketButton.setVisibility(View.GONE);
        evalButton.setVisibility(View.GONE);
        responseButton.setVisibility(View.GONE);
        // Hide attendance card for non-registered students
        attendanceStatusCard.setVisibility(View.GONE);
        Log.d(TAG, "Student is not registered. Showing register button and hiding attendance card.");
    }

    /**
     * Hide all buttons
     */
    private void hideAllButtons() {
        registerButton.setVisibility(View.GONE);
        ticketButton.setVisibility(View.GONE);
        evalButton.setVisibility(View.GONE);
        responseButton.setVisibility(View.GONE);
        // Hide attendance card when all buttons are hidden
        attendanceStatusCard.setVisibility(View.GONE);
    }

    /**
     * Register the current student for the event
     */
    private void registerForEvent() {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            hideRegisterLoading();
            return;
        }

        final DatabaseReference[] eventRef = {FirebaseDatabase.getInstance().getReference("events").child(eventUID)};
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference("students").child(studentID).child("tickets");

        eventRef[0].child("registrationAllowed").get().addOnCompleteListener(eventTask -> {
            if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                handleRegistration(eventTask.getResult(), ticketsRef, "events");
            } else {
                // Try archive_events
                eventRef[0] = FirebaseDatabase.getInstance().getReference("archive_events").child(eventUID);
                eventRef[0].child("registrationAllowed").get().addOnCompleteListener(archiveTask -> {
                    if (archiveTask.isSuccessful() && archiveTask.getResult().exists()) {
                        handleRegistration(archiveTask.getResult(), ticketsRef, "archive_events");
                    } else {
                        Toast.makeText(this, "Error checking registration status.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Event not found in events or archive_events");
                        hideRegisterLoading();
                    }
                });
            }
        });
    }

    private void handleRegistration(DataSnapshot eventSnapshot, DatabaseReference ticketsRef, String node) {
        Boolean registrationAllowed = eventSnapshot.getValue(Boolean.class);

        if (registrationAllowed == null || !registrationAllowed) {
            hideAllButtons();
            Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
            hideRegisterLoading();
            return;
        }

        ticketsRef.get().addOnCompleteListener(ticketTask -> {
            if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                for (DataSnapshot ticketSnapshot : ticketTask.getResult().getChildren()) {
                    if (ticketSnapshot.getKey().equals(eventUID)) {
                        Toast.makeText(this, "You are already registered!", Toast.LENGTH_SHORT).show();
                        updateButtonsForRegistered();
                        hideRegisterLoading();
                        return;
                    }
                }
            }

            DatabaseReference eventForRef = FirebaseDatabase.getInstance().getReference(node).child(eventUID).child("eventFor");
            eventForRef.get().addOnCompleteListener(eventTaskFor -> {
                if (eventTaskFor.isSuccessful() && eventTaskFor.getResult().exists()) {
                    String eventFor = eventTaskFor.getResult().getValue(String.class);

                    if (isStudentEligible(eventFor, studentYearLevel)) {
                        proceedWithRegistration();
                    } else {
                        hideAllButtons();
                        Toast.makeText(this, "You are not eligible for this event.", Toast.LENGTH_SHORT).show();
                        hideRegisterLoading();
                    }
                } else {
                    Toast.makeText(this, "Error fetching event details.", Toast.LENGTH_SHORT).show();
                    hideRegisterLoading();
                }
            });
        });
    }

    /**
     * Check evaluation status and update UI accordingly
     */
    private void checkEvaluationStatus(String node) {
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference(node).child(eventUID);
        DatabaseReference eventQuestionsRef = FirebaseDatabase.getInstance().getReference("eventQuestions").child(eventUID);
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        ticketRef.get().addOnCompleteListener(ticketTask -> {
            boolean isRegistered = ticketTask.isSuccessful() && ticketTask.getResult().exists();

            if (!isRegistered) {
                evalButton.setVisibility(View.VISIBLE);
                evalButton.setText("Not Registered");
                evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                evalButton.setEnabled(false);
                responseButton.setVisibility(View.GONE);
                attendanceStatusCard.setVisibility(View.GONE);
                Log.d(TAG, "Student not registered for this event. Showing 'Not Registered' message and hiding attendance card.");
                return;
            }

            attendanceStatusCard.setVisibility(View.VISIBLE);
            fetchAttendanceData();

            ticketRef.child("attendanceDays").get().addOnCompleteListener(attendanceTask -> {
                if (attendanceTask.isSuccessful() && attendanceTask.getResult().exists()) {
                    DataSnapshot attendanceDays = attendanceTask.getResult();

                    String attendanceStatus = "Absent";
                    boolean hasValidAttendance = false;
                    boolean hasAttendedAnyDay = false;

                    for (DataSnapshot daySnapshot : attendanceDays.getChildren()) {
                        String status = daySnapshot.child("attendance").getValue(String.class);
                        if (status != null) {
                            if ("Present".equals(status) || "Late".equals(status)) {
                                hasValidAttendance = true;
                                attendanceStatus = status;
                            }
                            if (!"Absent".equals(status)) {
                                hasAttendedAnyDay = true;
                            }
                        }
                    }

                    if (!hasValidAttendance) {
                        evalButton.setVisibility(View.VISIBLE);
                        evalButton.setText("No Evaluation");
                        evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                        evalButton.setEnabled(false);
                        responseButton.setVisibility(View.GONE);
                        Log.d(TAG, "Student has no valid attendance (not Present/Late). Showing 'No Evaluation'.");
                        return;
                    }

                    eventRef.get().addOnCompleteListener(eventTask -> {
                        if (eventTask.isSuccessful()) {
                            DataSnapshot eventSnapshot = eventTask.getResult();

                            boolean feedbackSubmitted;
                            if (eventSnapshot.child("studentsFeedback").exists()) {
                                feedbackSubmitted = eventSnapshot.child("studentsFeedback").hasChild(studentID);
                            } else {
                                feedbackSubmitted = false;
                            }

                            eventQuestionsRef.get().addOnCompleteListener(questionsTask -> {
                                if (questionsTask.isSuccessful()) {
                                    DataSnapshot questionsSnapshot = questionsTask.getResult();
                                    Boolean isSubmitted = questionsSnapshot.child("isSubmitted").getValue(Boolean.class);

                                    evalButton.setVisibility(View.VISIBLE);

                                    if (feedbackSubmitted) {
                                        evalButton.setText("View Response");
                                        evalButton.setBackgroundColor(getResources().getColor(R.color.green));
                                        evalButton.setEnabled(true);
                                        responseButton.setVisibility(View.VISIBLE);
                                        responseButton.setEnabled(true);
                                        Log.d(TAG, "User has submitted feedback. Showing View Response and See Response buttons.");
                                    } else if (isSubmitted != null && isSubmitted) {
                                        evalButton.setText("Answer Evaluation");
                                        evalButton.setBackgroundColor(getResources().getColor(R.color.bg_green));
                                        evalButton.setEnabled(true);
                                        responseButton.setVisibility(View.GONE);
                                        Log.d(TAG, "Event has ended. Evaluation not submitted yet. Showing Evaluate button.");
                                    } else {
                                        evalButton.setText("Event Ended");
                                        evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                                        evalButton.setEnabled(false);
                                        responseButton.setVisibility(View.GONE);
                                        Log.d(TAG, "Event has ended. No evaluation available. Showing red Event Ended indicator.");
                                    }
                                } else {
                                    Log.e(TAG, "Error checking eventQuestions: " +
                                            (questionsTask.getException() != null ? questionsTask.getException().getMessage() : "Unknown error"));
                                    evalButton.setVisibility(View.VISIBLE);
                                    evalButton.setText("Event Ended");
                                    evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                                    evalButton.setEnabled(false);
                                    responseButton.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            Log.e(TAG, "Error checking event data: " +
                                    (eventTask.getException() != null ? eventTask.getException().getMessage() : "Unknown error"));
                            evalButton.setVisibility(View.VISIBLE);
                            evalButton.setText("Event Ended");
                            evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                            evalButton.setEnabled(false);
                            responseButton.setVisibility(View.GONE);
                        }
                    });
                } else {
                    evalButton.setVisibility(View.VISIBLE);
                    evalButton.setText("No Attendance Data");
                    evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                    evalButton.setEnabled(false);
                    responseButton.setVisibility(View.GONE);
                    Log.e(TAG, "No attendance days data found for this ticket");
                }
            });
        });
    }

    /**
     * Proceed with actual registration after all checks
     */
    private DatabaseReference getEventRef(String eventId, String node) {
        return FirebaseDatabase.getInstance().getReference(node).child(eventId);
    }

    private void proceedWithRegistration() {
        DatabaseReference eventRef = getEventRef(eventUID, "events");
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        ticketRef.get().addOnCompleteListener(ticketTask -> {
            if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                Toast.makeText(this, "You are already registered for this event!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Student already registered for event: " + eventUID);
                updateButtonsForRegistered();
                hideRegisterLoading();
                return;
            }

            getCurrentTicketCount(eventUID, currentCount -> {
                if (currentCount == -1) {
                    Toast.makeText(this, "Error checking participant limit", Toast.LENGTH_SHORT).show();
                    hideRegisterLoading();
                    return;
                }

                Log.d(TAG, "Pre-registration check - Current tickets: " + currentCount + ", Target: " + targetParticipant);

                if (targetParticipant instanceof String) {
                    try {
                        int targetParticipantValue = Integer.parseInt((String) targetParticipant);
                        if (currentCount >= targetParticipantValue) {
                            Log.d(TAG, "Event already full before registration: " + currentCount + "/" + targetParticipantValue);
                            Toast.makeText(this, "Event is full! Registration closed.", Toast.LENGTH_SHORT).show();
                            registerButton.setEnabled(false);
                            updateButtonsForNonRegistered();
                            hideRegisterLoading();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid targetParticipant format", e);
                    }
                }

                // Try events first, then archive_events
                eventRef.get().addOnCompleteListener(eventTask -> {
                    if (!eventTask.isSuccessful() || !eventTask.getResult().exists()) {
                        DatabaseReference archiveRef = getEventRef(eventUID, "archive_events");
                        archiveRef.get().addOnCompleteListener(archiveTask -> {
                            if (archiveTask.isSuccessful() && archiveTask.getResult().exists()) {
                                handleRegistrationDataWithLimitCheck(archiveTask.getResult(), currentCount);
                            } else {
                                Toast.makeText(this, "Failed to fetch event details.", Toast.LENGTH_SHORT).show();
                                hideRegisterLoading();
                            }
                        });
                    } else {
                        handleRegistrationDataWithLimitCheck(eventTask.getResult(), currentCount);
                    }
                });
            });
        });
    }

    private void handleRegistrationDataWithLimitCheck(DataSnapshot eventSnapshot, int currentCountBeforeRegistration) {
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
            dayData.put("attendance", "pending");
            attendanceDays.put("day_1", dayData);
            ticketData.put("attendanceDays", attendanceDays);
        }

        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        Log.d(TAG, "Creating ticket. Current count BEFORE registration: " + currentCountBeforeRegistration);

        ticketRef.setValue(ticketData)
                .addOnSuccessListener(aVoid -> {
                    // **FIX: New total = count BEFORE + 1 (this registration)**
                    int newTotalCount = currentCountBeforeRegistration + 1;
                    Log.d(TAG, "Ticket created! New total count AFTER registration: " + newTotalCount);

                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Event registered at " + formattedTimestamp);

                    // Check if THIS registration filled the limit
                    closeRegistrationIfNeeded(eventUID, newTotalCount);

                    generateAndUploadQRCode();
                    updateButtonsForRegistered();

                    // **TRIGGER UI UPDATE** - This will refresh the registration status
                    checkRegistrationStatus();
                    hideRegisterLoading();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error registering event: " + e.getMessage());
                    hideRegisterLoading();
                });
    }

    private void logRegistrationStatus(String eventId) {
        // Check events node
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
        eventRef.child("registrationAllowed").get().addOnCompleteListener(task -> {
            Boolean allowed = task.getResult().getValue(Boolean.class);
            Log.d(TAG, "events node registrationAllowed: " + allowed);
        });

        // Check archive_events node
        DatabaseReference archiveRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventId);
        archiveRef.child("registrationAllowed").get().addOnCompleteListener(task -> {
            Boolean allowed = task.getResult().getValue(Boolean.class);
            Log.d(TAG, "archive_events node registrationAllowed: " + allowed);
        });
    }

    private void handleRegistrationData(DataSnapshot eventSnapshot) {
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
            dayData.put("attendance", "pending");
            attendanceDays.put("day_1", dayData);
            ticketData.put("attendanceDays", attendanceDays);
        }

        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        ticketRef.setValue(ticketData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Event registered at " + formattedTimestamp);
                    generateAndUploadQRCode();
                    updateButtonsForRegistered();
                    hideRegisterLoading();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error registering event: " + e.getMessage());
                    hideRegisterLoading();
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
     * Generate QR code for the ticket and upload to Firebase with retry mechanism
     */
    private void generateAndUploadQRCode() {
        final int MAX_RETRIES = 3;
        final long RETRY_DELAY_MS = 2000; // 2 seconds delay between retries

        class QRCodeRetryHandler {
            private int retryCount = 0;

            void attemptQRCodeGeneration() {
                QrCodeGenerator.generateQRCodeWithEventAndStudentInfo(StudentDashboardInside.this, eventUID, new QrCodeGenerator.OnQRCodeGeneratedListener() {
                    @Override
                    public void onQRCodeGenerated(Bitmap qrCodeBitmap) {
                        Log.d(TAG, "QR Code successfully generated.");
                        qrCodeGenerationFailed = false; // Reset failure flag
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
                                    qrCodeGenerationFailed = false; // Reset failure flag
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(StudentDashboardInside.this, "Failed to save QR Code URL!", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error saving QR Code URL: " + e.getMessage());
                                    qrCodeGenerationFailed = true; // Set failure flag
                                });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        retryCount++;
                        Log.e(TAG, "QR Code Generation Error (Attempt " + retryCount + "/" + MAX_RETRIES + "): " + errorMessage);
                        if (retryCount < MAX_RETRIES) {
                            Log.d(TAG, "Retrying QR code generation in " + RETRY_DELAY_MS + "ms...");
                            new Handler(Looper.getMainLooper()).postDelayed(this::attemptQRCodeGeneration, RETRY_DELAY_MS);
                        } else {
                            Toast.makeText(StudentDashboardInside.this, "QR Code Generation Failed after " + MAX_RETRIES + " attempts: " + errorMessage, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Max retries reached for QR Code Generation");
                            qrCodeGenerationFailed = true; // Set failure flag
                        }
                    }

                    @Override
                    public void attemptQRCodeGeneration() {
                        
                    }
                });
            }
        }

        new QRCodeRetryHandler().attemptQRCodeGeneration();
    }

    /**
     * Returns the current timestamp in the format MM/dd/yyyy hh:mm a (e.g., 03/14/2025 5:00 AM)
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void getTargetParticipant(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Cannot fetch target participant: eventId is null or empty");
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
        eventRef.child("targetParticipant").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String value = snapshot.getValue(String.class);
                    handleTargetParticipant(value);
                } else {
                    // Try archive_events
                    DatabaseReference archiveEventRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventId);
                    archiveEventRef.child("targetParticipant").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot archiveSnapshot) {
                            String value = archiveSnapshot.exists() ? archiveSnapshot.getValue(String.class) : null;
                            handleTargetParticipant(value);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fetching target participant from archive_events: " + error.getMessage());
                            targetParticipant = null;
                            getTicketCount(eventId);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching target participant from events: " + error.getMessage());
                // Try archive_events as fallback
                DatabaseReference archiveEventRef = FirebaseDatabase.getInstance().getReference("archive_events").child(eventId);
                archiveEventRef.child("targetParticipant").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot archiveSnapshot) {
                        String value = archiveSnapshot.exists() ? archiveSnapshot.getValue(String.class) : null;
                        handleTargetParticipant(value);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching target participant from archive_events: " + error.getMessage());
                        targetParticipant = null;
                        getTicketCount(eventId);
                    }
                });
            }
        });
    }

    private void handleTargetParticipant(String value) {
        if (value != null && !value.isEmpty()) {
            if ("none".equalsIgnoreCase(value)) {
                targetParticipant = "none";
                Log.d(TAG, "Fetched targetParticipant: none");
            } else {
                targetParticipant = value;
                Log.d(TAG, "Fetched targetParticipant: " + value);
            }
        } else {
            targetParticipant = null;
            Log.d(TAG, "Invalid targetParticipant: null or empty");
        }
        getTicketCount(eventUID);
    }
}