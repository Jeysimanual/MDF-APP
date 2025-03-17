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
        String endTime = ticketData.get("endTime");
        String graceTimeStr = ticketData.get("graceTime");

        if (studentId == null || ticketId == null || eventId == null || startDate == null || startTime == null || endTime == null || graceTimeStr == null) {
            showInvalidTicket();
            Toast.makeText(this, "Invalid QR Code: Missing required information", Toast.LENGTH_SHORT).show();
            return;
        }

        int timeStatus = checkTimeStatus(startDate, startTime, endTime, graceTimeStr);

        if (timeStatus == 2) {
            showInvalidTicket();
            Toast.makeText(this, "This ticket is invalid. Event has ended.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        String newStatus = (timeStatus == 1) ? "Late" : "Present";
                        ticketRef.child("status").setValue(newStatus)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(TeacherScanning.this, "Ticket is valid and marked as " + newStatus + ".", Toast.LENGTH_SHORT).show();
                                    showValidTicket();
                                })
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

    private int checkTimeStatus(String startDate, String startTime, String endTime, String graceTimeStr) {
        try {
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            String combinedStartDateTime = startDate + " " + startTime;
            Date eventStartTime = combinedFormat.parse(combinedStartDateTime);
            Date currentTime = new Date();
            String combinedEndDateTime = startDate + " " + endTime;
            Date eventEndTime = combinedFormat.parse(combinedEndDateTime);

            if (eventEndTime != null && currentTime.after(eventEndTime)) {
                return 2;
            }

            if (graceTimeStr.equalsIgnoreCase("none")) {
                return 0;
            }

            int graceTime = Integer.parseInt(graceTimeStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, graceTime);
            Date validEndTime = calendar.getTime();

            if (currentTime.before(eventStartTime)) {
                return -1;
            } else if (currentTime.after(validEndTime)) {
                return 1;
            } else {
                return 0;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return 1;
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