package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.capstone.mdfeventmanagementsystem.Adapters.DuplicateEmailAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EmailAdapter;
import com.capstone.mdfeventmanagementsystem.R;
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

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, totalCoordinatorTextView, graceTime, eventType, eventFor;
    private TextView ticketGeneratedTextView; // Added for ticket counting
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
        totalCoordinatorTextView = findViewById(R.id.total_coordinator);

        // Initialize the ticket count TextView
        ticketGeneratedTextView = findViewById(R.id.ticket_generated);

        // Check if any view is null
        if (eventName == null || eventDescription == null || startDate == null || endDate == null ||
                startTime == null || endTime == null || venue == null || eventSpan == null ||
                graceTime == null || eventType == null || eventFor == null || eventImage == null ||
                ticketGeneratedTextView == null) {
            Log.e("UI_ERROR", "One or more views are null. Check IDs in XML.");
            Toast.makeText(this, "Error loading UI components!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        getTotalCoordinators(eventUID);
        getTicketCount(eventUID); // Added to count tickets for this event

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

    // New method to count and display number of tickets for this event
    private void getTicketCount(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("TicketCount", "Cannot fetch tickets: eventId is null or empty");
            ticketGeneratedTextView.setText("0");
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int ticketCount = 0;

                // Extract the event key from the full path
                // This is important if eventId is the full path rather than just the key
                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                Log.d("TicketCount", "Searching for tickets matching event key: " + eventKey);

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");

                        // Check if this student has a ticket for this event
                        // by comparing the ticket key to the event key or looking for eventUID inside each ticket
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            Log.d("TicketCount", "Checking ticket: " + ticketKey);

                            // Method 1: Check if ticket key matches the event key
                            if (eventKey.equals(ticketKey)) {
                                ticketCount++;
                                Log.d("TicketCount", "Found matching ticket by key: " + ticketKey);
                                continue; // Found a match, move to next ticket
                            }

                            // Method 2: Look for an eventUID field in the ticket
                            if (ticketSnapshot.hasChild("eventUID")) {
                                String ticketEventUID = ticketSnapshot.child("eventUID").getValue(String.class);
                                if (eventId.equals(ticketEventUID) || eventKey.equals(ticketEventUID)) {
                                    ticketCount++;
                                    Log.d("TicketCount", "Found matching ticket by eventUID field: " + ticketEventUID);
                                }
                            }
                        }
                    }
                }

                // Update the UI with only the number
                ticketGeneratedTextView.setText(String.valueOf(ticketCount));
                Log.d("TicketCount", "Final ticket count: " + ticketCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching tickets: " + error.getMessage());
                ticketGeneratedTextView.setText("0");
            }
        });
    }

    private void showAddCoordinatorDialog() {
        Log.d("TestCoordinator", "showAddCoordinatorDialog started.");

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

            Log.d("TestCoordinator", "addEmailButton clicked, processing emails: " + emailInput);

            for (String individualEmail : emails) {
                String trimmedEmail = individualEmail.trim();
                if (!trimmedEmail.isEmpty() && !seenEmails.contains(trimmedEmail) &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    seenEmails.add(trimmedEmail);
                    filteredEmails.add(trimmedEmail);
                    Log.d("TestCoordinator", "Email added to filtered list: " + trimmedEmail);
                }
            }

            for (String email : filteredEmails) {
                if (addedEmails.contains(email)) {
                    duplicateEmails.add(email);
                    Log.d("TestCoordinator", "Duplicate email found: " + email);
                } else {
                    addedEmails.add(email);
                    Log.d("TestCoordinator", "Email added to addedEmails list: " + email);
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
            Log.d("TestCoordinator", "Coordinator addition cancelled.");
            alertDialog.dismiss();
        });

        addCoordinatorButton.setOnClickListener(v -> {
            String eventForValue = eventFor.getText().toString().trim();
            String gradeToSearch = eventForValue.replace("-", " "); // Convert format: Grade-8 → Grade 8

            Log.d("TestCoordinator", "addCoordinatorButton clicked, eventForValue: " + eventForValue);
            Log.d("TestCoordinator", "Searching for students with yearLevel: " + gradeToSearch);

            List<String> matchingEmails = new ArrayList<>();
            List<String> nonMatchingEmails = new ArrayList<>();
            AtomicInteger processedCount = new AtomicInteger(0); // Track completion of Firebase calls

            for (String email : addedEmails) {
                findMatchingStudentByEmail(email, gradeToSearch, matchingEmails, nonMatchingEmails, processedCount, addedEmails.size(), alertDialog);
            }
        });

        alertDialog.show();
    }

    private void findMatchingStudentByEmail(String emailToSearch, String gradeToSearch, List<String> matchingEmails,
                                            List<String> nonMatchingEmails, AtomicInteger processedCount, int totalEmails, AlertDialog alertDialog) {
        Log.d("TestCoordinator", "findMatchingStudentByEmail started, searching for email: " + emailToSearch + " and yearLevel: " + gradeToSearch);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference studentsRef = database.getReference("students");

        studentsRef.orderByChild("email").equalTo(emailToSearch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean emailAdded = false;

                        if (dataSnapshot.exists()) {
                            for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                                String studentEmail = studentSnapshot.child("email").getValue(String.class);
                                String studentYearLevel = studentSnapshot.child("yearLevel").getValue(String.class);

                                Log.d("TestCoordinator", "Found student email: " + studentEmail + ", yearLevel: " + studentYearLevel);

                                // If eventFor is "All", add without filtering
                                if ("All".equalsIgnoreCase(gradeToSearch) || gradeToSearch.equals(studentYearLevel)) {
                                    Log.d("TestCoordinator", "Matching student found: " + studentEmail);
                                    matchingEmails.add(studentEmail);
                                    emailAdded = true;
                                    break; // No need to check more if a match is found
                                }
                            }
                        }

                        if (!emailAdded) {
                            Log.d("TestCoordinator", "No matching student for: " + emailToSearch);
                            nonMatchingEmails.add(emailToSearch);
                        }

                        if (processedCount.incrementAndGet() == totalEmails) {
                            processFinalCoordinatorAddition(matchingEmails, nonMatchingEmails, alertDialog);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("TestCoordinator", "Error fetching students: " + databaseError.getMessage());
                        processedCount.incrementAndGet();
                    }
                });
    }

    private void processFinalCoordinatorAddition(List<String> matchingEmails, List<String> nonMatchingEmails, AlertDialog alertDialog) {
        if (!nonMatchingEmails.isEmpty()) {
            showNonMatchingEmailsDialog(nonMatchingEmails);
        }

        if (!matchingEmails.isEmpty()) {
            addCoordinatorsToFirebase(matchingEmails, alertDialog);
        }
    }

    private void showNonMatchingEmailsDialog(List<String> nonMatchingEmails) {
        StringBuilder message = new StringBuilder("The following emails are not eligible for this event:\n\n");
        for (String email : nonMatchingEmails) {
            message.append(email).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Non-Matching Emails")
                .setMessage(message.toString())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addCoordinatorsToFirebase(List<String> matchingEmails, AlertDialog alertDialog) {
        if (matchingEmails.isEmpty()) {
            Toast.makeText(this, "No valid students to add as coordinators.", Toast.LENGTH_SHORT).show();
            Log.d("TestCoordinator", "No valid students to add as coordinators.");
            alertDialog.dismiss();
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        AtomicInteger processedCount = new AtomicInteger(0);

        for (String email : matchingEmails) {
            eventRef.child("eventCoordinators").child(email.replace(".", ",")).setValue(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Coordinator added: " + email, Toast.LENGTH_SHORT).show();
                            Log.d("TestCoordinator", "Coordinator added: " + email);
                        } else {
                            Toast.makeText(this, "Failed to add: " + email, Toast.LENGTH_SHORT).show();
                            Log.d("TestCoordinator", "Failed to add: " + email);
                        }

                        if (processedCount.incrementAndGet() == matchingEmails.size()) {
                            alertDialog.dismiss();
                            Log.d("TestCoordinator", "All coordinators processed.");

                            // Update coordinator count after adding new coordinators
                            getTotalCoordinators(eventUID);
                        }
                    });
        }
    }

    private void showDuplicateEmailDialog(List<String> duplicateEmails) {
        if (duplicateEmails == null || duplicateEmails.isEmpty()) {
            return;
        }

        android.app.AlertDialog.Builder duplicateDialogBuilder = new android.app.AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_duplicate_email, null);
        duplicateDialogBuilder.setView(dialogView);

        // Set up RecyclerView for duplicate emails
        RecyclerView duplicateEmailsRecyclerView = dialogView.findViewById(R.id.duplicateEmailsRecyclerView);
        duplicateEmailsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create and set the adapter for duplicate emails
        DuplicateEmailAdapter duplicateEmailAdapter = new DuplicateEmailAdapter(duplicateEmails);
        duplicateEmailsRecyclerView.setAdapter(duplicateEmailAdapter);

        // Set up the dialog
        duplicateDialogBuilder.setTitle("Duplicate Emails");
        duplicateDialogBuilder.setPositiveButton("Okay", (dialog, which) -> {
            dialog.dismiss(); // Close the dialog
            duplicateEmails.clear(); // Clear the duplicateEmails list for the next batch
            duplicateEmailAdapter.notifyDataSetChanged();
        });

        duplicateDialogBuilder.show();
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
        Log.d("DetailDebug", "showCoordinatorEmailDialog: Starting to show coordinator email dialog.");

        // Check if eventUID is valid
        if (eventUID == null || eventUID.isEmpty()) {
            Log.e("DetailDebug", "showCoordinatorEmailDialog: eventUID is null or empty!");
            Toast.makeText(this, "Error: Invalid event ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log number of coordinators
        Log.d("DetailDebug", "showCoordinatorEmailDialog: Number of coordinator emails: " + coordinatorEmails.size());

        // Create an AlertDialog builder
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Coordinators");

        // Scrollable layout
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Iterate through coordinator emails
        for (String email : coordinatorEmails) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(20, 10, 10, 10);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);

            // Email TextView
            TextView emailTextView = new TextView(this);
            emailTextView.setText(email);
            emailTextView.setTextSize(16);
            emailTextView.setTextColor(Color.BLACK);
            emailTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            // Delete Button
            Button deleteButton = new Button(this);
            deleteButton.setText("Delete");
            deleteButton.setTextColor(Color.RED);
            deleteButton.setBackgroundColor(Color.TRANSPARENT);
            deleteButton.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to remove " + email + " from this event?")
                        .setPositiveButton("Yes", (dialog, which) -> removeCoordinatorFromEvent(email, rowLayout))
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            });

            // Add views to row layout
            rowLayout.addView(emailTextView);
            rowLayout.addView(deleteButton);

            // Add row to main layout
            layout.addView(rowLayout);
        }

        // Wrap the layout inside a ScrollView
        scrollView.addView(layout);
        builder.setView(scrollView);

        // Add a close button
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.create().show();
    }

    private void removeCoordinatorFromEvent(String email, LinearLayout rowLayout) {
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(this, "Error: Event ID is invalid!", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailKey = email.replace(".", ",");

        DatabaseReference eventRef = FirebaseDatabase.getInstance()
                .getReference("events")
                .child(eventUID)
                .child("eventCoordinators");

        eventRef.child(emailKey).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Coordinator removed successfully", Toast.LENGTH_SHORT).show();

                // Ensure UI updates happen on the main thread
                runOnUiThread(() -> {
                    ((ViewGroup) rowLayout.getParent()).removeView(rowLayout);
                    getTotalCoordinators(eventUID); // Refresh the count
                });
            } else {
                Toast.makeText(this, "Error removing coordinator", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getTotalCoordinators(String eventId) {
        // Ensure eventId is valid before making a Firebase call
        if (eventId == null || eventId.isEmpty()) {
            Log.e("OrganizerDetails", "Cannot fetch coordinators: eventId is null or empty");
            totalCoordinatorTextView.setText("Event ID is invalid");
            return;
        }

        // Reference to the coordinators node in Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");

        // Listen for real-time updates
        ref.orderByValue().equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if coordinators exist
                if (snapshot.exists()) {
                    long coordinatorCount = snapshot.getChildrenCount(); // Count coordinators
                    totalCoordinatorTextView.setText(coordinatorCount + " Coordinator(s)");
                } else {
                    totalCoordinatorTextView.setText("0 Coordinator(s)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching coordinators: " + error.getMessage());
                totalCoordinatorTextView.setText("Error loading coordinators");
            }
        });
    }
}