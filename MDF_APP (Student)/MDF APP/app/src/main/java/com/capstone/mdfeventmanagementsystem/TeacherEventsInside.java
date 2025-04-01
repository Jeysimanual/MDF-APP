package com.capstone.mdfeventmanagementsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TeacherEventsInside extends AppCompatActivity {

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, ticketType, graceTime, eventType, eventFor;
    private ImageView eventImage;
    private String eventUID; // Store event UID
    private Button showCoordinatorsBtn, addCoordinatorBtn;
    private EmailAdapter emailAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events_inside);


        showCoordinatorsBtn = findViewById(R.id.showCoordinatorsBtn);
        // Get event UID from Intent
        eventUID = getIntent().getStringExtra("eventUID");

        showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());

        addCoordinatorBtn = findViewById(R.id.addCoordinator);
        addCoordinatorBtn.setOnClickListener(v -> showAddCoordinatorDialog());

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
        eventType = findViewById(R.id.eventType);
        eventFor = findViewById(R.id.eventFor);
        eventImage = findViewById(R.id.eventPhotoUrl);

        // Check if any view is null
        if (eventName == null || eventDescription == null || startDate == null || endDate == null ||
                startTime == null || endTime == null || venue == null || eventSpan == null ||
                graceTime == null || eventType == null || eventFor == null || eventImage == null) {
            Log.e("UI_ERROR", "One or more views are null. Check IDs in XML.");
            Toast.makeText(this, "Error loading UI components!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get event details from intent
        Intent intent = getIntent();
        if (intent != null) {
            eventUID = intent.getStringExtra("eventUID"); // Store event UID
            String name = intent.getStringExtra("eventName");
            String description = intent.getStringExtra("eventDescription");
            String start = intent.getStringExtra("startDate");
            String end = intent.getStringExtra("endDate");
            String startT = intent.getStringExtra("startTime");
            String endT = intent.getStringExtra("endTime");
            String eventVenue = intent.getStringExtra("venue");
            String span = intent.getStringExtra("eventSpan");
            String grace = intent.getStringExtra("graceTime");
            String eType = intent.getStringExtra("eventType"); // Fetch event type
            String eFor = intent.getStringExtra("eventFor"); // Fetch event for
            String eventPhotoUrl = intent.getStringExtra("eventPhotoUrl");

            // Log fetched data with "dataTest" tag
            Log.d("dataTest", "Event UID: " + (eventUID != null ? eventUID : "NULL"));
            Log.d("dataTest", "Event Name: " + (name != null ? name : "NULL"));
            Log.d("dataTest", "Event Description: " + (description != null ? description : "NULL"));
            Log.d("dataTest", "Start Date: " + (start != null ? start : "NULL"));
            Log.d("dataTest", "End Date: " + (end != null ? end : "NULL"));
            Log.d("dataTest", "Start Time: " + (startT != null ? startT : "NULL"));
            Log.d("dataTest", "End Time: " + (endT != null ? endT : "NULL"));
            Log.d("dataTest", "Venue: " + (eventVenue != null ? eventVenue : "NULL"));
            Log.d("dataTest", "Event Span: " + (span != null ? span : "NULL"));
            Log.d("dataTest", "Grace Time: " + (grace != null ? grace : "NULL"));
            Log.d("dataTest", "Event Type: " + (eType != null ? eType : "NULL"));
            Log.d("dataTest", "Event For: " + (eFor != null ? eFor : "NULL"));
            Log.d("dataTest", "Event Photo URL: " + (eventPhotoUrl != null ? eventPhotoUrl : "NULL"));

            // Set data to UI with null checks
            eventName.setText(name != null ? name : "N/A");
            eventDescription.setText(description != null ? description : "N/A");
            startDate.setText(start != null ? start : "N/A");
            endDate.setText(end != null ? end : "N/A");
            startTime.setText(startT != null ? startT : "N/A");
            endTime.setText(endT != null ? endT : "N/A");
            venue.setText(eventVenue != null ? eventVenue : "N/A");
            eventSpan.setText(span != null ? span : "N/A");
            graceTime.setText(grace != null ? grace : "N/A");
            eventType.setText(eType != null ? eType : "N/A");
            eventFor.setText(eFor != null ? eFor : "N/A");

            // Load event image using Glide
            if (eventPhotoUrl != null && !eventPhotoUrl.isEmpty()) {
                Glide.with(this)
                        .load(eventPhotoUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            Toast.makeText(this, "Error loading event details!", Toast.LENGTH_SHORT).show();
            Log.e("dataTest", "No event data found in intent.");
            finish();
        }
    }

    private void showAddCoordinatorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_coordinator, null);
        builder.setView(dialogView);

        EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        Button addEmailButton = dialogView.findViewById(R.id.addEmailButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button addCoordinatorButton = dialogView.findViewById(R.id.addCoordinatorButton);
        RecyclerView addedEmailsRecyclerView = dialogView.findViewById(R.id.addedEmailsRecyclerView);

        List<String> addedEmails = new ArrayList<>();
        Set<String> duplicateEmails = new HashSet<>();

        addedEmailsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        emailAdapter = new EmailAdapter(addedEmails, email -> {
            addedEmails.remove(email);
            emailAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Email removed.", Toast.LENGTH_SHORT).show();
        });

        addedEmailsRecyclerView.setAdapter(emailAdapter);

        AlertDialog alertDialog = builder.create();

        addEmailButton.setOnClickListener(v -> {
            String emailInput = emailEditText.getText().toString().trim();
            String[] emails = emailInput.split(",");
            Set<String> seenEmails = new HashSet<>();
            List<String> filteredEmails = new ArrayList<>();

            for (String individualEmail : emails) {
                String trimmedEmail = individualEmail.trim();
                if (!trimmedEmail.isEmpty() && !seenEmails.contains(trimmedEmail) &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    seenEmails.add(trimmedEmail);
                    filteredEmails.add(trimmedEmail);
                }
            }

            for (String email : filteredEmails) {
                if (addedEmails.contains(email)) {
                    duplicateEmails.add(email);
                } else {
                    addedEmails.add(email);
                }
            }

            if (!duplicateEmails.isEmpty()) {
                showDuplicateEmailDialog(new ArrayList<>(duplicateEmails));
            }

            emailAdapter.notifyDataSetChanged();
            emailEditText.setText("");
        });

        cancelButton.setOnClickListener(v -> {
            Toast.makeText(this, "Adding of Coordinator is cancelled.", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });

        addCoordinatorButton.setOnClickListener(v -> {
            if (!addedEmails.isEmpty()) {
                addCoordinatorsToFirebase(addedEmails, alertDialog);
            } else {
                Toast.makeText(this, "No coordinators to add.", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.show();
    }

    private void addCoordinatorsToFirebase(List<String> addedEmails, AlertDialog alertDialog) {
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);

        AtomicInteger processedCount = new AtomicInteger(0);
        for (String email : addedEmails) {
            eventRef.child("eventCoordinators").child(email.replace(".", ",")).setValue(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Coordinator added: " + email, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to add: " + email, Toast.LENGTH_SHORT).show();
                        }

                        if (processedCount.incrementAndGet() == addedEmails.size()) {
                            alertDialog.dismiss();
                        }
                    });
        }
    }

    private void showDuplicateEmailDialog(List<String> duplicateEmails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Duplicate Emails Found");
        builder.setMessage("The following emails are duplicates:\n" + String.join("\n", duplicateEmails));
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void showCoordinatorsDialog() {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CoordinatorDebug", "Fetching coordinators for event ID: " + eventUID);

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events")
                .child(eventUID).child("eventCoordinators");

        eventRef.orderByValue().equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> coordinatorEmails = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String email = snapshot.getKey();
                    if (email != null) {
                        coordinatorEmails.add(email.replace(",", "."));  // Replace stored commas with dots
                    }
                }

                if (coordinatorEmails.isEmpty()) {
                    Toast.makeText(TeacherEventsInside.this, "No coordinators found.", Toast.LENGTH_SHORT).show();
                } else {
                    showCoordinatorEmailDialog(coordinatorEmails);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching coordinators: " + databaseError.getMessage());
                Toast.makeText(TeacherEventsInside.this, "Error loading coordinator list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCoordinatorEmailDialog(List<String> coordinatorEmails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Event Coordinators");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        for (String email : coordinatorEmails) {
            TextView emailTextView = new TextView(this);
            emailTextView.setText(email);
            emailTextView.setTextSize(16);
            emailTextView.setTextColor(Color.BLACK);
            emailTextView.setPadding(20, 10, 10, 10);

            layout.addView(emailTextView);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}

