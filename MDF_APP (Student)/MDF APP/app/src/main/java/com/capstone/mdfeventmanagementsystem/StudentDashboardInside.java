package com.capstone.mdfeventmanagementsystem;

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

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime;
    private ImageView eventImage;
    private Button registerButton, ticketButton;

    private FirebaseAuth mAuth;
    private DatabaseReference studentTicketsRef;
    private String eventUID; // Store event UID as a variable
    private String studentID; // Store the student ID retrieved from SharedPreferences

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard_inside);

        // Initialize UI elements
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

        // ✅ Add new TextViews for eventType and eventFor
        TextView eventType = findViewById(R.id.eventType);
        TextView eventFor = findViewById(R.id.eventFor);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Retrieve studentID from UserSession SharedPreferences (not MyAppPrefs)
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        studentID = sharedPreferences.getString("studentID", null);

        if (studentID == null || studentID.isEmpty()) {
            Toast.makeText(this, "Student ID not found! Please log in again.", Toast.LENGTH_SHORT).show();
            Log.e("TestApp", "No studentID found in SharedPreferences!");
            // Redirect back to login
            Intent intent = new Intent(StudentDashboardInside.this, StudentLogin.class);
            startActivity(intent);
            finish();
            return;
        }

        Log.d("TestApp", "Using studentID from SharedPreferences: " + studentID);

        // ✅ Get event details from intent and display them
        Intent intent = getIntent();
        eventUID = intent.getStringExtra("eventUID");
        eventName.setText(intent.getStringExtra("eventName"));
        eventDescription.setText(intent.getStringExtra("eventDescription"));
        startDate.setText(intent.getStringExtra("startDate"));
        endDate.setText(intent.getStringExtra("endDate"));
        startTime.setText(intent.getStringExtra("startTime"));
        endTime.setText(intent.getStringExtra("endTime"));
        venue.setText(intent.getStringExtra("venue"));
        eventSpan.setText(intent.getStringExtra("eventSpan"));
        graceTime.setText(intent.getStringExtra("graceTime"));

        // ✅ Display the new fields: eventType and eventFor
        eventType.setText(intent.getStringExtra("eventType"));
        eventFor.setText(intent.getStringExtra("eventFor"));

        Log.d("TestApp", "Received Event UID: " + eventUID);

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

        // Register button click
        registerButton.setOnClickListener(v -> registerForEvent());

        // Ticket button click
        ticketButton.setOnClickListener(v -> {
            // Implement navigation to ticket details
            Intent ticketIntent = new Intent(StudentDashboardInside.this, StudentTickets.class);
            ticketIntent.putExtra("eventUID", eventUID);
            startActivity(ticketIntent);
        });
    }

    private void registerForEvent() {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("students").child(studentID);
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference("students").child(studentID).child("tickets");

        Log.d("TestApp", "Checking if student is already registered for event: " + eventUID);

        // Check if student already has a ticket
        ticketsRef.get().addOnCompleteListener(ticketTask -> {
            if (ticketTask.isSuccessful() && ticketTask.getResult().exists()) {
                for (DataSnapshot ticketSnapshot : ticketTask.getResult().getChildren()) {
                    Log.d("TestApp", "Checking ticket: " + ticketSnapshot.getKey());
                    if (ticketSnapshot.getKey().equals(eventUID)) {
                        Toast.makeText(this, "You are already registered for this event!", Toast.LENGTH_SHORT).show();
                        Log.d("TestApp", "Student already registered for event: " + eventUID);
                        updateButtonState(true);
                        return;
                    }
                }
            }

            Log.d("TestApp", "Student is not registered, proceeding with eligibility check.");

            // Fetch student's yearLevel if not already registered
            studentRef.child("yearLevel").get().addOnCompleteListener(studentTask -> {
                if (studentTask.isSuccessful() && studentTask.getResult().exists()) {
                    String studentYearLevel = studentTask.getResult().getValue(String.class);
                    Log.d("TestApp", "Student Year Level: " + studentYearLevel);

                    // Fetch event's "eventFor" field
                    eventRef.child("eventFor").get().addOnCompleteListener(eventTask -> {
                        if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                            String eventFor = eventTask.getResult().getValue(String.class);
                            Log.d("TestApp", "Event For: " + eventFor);

                            // Normalize and compare values
                            String normalizedEventFor = eventFor.replace("-", " ").trim();
                            String normalizedYearLevel = studentYearLevel.trim();

                            if (normalizedEventFor.equalsIgnoreCase("All") || normalizedEventFor.equalsIgnoreCase(normalizedYearLevel)) {
                                Log.d("TestApp", "Student is eligible for registration.");
                                registerButton.setEnabled(true); // Enable the button if eligible
                                proceedWithRegistration();
                            } else {
                                registerButton.setEnabled(false); // Disable the button if not eligible
                                Toast.makeText(this, "You are not eligible for this event.", Toast.LENGTH_SHORT).show();
                                Log.d("TestApp", "Student year level does not match eventFor.");
                            }
                        } else {
                            Toast.makeText(this, "Failed to fetch event details.", Toast.LENGTH_SHORT).show();
                            Log.e("TestApp", "Error fetching eventFor: " + eventTask.getException().getMessage());
                        }
                    });
                } else {
                    Toast.makeText(this, "Failed to fetch student details.", Toast.LENGTH_SHORT).show();
                    Log.e("TestApp", "Error fetching student yearLevel: " + studentTask.getException().getMessage());
                }
            });
        });
    }

    // ✅ Function to proceed with registration if section matches
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
                Log.d("TestApp", "Student already registered for event: " + eventUID);
                updateButtonState(true);
            } else {
                // Get event details to determine if it's a multi-day event
                eventRef.get().addOnCompleteListener(eventTask -> {
                    if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                        DataSnapshot eventSnapshot = eventTask.getResult();
                        String eventSpanValue = eventSnapshot.child("eventSpan").getValue(String.class);
                        String startDateValue = eventSnapshot.child("startDate").getValue(String.class);
                        String endDateValue = eventSnapshot.child("endDate").getValue(String.class);

                        long currentTimeMillis = System.currentTimeMillis();
                        String formattedTimestamp = getCurrentTimestamp();

                        HashMap<String, Object> ticketData = new HashMap<>();
                        ticketData.put("registeredAt", formattedTimestamp);
                        ticketData.put("timestampMillis", currentTimeMillis);

                        // We no longer add the global status field
                        // REMOVED: ticketData.put("status", "pending");

                        // Check if it's a multi-day event
                        boolean isMultiDay = "multi-day".equals(eventSpanValue);
                        if (isMultiDay && startDateValue != null && endDateValue != null) {
                            // Create attendance records for each day of the multi-day event
                            // Each day will have its own date and status
                            Log.d("TestApp", "Creating attendance days for multi-day event");
                            HashMap<String, Object> attendanceDays = createAttendanceDaysForDateRange(startDateValue, endDateValue);
                            ticketData.put("attendanceDays", attendanceDays);
                        } else {
                            // For single-day events, still create one day with status
                            HashMap<String, Object> attendanceDays = new HashMap<>();
                            HashMap<String, Object> dayData = new HashMap<>();
                            dayData.put("date", startDateValue);
                            dayData.put("status", "pending");
                            attendanceDays.put("day_1", dayData);
                            ticketData.put("attendanceDays", attendanceDays);
                        }

                        // Register with the prepared data
                        ticketRef.setValue(ticketData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                                    Log.d("TestApp", "Event registered at " + formattedTimestamp);
                                    generateAndUploadQRCode();
                                    updateButtonState(true);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("TestApp", "Error registering event: " + e.getMessage());
                                });
                    } else {
                        Toast.makeText(this, "Failed to fetch event details.", Toast.LENGTH_SHORT).show();
                        Log.e("TestApp", "Error fetching event details: " +
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
                Log.e("TestApp", "Invalid date format for start or end date");
                return attendanceDays;
            }

            // Calculate number of days between start and end date (inclusive)
            long diffInMillis = endDate.getTime() - startDate.getTime();
            long dayCount = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS) + 1;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            // Create an entry for each day in the range with date and status
            for (int i = 1; i <= dayCount; i++) {
                String currentDateStr = dateFormat.format(calendar.getTime());

                // Create a HashMap for each day containing both date and status
                HashMap<String, Object> dayData = new HashMap<>();
                dayData.put("date", currentDateStr);
                dayData.put("status", "pending"); // Initialize status as pending for each day

                // Add this day's data to the attendanceDays map
                attendanceDays.put("day_" + i, dayData);

                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            Log.d("TestApp", "Created attendance days with status: " + attendanceDays.toString());
        } catch (ParseException e) {
            Log.e("TestApp", "Error parsing dates: " + e.getMessage());
        }

        return attendanceDays;
    }
    private void checkRegistrationStatus() {
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentID)
                .child("tickets")
                .child(eventUID);

        Log.d("TestApp", "Checking registration status for event: " + eventUID);

        ticketRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    Log.d("TestApp", "Student is already registered, updating button to SEE TICKET.");
                    updateButtonState(true);
                } else {
                    Log.d("TestApp", "Student is not registered, showing Register button.");
                    updateButtonState(false);
                }
            } else {
                Log.e("TestApp", "Error checking registration status: " + task.getException().getMessage());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkRegistrationStatus(); // Automatically check registration status when the activity starts
    }

    private void updateButtonState(boolean isRegistered) {
        Log.d("TestApp", "Updating button state: isRegistered = " + isRegistered);
        if (isRegistered) {
            registerButton.setVisibility(View.GONE);
            ticketButton.setVisibility(View.VISIBLE);
            ticketButton.setClickable(true);
            Log.d("TestApp", "Showing ticket button.");
        } else {
            registerButton.setVisibility(View.VISIBLE);
            ticketButton.setVisibility(View.GONE);
            ticketButton.setClickable(false);
            Log.d("TestApp", "Showing register button.");
        }
    }

    private void generateAndUploadQRCode() {
        QrCodeGenerator.generateQRCodeWithEventAndStudentInfo(this, eventUID, new QrCodeGenerator.OnQRCodeGeneratedListener() {
            @Override
            public void onQRCodeGenerated(Bitmap qrCodeBitmap) {
                Log.d("TestApp", "QR Code successfully generated.");
            }

            @Override
            public void onQRCodeUploaded(String downloadUrl, String ticketID) { // Updated to include ticketID
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
                            Log.d("TestApp", "QR Code URL saved in Firebase: " + downloadUrl);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(StudentDashboardInside.this, "Failed to save QR Code URL!", Toast.LENGTH_SHORT).show();
                            Log.e("TestApp", "Error saving QR Code URL: " + e.getMessage());
                        });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(StudentDashboardInside.this, "QR Code Generation Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("TestApp", "QR Code Generation Error: " + errorMessage);
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