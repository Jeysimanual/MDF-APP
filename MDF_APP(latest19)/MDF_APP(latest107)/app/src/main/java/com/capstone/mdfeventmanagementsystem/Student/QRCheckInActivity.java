package com.capstone.mdfeventmanagementsystem.Student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

class CircleTransformQRCheckIn implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.BitmapShader shader = new android.graphics.BitmapShader(
                squaredBitmap, android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP);

        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "circle";
    }
}

public class QRCheckInActivity extends BaseActivity {

    private TextView instructionForScanning, validText, usedText, invalidText, notAllowedText;
    private TextView getStarted, noPermissionText; // Added this to match layout
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
    // Constants for time status results
    private static final int TIME_STATUS_TOO_EARLY = -1;     // Event hasn't started yet
    private static final int TIME_STATUS_ON_TIME = 0;        // Within grace period
    private static final int TIME_STATUS_LATE = 1;           // After grace period
    private static final int TIME_STATUS_CAN_CHECKOUT = 2;   // After end time but can check out (1-hour window)
    private static final int TIME_STATUS_ENDED = 3;          // Event has completely ended
    private static final int TIME_STATUS_CHECKIN_ENDED = 4;  // Time for check-in has ended

    private ImageView profileImageView;
    private DatabaseReference studentsRef;
    private DatabaseReference profilesRef;
    private static final String TAG = "QRCheckInActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.capstone.mdfeventmanagementsystem.R.layout.activity_qrcheck_in);

        profileImageView = findViewById(R.id.profile_image);
        studentsRef = FirebaseDatabase.getInstance().getReference().child("students");
        profilesRef = FirebaseDatabase.getInstance().getReference().child("user_profiles");

        loadCachedProfileImage();

        findViewById(R.id.fab_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QRCheckInActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setBackgroundTint(ColorStateList.valueOf(Color.WHITE));

        FloatingActionButton fab = findViewById(R.id.fab_scan);
        fab.setColorFilter(getResources().getColor(R.color.green));

        findViewById(R.id.profile_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QRCheckInActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });


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
        noPermissionText= findViewById(com.capstone.mdfeventmanagementsystem.R.id.noPermission);

        // Initial UI setup
        barcodeView.setVisibility(DecoratedBarcodeView.GONE);
        getStarted.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
        cancelScanBtn.setVisibility(Button.GONE);
        noPermissionText.setVisibility(TextView.GONE);

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

        // Load profile image
        loadUserProfile();
    }

    public interface OnCoordinatorCheckListener {
        void onCheck(boolean isCoordinator, String eventUID);
    }


    private void startScanning() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("coordinatorTesting", "User not logged in.");
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = user.getEmail();
        Log.d("coordinatorTesting", "Checking if user is a coordinator: " + userEmail);
        checkIfUserIsCoordinator(userEmail, (isCoordinator, eventUID) -> {
            if (isCoordinator) {
                Log.d("coordinatorTesting", "User is a coordinator for event: " + eventUID + ". Starting scanning process.");

                // Hide no permission text if the user has permission to scan
                noPermissionText.setVisibility(TextView.GONE);

                // Show scanning UI elements
                getStarted.setVisibility(TextView.GONE);  // Hide "Get Started" message
                scanTicketBtn.setVisibility(Button.GONE);  // Hide the scan button
                instructionForScanning.setVisibility(TextView.VISIBLE);  // Show scanning instructions
                barcodeView.setVisibility(DecoratedBarcodeView.VISIBLE);  // Show barcode scanner
                cancelScanBtn.setVisibility(Button.VISIBLE);  // Show cancel button

                // Always hide ticket views when scanning starts
                hideAllTicketViews();
                scanning = true;

                // Start the barcode scanning
                barcodeView.decodeSingle(new BarcodeCallback() {
                    @Override
                    public void barcodeResult(BarcodeResult result) {
                        if (result.getText() != null && scanning) {
                            Log.d("coordinatorTesting", "Scanned ticket: " + result.getText());
                            scanning = false;
                            validateTicket(result.getText(), eventUID);
                            barcodeView.setVisibility(DecoratedBarcodeView.GONE);  // Hide barcode scanner after scan
                            cancelScanBtn.setVisibility(Button.GONE);  // Hide cancel button after scan
                        }
                    }
                });
                barcodeView.resume();
            } else {
                Log.w("coordinatorTesting", "User is not a coordinator. Access denied.");
                Toast.makeText(this, "You do not have permission to scan tickets.", Toast.LENGTH_SHORT).show();

                // Show the "No Permission" text and reset UI
                noPermissionText.setVisibility(TextView.VISIBLE);  // Show no permission message
                getStarted.setVisibility(TextView.GONE);  // Hide "Get Started" message
                instructionForScanning.setVisibility(TextView.GONE);  // Hide scanning instructions
                barcodeView.setVisibility(DecoratedBarcodeView.GONE);  // Hide barcode scanner
                scanTicketBtn.setVisibility(Button.GONE);  // Hide the scan button
                cancelScanBtn.setVisibility(Button.GONE);  // Hide cancel button
            }
        });
    }

    private void checkIfUserIsCoordinator(String email, OnCoordinatorCheckListener listener) {
        if (email == null) {
            Log.e("coordinatorTesting", "Email is null. Cannot check permissions.");
            listener.onCheck(false, null);
            return;
        }

        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
        Log.d("coordinatorTesting", "Checking coordinator status for email: " + email);

        // Log the event fetching process
        Log.d("coordinatorTesting", "Fetching events from Firebase...");

        // Attempt to fetch the events and handle the result
        eventsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("coordinatorTesting", "Successfully fetched events.");

                // Check if the result contains data
                if (task.getResult().exists()) {
                    Log.d("coordinatorTesting", "Events found in the database. Processing events...");

                    // Iterate through each event to check if the user is a coordinator
                    for (DataSnapshot eventSnapshot : task.getResult().getChildren()) {
                        String eventID = eventSnapshot.getKey();
                        DataSnapshot coordinatorsSnapshot = eventSnapshot.child("eventCoordinators");

                        // Log for each event being checked
                        Log.d("coordinatorTesting", "Checking coordinators for event ID: " + eventID);

                        if (coordinatorsSnapshot.hasChild(email.replace(".", ","))) {
                            String eventUID = eventSnapshot.getKey();
                            Log.d("coordinatorTesting", "User is a coordinator for event: " + eventUID);
                            listener.onCheck(true, eventUID);
                            return; // Exit after finding the first event the user is a coordinator for
                        }
                    }
                    Log.w("coordinatorTesting", "User is not a coordinator for any event.");
                    listener.onCheck(false, null);
                } else {
                    Log.w("coordinatorTesting", "No events found in the database.");
                    listener.onCheck(false, null);
                }
            } else {
                // Log failure to fetch events
                Log.e("coordinatorTesting", "Failed to fetch events. Error: " + task.getException());
                listener.onCheck(false, null);
            }
        });
    }

    private void validateTicket(String scannedTicket, String coordinatorEventUID) {
        // If we're persisting NotAllowedTicket, immediately show it
        if (persistNotAllowedTicket) {
            showNotAllowedTicket();
            return;
        }

        Map<String, String> ticketData = parseQRContent(scannedTicket);
        String studentId = ticketData.get("studentID");
        String eventId = ticketData.get("eventUID");

        if (studentId == null || eventId == null) {
            showInvalidTicket();
            return;
        }

        // Check if the scanned ticket belongs to the coordinator's event
        if (!eventId.equals(coordinatorEventUID)) {
            Log.w("coordinatorTesting", "Ticket does not belong to the event the coordinator manages.");
            showNotAllowedTicket();
            notAllowedText.setText("You are not assigned as coordinator for this event.");
            persistNotAllowedTicket = true;
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
                Toast.makeText(QRCheckInActivity.this, "Error checking event: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                    String currentStatus = "N/A"; // Default status

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

                                    // Get current status if it exists
                                    if (daySnapshot.child("status").exists()) {
                                        currentStatus = daySnapshot.child("status").getValue(String.class);
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

                            // Get current status if it exists
                            if (dayData.child("status").exists()) {
                                currentStatus = dayData.child("status").getValue(String.class);
                            }
                        }
                    }

                    // Check time status
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
                        // Already checked in - can only check out if event has ended
                        if (timeStatus == TIME_STATUS_CAN_CHECKOUT || timeStatus == TIME_STATUS_ENDED) {
                            // Can check out (during or after event)
                            processCheckOut(ticketRef, dayKey, isMultiDay, isLateCheckin);
                        } else {
                            // Already checked in but too early for checkout
                            showUsedTicket();
                            usedText.setText("Already checked in. Check-out available after event ends.");
                        }
                    } else {
                        // Not checked in yet
                        if (timeStatus == TIME_STATUS_TOO_EARLY) {
                            // Event hasn't started yet
                            showNotAllowedTicket();
                            notAllowedText.setText("The event hasn't started yet");
                            persistNotAllowedTicket = true;
                        } else if (timeStatus == TIME_STATUS_ENDED) {
                            // Event has completely ended - too late for check-in/out
                            showNotAllowedTicket();
                            notAllowedText.setText("The event has ended. Check-in/out period closed.");
                            persistNotAllowedTicket = true;
                        } else if (timeStatus == TIME_STATUS_CAN_CHECKOUT) {
                            // Event ended but didn't check in - allow late check-in with late status
                            processCheckIn(ticketRef, dayKey, true);
                        } else {
                            // Can check in - either on time or late
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
                Toast.makeText(QRCheckInActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showInvalidTicket();
            }
        });
    }
    private void updateAttendanceStatus(DatabaseReference ticketRef, String dayKey, String newStatus) {
        // Update the status field in the database
        ticketRef.child("attendanceDays").child(dayKey).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("StatusUpdate", "Status updated to: " + newStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e("StatusUpdate", "Failed to update status: " + e.getMessage());
                });
    }

    private void processCheckIn(DatabaseReference ticketRef, String dayKey, boolean isLate) {
        String currentTime = getCurrentTime();

        Map<String, Object> updates = new HashMap<>();
        updates.put("in", currentTime);
        updates.put("isLate", isLate);

        ticketRef.child("attendanceDays").child(dayKey).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    String msg = isLate ?
                            "Check-in successful. Note: Arrived late!" :
                            "Check-in successful";
                    Toast.makeText(QRCheckInActivity.this, msg, Toast.LENGTH_SHORT).show();

                    // Update status from N/A to Ongoing
                    updateAttendanceStatus(ticketRef, dayKey, "Ongoing");
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

        // Change attendance status based on isLate flag during check-out
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
                    Toast.makeText(QRCheckInActivity.this, msg, Toast.LENGTH_SHORT).show();

                    // Update status from Ongoing to Pending
                    updateAttendanceStatus(ticketRef, dayKey, "Pending");
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

            // If current time is after end time, check if it's within checkout window
            if (currentDate.after(eventEndTime)) {
                // Add 1 hour grace period for checkout
                Calendar checkoutDeadline = Calendar.getInstance();
                checkoutDeadline.setTime(eventEndTime);
                checkoutDeadline.add(Calendar.HOUR, 1); // 1 hour grace period for checkout

                if (currentDate.after(checkoutDeadline.getTime())) {
                    // Too late for checkout - beyond 1 hour grace period
                    return TIME_STATUS_ENDED;
                } else {
                    // Within the 1-hour checkout window
                    return TIME_STATUS_CAN_CHECKOUT;
                }
            }

            // Handle grace period for check-in (late arrival)
            // Special handling for "none" or null graceTime - no late arrivals possible
            if (graceTimeStr == null || graceTimeStr.isEmpty() ||
                    graceTimeStr.equalsIgnoreCase("none") || graceTimeStr.equalsIgnoreCase("null")) {
                // With no grace time specified or "none", all arrivals during event are considered on time
                return TIME_STATUS_ON_TIME;
            }

            // Parse grace time for normal cases
            int graceTime;
            try {
                graceTime = Integer.parseInt(graceTimeStr);
            } catch (NumberFormatException e) {
                // If not "none" but still can't parse, default to 0
                graceTime = 0;
            }

            Calendar graceEndCalendar = Calendar.getInstance();
            graceEndCalendar.setTime(eventStartTime);
            graceEndCalendar.add(Calendar.MINUTE, graceTime);
            Date graceEndTime = graceEndCalendar.getTime();

            // Check if we're within the grace period or late
            if (currentDate.after(graceEndTime)) {
                return TIME_STATUS_LATE; // Late - after grace period
            } else {
                return TIME_STATUS_ON_TIME; // On time - within grace period
            }
        } catch (ParseException e) {
            return TIME_STATUS_ENDED; // Error in parsing dates
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
        loadUserProfile();
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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(com.capstone.mdfeventmanagementsystem.R.id.bottom_navigation);

        // Clear any pre-selected items first
        bottomNavigationView.getMenu().setGroupCheckable(0, false, true);

        // This will deselect all navigation items, ensuring none are highlighted
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

        // Now set your FAB to be visually distinguished
        findViewById(R.id.fab_scan).setSelected(true);

        // Hide the labels in the bottom navigation
        bottomNavigationView.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_UNLABELED);

        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_home) {
                startActivity(new Intent(this, MainActivity2.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_event) {
                startActivity(new Intent(this, StudentDashboard.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_ticket) {
                startActivity(new Intent(this, StudentTickets.class));
                finish();
            } else if (itemId == com.capstone.mdfeventmanagementsystem.R.id.nav_cert) {
                startActivity(new Intent(this, StudentCertificate.class));
                finish();
            }

            overridePendingTransition(0, 0); // Remove animation between transitions
            return true;
        });
    }

    /**
     * Load cached profile image immediately on startup
     */
    private void loadCachedProfileImage() {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        String cachedImageUrl = prefs.getString("profileImageUrl", "");

        if (!cachedImageUrl.isEmpty()) {
            // Load the cached image immediately
            loadProfileImageFromCache(cachedImageUrl);
        } else {
            // Set default if no cached image
            setDefaultProfileImage();
        }
    }

    /**
     * Load profile image from cache with no network operations
     */
    private void loadProfileImageFromCache(String imageUrl) {
        if (profileImageView == null) {
            return;
        }

        // Use Picasso to load from cache only - no network
        Picasso.get()
                .load(imageUrl)
                .transform(new CircleTransformDashboard())
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .networkPolicy(com.squareup.picasso.NetworkPolicy.OFFLINE) // Only load from cache
                .into(profileImageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Profile image loaded from cache");
                    }

                    @Override
                    public void onError(Exception e) {
                        // If cache loading fails, use the default
                        setDefaultProfileImage();
                    }
                });
    }

    /**
     * Save profile image URL to cache when successfully loaded
     */
    private void cacheProfileImageUrl(String imageUrl) {
        SharedPreferences prefs = getSharedPreferences("ProfileImageCache", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImageUrl", imageUrl);
        editor.apply();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Check for profile image
            checkProfileImage(user.getUid());
        }
    }

    /**
     * Checks for profile image in student_profiles collection
     * @param uid User ID for lookup
     */
    private void checkProfileImage(String uid) {
        profilesRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("profileImage")) {
                    String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in student_profiles: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        // If no image in profiles, check the students collection
                        checkStudentProfileImage(uid);
                    }
                } else {
                    Log.d(TAG, "No profile image found in student_profiles, checking students collection");
                    checkStudentProfileImage(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student_profiles: " + error.getMessage());
                // Fallback to students collection
                checkStudentProfileImage(uid);
            }
        });
    }

    /**
     * Checks for profile image in students collection
     * @param uid User ID for lookup
     */
    private void checkStudentProfileImage(String uid) {
        studentsRef.child(uid).child("profileImage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String profileImageUrl = dataSnapshot.getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Log.d(TAG, "Found profile image in students collection: " + profileImageUrl);
                        loadProfileImage(profileImageUrl);
                    } else {
                        Log.d(TAG, "Profile image field exists but is empty");
                        // Don't set default here as we already have cached or default
                    }
                } else {
                    Log.d(TAG, "No profile image in students collection");
                    // Check if firebase user has photo URL as last resort
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getPhotoUrl() != null) {
                        loadProfileImage(user.getPhotoUrl().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking student profile image: " + error.getMessage());
                // Don't set default here as we already have cached or default
            }
        });
    }

    /**
     * Loads profile image using Picasso with circle transformation
     * @param imageUrl URL of the profile image
     */
    private void loadProfileImage(String imageUrl) {
        if (profileImageView == null) {
            Log.e(TAG, "Cannot load profile image: ImageView is null");
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d(TAG, "Loading profile image from: " + imageUrl);

            // Use Picasso to load the image with transformation for circular display
            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircleTransformDashboard()) // Use CircleTransform for circular images
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile image loaded successfully");
                            // Cache the successful URL for next time
                            cacheProfileImageUrl(imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading profile image: " + (e != null ? e.getMessage() : "unknown error"));
                            // Don't set default here as we already have cached or default
                        }
                    });
        }
    }

    /**
     * Sets default profile image placeholder
     */
    private void setDefaultProfileImage() {
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.profile_placeholder);
        }
    }

}