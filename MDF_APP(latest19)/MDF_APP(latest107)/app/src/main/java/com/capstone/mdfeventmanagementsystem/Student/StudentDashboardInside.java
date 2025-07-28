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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

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
    private Button registerButton, ticketButton, evalButton;
    private Object targetParticipant = null;
    private TextView textViewParticipantCount;

    private FirebaseAuth mAuth;
    private String eventUID;
    private String studentID;
    private String studentYearLevel;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard_inside);

        // Initialize UI elements
        initializeViews();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initially hide attendance status card
        attendanceStatusCard.setVisibility(View.GONE);

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
        textViewParticipantCount = findViewById(R.id.textViewParticipantCount);
        attendanceStatusCard = findViewById(R.id.attendanceStatusCard);
        textViewCheckInTime = findViewById(R.id.textViewCheckInTime);
        textViewCheckOutTime = findViewById(R.id.textViewCheckOutTime);
        textViewCurrentDay = findViewById(R.id.textViewCurrentDay);
        textViewStatus = findViewById(R.id.textViewStatus);
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
            if (!isStudentEligible(eventForValue, studentYearLevel)) {
                Log.d(TAG, "Student yearLevel does not match eventFor. Disabling register button.");
                registerButton.setEnabled(false);
                registerButton.setAlpha(0.5f); // Dim the button
                Toast.makeText(this, "This event is not for your year level.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Student yearLevel matches eventFor or eventFor is 'All'. Register button enabled.");
            }
        } else {
            Log.d(TAG, "studentYearLevel or eventForValue is null. Disabling register button.");
            registerButton.setEnabled(false);
            registerButton.setAlpha(0.5f);
            Toast.makeText(this, "Unable to verify eligibility.", Toast.LENGTH_LONG).show();
        }

        // Fetch target participant and ticket count
        getTargetParticipant(eventUID);
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
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        eventRef.get().addOnCompleteListener(eventTask -> {
            if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                DataSnapshot eventSnapshot = eventTask.getResult();

                String eventFor = eventSnapshot.child("eventFor").getValue(String.class);
                Boolean registrationAllowed = eventSnapshot.child("registrationAllowed").getValue(Boolean.class);
                String status = eventSnapshot.child("status").getValue(String.class);
                String currentEventVersion = eventSnapshot.child("version").getValue(String.class);

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
                    checkEvaluationStatus();
                    return;
                }

                // Check ticket count to determine if event is full
                studentsRef.get().addOnCompleteListener(studentsTask -> {
                    if (studentsTask.isSuccessful()) {
                        final int[] ticketCount = {0}; // Use array to allow modification in lambda
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
                        Log.d(TAG, "Ticket count for event " + eventUID + ": " + ticketCount[0]);

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
                                            attendanceStatusCard.setVisibility(View.GONE);
                                        }).addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to remove outdated ticket: " + e.getMessage());
                                        });
                                    } else {
                                        // Valid ticket
                                        Log.d(TAG, "Ticket is valid for current version: " + currentEventVersion);
                                        ticketButton.setText("See Ticket");
                                        ticketButton.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_green));
                                        ticketButton.setEnabled(true);
                                        updateButtonsForRegistered();
                                        attendanceStatusCard.setVisibility(View.VISIBLE);
                                        fetchAttendanceData();
                                    }
                                } else {
                                    // No ticket
                                    if (registrationAllowed == null || !registrationAllowed) {
                                        // Registration is closed, check if event is full
                                        if (targetParticipant instanceof Long && ticketCount[0] >= (Long) targetParticipant) {
                                            // Event is full
                                            registerButton.setVisibility(View.GONE);
                                            ticketButton.setVisibility(View.VISIBLE);
                                            ticketButton.setText("Event Full");
                                            ticketButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                                            ticketButton.setEnabled(false);
                                            evalButton.setVisibility(View.GONE);
                                            attendanceStatusCard.setVisibility(View.GONE);
                                            Toast.makeText(this, "The event has reached its participant limit.", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Event is full (ticketCount: " + ticketCount[0] + ", targetParticipant: " + targetParticipant + "). Showing 'Event Full' on ticket button.");
                                        } else {
                                            // Registration closed but not full
                                            registerButton.setVisibility(View.GONE);
                                            ticketButton.setVisibility(View.GONE);
                                            evalButton.setVisibility(View.GONE);
                                            attendanceStatusCard.setVisibility(View.GONE);
                                            Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Registration is closed but event is not full (ticketCount: " + ticketCount[0] + "). Hiding all buttons.");
                                        }
                                    } else {
                                        // Registration is open
                                        updateButtonsForNonRegistered();
                                        ticketButton.setText("See Ticket");
                                        ticketButton.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_green));
                                        ticketButton.setEnabled(true);
                                        attendanceStatusCard.setVisibility(View.GONE);
                                    }
                                }
                            } else {
                                Log.e(TAG, "Error checking ticket: " +
                                        (ticketTask.getException() != null ? ticketTask.getException().getMessage() : "Unknown error"));
                            }
                        });
                    } else {
                        Log.e(TAG, "Error fetching ticket count: " +
                                (studentsTask.getException() != null ? studentsTask.getException().getMessage() : "Unknown error"));
                    }
                });
            } else {
                Log.e(TAG, "Error fetching event data: " +
                        (eventTask.getException() != null ? eventTask.getException().getMessage() : "Unknown error"));
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

        final int[] ticketCount = {0}; // Use array to allow modification in lambda

        studentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ticketCount[0] = 0; // Reset count

                // Extract the event key from the full path
                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                Log.d(TAG, "Searching for tickets matching event key: " + eventKey);

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            Log.d(TAG, "Checking ticket: " + ticketKey);

                            // Method 1: Check if ticket key matches the event key
                            if (eventKey.equals(ticketKey)) {
                                ticketCount[0]++;
                                Log.d(TAG, "Found matching ticket by key: " + ticketKey);
                                continue;
                            }

                            // Method 2: Look for an eventUID field in the ticket
                            if (ticketSnapshot.hasChild("eventUID")) {
                                String ticketEventUID = ticketSnapshot.child("eventUID").getValue(String.class);
                                if (eventId.equals(ticketEventUID) || eventKey.equals(ticketEventUID)) {
                                    ticketCount[0]++;
                                    Log.d(TAG, "Found matching ticket by eventUID field: " + ticketEventUID);
                                }
                            }
                        }
                    }
                }

                // Update the UI with the count
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

                // Auto-close registration if targetParticipant is reached
                if (targetParticipant instanceof String) {
                    try {
                        int targetParticipantValue = Integer.parseInt((String) targetParticipant);
                        if (ticketCount[0] >= targetParticipantValue) {
                            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
                            eventRef.child("registrationAllowed").setValue(false)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Registration auto-closed as ticket count reached target: " + ticketCount[0] + "/" + targetParticipant);
                                            Toast.makeText(StudentDashboardInside.this,
                                                    "Registration closed automatically as participant limit reached",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e(TAG, "Failed to auto-close registration: " + task.getException());
                                        }
                                    });
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid targetParticipant format for auto-close: " + targetParticipant, e);
                    }
                }
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
                String currentDay = "Day 1"; // Default to Day 1
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

                    // If today's date not found, look for the most recent past day with attendance data
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

                    // If still no match, just use the first day as fallback
                    if (activeDaySnapshot == null && attendanceDaysSnapshot.getChildren().iterator().hasNext()) {
                        activeDaySnapshot = attendanceDaysSnapshot.getChildren().iterator().next();
                        activeDayKey = activeDaySnapshot.getKey();
                    }

                    // Get the actual day from the key (e.g., "day_2" -> "Day 2")
                    if (activeDayKey != null) {
                        currentDay = activeDayKey.replace("_", " ").toUpperCase().charAt(0) + activeDayKey.replace("_", " ").substring(1);
                    }

                    // Get check-in and check-out times and convert to 12-hour format with AM/PM
                    if (activeDaySnapshot.child("in").exists()) {
                        String rawCheckInTime = activeDaySnapshot.child("in").getValue(String.class);
                        checkInTime = formatTo12HourTime(rawCheckInTime);
                    }

                    if (activeDaySnapshot.child("out").exists()) {
                        String rawCheckOutTime = activeDaySnapshot.child("out").getValue(String.class);
                        checkOutTime = formatTo12HourTime(rawCheckOutTime);
                    }

                    // Determine attendance status
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
                            case "pending":
                                // If check-in exists but not check-out, status is Ongoing
                                if (!checkInTime.equals("--:--") && checkOutTime.equals("--:--")) {
                                    status = "Ongoing";
                                    statusColor = R.color.blue;
                                } else {
                                    status = "Pending";
                                    statusColor = R.color.gray;
                                }
                                break;
                            default:
                                status = "Pending";
                                statusColor = R.color.gray;
                                break;
                        }
                    }
                }

                // Update UI with attendance info
                updateAttendanceStatusUI(checkInTime, checkOutTime, currentDay, status, statusColor);

            } else {
                // No ticket found - show default "Not Registered" status
                updateAttendanceStatusUI("Not registered", "Not registered", "N/A", "Not Registered", R.color.red);
            }
        });
    }

    /**
     * Format time string to 12-hour format with AM/PM
     * @param timeString Input time string (expected format: "HH:mm" or "HH:mm:ss")
     * @return Formatted time string in "hh:mm a" format (e.g., "09:30 AM")
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

    /**
     * Update the UI elements of the attendance status card
     */
    private void updateAttendanceStatusUI(String checkInTime, String checkOutTime, String currentDay,
                                          String status, int statusColorResId) {
        textViewCheckInTime.setText(checkInTime);
        textViewCheckOutTime.setText(checkOutTime);
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

        // Split eventFor by commas and check each level
        String[] eventForLevels = normalizedEventFor.split(",");
        Log.d(TAG, "EventFor levels: " + Arrays.toString(eventForLevels));

        for (String level : eventForLevels) {
            String normalizedLevel = level.trim();
            Log.d(TAG, "Checking eventFor level: " + normalizedLevel);

            // Flexible matching: check exact match or numeric part (e.g., "grade11" vs "grade-11")
            if (normalizedLevel.equals(normalizedYearLevel) ||
                    normalizedLevel.replace("-", "").equals(normalizedYearLevel.replace("-", ""))) {
                Log.d(TAG, "Student yearLevel matches eventFor level: " + normalizedLevel);
                return true;
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
        // Hide attendance card when all buttons are hidden
        attendanceStatusCard.setVisibility(View.GONE);
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

            // First check if the event has valid attendance data
            ticketRef.child("attendanceDays").get().addOnCompleteListener(attendanceTask -> {
                if (attendanceTask.isSuccessful() && attendanceTask.getResult().exists()) {
                    DataSnapshot attendanceDays = attendanceTask.getResult();

                    // Check attendance status from the ticket data
                    String attendanceStatus = "Absent"; // Default status changed from "pending" to "Absent"

                    // For multi-day events, we need to check the attendance of each day
                    // For simplicity, if ANY day has "Present" or "Late", we'll allow evaluation
                    boolean hasValidAttendance = false;
                    boolean hasAttendedAnyDay = false;

                    for (DataSnapshot daySnapshot : attendanceDays.getChildren()) {
                        String status = daySnapshot.child("attendance").getValue(String.class);

                        if (status != null) {
                            // If any day has Present or Late status, student can evaluate
                            if ("Present".equals(status) || "Late".equals(status)) {
                                hasValidAttendance = true;
                                attendanceStatus = status; // Store the most recent valid status
                            }

                            // Student has attended at least one day (not just Absent)
                            if (!"Absent".equals(status)) {
                                hasAttendedAnyDay = true;
                            }
                        }
                    }

                    if (!hasValidAttendance) {
                        // No valid attendance record found - show No Evaluation instead of No Appearance
                        evalButton.setVisibility(View.VISIBLE);
                        evalButton.setText("No Evaluation");
                        evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                        evalButton.setEnabled(false); // Disable the button
                        Log.d(TAG, "Student has no valid attendance (not Present/Late). Showing 'No Evaluation'.");
                        return;
                    }

                    // Student has valid attendance, now check feedback and evaluation status
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
                                        evalButton.setEnabled(true);
                                        Log.d(TAG, "User has submitted feedback. Showing View Response button.");
                                    } else if (isSubmitted != null && isSubmitted) {
                                        // Event has questions ready for evaluation but user hasn't submitted
                                        evalButton.setText("ANSWER EVALUATION");
                                        evalButton.setBackgroundColor(getResources().getColor(R.color.bg_green));
                                        evalButton.setEnabled(true);
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
                } else {
                    // No attendance data found
                    evalButton.setVisibility(View.VISIBLE);
                    evalButton.setText("No Attendance Data");
                    evalButton.setBackgroundColor(getResources().getColor(R.color.red));
                    evalButton.setEnabled(false);
                    Log.e(TAG, "No attendance days data found for this ticket");
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
                    if (value != null && !value.isEmpty()) {
                        if ("none".equalsIgnoreCase(value)) {
                            targetParticipant = "none";
                            Log.d(TAG, "Fetched targetParticipant: none");
                        } else {
                            targetParticipant = value; // Store as String
                            Log.d(TAG, "Fetched targetParticipant: " + value);
                        }
                    } else {
                        targetParticipant = null;
                        Log.d(TAG, "Invalid targetParticipant: null or empty");
                    }
                } else {
                    targetParticipant = null;
                    Log.d(TAG, "No targetParticipant found for event: " + eventId);
                }
                // Update ticket count display
                getTicketCount(eventId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching target participant: " + error.getMessage());
                targetParticipant = null;
                // Update ticket count display without targetParticipant
                getTicketCount(eventId);
            }
        });
    }
}