package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.*;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TeacherScanning extends BaseActivity {

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

    // Constants for time status results
    private static final int TIME_STATUS_TOO_EARLY = -1;     // Event hasn't started yet
    private static final int TIME_STATUS_ON_TIME = 0;        // Within grace period
    private static final int TIME_STATUS_LATE = 1;           // After grace period
    private static final int TIME_STATUS_CAN_CHECKOUT = 2;   // After end time but can check out
    private static final int TIME_STATUS_ENDED = 3;          // Event has completely ended
    private static final int TIME_STATUS_CHECKIN_ENDED = 4;  // Time for check-in has ended

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.capstone.mdfeventmanagementsystem.R.layout.activity_teacher_scanning);

        findViewById(R.id.fab_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), TeacherCreateEventActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        // Initialize views
        instructionForScanning = findViewById(com.capstone.mdfeventmanagementsystem.R.id.instruction_for_scanning);
        getStarted = findViewById(com.capstone.mdfeventmanagementsystem.R.id.getStarted);
        validText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.valid_text);
        usedText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.used_text);
        invalidText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.invalid_text);
        notAllowedText = findViewById(com.capstone.mdfeventmanagementsystem.R.id.not_allowed_text);
        validTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.validTicket);
        usedTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.usedTicket);
        invalidTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.invalidTicket);
        notAllowedTicket = findViewById(com.capstone.mdfeventmanagementsystem.R.id.notAllowedTicket);
        scanTicketBtn = findViewById(com.capstone.mdfeventmanagementsystem.R.id.scanTicketBtn);
        cancelScanBtn = findViewById(com.capstone.mdfeventmanagementsystem.R.id.cancelScanBtn);
        barcodeView = findViewById(com.capstone.mdfeventmanagementsystem.R.id.barcode_scanner);

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
        BottomNavigationView bottomNavigationView = findViewById(com.capstone.mdfeventmanagementsystem.R.id.bottom_navigation_teacher);
        bottomNavigationView.setSelectedItemId(com.capstone.mdfeventmanagementsystem.R.id.nav_scan_teacher);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_home_teacher) {
                startActivity(new Intent(this, TeacherDashboard.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_scan_teacher) {
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

                    // First, check the ticket status to see if check-in exists
                    checkAttendanceStatus(studentId, eventId, ticketData, isMultiDay);
                }
                else {
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

    private void checkAttendanceStatus(String studentId, String eventId, Map<String, String> ticketData, boolean isMultiDay) {
        DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentDate = getCurrentDate();
                    boolean hasCheckedIn = false;
                    boolean hasCheckedOut = false;
                    boolean isLateCheckin = false;
                    DataSnapshot dayData = null;
                    String dayKey = null;

                    if (isMultiDay) {
                        // For multi-day event, find the current day's data
                        DataSnapshot attendanceDays = snapshot.child("attendanceDays");
                        if (attendanceDays.exists()) {
                            for (DataSnapshot daySnapshot : attendanceDays.getChildren()) {
                                if (daySnapshot.child("date").exists() &&
                                        currentDate.equals(daySnapshot.child("date").getValue(String.class))) {
                                    dayData = daySnapshot;
                                    dayKey = daySnapshot.getKey();

                                    String checkInTime = daySnapshot.child("in").getValue(String.class);
                                    hasCheckedIn = checkInTime != null && !"N/A".equals(checkInTime);

                                    String checkOutTime = daySnapshot.child("out").getValue(String.class);
                                    hasCheckedOut = checkOutTime != null && !"N/A".equals(checkOutTime);

                                    // Get late status if it exists
                                    if (daySnapshot.child("isLate").exists()) {
                                        isLateCheckin = daySnapshot.child("isLate").getValue(Boolean.class);
                                    }

                                    break;
                                }
                            }
                        }
                    } else {
                        // For single-day event
                        dayData = snapshot.child("attendanceDays").child("day_1");
                        dayKey = "day_1";

                        if (dayData.exists()) {
                            String checkInTime = dayData.child("in").getValue(String.class);
                            hasCheckedIn = checkInTime != null && !"N/A".equals(checkInTime);

                            String checkOutTime = dayData.child("out").getValue(String.class);
                            hasCheckedOut = checkOutTime != null && !"N/A".equals(checkOutTime);

                            // Get late status if it exists
                            if (dayData.child("isLate").exists()) {
                                isLateCheckin = dayData.child("isLate").getValue(Boolean.class);
                            }
                        }
                    }

                    // Now check time status
                    int timeStatus = checkTimeStatus(ticketData);

                    // Process based on check-in status and time status
                    if (dayData == null) {
                        // No attendance record found for today
                        showNotAllowedTicket();
                        notAllowedText.setText("No attendance record found for today");
                        persistNotAllowedTicket = true;
                    } else if (hasCheckedOut) {
                        // Already checked out
                        showUsedTicket();
                        usedText.setText("Already checked out for today");
                    } else if (hasCheckedIn) {
                        // Already checked in, now can check out
                        // Even if event has ended, check-out is allowed
                        processCheckOut(ticketRef, dayKey, isMultiDay, isLateCheckin);
                    } else {
                        // Not checked in yet
                        if (timeStatus == TIME_STATUS_TOO_EARLY) {
                            // Event hasn't started yet
                            showNotAllowedTicket();
                            notAllowedText.setText("The event hasn't started yet");
                            persistNotAllowedTicket = true;
                        } else if (timeStatus == TIME_STATUS_ENDED || timeStatus == TIME_STATUS_CHECKIN_ENDED) {
                            // Event has completely ended or check-in time has passed
                            showNotAllowedTicket();
                            notAllowedText.setText("The event has ended");
                            persistNotAllowedTicket = true;
                        } else {
                            // Can check in - determine if it's a late check-in based on the timeStatus
                            boolean isLate = (timeStatus == TIME_STATUS_LATE);
                            processCheckIn(ticketRef, dayKey, isLate);
                        }
                    }
                } else {
                    // Ticket doesn't exist
                    showNotAllowedTicket();
                    notAllowedText.setText("No ticket found for this student");
                    persistNotAllowedTicket = true;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherScanning.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showInvalidTicket();
            }
        });
    }

    private void processCheckIn(DatabaseReference ticketRef, String dayKey, boolean isLate) {
        String currentTime = getCurrentTime();

        Map<String, Object> updates = new HashMap<>();
        updates.put("in", currentTime);
        updates.put("status", "Ongoing");  // Changed from Pending to Ongoing when checking in

        // MODIFIED: Always set attendance as "Ongoing" for check-in regardless of whether it's late or not
        updates.put("attendance", "Ongoing");

        // Still store the isLate flag for use during check-out
        updates.put("isLate", isLate);

        ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    String msg = isLate ?
                            "Check-in successful. Note: Arrived late!" :
                            "Check-in successful";
                    Toast.makeText(TeacherScanning.this, msg, Toast.LENGTH_SHORT).show();
                });

        showValidTicket();
        if (isLate) {
            validText.setText("Checked in at " + currentTime + " (Arrived late)");
        } else {
            validText.setText("Checked in at " + currentTime);
        }
    }

    private void processCheckOut(DatabaseReference ticketRef, String dayKey, boolean isMultiDay, boolean isLateCheckin) {
        String currentTime = getCurrentTime();

        Map<String, Object> updates = new HashMap<>();
        updates.put("out", currentTime);
        updates.put("status", "Pending");  // Keep it as Pending during checkout

        // MODIFIED: Change attendance status based on isLate flag during check-out
        if (isLateCheckin) {
            updates.put("attendance", "Late");
        } else {
            updates.put("attendance", "Present");
        }

        ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    String msg = isLateCheckin ?
                            "Check-out successful (Attendance marked as Late)" :
                            "Check-out successful (Attendance marked as Present)";
                    Toast.makeText(TeacherScanning.this, msg, Toast.LENGTH_SHORT).show();
                });

        showValidTicket();
        if (isLateCheckin) {
            validText.setText("Checked out at " + currentTime + " (Attendance: Late)");
        } else {
            validText.setText("Checked out at " + currentTime + " (Attendance: Present)");
        }
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

    // Helper method to get current time in HH:mm format
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
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
                return TIME_STATUS_ENDED; // Missing essential time data, treat as invalid
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
                    return TIME_STATUS_TOO_EARLY;
                }

                // If current date is after event end date, event has completely ended
                if (currentDateOnly.after(eventEndDate)) {
                    return TIME_STATUS_ENDED;
                }
            } else {
                // For single-day events
                // If today is not the event date, it's either too early or too late
                if (!currentDateStr.equals(startDate)) {
                    if (currentDateOnly.before(eventStartDate)) {
                        return TIME_STATUS_TOO_EARLY;
                    } else {
                        return TIME_STATUS_ENDED;
                    }
                }
            }

            // Now check the time (assuming we're on a valid event date)
            Date eventStartTime = combinedFormat.parse(currentDateStr + " " + startTime);
            Date eventEndTime = combinedFormat.parse(currentDateStr + " " + endTime);

            // If current time is before start time, it's too early to scan
            if (currentDate.before(eventStartTime)) {
                return TIME_STATUS_TOO_EARLY;
            }

            // If current time is after end time, we need to handle check-in differently
            if (currentDate.after(eventEndTime)) {
                // For first-time check-in attempts after event end time, show "event has ended"
                return TIME_STATUS_CHECKIN_ENDED;
            }

            // If there's no grace period, mark as present
            if (graceTimeStr == null || graceTimeStr.isEmpty() || "none".equalsIgnoreCase(graceTimeStr)) {
                return TIME_STATUS_ON_TIME;
            }

            // Calculate the end of grace period
            int graceTime;
            try {
                graceTime = Integer.parseInt(graceTimeStr);
            } catch (NumberFormatException e) {
                return TIME_STATUS_ON_TIME; // Invalid grace time format, treat as no grace period
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, graceTime);
            Date graceEndTime = calendar.getTime();

            // Check if we're within the grace period or late
            if (currentDate.after(graceEndTime)) {
                return TIME_STATUS_LATE; // Late - after grace period
            } else {
                return TIME_STATUS_ON_TIME; // On time - within grace period
            }
        } catch (ParseException e) {
            // Any parsing error means the data is invalid
            return TIME_STATUS_ENDED; // Invalid data
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