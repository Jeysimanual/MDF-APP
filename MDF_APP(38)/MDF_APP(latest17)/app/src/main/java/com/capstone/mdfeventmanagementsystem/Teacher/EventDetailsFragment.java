package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class EventDetailsFragment extends Fragment {
    private String description;
    private String photoUrl;
    private String eventUID;
    private String eventForValue;
    private TextView eventNameTextView, startDateTextView, endDateTextView, startTimeTextView, endTimeTextView, venueTextView, eventSpanTextView, graceTimeTextView, eventTypeTextView, eventForTextView;
    private TextView descriptionTextView;
    private ImageView photoImageView;
    private TextView ticketGeneratedTextView;
    private TextView totalCoordinatorTextView;
    private Button showCoordinatorsBtn, addCoordinatorBtn;
    private EmailAdapter emailAdapter;
    private CardView registrationCard;
    private SwitchCompat registrationSwitch;
    private TextView registrationStatusText;
    private DatabaseReference eventRef;
    private ImageButton editEventButton;
    private boolean canEditEvent = false; // Tracks if teacher is allowed to edit
    private boolean isEventCreator = false; // Tracks if the current teacher is the creator
    private Object targetParticipant = null; // Store targetParticipant value (can be Long or String)

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
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        descriptionTextView = view.findViewById(R.id.eventDescription);
        photoImageView = view.findViewById(R.id.eventPhotoUrl);
        ticketGeneratedTextView = view.findViewById(R.id.ticket_generated);
        totalCoordinatorTextView = view.findViewById(R.id.total_coordinator);
        showCoordinatorsBtn = view.findViewById(R.id.showCoordinatorsBtn);
        addCoordinatorBtn = view.findViewById(R.id.addCoordinator);
        registrationSwitch = view.findViewById(R.id.registrationSwitch);
        registrationStatusText = view.findViewById(R.id.registrationStatusText);
        registrationCard = view.findViewById(R.id.registrationCard);
        editEventButton = view.findViewById(R.id.editEventButton);

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
            Log.d("EventDetails", "Some optional views not found in layout: " + e.getMessage());
        }

        if (description != null && descriptionTextView != null) {
            descriptionTextView.setText(description);
        }

        if (photoUrl != null && !photoUrl.isEmpty() && photoImageView != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(photoImageView);
        } else if (photoImageView != null) {
            photoImageView.setImageResource(R.drawable.placeholder_image);
        }

        if (showCoordinatorsBtn != null) {
            showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());
        }
        if (addCoordinatorBtn != null) {
            addCoordinatorBtn.setOnClickListener(v -> showAddCoordinatorDialog());
        }
        if (editEventButton != null) {
            editEventButton.setOnClickListener(v -> handleEditButtonClick());
            // Initially hide the edit button until creator permission is confirmed
            editEventButton.setVisibility(View.GONE);
        }

        if (eventUID != null && !eventUID.isEmpty()) {
            eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
            getEventDetails(eventUID);
            getTicketCount(eventUID);
            getTargetParticipant(eventUID); // Updated method name
            getTotalCoordinators(eventUID);
            setupRegistrationControl();
            checkEditPermission();
            checkCreatorPermission();
        } else {
            Log.e("EventDetailsFragment", "Event UID is null or empty, cannot proceed with loading event details.");
        }
    }

    private void handleEditButtonClick() {
        if (!isAdded()) return; // Ensure fragment is still attached

        if (canEditEvent) {
            // Ensure all TextViews are initialized and have values
            if (!areTextViewsInitialized()) {
                Toast.makeText(getContext(), "Event details are not fully loaded. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Navigate to TeacherCreateEventActivity with event data
            Intent intent = new Intent(getContext(), TeacherCreateEventActivity.class);
            intent.putExtra("IS_EDITING", true);
            intent.putExtra("EVENT_ID", eventUID);
            intent.putExtra("EVENT_NAME", eventNameTextView != null ? eventNameTextView.getText().toString() : "");
            intent.putExtra("EVENT_DESCRIPTION", descriptionTextView != null ? descriptionTextView.getText().toString() : "");
            intent.putExtra("EVENT_VENUE", venueTextView != null ? venueTextView.getText().toString() : "");
            intent.putExtra("EVENT_START_DATE", startDateTextView != null ? startDateTextView.getText().toString() : "");
            intent.putExtra("EVENT_END_DATE", endDateTextView != null ? endDateTextView.getText().toString() : "");
            intent.putExtra("EVENT_START_TIME", startTimeTextView != null ? startTimeTextView.getText().toString() : "");
            intent.putExtra("EVENT_END_TIME", endTimeTextView != null ? endTimeTextView.getText().toString() : "");
            intent.putExtra("EVENT_SPAN", eventSpanTextView != null ? eventSpanTextView.getText().toString() : "");
            intent.putExtra("EVENT_GRACE_TIME", graceTimeTextView != null ? graceTimeTextView.getText().toString() : "");
            intent.putExtra("EVENT_TYPE", eventTypeTextView != null ? eventTypeTextView.getText().toString() : "");
            intent.putExtra("EVENT_FOR", eventForTextView != null ? eventForTextView.getText().toString() : "");
            intent.putExtra("EVENT_PHOTO_URL", photoUrl);
            startActivity(intent);
        } else {
            // Show confirmation dialog to request edit
            new AlertDialog.Builder(getContext())
                    .setTitle("Request Edit")
                    .setMessage("Are you sure you want to request to edit this event?")
                    .setPositiveButton("Yes", (dialog, which) -> sendEditRequest())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        }
    }

    private boolean areTextViewsInitialized() {
        // Check if all required TextViews are initialized and have non-empty text
        return (eventNameTextView != null && eventNameTextView.getText() != null && !eventNameTextView.getText().toString().isEmpty()) &&
                (descriptionTextView != null && descriptionTextView.getText() != null) &&
                (venueTextView != null && venueTextView.getText() != null && !venueTextView.getText().toString().isEmpty()) &&
                (startDateTextView != null && startDateTextView.getText() != null && !startDateTextView.getText().toString().isEmpty()) &&
                (endDateTextView != null && endDateTextView.getText() != null && !endDateTextView.getText().toString().isEmpty()) &&
                (startTimeTextView != null && startTimeTextView.getText() != null && !startTimeTextView.getText().toString().isEmpty()) &&
                (endTimeTextView != null && endTimeTextView.getText() != null && !endTimeTextView.getText().toString().isEmpty()) &&
                (eventSpanTextView != null && eventSpanTextView.getText() != null && !eventSpanTextView.getText().toString().isEmpty()) &&
                (graceTimeTextView != null && graceTimeTextView.getText() != null && !graceTimeTextView.getText().toString().isEmpty()) &&
                (eventTypeTextView != null && eventTypeTextView.getText() != null && !eventTypeTextView.getText().toString().isEmpty()) &&
                (eventForTextView != null && eventForTextView.getText() != null && !eventForTextView.getText().toString().isEmpty());
    }

    private void sendEditRequest() {
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference editRequestsRef = FirebaseDatabase.getInstance().getReference("editRequests").child(eventUID);

        // Create edit request data
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("teacherId", teacherId);
        requestData.put("eventId", eventUID);
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        editRequestsRef.setValue(requestData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Edit request sent to admin", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to send edit request", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkEditPermission() {
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference editRequestsRef = FirebaseDatabase.getInstance().getReference("editRequests").child(eventUID);

        editRequestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Ensure fragment is still attached

                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String requesterId = snapshot.child("teacherId").getValue(String.class);
                    if ("approved".equals(status) && teacherId.equals(requesterId)) {
                        canEditEvent = true;
                        if (editEventButton != null && !isEventCreator) {
                            editEventButton.setImageResource(R.drawable.ic_edit);
                            editEventButton.setVisibility(View.VISIBLE);
                            Log.d("EditPermission", "Edit request approved for this teacher, showing edit button.");
                        }
                    } else {
                        canEditEvent = false;
                        if (editEventButton != null && !isEventCreator) {
                            editEventButton.setImageResource(R.drawable.ic_edit);
                            editEventButton.setVisibility(View.GONE);
                            Log.d("EditPermission", "Edit request not approved, hiding edit button.");
                        }
                    }
                } else {
                    canEditEvent = false;
                    if (editEventButton != null && !isEventCreator) {
                        editEventButton.setImageResource(R.drawable.ic_edit);
                        editEventButton.setVisibility(View.GONE);
                        Log.d("EditPermission", "No edit request exists, hiding edit button.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EditPermission", "Error checking edit permission: " + error.getMessage());
            }
        });
    }

    private void checkCreatorPermission() {
        if (eventUID == null || eventUID.isEmpty() || editEventButton == null || registrationCard == null || !isAdded()) {
            Log.e("CreatorPermission", "Cannot check creator permission: eventUID, editEventButton, or registrationCard is null, or fragment is not attached.");
            if (editEventButton != null) {
                editEventButton.setVisibility(View.GONE);
            }
            if (registrationCard != null) {
                registrationCard.setVisibility(View.GONE);
            }
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (teacherId == null || teacherId.isEmpty()) {
            Log.e("CreatorPermission", "Teacher ID is null or empty, cannot check creator permission.");
            if (editEventButton != null) {
                editEventButton.setVisibility(View.GONE);
            }
            if (registrationCard != null) {
                registrationCard.setVisibility(View.GONE);
            }
            return;
        }

        Log.d("CreatorPermission", "Checking creator permission for eventUID: " + eventUID + ", teacherId: " + teacherId);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Log.d("CreatorPermission", "Firebase snapshot received for eventUID: " + eventUID);

                if (snapshot.exists()) {
                    Log.d("CreatorPermission", "Event data exists. Checking for 'createdBy' child.");
                    if (snapshot.hasChild("createdBy")) {
                        String creatorId = snapshot.child("createdBy").getValue(String.class);
                        Log.d("CreatorPermission", "Found 'createdBy' node with value: " + creatorId);

                        if (creatorId != null && !creatorId.isEmpty() && teacherId.equals(creatorId)) {
                            isEventCreator = true;
                            canEditEvent = true;
                            if (editEventButton != null) {
                                editEventButton.setVisibility(View.VISIBLE);
                                Log.d("CreatorPermission", "Teacher is the creator (teacherId: " + teacherId + ", creatorId: " + creatorId + "), showing edit button.");
                            }
                            // Check event start time to determine registration card visibility
                            String startDate = snapshot.child("startDate").getValue(String.class);
                            String startTime = snapshot.child("startTime").getValue(String.class);
                            if (startDate != null && startTime != null) {
                                toggleRegistrationCardVisibility(startDate, startTime, true);
                            } else {
                                if (registrationCard != null) {
                                    registrationCard.setVisibility(View.VISIBLE);
                                    Log.d("CreatorPermission", "No start date/time, showing registration card for creator.");
                                }
                            }
                        } else {
                            isEventCreator = false;
                            canEditEvent = false;
                            if (editEventButton != null) {
                                editEventButton.setVisibility(View.GONE);
                                Log.d("CreatorPermission", "Teacher is not the creator (teacherId: " + teacherId + ", creatorId: " + creatorId + "), hiding edit button.");
                            }
                            if (registrationCard != null) {
                                registrationCard.setVisibility(View.GONE);
                                Log.d("CreatorPermission", "Teacher is not the creator, hiding registration card.");
                            }
                        }
                    } else {
                        isEventCreator = false;
                        canEditEvent = false;
                        if (editEventButton != null) {
                            editEventButton.setVisibility(View.GONE);
                            Log.d("CreatorPermission", "No 'createdBy' node found in event data, hiding edit button.");
                        }
                        if (registrationCard != null) {
                            registrationCard.setVisibility(View.GONE);
                            Log.d("CreatorPermission", "No 'createdBy' node found, hiding registration card.");
                        }
                    }
                } else {
                    isEventCreator = false;
                    canEditEvent = false;
                    if (editEventButton != null) {
                        editEventButton.setVisibility(View.GONE);
                        Log.d("CreatorPermission", "Event data does not exist for eventUID: " + eventUID + ", hiding edit button.");
                    }
                    if (registrationCard != null) {
                        registrationCard.setVisibility(View.GONE);
                        Log.d("CreatorPermission", "Event data does not exist for eventUID: " + eventUID + ", hiding registration card.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CreatorPermission", "Error checking creator permission: " + error.getMessage());
                if (isAdded()) {
                    isEventCreator = false;
                    canEditEvent = false;
                    if (editEventButton != null) {
                        editEventButton.setVisibility(View.GONE);
                        Log.d("CreatorPermission", "Firebase query cancelled, hiding edit button.");
                    }
                    if (registrationCard != null) {
                        registrationCard.setVisibility(View.GONE);
                        Log.d("CreatorPermission", "Firebase query cancelled, hiding registration card.");
                    }
                }
            }
        });
    }

    private void toggleRegistrationCardVisibility(String startDate, String startTime, boolean isCreator) {
        if (!isAdded() || registrationCard == null) return;

        if (!isCreator) {
            Log.d("RegistrationCardControl", "Teacher is not the creator, hiding registration card.");
            registrationCard.setVisibility(View.GONE);
            return;
        }

        try {
            Log.d("RegistrationCardControl", "Checking event start: Date=" + startDate + ", Time=" + startTime);

            // Parse the date
            Calendar eventStartTime = Calendar.getInstance();
            if (startDate.contains("/")) {
                String[] dateParts = startDate.split("/");
                if (dateParts.length != 3) {
                    Log.e("RegistrationCardControl", "Invalid date format (DD/MM/YYYY expected): " + startDate);
                    return;
                }
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int year = Integer.parseInt(dateParts[2]);
                eventStartTime.set(Calendar.YEAR, year);
                eventStartTime.set(Calendar.MONTH, month);
                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
            } else if (startDate.contains("-")) {
                String[] dateParts = startDate.split("-");
                if (dateParts.length != 3) {
                    Log.e("RegistrationCardControl", "Invalid date format (YYYY-MM-DD expected): " + startDate);
                    return;
                }
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int day = Integer.parseInt(dateParts[2]);
                eventStartTime.set(Calendar.YEAR, year);
                eventStartTime.set(Calendar.MONTH, month);
                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
            } else {
                Log.e("RegistrationCardControl", "Unrecognized date format: " + startDate);
                return;
            }

            // Parse the time
            int hour, minute;
            if (startTime.contains("AM") || startTime.contains("PM")) {
                String[] timeParts = startTime.replace(" AM", "").replace(" PM", "").split(":");
                if (timeParts.length != 2) {
                    Log.e("RegistrationCardControl", "Invalid 12-hour time format: " + startTime);
                    return;
                }
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
                if (startTime.contains("PM") && hour != 12) {
                    hour += 12;
                } else if (startTime.contains("AM") && hour == 12) {
                    hour = 0;
                }
            } else {
                String[] timeParts = startTime.split(":");
                if (timeParts.length != 2) {
                    Log.e("RegistrationCardControl", "Invalid 24-hour time format: " + startTime);
                    return;
                }
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            }

            eventStartTime.set(Calendar.HOUR_OF_DAY, hour);
            eventStartTime.set(Calendar.MINUTE, minute);
            eventStartTime.set(Calendar.SECOND, 0);
            eventStartTime.set(Calendar.MILLISECOND, 0);

            Calendar currentTime = Calendar.getInstance();
            Log.d("RegistrationCardControl", "Event start time: " + eventStartTime.getTime());
            Log.d("RegistrationCardControl", "Current time: " + currentTime.getTime());

            // Check if event has started
            if (currentTime.compareTo(eventStartTime) >= 0) {
                Log.d("RegistrationCardControl", "Event has started, hiding registration card");
                registrationCard.setVisibility(View.GONE);
                if (eventRef != null) {
                    eventRef.child("registrationAllowed").setValue(false)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("RegistrationCardControl", "Registration auto-closed due to event start time");
                                    if (isAdded()) {
                                        Toast.makeText(getContext(),
                                                "Registration closed automatically as event has started",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e("RegistrationCardControl", "Failed to auto-close registration: " + task.getException());
                                }
                            });
                }
            } else {
                Log.d("RegistrationCardControl", "Event has not started, showing registration card for creator");
                registrationCard.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e("RegistrationCardControl", "Error parsing date/time: " + e.getMessage(), e);
            if (isCreator) {
                registrationCard.setVisibility(View.VISIBLE);
                Log.d("RegistrationCardControl", "Error parsing date/time, showing registration card for creator as fallback");
            } else {
                registrationCard.setVisibility(View.GONE);
                Log.d("RegistrationCardControl", "Error parsing date/time, hiding registration card for non-creator");
            }
        }
    }

    private void setupRegistrationControl() {
        if (registrationSwitch == null || registrationStatusText == null || eventRef == null) {
            Log.e("RegistrationControl", "Registration control components not initialized correctly");
            return;
        }

        // Check creator status before setting up registration control
        checkCreatorPermission();

        // Check the current registration status and update the UI
        eventRef.child("registrationAllowed").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (!isEventCreator) {
                    Log.d("RegistrationControl", "Teacher is not the creator, skipping registration status update");
                    if (registrationCard != null) {
                        registrationCard.setVisibility(View.GONE);
                    }
                    return;
                }

                Boolean isRegistrationAllowed = snapshot.getValue(Boolean.class);

                if (isRegistrationAllowed == null) {
                    isRegistrationAllowed = false;
                }

                registrationSwitch.setOnCheckedChangeListener(null);
                registrationSwitch.setChecked(isRegistrationAllowed);

                updateRegistrationStatusText(isRegistrationAllowed);
                toggleEditButtonBasedOnRegistration(isRegistrationAllowed);

                setRegistrationSwitchListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;

                Log.e("FirebaseError", "Failed to read registration status: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load registration status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRegistrationStatusText(boolean isRegistrationAllowed) {
        if (!isAdded()) return; // Check if fragment is still attached

        if (isRegistrationAllowed) {
            registrationStatusText.setText("Registration is currently OPEN");
            registrationStatusText.setTextColor(getResources().getColor(R.color.green));
        } else {
            registrationStatusText.setText("Registration is currently CLOSED");
            registrationStatusText.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private void setRegistrationSwitchListener() {
        if (!isAdded() || registrationSwitch == null) return; // Check if fragment is still attached

        registrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // User wants to open registration
                showOpenRegistrationConfirmationDialog();
            } else {
                // User wants to close registration
                showCloseRegistrationConfirmationDialog();
            }
        });
    }

    private void showOpenRegistrationConfirmationDialog() {
        if (!isAdded()) return; // Check if fragment is still attached

        new AlertDialog.Builder(getContext())
                .setTitle("Open Registration")
                .setMessage("Are you sure you want to open registration for this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Update Firebase
                    updateRegistrationStatus(true);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Revert switch state without triggering listener
                    registrationSwitch.setOnCheckedChangeListener(null);
                    registrationSwitch.setChecked(false);
                    setRegistrationSwitchListener();
                })
                .setCancelable(false)
                .show();
    }

    private void showCloseRegistrationConfirmationDialog() {
        if (!isAdded()) return; // Check if fragment is still attached

        new AlertDialog.Builder(getContext())
                .setTitle("Close Registration")
                .setMessage("Are you sure you want to close registration for this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Update Firebase
                    updateRegistrationStatus(false);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Revert switch state without triggering listener
                    registrationSwitch.setOnCheckedChangeListener(null);
                    registrationSwitch.setChecked(true);
                    setRegistrationSwitchListener();
                })
                .setCancelable(false)
                .show();
    }

    private void updateRegistrationStatus(boolean isAllowed) {
        if (!isAdded() || eventRef == null) return; // Check if fragment is still attached

        eventRef.child("registrationAllowed").setValue(isAllowed)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return; // Check if fragment is still attached

                    if (task.isSuccessful()) {
                        // Success
                        String message = isAllowed ?
                                "Registration is now open for this event" :
                                "Registration is now closed for this event";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        // Error
                        Toast.makeText(getContext(),
                                "Failed to update registration status", Toast.LENGTH_SHORT).show();

                        // Revert the switch to its previous state
                        registrationSwitch.setOnCheckedChangeListener(null);
                        registrationSwitch.setChecked(!isAllowed);
                        setRegistrationSwitchListener();
                    }
                });
    }

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

                        String startTime = null;
                        if (startTimeTextView != null) {
                            startTime = dataSnapshot.child("startTime").getValue(String.class);
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

                        // Check if event has started and handle registration visibility
                        String startDate = dataSnapshot.child("startDate").getValue(String.class);
                        if (startDate != null && startTime != null) {
                            checkEventStartTimeAndUpdateRegistration(startDate, startTime);
                            hideEditButtonIfEventStarted(startDate, startTime);
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

    private void checkEventStartTimeAndUpdateRegistration(String startDate, String startTime) {
        if (!isAdded() || registrationCard == null) return;

        try {
            Log.d("EventStartTime", "Processing event start: Date=" + startDate + ", Time=" + startTime);

            // Parse the date (support DD/MM/YYYY and YYYY-MM-DD formats)
            Calendar eventStartTime = Calendar.getInstance();
            if (startDate.contains("/")) {
                String[] dateParts = startDate.split("/");
                if (dateParts.length != 3) {
                    Log.e("EventStartTime", "Invalid date format (DD/MM/YYYY expected): " + startDate);
                    return;
                }
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int year = Integer.parseInt(dateParts[2]);
                eventStartTime.set(Calendar.YEAR, year);
                eventStartTime.set(Calendar.MONTH, month);
                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
            } else if (startDate.contains("-")) {
                String[] dateParts = startDate.split("-");
                if (dateParts.length != 3) {
                    Log.e("EventStartTime", "Invalid date format (YYYY-MM-DD expected): " + startDate);
                    return;
                }
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int day = Integer.parseInt(dateParts[2]);
                eventStartTime.set(Calendar.YEAR, year);
                eventStartTime.set(Calendar.MONTH, month);
                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
            } else {
                Log.e("EventStartTime", "Unrecognized date format: " + startDate);
                return;
            }

            // Parse the time (support HH:MM and hh:MM AM/PM formats)
            int hour, minute;
            if (startTime.contains("AM") || startTime.contains("PM")) {
                // 12-hour format (e.g., "06:00 AM")
                String[] timeParts = startTime.replace(" AM", "").replace(" PM", "").split(":");
                if (timeParts.length != 2) {
                    Log.e("EventStartTime", "Invalid 12-hour time format: " + startTime);
                    return;
                }
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
                if (startTime.contains("PM") && hour != 12) {
                    hour += 12;
                } else if (startTime.contains("AM") && hour == 12) {
                    hour = 0;
                }
            } else {
                // 24-hour format (e.g., "06:00")
                String[] timeParts = startTime.split(":");
                if (timeParts.length != 2) {
                    Log.e("EventStartTime", "Invalid 24-hour time format: " + startTime);
                    return;
                }
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            }

            eventStartTime.set(Calendar.HOUR_OF_DAY, hour);
            eventStartTime.set(Calendar.MINUTE, minute);
            eventStartTime.set(Calendar.SECOND, 0);
            eventStartTime.set(Calendar.MILLISECOND, 0);

            Calendar currentTime = Calendar.getInstance();
            Log.d("EventStartTime", "Event start time: " + eventStartTime.getTime());
            Log.d("EventStartTime", "Current time: " + currentTime.getTime());

            // Check if event has started
            if (currentTime.compareTo(eventStartTime) >= 0) {
                Log.d("EventStartTime", "Event has started, hiding registration");
                registrationCard.setVisibility(View.GONE);
                if (eventRef != null) {
                    eventRef.child("registrationAllowed").setValue(false)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("EventStartTime", "Registration auto-closed due to event start time");
                                    if (isAdded()) {
                                        Toast.makeText(getContext(),
                                                "Registration closed automatically as event has started",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e("EventStartTime", "Failed to auto-close registration: " + task.getException());
                                }
                            });
                }
            } else {
                registrationCard.setVisibility(View.VISIBLE);
                long delayMillis = eventStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
                if (delayMillis > 0) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && registrationCard != null) {
                            registrationCard.setVisibility(View.GONE);
                            if (eventRef != null) {
                                eventRef.child("registrationAllowed").setValue(false)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful() && isAdded()) {
                                                Log.d("EventStartTime", "Registration auto-closed due to event start time");
                                                Toast.makeText(getContext(),
                                                        "Registration closed automatically as event has started",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }, delayMillis);
                    Log.d("EventStartTime", "Timer set to hide registration in " + delayMillis + " ms");
                }
            }
        } catch (Exception e) {
            Log.e("EventStartTime", "Error parsing date/time: " + e.getMessage(), e);
        }
    }

    private void hideEditButtonIfEventStarted(String startDate, String startTime) {
        if (!isAdded() || editEventButton == null) return;

        try {
            Log.d("EditButtonControl", "Checking if event has started: Date=" + startDate + ", Time=" + startTime);

            // Parse the date
            Calendar eventStartTime = Calendar.getInstance();
            if (startDate.contains("/")) {
                String[] dateParts = startDate.split("/");
                if (dateParts.length != 3) {
                    Log.e("EditButtonControl", "Invalid date format (DD/MM/YYYY expected): " + startDate);
                    return;
                }
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int year = Integer.parseInt(dateParts[2]);
                eventStartTime.set(Calendar.YEAR, year);
                eventStartTime.set(Calendar.MONTH, month);
                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
            } else if (startDate.contains("-")) {
                String[] dateParts = startDate.split("-");
                if (dateParts.length != 3) {
                    Log.e("EditButtonControl", "Invalid date format (YYYY-MM-DD expected): " + startDate);
                    return;
                }
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int day = Integer.parseInt(dateParts[2]);
                eventStartTime.set(Calendar.YEAR, year);
                eventStartTime.set(Calendar.MONTH, month);
                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
            } else {
                Log.e("EditButtonControl", "Unrecognized date format: " + startDate);
                return;
            }

            // Parse the time
            int hour, minute;
            if (startTime.contains("AM") || startTime.contains("PM")) {
                String[] timeParts = startTime.replace(" AM", "").replace(" PM", "").split(":");
                if (timeParts.length != 2) {
                    Log.e("EditButtonControl", "Invalid 12-hour time format: " + startTime);
                    return;
                }
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
                if (startTime.contains("PM") && hour != 12) {
                    hour += 12;
                } else if (startTime.contains("AM") && hour == 12) {
                    hour = 0;
                }
            } else {
                String[] timeParts = startTime.split(":");
                if (timeParts.length != 2) {
                    Log.e("EditButtonControl", "Invalid 24-hour time format: " + startTime);
                    return;
                }
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            }

            eventStartTime.set(Calendar.HOUR_OF_DAY, hour);
            eventStartTime.set(Calendar.MINUTE, minute);
            eventStartTime.set(Calendar.SECOND, 0);
            eventStartTime.set(Calendar.MILLISECOND, 0);

            Calendar currentTime = Calendar.getInstance();
            Log.d("EditButtonControl", "Event start time: " + eventStartTime.getTime());
            Log.d("EditButtonControl", "Current time: " + currentTime.getTime());

            // Check if event has started
            if (currentTime.compareTo(eventStartTime) >= 0) {
                Log.d("EditButtonControl", "Event has started, hiding edit button");
                editEventButton.setVisibility(View.GONE);
            } else {
                Log.d("EditButtonControl", "Event has not started, checking creator status");
                // If the event hasn't started, the edit button visibility should already be set by checkCreatorPermission()
                // Only proceed to registration check if needed
                if (isEventCreator) {
                    Log.d("EditButtonControl", "Teacher is creator and event has not started, ensuring edit button is visible");
                    editEventButton.setVisibility(View.VISIBLE);
                    // Now check registration status
                    if (eventRef != null) {
                        eventRef.child("registrationAllowed").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!isAdded()) return;
                                Boolean isRegistrationAllowed = snapshot.getValue(Boolean.class);
                                if (isRegistrationAllowed == null) {
                                    isRegistrationAllowed = false;
                                }
                                toggleEditButtonBasedOnRegistration(isRegistrationAllowed);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("EditButtonControl", "Failed to check registration status: " + error.getMessage());
                            }
                        });
                    }
                } else {
                    editEventButton.setVisibility(View.GONE);
                    Log.d("EditButtonControl", "Teacher is not creator, checking edit permission");
                    checkEditPermission();
                }
            }
        } catch (Exception e) {
            Log.e("EditButtonControl", "Error parsing date/time: " + e.getMessage(), e);
        }
    }

    private void toggleEditButtonBasedOnRegistration(boolean isRegistrationAllowed) {
        if (!isAdded() || editEventButton == null) return;

        // If the teacher is not the creator and doesn't have edit permission, the button should remain hidden
        if (!isEventCreator && !canEditEvent) {
            Log.d("EditButtonControl", "Teacher is not the creator and edit request not approved, edit button remains hidden.");
            editEventButton.setVisibility(View.GONE);
            return;
        }

        // If the event has already started, the button should be hidden regardless
        if (eventRef != null) {
            eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    String startDate = snapshot.child("startDate").getValue(String.class);
                    String startTime = snapshot.child("startTime").getValue(String.class);
                    if (startDate != null && startTime != null) {
                        try {
                            Calendar eventStartTime = Calendar.getInstance();
                            // Parse date
                            if (startDate.contains("/")) {
                                String[] dateParts = startDate.split("/");
                                int day = Integer.parseInt(dateParts[0]);
                                int month = Integer.parseInt(dateParts[1]) - 1;
                                int year = Integer.parseInt(dateParts[2]);
                                eventStartTime.set(Calendar.YEAR, year);
                                eventStartTime.set(Calendar.MONTH, month);
                                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
                            } else if (startDate.contains("-")) {
                                String[] dateParts = startDate.split("-");
                                int year = Integer.parseInt(dateParts[0]);
                                int month = Integer.parseInt(dateParts[1]) - 1;
                                int day = Integer.parseInt(dateParts[2]);
                                eventStartTime.set(Calendar.YEAR, year);
                                eventStartTime.set(Calendar.MONTH, month);
                                eventStartTime.set(Calendar.DAY_OF_MONTH, day);
                            }

                            // Parse time
                            int hour, minute;
                            if (startTime.contains("AM") || startTime.contains("PM")) {
                                String[] timeParts = startTime.replace(" AM", "").replace(" PM", "").split(":");
                                hour = Integer.parseInt(timeParts[0]);
                                minute = Integer.parseInt(timeParts[1]);
                                if (startTime.contains("PM") && hour != 12) {
                                    hour += 12;
                                } else if (startTime.contains("AM") && hour == 12) {
                                    hour = 0;
                                }
                            } else {
                                String[] timeParts = startTime.split(":");
                                hour = Integer.parseInt(timeParts[0]);
                                minute = Integer.parseInt(timeParts[1]);
                            }

                            eventStartTime.set(Calendar.HOUR_OF_DAY, hour);
                            eventStartTime.set(Calendar.MINUTE, minute);
                            eventStartTime.set(Calendar.SECOND, 0);
                            eventStartTime.set(Calendar.MILLISECOND, 0);

                            Calendar currentTime = Calendar.getInstance();
                            if (currentTime.compareTo(eventStartTime) >= 0) {
                                Log.d("EditButtonControl", "Event has started, hiding edit button");
                                editEventButton.setVisibility(View.GONE);
                            } else {
                                Log.d("EditButtonControl", "Event not started, applying registration logic for creator or approved editor");
                                // Since teacher is either creator or has approved edit permission and event hasn't started, apply registration logic
                                if (isRegistrationAllowed) {
                                    Log.d("EditButtonControl", "Registration is open, hiding edit button");
                                    editEventButton.setVisibility(View.GONE);
                                } else {
                                    Log.d("EditButtonControl", "Registration is closed, showing edit button for creator or approved editor");
                                    editEventButton.setVisibility(View.VISIBLE);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EditButtonControl", "Error parsing date/time: " + e.getMessage(), e);
                            // Fallback: apply registration logic
                            if (isRegistrationAllowed) {
                                editEventButton.setVisibility(View.GONE);
                            } else {
                                editEventButton.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        // Fallback: apply registration logic
                        if (isRegistrationAllowed) {
                            editEventButton.setVisibility(View.GONE);
                        } else {
                            editEventButton.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("EditButtonControl", "Failed to fetch event details: " + error.getMessage());
                    // Fallback: apply registration logic
                    if (isRegistrationAllowed) {
                        editEventButton.setVisibility(View.GONE);
                    } else {
                        editEventButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    private String convertTo12HourFormat(String time24Hour) {
        if (time24Hour == null || time24Hour.isEmpty()) {
            return "N/A";
        }
        if (time24Hour.contains("AM") || time24Hour.contains("PM")) {
            return time24Hour; // Already in 12-hour format
        }
        try {
            String[] timeParts = time24Hour.split(":");
            if (timeParts.length != 2) {
                return time24Hour;
            }
            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[1]);
            String amPm = (hours >= 12) ? "PM" : "AM";
            if (hours == 0) {
                hours = 12;
            } else if (hours > 12) {
                hours = hours - 12;
            }
            return String.format("%02d:%02d %s", hours, minutes, amPm);
        } catch (Exception e) {
            Log.e("TimeConverter", "Error converting time: " + e.getMessage());
            return time24Hour;
        }
    }

    private void getTicketCount(String eventId) {
        if (eventId == null || eventId.isEmpty() || ticketGeneratedTextView == null) {
            Log.e("TicketCount", "Cannot fetch tickets: eventId is null/empty or view not found");
            if (ticketGeneratedTextView != null) {
                ticketGeneratedTextView.setText("0");
            }
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");

        studentsRef.addValueEventListener(new ValueEventListener() {
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
                    String displayText;
                    if ("none".equalsIgnoreCase(String.valueOf(targetParticipant))) {
                        displayText = String.valueOf(ticketCount);
                    } else if (targetParticipant instanceof Long) {
                        displayText = ticketCount + "/" + targetParticipant;
                    } else {
                        displayText = String.valueOf(ticketCount);
                    }
                    ticketGeneratedTextView.setText(displayText);
                }
                Log.d("TicketCount", "Final ticket count: " + ticketCount);

                // Check if ticket count has reached targetParticipant and close registration if necessary
                if (targetParticipant instanceof Long && ticketCount >= (Long) targetParticipant && eventRef != null) {
                    int finalTicketCount = ticketCount;
                    eventRef.child("registrationAllowed").setValue(false)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("TicketCount", "Registration auto-closed as ticket count reached target: " + finalTicketCount + "/" + targetParticipant);
                                    if (isAdded()) {
                                        Toast.makeText(getContext(),
                                                "Registration closed automatically as participant limit reached",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e("TicketCount", "Failed to auto-close registration: " + task.getException());
                                }
                            });
                }
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

    private void getTargetParticipant(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e("TargetParticipant", "Cannot fetch target participant: eventId is null or empty");
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventId);

        eventRef.child("targetParticipant").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    Object value = snapshot.getValue();
                    if (value instanceof String && "none".equalsIgnoreCase((String) value)) {
                        targetParticipant = "none";
                        Log.d("TargetParticipant", "Fetched targetParticipant: none");
                    } else if (value instanceof Long) {
                        targetParticipant = (Long) value;
                        Log.d("TargetParticipant", "Fetched targetParticipant: " + targetParticipant);
                    } else {
                        targetParticipant = null;
                        Log.d("TargetParticipant", "Invalid targetParticipant format: " + value);
                    }
                    // Update ticket count display
                    getTicketCount(eventId);
                } else {
                    targetParticipant = null;
                    Log.d("TargetParticipant", "No targetParticipant found for event: " + eventId);
                    // Update ticket count display without targetParticipant
                    getTicketCount(eventId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TargetParticipant", "Error fetching target participant: " + error.getMessage());
                targetParticipant = null;
                // Update ticket count display without targetParticipant
                if (isAdded()) {
                    getTicketCount(eventId);
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
                    totalCoordinatorTextView.setText(coordinatorCount + " Student Assistant(s)");
                } else {
                    totalCoordinatorTextView.setText("0 Student Assistant(s)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                sendEditRequest();
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
                    Toast.makeText(getContext(), "No Student Assistant found.", Toast.LENGTH_SHORT).show();
                } else {
                    showCoordinatorEmailDialog(coordinatorEmails);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isAdded()) return; // Check if fragment is still attached

                Log.e("FirebaseError", "Error fetching coordinators: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Error loading student assistant list", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Student Assistant removed successfully", Toast.LENGTH_SHORT).show();

                // Update UI
                ((ViewGroup) rowLayout.getParent()).removeView(rowLayout);
                getTotalCoordinators(eventUID); // Refresh the count
            } else {
                Toast.makeText(getContext(), "Error removing student assistant", Toast.LENGTH_SHORT).show();
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
        ImageButton addEmailButton = dialogView.findViewById(R.id.addEmailButton);
        ImageButton cancelButton = dialogView.findViewById(R.id.cancelButton);
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
                Toast.makeText(getContext(), "Adding of Student Assistant is cancelled.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "No valid students to add as student assistant.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getContext(), "Student Assistant added: " + email, Toast.LENGTH_SHORT).show();
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