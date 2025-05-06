package com.capstone.mdfeventmanagementsystem.Teacher;

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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.Adapters.DuplicateEmailAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EmailAdapter;
import com.capstone.mdfeventmanagementsystem.R;
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

public class EventDetailsFragment extends Fragment {
    private String description;
    private String photoUrl;
    private String eventUID;
    private String eventForValue; // To store the grade level information
    private TextView eventNameTextView, startDateTextView, endDateTextView, startTimeTextView, endTimeTextView, venueTextView, eventSpanTextView,graceTimeTextView, eventTypeTextView, eventForTextView;
    private TextView descriptionTextView;
    private ImageView photoImageView;
    private TextView ticketGeneratedTextView;
    private TextView totalCoordinatorTextView;
    private Button showCoordinatorsBtn, addCoordinatorBtn;
    private EmailAdapter emailAdapter;

    public EventDetailsFragment() {
        // Required empty public constructor
    }

    public static EventDetailsFragment newInstance(String description, String photoUrl, String eventUID, String eventFor) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString("description", description);
        args.putString("photoUrl", photoUrl);
        args.putString("eventUID", eventUID);
        args.putString("eventFor", eventFor);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            description = getArguments().getString("description");
            photoUrl = getArguments().getString("photoUrl");
            eventUID = getArguments().getString("eventUID");
            eventForValue = getArguments().getString("eventFor");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize required views
        descriptionTextView = view.findViewById(R.id.eventDescription);
        photoImageView = view.findViewById(R.id.eventPhotoUrl);
        ticketGeneratedTextView = view.findViewById(R.id.ticket_generated);
        totalCoordinatorTextView = view.findViewById(R.id.total_coordinator);
        showCoordinatorsBtn = view.findViewById(R.id.showCoordinatorsBtn);
        addCoordinatorBtn = view.findViewById(R.id.addCoordinator);

        // Initialize optional additional views (if present in layout)
        try {
            eventNameTextView = view.findViewById(R.id.eventName);
            startDateTextView = view.findViewById(R.id.startDate);
            endDateTextView = view.findViewById(R.id.endDate);
            startTimeTextView = view.findViewById(R.id.startTime);
            endTimeTextView = view.findViewById(R.id.endTime);
            venueTextView = view.findViewById(R.id.venue);
            eventSpanTextView = view.findViewById(R.id.eventSpan);
            graceTimeTextView = view.findViewById(R.id.graceTime);
            eventTypeTextView = view.findViewById(R.id.eventType);
            eventForTextView = view.findViewById(R.id.eventFor);
        } catch (Exception e) {
            // It's okay if some views are not found - they may not be in this layout
            Log.d("EventDetails", "Some optional views not found in layout: " + e.getMessage());
        }
        // Set description if available from arguments
        if (description != null && descriptionTextView != null) {
            descriptionTextView.setText(description);
        }
        // Load image from arguments if URL is available
        if (photoUrl != null && !photoUrl.isEmpty() && photoImageView != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(photoImageView);
        } else if (photoImageView != null) {
            photoImageView.setImageResource(R.drawable.placeholder_image);
        }
        // Set up button click listeners
        if (showCoordinatorsBtn != null) {
            showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());
        }
        if (addCoordinatorBtn != null) {
            addCoordinatorBtn.setOnClickListener(v -> showAddCoordinatorDialog());
        }
        // Fetch complete event details from Firebase
        if (eventUID != null && !eventUID.isEmpty()) {
            getEventDetails(eventUID);
            getTicketCount(eventUID);
            getTotalCoordinators(eventUID);
        }
    }

    // Method to fetch additional event details from Firebase
    private void getEventDetails(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("EventDetails", "Cannot fetch event details: eventId is null or empty");
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return; // Check if fragment is still attached

                if (dataSnapshot.exists()) {
                    // Populate views with event details if they're found in the layout
                    try {
                        if (eventNameTextView != null) {
                            String eventName = dataSnapshot.child("eventName").getValue(String.class);
                            eventNameTextView.setText(eventName != null ? eventName : "N/A");
                        }

                        if (startDateTextView != null) {
                            String startDate = dataSnapshot.child("startDate").getValue(String.class);
                            startDateTextView.setText(startDate != null ? startDate : "N/A");
                        }

                        if (endDateTextView != null) {
                            String endDate = dataSnapshot.child("endDate").getValue(String.class);
                            endDateTextView.setText(endDate != null ? endDate : "N/A");
                        }

                        if (startTimeTextView != null) {
                            String startTime = dataSnapshot.child("startTime").getValue(String.class);
                            // Convert 24-hour time to 12-hour time with AM/PM
                            if (startTime != null && !startTime.isEmpty()) {
                                startTimeTextView.setText(convertTo12HourFormat(startTime));
                            } else {
                                startTimeTextView.setText("N/A");
                            }
                        }

                        if (endTimeTextView != null) {
                            String endTime = dataSnapshot.child("endTime").getValue(String.class);
                            // Convert 24-hour time to 12-hour time with AM/PM
                            if (endTime != null && !endTime.isEmpty()) {
                                endTimeTextView.setText(convertTo12HourFormat(endTime));
                            } else {
                                endTimeTextView.setText("N/A");
                            }
                        }

                        if (venueTextView != null) {
                            String venue = dataSnapshot.child("venue").getValue(String.class);
                            venueTextView.setText(venue != null ? venue : "N/A");
                        }

                        if (eventSpanTextView != null) {
                            String eventSpan = dataSnapshot.child("eventSpan").getValue(String.class);
                            eventSpanTextView.setText(eventSpan != null ? eventSpan : "N/A");
                        }

                        if (graceTimeTextView != null) {
                            String graceTime = dataSnapshot.child("graceTime").getValue(String.class);
                            graceTimeTextView.setText(graceTime != null ? graceTime : "N/A");
                        }

                        if (eventTypeTextView != null) {
                            String eventType = dataSnapshot.child("eventType").getValue(String.class);
                            eventTypeTextView.setText(eventType != null ? eventType : "N/A");
                        }

                        if (eventForTextView != null) {
                            String eventFor = dataSnapshot.child("eventFor").getValue(String.class);
                            eventForTextView.setText(eventFor != null ? eventFor : "N/A");
                        }

                        // If description wasn't passed through args, try to get it from database
                        if ((description == null || description.isEmpty()) && descriptionTextView != null) {
                            String descriptionFromDb = dataSnapshot.child("description").getValue(String.class);
                            if (descriptionFromDb != null && !descriptionFromDb.isEmpty()) {
                                descriptionTextView.setText(descriptionFromDb);
                            }
                        }

                        // If photoUrl wasn't passed through args, try to get it from database
                        if ((photoUrl == null || photoUrl.isEmpty()) && photoImageView != null) {
                            String photoUrlFromDb = dataSnapshot.child("photoUrl").getValue(String.class);
                            if (photoUrlFromDb != null && !photoUrlFromDb.isEmpty()) {
                                Glide.with(EventDetailsFragment.this)
                                        .load(photoUrlFromDb)
                                        .placeholder(R.drawable.placeholder_image)
                                        .error(R.drawable.error_image)
                                        .into(photoImageView);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("EventDetails", "Error setting event details: " + e.getMessage());
                    }
                } else {
                    Log.d("EventDetails", "Event data not found for ID: " + eventId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded()) { // Check if fragment is still attached
                    Log.e("FirebaseError", "Error fetching event details: " + databaseError.getMessage());
                }
            }
        });
    }

    /**
     * Converts time from 24-hour format to 12-hour format with AM/PM
     * @param time24Hour Time in 24-hour format (HH:MM)
     * @return Time in 12-hour format with AM/PM (hh:MM AM/PM)
     */
    private String convertTo12HourFormat(String time24Hour) {
        try {
            // Parse the time string
            String[] timeParts = time24Hour.split(":");
            if (timeParts.length != 2) {
                return time24Hour; // Return original if format is unexpected
            }

            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[1]);

            // Determine AM/PM
            String amPm = (hours >= 12) ? "PM" : "AM";

            // Convert hours to 12-hour format
            if (hours == 0) {
                hours = 12; // 00:XX becomes 12:XX AM
            } else if (hours > 12) {
                hours = hours - 12; // 13:XX becomes 01:XX PM
            }

            // Format the time with leading zeros for hours and minutes if needed
            return String.format("%02d:%02d %s", hours, minutes, amPm);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Log.e("TimeConverter", "Error converting time: " + e.getMessage());
            return time24Hour; // Return original if parsing fails
        }
    }
    // Method to count and display number of tickets for this event
    private void getTicketCount(String eventId) {
        if (eventId == null || eventId.isEmpty() || ticketGeneratedTextView == null) {
            Log.e("TicketCount", "Cannot fetch tickets: eventId is null/empty or view not found");
            if (ticketGeneratedTextView != null) {
                ticketGeneratedTextView.setText("0");
            }
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int ticketCount = 0;

                // Extract the event key from the full path
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

                // Update the UI with the count if fragment is still attached
                if (isAdded() && ticketGeneratedTextView != null) {
                    ticketGeneratedTextView.setText(String.valueOf(ticketCount));
                }
                Log.d("TicketCount", "Final ticket count: " + ticketCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching tickets: " + error.getMessage());
                if (isAdded() && ticketGeneratedTextView != null) {
                    ticketGeneratedTextView.setText("0");
                }
            }
        });
    }

    private void getTotalCoordinators(String eventId) {
        // Ensure eventId is valid before making a Firebase call
        if (eventId == null || eventId.isEmpty() || totalCoordinatorTextView == null) {
            Log.e("OrganizerDetails", "Cannot fetch coordinators: eventId is null or empty or view not found");
            if (isAdded() && totalCoordinatorTextView != null) {
                totalCoordinatorTextView.setText("Event ID is invalid");
            }
            return;
        }

        // Reference to the coordinators node in Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");

        // Listen for real-time updates
        ref.orderByValue().equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if fragment is still attached before updating UI
                if (!isAdded()) return;

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
                if (isAdded() && totalCoordinatorTextView != null) {
                    totalCoordinatorTextView.setText("Error loading coordinators");
                }
            }
        });
    }

    private void showCoordinatorsDialog() {
        if (eventUID == null || eventUID.isEmpty() || !isAdded()) {
            Toast.makeText(getContext(), "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CoordinatorDebug", "Fetching coordinators for event ID: " + eventUID);

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events")
                .child(eventUID).child("eventCoordinators");

        eventRef.orderByValue().equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return; // Check if fragment is still attached

                List<String> coordinatorEmails = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String email = snapshot.getKey();
                    if (email != null) {
                        coordinatorEmails.add(email.replace(",", "."));  // Replace stored commas with dots
                    }
                }

                if (coordinatorEmails.isEmpty()) {
                    Toast.makeText(getContext(), "No coordinators found.", Toast.LENGTH_SHORT).show();
                } else {
                    showCoordinatorEmailDialog(coordinatorEmails);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isAdded()) return; // Check if fragment is still attached

                Log.e("FirebaseError", "Error fetching coordinators: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Error loading coordinator list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCoordinatorEmailDialog(List<String> coordinatorEmails) {
        if (!isAdded()) return; // Check if fragment is still attached

        Log.d("DetailDebug", "showCoordinatorEmailDialog: Starting to show coordinator email dialog.");

        // Check if eventUID is valid
        if (eventUID == null || eventUID.isEmpty()) {
            Log.e("DetailDebug", "showCoordinatorEmailDialog: eventUID is null or empty!");
            Toast.makeText(getContext(), "Error: Invalid event ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log number of coordinators
        Log.d("DetailDebug", "showCoordinatorEmailDialog: Number of coordinator emails: " + coordinatorEmails.size());

        // Create an AlertDialog builder
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Coordinators");

        // Scrollable layout
        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Iterate through coordinator emails
        for (String email : coordinatorEmails) {
            LinearLayout rowLayout = new LinearLayout(getContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(20, 10, 10, 10);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);

            // Email TextView
            TextView emailTextView = new TextView(getContext());
            emailTextView.setText(email);
            emailTextView.setTextSize(16);
            emailTextView.setTextColor(Color.BLACK);
            emailTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            // Delete Button
            Button deleteButton = new Button(getContext());
            deleteButton.setText("Delete");
            deleteButton.setTextColor(Color.RED);
            deleteButton.setBackgroundColor(Color.TRANSPARENT);
            deleteButton.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(getContext())
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
        if (!isAdded()) return; // Check if fragment is still attached

        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(getContext(), "Error: Event ID is invalid!", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailKey = email.replace(".", ",");

        DatabaseReference eventRef = FirebaseDatabase.getInstance()
                .getReference("events")
                .child(eventUID)
                .child("eventCoordinators");

        eventRef.child(emailKey).removeValue().addOnCompleteListener(task -> {
            if (!isAdded()) return; // Check if fragment is still attached

            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Coordinator removed successfully", Toast.LENGTH_SHORT).show();

                // Update UI
                ((ViewGroup) rowLayout.getParent()).removeView(rowLayout);
                getTotalCoordinators(eventUID); // Refresh the count
            } else {
                Toast.makeText(getContext(), "Error removing coordinator", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCoordinatorDialog() {
        if (!isAdded()) return; // Check if fragment is still attached

        Log.d("TestCoordinator", "showAddCoordinatorDialog started.");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_coordinator, null);
        builder.setView(dialogView);

        EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        Button addEmailButton = dialogView.findViewById(R.id.addEmailButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button addCoordinatorButton = dialogView.findViewById(R.id.addCoordinatorButton);
        RecyclerView addedEmailsRecyclerView = dialogView.findViewById(R.id.addedEmailsRecyclerView);

        List<String> addedEmails = new ArrayList<>();
        Set<String> duplicateEmails = new HashSet<>();

        addedEmailsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emailAdapter = new EmailAdapter(addedEmails, email -> {
            addedEmails.remove(email);
            emailAdapter.notifyDataSetChanged();
            if (isAdded()) {
                Toast.makeText(getContext(), "Email removed.", Toast.LENGTH_SHORT).show();
            }
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
            if (isAdded()) {
                Toast.makeText(getContext(), "Adding of Coordinator is cancelled.", Toast.LENGTH_SHORT).show();
            }
            Log.d("TestCoordinator", "Coordinator addition cancelled.");
            alertDialog.dismiss();
        });

        addCoordinatorButton.setOnClickListener(v -> {
            String gradeToSearch = eventForValue != null ? eventForValue.replace("-", " ") : "All"; // Convert format or default to "All"

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
                        if (!isAdded()) return; // Check if fragment is still attached

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
        if (!isAdded()) return; // Check if fragment is still attached

        if (!nonMatchingEmails.isEmpty()) {
            showNonMatchingEmailsDialog(nonMatchingEmails);
        }

        if (!matchingEmails.isEmpty()) {
            addCoordinatorsToFirebase(matchingEmails, alertDialog);
        }
    }

    private void showNonMatchingEmailsDialog(List<String> nonMatchingEmails) {
        if (!isAdded()) return; // Check if fragment is still attached

        StringBuilder message = new StringBuilder("The following emails are not eligible for this event:\n\n");
        for (String email : nonMatchingEmails) {
            message.append(email).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Non-Matching Emails")
                .setMessage(message.toString())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addCoordinatorsToFirebase(List<String> matchingEmails, AlertDialog alertDialog) {
        if (!isAdded()) return; // Check if fragment is still attached

        if (matchingEmails.isEmpty()) {
            Toast.makeText(getContext(), "No valid students to add as coordinators.", Toast.LENGTH_SHORT).show();
            Log.d("TestCoordinator", "No valid students to add as coordinators.");
            alertDialog.dismiss();
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        AtomicInteger processedCount = new AtomicInteger(0);

        for (String email : matchingEmails) {
            eventRef.child("eventCoordinators").child(email.replace(".", ",")).setValue(true)
                    .addOnCompleteListener(task -> {
                        if (!isAdded()) return; // Check if fragment is still attached

                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Coordinator added: " + email, Toast.LENGTH_SHORT).show();
                            Log.d("TestCoordinator", "Coordinator added: " + email);
                        } else {
                            Toast.makeText(getContext(), "Failed to add: " + email, Toast.LENGTH_SHORT).show();
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
        if (!isAdded() || duplicateEmails == null || duplicateEmails.isEmpty()) {
            return;
        }

        android.app.AlertDialog.Builder duplicateDialogBuilder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_duplicate_email, null);
        duplicateDialogBuilder.setView(dialogView);

        // Set up RecyclerView for duplicate emails
        RecyclerView duplicateEmailsRecyclerView = dialogView.findViewById(R.id.duplicateEmailsRecyclerView);
        duplicateEmailsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

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
}