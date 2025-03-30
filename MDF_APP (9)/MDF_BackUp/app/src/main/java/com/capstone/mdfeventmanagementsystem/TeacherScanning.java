package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TeacherScanning extends AppCompatActivity {

    private TextView instructionForScanning, validText, usedText, invalidText, notAllowedText;
    private ImageView validTicket, usedTicket, invalidTicket, notAllowedTicket;
    private Button scanTicketBtn, cancelScanBtn, scanNewBtn;

    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_scanning);

        instructionForScanning = findViewById(R.id.instruction_for_scanning);
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
        scanNewBtn = findViewById(R.id.scanNewBtn);

        scanTicketBtn.setOnClickListener(v -> startQRScanner());
        cancelScanBtn.setOnClickListener(v -> finish());
        scanNewBtn.setOnClickListener(v -> {
            resetScanUI();
            startQRScanner();
        });

        // Initialize Firebase database reference
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.keepSynced(true);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("TicketStatus", MODE_PRIVATE);
    }

    private void resetScanUI() {
        // Hide all result views and show the scanning instruction
        hideAllTicketViews();
        instructionForScanning.setVisibility(TextView.VISIBLE);
        scanTicketBtn.setVisibility(Button.VISIBLE);
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            validateTicket(result.getContents());
        } else {
            Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateTicket(String qrContent) {
        Map<String, String> ticketData = parseQRContent(qrContent);
        String studentId = ticketData.get("studentID");
        String eventId = ticketData.get("eventUID");

        if (studentId == null || eventId == null) {
            showInvalidTicket();
            return;
        }

        // Check if this is a multi-day event
        databaseRef.child("events").child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String eventSpan = snapshot.child("eventSpan").getValue(String.class);
                    boolean isMultiDay = "multi-day".equals(eventSpan);

                    // Now check the time status
                    int timeStatus = checkTimeStatus(ticketData);

                    if (timeStatus == 2) {
                        showInvalidTicket();
                        return;
                    }

                    // Proceed to check attendance status
                    validateAttendance(studentId, eventId, isMultiDay, timeStatus);
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

    private void validateAttendance(String studentId, String eventId, boolean isMultiDay, int timeStatus) {
        DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentStatus = snapshot.child("status").getValue(String.class);
                    String currentDate = getCurrentDate();

                    if (isMultiDay) {
                        // Check if they've already attended today
                        boolean alreadyAttendedToday = false;
                        DataSnapshot attendanceDays = snapshot.child("attendanceDays");

                        if (attendanceDays.exists()) {
                            for (DataSnapshot daySnapshot : attendanceDays.getChildren()) {
                                if (currentDate.equals(daySnapshot.getValue(String.class))) {
                                    alreadyAttendedToday = true;
                                    break;
                                }
                            }
                        }

                        if (alreadyAttendedToday) {
                            // Already attended today
                            showUsedTicket();
                        } else {
                            // First attendance today, update the status
                            String newStatus = (timeStatus == 1) ? "Late" : "Present";

                            // Update the main status
                            ticketRef.child("status").setValue(newStatus);

                            // Add today to attendance days
                            long dayCount = attendanceDays.getChildrenCount();
                            ticketRef.child("attendanceDays").child("day_" + (dayCount + 1)).setValue(currentDate);

                            // Save local status
                            saveTicketStatus(eventId, newStatus);

                            showValidTicket();
                        }
                    } else {
                        // Single-day event processing
                        if ("Present".equals(currentStatus) || "Late".equals(currentStatus)) {
                            showUsedTicket();
                        } else if ("pending".equals(currentStatus)) {
                            String newStatus = (timeStatus == 1) ? "Late" : "Present";
                            ticketRef.child("status").setValue(newStatus);
                            ticketRef.child("attendanceDate").setValue(currentDate);
                            saveTicketStatus(eventId, newStatus);
                            showValidTicket();
                        } else {
                            showInvalidTicket();
                        }
                    }
                } else {
                    showInvalidTicket();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                String offlineStatus = getTicketStatus(eventId);
                if (offlineStatus != null) {
                    if ("Present".equals(offlineStatus) || "Late".equals(offlineStatus)) {
                        showUsedTicket();
                    } else {
                        showValidTicket();
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
        qrContent = qrContent.replaceAll("[{}]", ""); // Remove curly braces if present
        String[] pairs = qrContent.split(", ");
        for (String pair : pairs) {
            String[] entry = pair.split("=");
            if (entry.length == 2) {
                map.put(entry[0].trim(), entry[1].trim());
            }
        }
        return map;
    }

    private int checkTimeStatus(Map<String, String> ticketData) {
        try {
            String startDate = ticketData.get("startDate");
            String endDate = ticketData.get("endDate"); // Added for multi-day events
            String startTime = ticketData.get("startTime");
            String endTime = ticketData.get("endTime");
            String graceTimeStr = ticketData.get("graceTime");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            Date currentDate = new Date();
            String currentDateStr = dateFormat.format(currentDate);

            // For multi-day events, check if current date is within event date range
            if (endDate != null && !endDate.isEmpty()) {
                Date eventStartDate = dateFormat.parse(startDate);
                Date eventEndDate = dateFormat.parse(endDate);
                Date currentDateOnly = dateFormat.parse(currentDateStr);

                if (currentDateOnly.before(eventStartDate) || currentDateOnly.after(eventEndDate)) {
                    return 2; // Outside event date range
                }
            }

            // Check time constraints for the current day
            Date eventStartTime = combinedFormat.parse(currentDateStr + " " + startTime);
            Date eventEndTime = combinedFormat.parse(currentDateStr + " " + endTime);

            if (currentDate.after(eventEndTime)) {
                return 2; // Event has ended for today
            }

            if ("none".equalsIgnoreCase(graceTimeStr)) {
                return 0; // No grace period, valid if before end time
            }

            int graceTime = Integer.parseInt(graceTimeStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, graceTime);
            Date validEndTime = calendar.getTime();

            if (currentDate.before(eventStartTime)) {
                return -1; // Too early
            } else if (currentDate.after(validEndTime)) {
                return 1; // Late
            } else {
                return 0; // On time
            }
        } catch (ParseException e) {
            return 1; // Default to "late" if there's a parsing error
        }
    }

    private void showValidTicket() {
        hideAllTicketViews();
        validTicket.setVisibility(ImageView.VISIBLE);
        validText.setVisibility(TextView.VISIBLE);
        scanNewBtn.setVisibility(Button.VISIBLE);
    }

    private void showUsedTicket() {
        hideAllTicketViews();
        usedTicket.setVisibility(ImageView.VISIBLE);
        usedText.setVisibility(TextView.VISIBLE);
        scanNewBtn.setVisibility(Button.VISIBLE);
    }

    private void showInvalidTicket() {
        hideAllTicketViews();
        invalidTicket.setVisibility(ImageView.VISIBLE);
        invalidText.setVisibility(TextView.VISIBLE);
        scanNewBtn.setVisibility(Button.VISIBLE);
    }

    private void showNotAllowedTicket() {
        hideAllTicketViews();
        notAllowedTicket.setVisibility(ImageView.VISIBLE);
        notAllowedText.setVisibility(TextView.VISIBLE);
        scanNewBtn.setVisibility(Button.VISIBLE);
    }

    private void hideAllTicketViews() {
        instructionForScanning.setVisibility(TextView.GONE);
        validTicket.setVisibility(ImageView.GONE);
        validText.setVisibility(TextView.GONE);
        usedTicket.setVisibility(ImageView.GONE);
        usedText.setVisibility(TextView.GONE);
        invalidTicket.setVisibility(ImageView.GONE);
        invalidText.setVisibility(TextView.GONE);
        notAllowedTicket.setVisibility(ImageView.GONE);
        notAllowedText.setVisibility(TextView.GONE);
        scanNewBtn.setVisibility(Button.GONE);
        scanTicketBtn.setVisibility(Button.GONE);
    }
}