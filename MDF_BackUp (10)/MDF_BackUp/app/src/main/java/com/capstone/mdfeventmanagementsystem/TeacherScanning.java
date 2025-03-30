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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TeacherScanning extends AppCompatActivity {

    private TextView instructionForScanning, validText, usedText, invalidText, notAllowedText;
    private ImageView validTicket, usedTicket, invalidTicket, notAllowedTicket;
    private Button scanTicketBtn, cancelScanBtn;

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

        scanTicketBtn.setOnClickListener(v -> startQRScanner());
        cancelScanBtn.setOnClickListener(v -> finish());

        // Initialize Firebase database reference
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.keepSynced(true);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("TicketStatus", MODE_PRIVATE);


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

        int timeStatus = checkTimeStatus(ticketData);

        if (timeStatus == 2) {
            showInvalidTicket();
            return;
        }

        DatabaseReference ticketRef = databaseRef.child("students").child(studentId).child("tickets").child(eventId);
        ticketRef.keepSynced(true);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentStatus = snapshot.child("status").getValue(String.class);
                    saveTicketStatus(eventId, currentStatus);

                    if ("Present".equals(currentStatus)) {
                        showUsedTicket();
                    } else if ("pending".equals(currentStatus)) {
                        String newStatus = (timeStatus == 1) ? "Late" : "Present";
                        ticketRef.child("status").setValue(newStatus);
                        saveTicketStatus(eventId, newStatus);
                        showValidTicket();
                    } else {
                        showInvalidTicket();
                    }
                } else {
                    showInvalidTicket();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                String offlineStatus = getTicketStatus(eventId);
                if (offlineStatus != null) {
                    if ("Present".equals(offlineStatus)) {
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
            String startTime = ticketData.get("startTime");
            String endTime = ticketData.get("endTime");
            String graceTimeStr = ticketData.get("graceTime");

            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date eventStartTime = combinedFormat.parse(startDate + " " + startTime);
            Date eventEndTime = combinedFormat.parse(startDate + " " + endTime);
            Date currentTime = new Date();

            if (currentTime.after(eventEndTime)) {
                return 2; // Event has ended, ticket is invalid
            }

            if ("none".equalsIgnoreCase(graceTimeStr)) {
                return 0; // No grace period, valid if before end time
            }

            int graceTime = Integer.parseInt(graceTimeStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartTime);
            calendar.add(Calendar.MINUTE, graceTime);
            Date validEndTime = calendar.getTime();

            if (currentTime.before(eventStartTime)) {
                return -1; // Too early
            } else if (currentTime.after(validEndTime)) {
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
