package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.*;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TeacherScanning extends AppCompatActivity {

    private TextView instructionForScanning, validText, usedText, invalidText, notAllowedText;
    private TextView getStarted; // Added this to match layout
    private ImageView validTicket, usedTicket, invalidTicket, notAllowedTicket;
    private Button scanTicketBtn, cancelScanBtn;
    private DecoratedBarcodeView barcodeView;
    private CaptureManager captureManager;
    private boolean scanning = false;

    // Add a new variable to track if NotAllowedTicket should persist
    private boolean persistNotAllowedTicket = false;

    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_scanning);

        // Initialize views
        instructionForScanning = findViewById(R.id.instruction_for_scanning);
        getStarted = findViewById(R.id.getStarted);
        validText = findViewById(R.id.valid_text);
        usedText = findViewById(R.id.used_text);
        invalidText = findViewById(R.id.invalid_text);
        notAllowedText = findViewById(R.id.not_allowed_text);
        validTicket = findViewById(R.id.validTicket);
        usedTicket = findViewById(R.id.usedTicket);
        invalidTicket = findViewById(R.id.invalidTicket);
        notAllowedTicket = findViewById(R.id.notAllowedTicket);
        scanTicketBtn = findViewById(R.id.scanTicketBtn);
        cancelScanBtn = findViewById(R.id.cancelScanBtn);
        barcodeView = findViewById(R.id.barcode_scanner);

        // Initial UI setup
        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        getStarted.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        cancelScanBtn.setVisibility(Button.GONE);

        // Initialize barcode scanner
        captureManager = new CaptureManager(this, barcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);

        // Set up button click listeners
        scanTicketBtn.setOnClickListener(v -> startScanning());
        cancelScanBtn.setOnClickListener(v -> stopScanning());

        // Initialize Firebase database reference
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.keepSynced(true);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("TicketStatus", MODE_PRIVATE);

        // Set up bottom navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(R.id.nav_scan_teacher);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(this, TeacherDashboard.class));
                finish();
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish();
            } else if (itemId == R.id.nav_scan_teacher) {
                return true; // Stay on the same page
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(this, TeacherProfile.class));
                finish();
            }

            overridePendingTransition(0, 0); // Remove animation between transitions
            return true;
        });
    }

    private void startScanning() {
        // Hide start instructions and show scanning UI
        getStarted.setVisibility(TextView.GONE);
        instructionForScanning.setVisibility(TextView.VISIBLE);
        barcodeView.setVisibility(DecoratedBarcodeView.VISIBLE);
        scanTicketBtn.setVisibility(Button.GONE);
        cancelScanBtn.setVisibility(Button.VISIBLE);

        // MODIFIED: Always hide notification icons and text when scanning
        hideAllTicketViews();

        // Start the barcode scanning
        scanning = true;
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && scanning) {
                    // Process the scan result
                    scanning = false;
                    validateTicket(result.getText());
                    barcodeView.setVisibility(DecoratedBarcodeView.GONE);
                    cancelScanBtn.setVisibility(Button.GONE);
                }
            }
        });
        barcodeView.resume();
    }

    private void validateTicket(String qrContent) {
        // If we're persisting NotAllowedTicket, immediately show it
        if (persistNotAllowedTicket) {
            showNotAllowedTicket();
            return;
        }

        Map<String, String> ticketData = parseQRContent(qrContent);
        String studentId = ticketData.get("studentID");
        String eventId = ticketData.get("eventUID");

        if (studentId == null || eventId == null) {
            showInvalidTicket();
            return;
        }

        // Enhanced time check - first fetch the current event details from Firebase
        databaseRef.child("events").child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get event data from Firebase (this ensures we have the most up-to-date event info)
                    String startDate = snapshot.child("startDate").getValue(String.class);
                    String endDate = snapshot.child("endDate").getValue(String.class);
                    String startTime = snapshot.child("startTime").getValue(String.class);
                    String endTime = snapshot.child("endTime").getValue(String.class);
                    String graceTimeStr = snapshot.child("graceTime").getValue(String.class);
                    String eventSpan = snapshot.child("eventSpan").getValue(String.class);
                    boolean isMultiDay = "multi-day".equals(eventSpan);

                    // Update ticket data with the latest event information
                    ticketData.put("startDate", startDate);
                    ticketData.put("endDate", endDate);
                    ticketData.put("startTime", startTime);
                    ticketData.put("endTime", endTime);
                    ticketData.put("graceTime", graceTimeStr);

                    // Check if the event is currently active and can be scanned
                    int timeStatus = checkTimeStatus(ticketData);

                    if (timeStatus == -1) {
                        // Event hasn't started yet
                        showNotAllowedTicket();
                        notAllowedText.setText("The event hasn't started yet");
                        persistNotAllowedTicket = true; // Now we'll persist this view
                    } else if (timeStatus == 2) {
                        // Event has ended
                        showNotAllowedTicket();
                        notAllowedText.setText("The event has ended");
                        persistNotAllowedTicket = true; // Now we'll persist this view
                    } else {
                        // Event is active, proceed to validate attendance
                        validateAttendance(studentId, eventId, isMultiDay, timeStatus);
                    }
                } else {
                    showInvalidTicket();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherScanning.this, "Error checking event: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showInvalidTicket();
            }
        });
    }


    private void stopScanning() {
        scanning = false;
        barcodeView.pauseAndWait();
        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        cancelScanBtn.setVisibility(Button.GONE);

        // If we should keep showing NotAllowedTicket, don't fully reset
        if (persistNotAllowedTicket) {
            scanTicketBtn.setVisibility(Button.VISIBLE);
        } else {
            resetScanUI();
        }
    }

    private void resetScanUI() {
        // If we should keep showing NotAllowedTicket, don't fully reset
        if (persistNotAllowedTicket) {
            // Keep NotAllowedTicket visible but hide other UI
            getStarted.setVisibility(TextView.GONE);
            instructionForScanning.setVisibility(TextView.GONE);
            validTicket.setVisibility(ImageView.GONE);
            validText.setVisibility(TextView.GONE);
            usedTicket.setVisibility(ImageView.GONE);
            usedText.setVisibility(TextView.GONE);
            invalidTicket.setVisibility(ImageView.GONE);
            invalidText.setVisibility(TextView.GONE);
            notAllowedTicket.setVisibility(ImageView.VISIBLE);
            notAllowedText.setVisibility(TextView.VISIBLE);
            scanTicketBtn.setVisibility(Button.VISIBLE);
        } else {
            // Hide all result views and show the initial instructions
            hideAllTicketViews();
            getStarted.setVisibility(TextView.VISIBLE);
            instructionForScanning.setVisibility(TextView.GONE);
            scanTicketBtn.setVisibility(Button.VISIBLE);
        }
    }


    private void validateAttendance(String studentId, String eventId, boolean isMultiDay, int timeStatus) {
        DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentDate = getCurrentDate();

                    if (isMultiDay) {
                        // Check if they've already attended today
                        boolean alreadyAttendedToday = false;
                        String attendedDayKey = null;
                        DataSnapshot attendanceDays = snapshot.child("attendanceDays");

                        if (attendanceDays.exists()) {
                            for (DataSnapshot daySnapshot : attendanceDays.getChildren()) {
                                // Check if this day matches today's date
                                if (daySnapshot.child("date").exists() &&
                                        currentDate.equals(daySnapshot.child("date").getValue(String.class))) {
                                    attendedDayKey = daySnapshot.getKey();
                                    String dayStatus = daySnapshot.child("status").getValue(String.class);

                                    if ("Present".equals(dayStatus) || "Late".equals(dayStatus)) {
                                        alreadyAttendedToday = true;
                                    }
                                    break;
                                }
                            }
                        }

                        if (alreadyAttendedToday) {
                            // Already attended today
                            showUsedTicket();
                        } else if (attendedDayKey != null) {
                            // Found the day entry for today, update its status
                            String newStatus = (timeStatus == 1) ? "Late" : "Present";

                            // Update the status for this specific day
                            ticketRef.child("attendanceDays").child(attendedDayKey).child("status").setValue(newStatus)
                                    .addOnSuccessListener(aVoid -> {
                                        // Log success
                                        Toast.makeText(TeacherScanning.this, "Attendance marked successfully for " + currentDate, Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Log failure
                                        Toast.makeText(TeacherScanning.this, "Failed to update attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                            // Save local status for this event date combination
                            saveTicketStatus(eventId + "_" + currentDate, newStatus);

                            showValidTicket();
                            if (timeStatus == 1) {
                                validText.setText("Marked as Late for " + currentDate);
                            } else {
                                validText.setText("Marked as Present for " + currentDate);
                            }
                        } else {
                            // No entry found for today's date
                            showNotAllowedTicket();
                            notAllowedText.setText("No attendance record found for today");
                            persistNotAllowedTicket = true;
                        }
                    } else {
                        // Single-day event processing - keep the original logic
                        String currentStatus = snapshot.child("attendanceDays").child("day_1").child("status").getValue(String.class);

                        if ("Present".equals(currentStatus) || "Late".equals(currentStatus)) {
                            showUsedTicket();
                        } else if ("pending".equals(currentStatus)) {
                            String newStatus = (timeStatus == 1) ? "Late" : "Present";
                            ticketRef.child("attendanceDays").child("day_1").child("status").setValue(newStatus);
                            saveTicketStatus(eventId, newStatus);
                            showValidTicket();
                            if (timeStatus == 1) {
                                validText.setText("Marked as Late");
                            } else {
                                validText.setText("Marked as Present");
                            }
                        } else {
                            showNotAllowedTicket();
                            notAllowedText.setText("This ticket is not valid for attendance");
                            persistNotAllowedTicket = true;
                        }
                    }
                } else {
                    // Ticket doesn't exist for this student-event combination
                    showNotAllowedTicket();
                    notAllowedText.setText("No ticket found for this student");
                    persistNotAllowedTicket = true;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                String offlineStatus = getTicketStatus(eventId);
                if (offlineStatus != null) {
                    if ("Present".equals(offlineStatus) || "Late".equals(offlineStatus)) {
                        showUsedTicket();
                    } else if ("pending".equals(offlineStatus)) {
                        showValidTicket();
                    } else {
                        showNotAllowedTicket();
                        persistNotAllowedTicket = true;
                    }
                } else {
                    showInvalidTicket();
                }
            }
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveTicketStatus(String eventId, String status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(eventId, status);
        editor.apply();
    }

    private String getTicketStatus(String eventId) {
        return sharedPreferences.getString(eventId, null);
    }

    private Map<String, String> parseQRContent(String qrContent) {
        Map<String, String> map = new HashMap<>();
        try {
            qrContent = qrContent.replaceAll("[{}]", ""); // Remove curly braces if present
            String[] pairs = qrContent.split(", ");
            for (String pair : pairs) {
                String[] entry = pair.split("=");
                if (entry.length == 2) {
                    map.put(entry[0].trim(), entry[1].trim());
                }
            }
        } catch (Exception e) {
            // Handle malformed QR content
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
        }
        return map;
    }

    private int checkTimeStatus(Map<String, String> ticketData) {
        try {
            String startDate = ticketData.get("startDate");
            String endDate = ticketData.get("endDate");
            String startTime = ticketData.get("startTime");
            String endTime = ticketData.get("endTime");
            String graceTimeStr = ticketData.get("graceTime");

            // Validate that we have all the required data
            if (startDate == null || startTime == null || endTime == null) {
                return 2; // Missing essential time data, treat as invalid
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            Date currentDate = new Date();
            String currentDateStr = dateFormat.format(currentDate);
            Date currentDateOnly = dateFormat.parse(currentDateStr);

            // Check if today is within the event date range
            Date eventStartDate = dateFormat.parse(startDate);

            // For multi-day events, check if today is within the event dates
            if (endDate != null && !endDate.isEmpty()) {
                Date eventEndDate = dateFormat.parse(endDate);

                // If current date is before event start date, event hasn't started yet
                if (currentDateOnly.before(eventStartDate)) {
                    return -1; // Too early - event hasn't started yet
                }

                // If current date is after event end date, event has ended
                if (currentDateOnly.after(eventEndDate)) {
                    return 2; // Too late - event has ended
                }
            } else {
                // For single-day events
                // If today is not the event date, it's either too early or too late
                if (!currentDateStr.equals(startDate)) {
                    if (currentDateOnly.before(eventStartDate)) {
                        return -1; // Too early - event is in the future
                    } else {
                        return 2; // Too late - event was in the past
                    }
                }
            }

            // Now check the time (assuming we're on a valid event date)
            Date eventStartTime = combinedFormat.parse(currentDateStr + " " + startTime);
            Date eventEndTime = combinedFormat.parse(currentDateStr + " " + endTime);

            // If current time is before start time, it's too early to scan
            if (currentDate.before(eventStartTime)) {
                return -1; // Too early - event hasn't started today
            }

            // If current time is after end time, it's too late to scan
            if (currentDate.after(eventEndTime)) {
                return 2; // Too late - event has ended for today
            }

            // If there's no grace period, mark as present
            if (graceTimeStr == null || graceTimeStr.isEmpty() || "none".equalsIgnoreCase(graceTimeStr)) {
                return 0; // No grace period, valid if before end time
            }

            // Calculate the end of grace period
            int graceTime;
            try {
                graceTime = Integer.parseInt(graceTimeStr);
            } catch (NumberFormatException e) {
                return 0; // Invalid grace time format, treat as no grace period
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, graceTime);
            Date graceEndTime = calendar.getTime();

            // Check if we're within the grace period or late
            if (currentDate.after(graceEndTime)) {
                return 1; // Late - after grace period
            } else {
                return 0; // On time - within grace period
            }
        } catch (ParseException e) {
            // Any parsing error means the data is invalid
            return 2; // Invalid data
        }
    }

    private void showValidTicket() {
        hideAllTicketViews();
        validTicket.setVisibility(ImageView.VISIBLE);
        validText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        persistNotAllowedTicket = false; // Reset persistence flag
    }

    private void showUsedTicket() {
        hideAllTicketViews();
        usedTicket.setVisibility(ImageView.VISIBLE);
        usedText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        persistNotAllowedTicket = false; // Reset persistence flag
    }

    private void showInvalidTicket() {
        hideAllTicketViews();
        invalidTicket.setVisibility(ImageView.VISIBLE);
        invalidText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        persistNotAllowedTicket = false; // Reset persistence flag
    }

    private void showNotAllowedTicket() {
        hideAllTicketViews();
        notAllowedTicket.setVisibility(ImageView.VISIBLE);
        notAllowedText.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        // We don't reset the persistence flag here - it's set in the calling methods
    }

    private void hideAllTicketViews() {
        // MODIFIED: Hide all notification views regardless of persistence flag
        getStarted.setVisibility(TextView.GONE);
        instructionForScanning.setVisibility(TextView.GONE);
        validTicket.setVisibility(ImageView.GONE);
        validText.setVisibility(TextView.GONE);
        usedTicket.setVisibility(ImageView.GONE);
        usedText.setVisibility(TextView.GONE);
        invalidTicket.setVisibility(ImageView.GONE);
        invalidText.setVisibility(TextView.GONE);
        notAllowedTicket.setVisibility(ImageView.GONE);
        notAllowedText.setVisibility(TextView.GONE);
    }

    // Add a method to reset the persistence state
    public void resetPersistentState() {
        persistNotAllowedTicket = false;
        resetScanUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }
}