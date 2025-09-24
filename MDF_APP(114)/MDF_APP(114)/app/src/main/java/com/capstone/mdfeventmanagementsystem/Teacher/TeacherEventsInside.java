package com.capstone.mdfeventmanagementsystem.Teacher;

import android.annotation.SuppressLint;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.Adapters.DuplicateEmailAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EmailAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.EventTabsPagerAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
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

public class TeacherEventsInside extends BaseActivity {

    private TextView eventName, eventDescription, startDate, endDate, startTime, endTime, venue, eventSpan, totalCoordinatorTextView, graceTime, eventType, eventFor;
    private TextView ticketGeneratedTextView;
    private ImageView eventImage;
    private String eventId;
    private Button showCoordinatorsBtn, addCoordinatorBtn;
    private EmailAdapter emailAdapter;
    private List<String> coordinatorEmails;

    // Tab UI elements
    private ViewPager2 viewPager;
    private TextView tabEventDetails, tabParticipants;
    private EventTabsPagerAdapter tabAdapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_events_inside);

        // Initialize tab-related UI elements
        viewPager = findViewById(R.id.viewPager);
        tabEventDetails = findViewById(R.id.tabEventDetails);
        tabParticipants = findViewById(R.id.tabParticipants);

        // Initialize regular UI elements
        eventName = findViewById(R.id.eventName);
        eventDescription = findViewById(R.id.eventDescription);
        eventImage = findViewById(R.id.event_image);
        totalCoordinatorTextView = findViewById(R.id.totalCoordinatorTextView);
        ticketGeneratedTextView = findViewById(R.id.ticketGeneratedTextView);

        // Initialize coordinator list and adapter
        coordinatorEmails = new ArrayList<>();
        emailAdapter = new EmailAdapter(coordinatorEmails, email -> {
            DatabaseReference coordinatorsRef = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");
            coordinatorsRef.child(email).removeValue();
            getTotalCoordinators(eventId);
        });

        // Get event details from Intent
        Intent intent = getIntent();
        if (intent != null) {
            eventId = intent.getStringExtra("eventId");
            String name = intent.getStringExtra("eventName");
            String description = intent.getStringExtra("eventDescription");
            String eventPhotoUrl = intent.getStringExtra("eventPhotoUrl");
            String eventForValue = intent.getStringExtra("eventFor");
            boolean isRead = intent.getBooleanExtra("isRead", false);

            // Set data to UI with null checks
            if (eventName != null) {
                eventName.setText(name != null ? name : "N/A");
            }
            if (eventDescription != null) {
                eventDescription.setText(description != null ? description : "No description available");
            }
            if (eventImage != null && eventPhotoUrl != null && !eventPhotoUrl.isEmpty()) {
                Glide.with(this)
                        .load(eventPhotoUrl)
                        .placeholder(R.drawable.placeholder_image) // Add a placeholder image in res/drawable
                        .error(R.drawable.error_image) // Add an error image in res/drawable
                        .into(eventImage);
            } else if (eventImage != null) {
                eventImage.setImageResource(R.drawable.placeholder_image); // Fallback image
            }

            // Setup ViewPager2 with tabs
            setupTabs(eventId, description, eventPhotoUrl, eventForValue);

            // Load data for coordinators and tickets
            if (eventId != null) {
                getTotalCoordinators(eventId);
                getTicketCount(eventId);

                // Update isRead status in Firebase if not already read
                if (!isRead) {
                    DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notification_items").child(eventId);
                    notificationRef.child("isRead").setValue(true);
                }
            }
        } else {
            Toast.makeText(this, "Error loading event details!", Toast.LENGTH_SHORT).show();
            Log.e("dataTest", "No event data found in intent.");
            finish();
        }

        // Set button click listeners
        showCoordinatorsBtn = findViewById(R.id.showCoordinatorsBtn);
        addCoordinatorBtn = findViewById(R.id.addCoordinator);
        if (showCoordinatorsBtn != null) {
            showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());
        }

        if (addCoordinatorBtn != null) {
            addCoordinatorBtn.setOnClickListener(v -> showAddCoordinatorDialog());
        }

        // Setup back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Load event details from Firebase if eventId is available
        if (eventId != null && !eventId.isEmpty()) {
            DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);
            eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("eventName").getValue(String.class);
                        String description = snapshot.child("eventDescription").getValue(String.class);
                        String photoUrl = snapshot.child("eventPhotoUrl").getValue(String.class);
                        String eventForValue = snapshot.child("eventFor").getValue(String.class);

                        if (eventName != null) {
                            eventName.setText(name != null ? name : "N/A");
                        }
                        if (eventDescription != null) {
                            eventDescription.setText(description != null ? description : "No description available");
                        }
                        if (eventImage != null && photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(TeacherEventsInside.this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.placeholder_image)
                                    .error(R.drawable.error_image)
                                    .into(eventImage);
                        } else if (eventImage != null) {
                            eventImage.setImageResource(R.drawable.placeholder_image);
                        }

                        // Update tabs with fetched data
                        setupTabs(eventId, description, photoUrl, eventForValue);
                    } else {
                        Log.e("TeacherEventsInside", "No event found with eventId: " + eventId);
                        Toast.makeText(TeacherEventsInside.this, "Event not found!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseError", "Error querying events: " + error.getMessage());
                    Toast.makeText(TeacherEventsInside.this, "Error loading event!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("TeacherEventsInside", "eventId is null or empty");
            Toast.makeText(this, "Invalid event ID!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showCoordinatorsDialog() {
        Log.d("DialogDebug", "Attempting to show coordinators dialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_coordinators, null);

        if (dialogView == null) {
            Log.e("DialogError", "Failed to inflate dialog_coordinators.xml");
            Toast.makeText(this, "Error inflating dialog layout.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("DialogDebug", "dialogView inflated successfully. Number of coordinator emails: " + coordinatorEmails.size());

        builder.setView(dialogView);

        TextView coordinatorsTitle = dialogView.findViewById(R.id.coordinatorsTitle);
        RecyclerView coordinatorsRecyclerView = dialogView.findViewById(R.id.coordinatorsRecyclerView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        Switch scanToggle = dialogView.findViewById(R.id.scanToggle);
        TextView scanLabel = dialogView.findViewById(R.id.scanLabel);

        Log.d("DialogDebug", "coordinatorsTitle: " + (coordinatorsTitle != null ? "Found" : "NOT Found"));
        Log.d("DialogDebug", "coordinatorsRecyclerView: " + (coordinatorsRecyclerView != null ? "Found" : "NOT Found"));
        Log.d("DialogDebug", "closeButton: " + (closeButton != null ? "Found" : "NOT Found"));
        Log.d("DialogDebug", "scanToggle: " + (scanToggle != null ? "Found" : "NOT Found"));
        Log.d("DialogDebug", "scanLabel: " + (scanLabel != null ? "Found" : "NOT Found"));

        if (coordinatorsTitle == null || coordinatorsRecyclerView == null || closeButton == null || scanToggle == null || scanLabel == null) {
            Log.e("DialogError", "One or more dialog components not found in dialog_coordinators.xml");
            Toast.makeText(this, "Error loading dialog components.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DialogDebug", "Initial coordinatorsTitle visibility: " + (coordinatorsTitle.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        Log.d("DialogDebug", "Initial coordinatorsRecyclerView visibility: " + (coordinatorsRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        Log.d("DialogDebug", "Initial closeButton visibility: " + (closeButton.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        Log.d("DialogDebug", "Initial scanToggle visibility: " + (scanToggle.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        Log.d("DialogDebug", "Initial scanLabel visibility: " + (scanLabel.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));

        if (coordinatorsRecyclerView != null) {
            coordinatorsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            coordinatorsRecyclerView.setAdapter(emailAdapter);
            Log.d("DialogDebug", "RecyclerView set up with adapter. Item count: " + emailAdapter.getItemCount());
        } else {
            Log.e("DialogError", "coordinatorsRecyclerView is null, cannot set up");
        }

        if (scanLabel != null && scanToggle != null) {
            scanLabel.setText("Enable Scanning");
            scanToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d("ToggleDebug", "Scan toggle changed to: " + isChecked);
                DatabaseReference scanRef = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("scanEnabled");
                scanRef.setValue(isChecked);
                if (coordinatorsRecyclerView != null) {
                    coordinatorsRecyclerView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
                if (isChecked) {
                    loadCoordinators(eventId);
                } else {
                    coordinatorEmails.clear();
                    if (emailAdapter != null) {
                        emailAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(this, "Scanning disabled, coordinators cleared.", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d("DialogDebug", "Scan toggle listener set");
        } else {
            Log.e("DialogError", "scanLabel or scanToggle is null, cannot set listener");
        }

        if (scanToggle != null) {
            DatabaseReference scanRef = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("scanEnabled");
            scanRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean isEnabled = snapshot.getValue(Boolean.class);
                    Log.d("ToggleDebug", "Fetched scanEnabled state: " + isEnabled);
                    if (isEnabled != null && isEnabled) {
                        scanToggle.setChecked(true);
                        if (coordinatorsRecyclerView != null) {
                            coordinatorsRecyclerView.setVisibility(View.VISIBLE);
                        }
                        loadCoordinators(eventId);
                    } else {
                        scanToggle.setChecked(false);
                        if (coordinatorsRecyclerView != null) {
                            coordinatorsRecyclerView.setVisibility(View.GONE);
                        }
                        coordinatorEmails.clear();
                        if (emailAdapter != null) {
                            emailAdapter.notifyDataSetChanged();
                        }
                    }
                    Log.d("DialogDebug", "Post-state scanToggle visibility: " + (scanToggle.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("FirebaseError", "Error fetching scan state: " + error.getMessage());
                    if (scanToggle != null) {
                        scanToggle.setChecked(false);
                    }
                    if (coordinatorsRecyclerView != null) {
                        coordinatorsRecyclerView.setVisibility(View.GONE);
                    }
                    coordinatorEmails.clear();
                    if (emailAdapter != null) {
                        emailAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(TeacherEventsInside.this, "Error loading scan state.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("DialogError", "scanToggle is null, cannot set initial state");
        }

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                Log.d("DialogDebug", "Close button clicked, dismissing dialog");
                builder.create().dismiss();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        try {
            dialog.show();
            Log.d("DialogDebug", "Dialog shown successfully");
        } catch (Exception e) {
            Log.e("DialogError", "Failed to show dialog: " + e.getMessage());
            Toast.makeText(this, "Error displaying dialog.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCoordinators(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("FirebaseError", "Invalid eventId for loading coordinators");
            coordinatorEmails.clear();
            if (emailAdapter != null) {
                emailAdapter.notifyDataSetChanged();
            }
            Toast.makeText(this, "Invalid event ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference coordinatorsRef = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");
        coordinatorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                coordinatorEmails.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot coordinatorSnapshot : snapshot.getChildren()) {
                        String email = coordinatorSnapshot.getKey();
                        if (email != null) {
                            coordinatorEmails.add(email);
                        }
                    }
                    if (coordinatorEmails.isEmpty()) {
                        Toast.makeText(TeacherEventsInside.this, "No coordinators available.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TeacherEventsInside.this, "No coordinators available.", Toast.LENGTH_SHORT).show();
                }
                if (emailAdapter != null) {
                    emailAdapter.notifyDataSetChanged();
                }
                Log.d("CoordinatorDebug", "Loaded " + coordinatorEmails.size() + " coordinators");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching coordinators: " + error.getMessage());
                coordinatorEmails.clear();
                if (emailAdapter != null) {
                    emailAdapter.notifyDataSetChanged();
                }
                Toast.makeText(TeacherEventsInside.this, "Error loading coordinators.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabs(String eventId, String description, String photoUrl, String eventFor) {
        tabAdapter = new EventTabsPagerAdapter(this, eventId, description, photoUrl, eventFor);
        viewPager.setAdapter(tabAdapter);

        tabEventDetails.setOnClickListener(v -> {
            viewPager.setCurrentItem(0);
            updateTabStyles(0);
        });

        tabParticipants.setOnClickListener(v -> {
            viewPager.setCurrentItem(1);
            updateTabStyles(1);
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabStyles(position);
            }
        });
    }

    private void updateTabStyles(int selectedTab) {
        if (selectedTab == 0) {
            tabEventDetails.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabEventDetails.setTextColor(Color.BLACK);
            tabParticipants.setBackground(null);
            tabParticipants.setTextColor(getResources().getColor(R.color.gray));
        } else {
            tabParticipants.setBackground(getResources().getDrawable(R.drawable.tab_selected));
            tabParticipants.setTextColor(Color.BLACK);
            tabEventDetails.setBackground(null);
            tabEventDetails.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    private void getTicketCount(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("TicketCount", "Cannot fetch tickets: eventId is null or empty");
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

                String eventKey = eventId;
                if (eventId.contains("/")) {
                    String[] parts = eventId.split("/");
                    eventKey = parts[parts.length - 1];
                }

                Log.d("TicketCount", "Searching for tickets matching event key: " + eventKey);

                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    if (studentSnapshot.hasChild("tickets")) {
                        DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");

                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketKey = ticketSnapshot.getKey();
                            Log.d("TicketCount", "Checking ticket: " + ticketKey);

                            if (eventKey.equals(ticketKey)) {
                                ticketCount++;
                                Log.d("TicketCount", "Found matching ticket by key: " + ticketKey);
                                continue;
                            }

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

                if (ticketGeneratedTextView != null) {
                    ticketGeneratedTextView.setText(String.valueOf(ticketCount));
                }
                Log.d("TicketCount", "Final ticket count: " + ticketCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching tickets: " + error.getMessage());
                if (ticketGeneratedTextView != null) {
                    ticketGeneratedTextView.setText("0");
                }
            }
        });
    }

    private void getTotalCoordinators(String eventId) {
        if (eventId == null || eventId.isEmpty() || totalCoordinatorTextView == null) {
            Log.e("OrganizerDetails", "Cannot fetch coordinators: eventId is null or empty");
            if (totalCoordinatorTextView != null) {
                totalCoordinatorTextView.setText("Event ID is invalid");
            }
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");

        ref.orderByValue().equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long coordinatorCount = snapshot.getChildrenCount();
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

    private void showAddCoordinatorDialog() {
        // Implementation omitted for brevity - same as your original code
    }
}