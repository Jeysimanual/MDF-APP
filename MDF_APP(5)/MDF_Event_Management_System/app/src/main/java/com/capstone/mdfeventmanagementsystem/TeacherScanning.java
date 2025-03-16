package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TeacherScanning extends AppCompatActivity {

    private TextView instructionForScanning;
    private TextView validText;
    private TextView usedText;
    private TextView invalidText;
    private TextView notAllowedText;
    private ImageView validTicket;
    private ImageView usedTicket;
    private ImageView invalidTicket;
    private ImageView notAllowedTicket;
    private Button scanTicketBtn;
    private Button cancelScanBtn;

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

        scanTicketBtn.setOnClickListener(v -> startQRScanner());
        cancelScanBtn.setOnClickListener(v -> finish());
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
        if (result != null) {
            if (result.getContents() != null) {
                validateTicket(result.getContents());
            } else {
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void validateTicket(String qrContent) {
        Map<String, String> ticketData = parseQRContent(qrContent);
        String studentId = ticketData.get("studentID");
        String ticketId = ticketData.get("ticketID");
        String eventId = ticketData.get("eventUID");
        String startDate = ticketData.get("startDate");
        String startTime = ticketData.get("startTime");

        if (studentId == null || ticketId == null || eventId == null || startDate == null || startTime == null) {
            showInvalidTicket();
            Toast.makeText(this, "Invalid QR Code: Missing required information", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if current time is within valid window
        int timeStatus = checkTimeStatus(startDate, startTime);
        if (timeStatus != 0) {
            // If event hasn't started yet, show lock icon
            if (timeStatus == -1) {
                showNotAllowedTicket();
                Toast.makeText(this, "This ticket is not allowed to scan yet. Event has not started.", Toast.LENGTH_SHORT).show();
            } else {
                showInvalidTicket();
                Toast.makeText(this, "This ticket has expired. The 30-minute window after event start has passed.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Reference to student's ticket for the event
        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                .getReference("students")
                .child(studentId)
                .child("tickets")
                .child(eventId);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentStatus = snapshot.child("status").getValue(String.class);

                    if ("Present".equals(currentStatus)) {
                        showUsedTicket();
                    } else if ("pending".equals(currentStatus)) {
                        ticketRef.child("status").setValue("Present")
                                .addOnSuccessListener(aVoid -> showValidTicket())
                                .addOnFailureListener(e ->
                                        Toast.makeText(TeacherScanning.this, "Failed to update status!", Toast.LENGTH_SHORT).show()
                                );
                    } else {
                        showInvalidTicket();
                    }
                } else {
                    showInvalidTicket();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherScanning.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Checks the time status of the ticket.
     * @return 0 if the current time is within the valid window,
     *         -1 if the event hasn't started yet,
     *         1 if the event's valid window has passed
     */
    private int checkTimeStatus(String startDate, String startTime) {
        try {
            // Parse date and time from QR code
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            // Combine date and time
            String combinedDateTime = startDate + " " + startTime;
            Date eventStartTime = combinedFormat.parse(combinedDateTime);

            if (eventStartTime == null) {
                return 1; // Invalid time format, treat as expired
            }

            // Get current time
            Date currentTime = new Date();

            // Calculate end of valid window (start time + 30 minutes)
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, 30);
            Date validEndTime = calendar.getTime();

            // Check time status
            if (currentTime.before(eventStartTime)) {
                return -1; // Event hasn't started yet
            } else if (currentTime.after(validEndTime)) {
                return 1;  // Event's valid window has passed
            } else {
                return 0;  // Current time is within valid window
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return 1; // Error in parsing, treat as expired
        }
    }

    private Map<String, String> parseQRContent(String qrContent) {
        Map<String, String> map = new HashMap<>();
        qrContent = qrContent.replaceAll("[{}]", "");

        String[] pairs = qrContent.split(", ");
        for (String pair : pairs) {
            String[] entry = pair.split("=");
            if (entry.length == 2) {
                map.put(entry[0].trim(), entry[1].trim());
            }
        }
        return map;
    }

    private void showValidTicket() {
        hideAllTicketViews();
        validTicket.setVisibility(ImageView.VISIBLE);
        validText.setVisibility(TextView.VISIBLE);
    }

    private void showUsedTicket() {
        hideAllTicketViews();
        usedTicket.setVisibility(ImageView.VISIBLE);
        usedText.setVisibility(TextView.VISIBLE);
    }

    private void showInvalidTicket() {
        hideAllTicketViews();
        invalidTicket.setVisibility(ImageView.VISIBLE);
        invalidText.setVisibility(TextView.VISIBLE);
    }

    private void showNotAllowedTicket() {
        hideAllTicketViews();
        notAllowedTicket.setVisibility(ImageView.VISIBLE);
        notAllowedText.setVisibility(TextView.VISIBLE);
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
    }
}