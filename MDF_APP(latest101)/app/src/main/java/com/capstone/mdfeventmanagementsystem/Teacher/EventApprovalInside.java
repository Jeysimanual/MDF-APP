package com.capstone.mdfeventmanagementsystem.Teacher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.mdfeventmanagementsystem.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventApprovalInside extends AppCompatActivity {

    private static final String TAG = "EventApprovalInside";

    // UI elements
    private TextView eventNameText;
    private TextView eventDescriptionText;
    private TextView venueText;
    private TextView startDateText;
    private TextView endDateText;
    private TextView startTimeText;
    private TextView endTimeText;
    private TextView eventSpanText;
    private TextView graceTimeText;
    private TextView eventTypeText;
    private TextView eventForText;
    private ImageView eventPhotoUrlImage;
    private MaterialCardView reasonContainer;
    private TextView reasonText;

    // Labels for visibility control
    private TextView endDateLabel;
    private TextView venueLabel;

    // Event data
    private String eventId;
    private String eventStatus;
    private String eventName;
    private DatabaseReference eventProposalsRef;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_approval_inside);

        Log.d(TAG, "onCreate: Initializing EventApprovalInside activity");

        // Initialize Firebase Database reference
        eventProposalsRef = FirebaseDatabase.getInstance().getReference("eventProposals");

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_event_approval_inside_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        initializeViews();

        // Get data from Intent
        receiveIntentData();

        // Fetch event data from Firebase
        fetchEventDataFromDatabase();
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews: Finding views by IDs");

        // Find all views by ID
        eventNameText = findViewById(R.id.eventName);
        eventDescriptionText = findViewById(R.id.eventDescription);
        venueText = findViewById(R.id.venue);
        startDateText = findViewById(R.id.startDate);
        endDateText = findViewById(R.id.endDate);
        startTimeText = findViewById(R.id.startTime);
        endTimeText = findViewById(R.id.endTime);
        eventSpanText = findViewById(R.id.eventSpan);
        graceTimeText = findViewById(R.id.graceTime);
        eventTypeText = findViewById(R.id.eventType);
        eventForText = findViewById(R.id.eventFor);
        eventPhotoUrlImage = findViewById(R.id.eventPhotoUrl);
        reasonContainer = findViewById(R.id.reason_container);
        reasonText = findViewById(R.id.reason_text);

        // Find labels for visibility control
        endDateLabel = findViewById(R.id.textView17); // This is the "Event End:" label in your XML
        venueLabel = findViewById(R.id.textView18); // This is the "Event Venue:" label in your XML
    }

    private void receiveIntentData() {
        Log.d(TAG, "receiveIntentData: Getting event data from intent");

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            eventId = intent.getStringExtra("EVENT_ID");
            eventStatus = intent.getStringExtra("EVENT_STATUS");
            eventName = intent.getStringExtra("EVENT_NAME");

            Log.d(TAG, "receiveIntentData: Received event with ID: " + eventId +
                    ", name: " + eventName + " and status: " + eventStatus);
        } else {
            Log.e(TAG, "receiveIntentData: Intent is null");
        }
    }

    private void fetchEventDataFromDatabase() {
        Log.d(TAG, "fetchEventDataFromDatabase: Fetching event data from Firebase for event ID: " + eventId);

        if (eventId != null && !eventId.isEmpty()) {
            // Query the database for the specific event by its ID
            eventProposalsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "onDataChange: Event data found in database");
                        displayEventDataFromDatabase(dataSnapshot);
                    } else {
                        Log.d(TAG, "onDataChange: Event not found in database with ID: " + eventId);
                        // Try using intent data as fallback
                        displayEventDataFromIntent();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "fetchEventDataFromDatabase: Database error: ", databaseError.toException());
                    // Use intent data as fallback
                    displayEventDataFromIntent();
                }
            });
        } else if (eventName != null && !eventName.isEmpty()) {
            // If eventId is null but eventName is available, query by name
            Log.d(TAG, "fetchEventDataFromDatabase: Trying to fetch by event name: " + eventName);

            Query query = eventProposalsRef.orderByChild("eventName").equalTo(eventName);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        // Get the first matching event
                        DataSnapshot firstEvent = dataSnapshot.getChildren().iterator().next();
                        Log.d(TAG, "onDataChange: Event found by name: " + eventName);
                        displayEventDataFromDatabase(firstEvent);
                    } else {
                        Log.d(TAG, "onDataChange: Event not found by name: " + eventName);
                        // Use intent data as fallback
                        displayEventDataFromIntent();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Query by name cancelled", databaseError.toException());
                    // Use intent data as fallback
                    displayEventDataFromIntent();
                }
            });
        } else {
            Log.e(TAG, "fetchEventDataFromDatabase: Both event ID and name are null or empty");
            // Use intent data as fallback
            displayEventDataFromIntent();
        }
    }

    private void displayEventDataFromDatabase(DataSnapshot dataSnapshot) {
        Log.d(TAG, "displayEventDataFromDatabase: Setting event data from database");

        // Extract event data from database snapshot
        String name = dataSnapshot.child("eventName").getValue(String.class);
        String description = dataSnapshot.child("eventDescription").getValue(String.class);
        String venue = dataSnapshot.child("venue").getValue(String.class);
        String startDate = dataSnapshot.child("dateCreated").getValue(String.class);
        String endDate = dataSnapshot.child("endDate").getValue(String.class);
        String startTime = dataSnapshot.child("startTime").getValue(String.class);
        String endTime = dataSnapshot.child("endTime").getValue(String.class);
        String eventSpan = dataSnapshot.child("eventSpan").getValue(String.class);
        String graceTime = dataSnapshot.child("graceTime").getValue(String.class);
        String eventType = dataSnapshot.child("eventType").getValue(String.class);
        String eventFor = dataSnapshot.child("eventFor").getValue(String.class);
        String photoUrl = dataSnapshot.child("eventPhotoUrl").getValue(String.class);
        String status = dataSnapshot.child("status").getValue(String.class);
        String reason = dataSnapshot.child("reason").getValue(String.class);

        // Set event data to views
        setEventDataToViews(name, description, venue, startDate, endDate, startTime, endTime,
                eventSpan, graceTime, eventType, eventFor, photoUrl, status, reason);
    }

    private void displayEventDataFromIntent() {
        Log.d(TAG, "displayEventDataFromIntent: Using intent data as fallback");

        Intent intent = getIntent();
        if (intent != null) {
            // Extract data from intent
            String name = intent.getStringExtra("EVENT_NAME");
            String description = intent.getStringExtra("EVENT_DESCRIPTION");
            String venue = intent.getStringExtra("EVENT_VENUE");
            String startDate = intent.getStringExtra("EVENT_START_DATE");
            String endDate = intent.getStringExtra("EVENT_END_DATE");
            String startTime = intent.getStringExtra("EVENT_START_TIME");
            String endTime = intent.getStringExtra("EVENT_END_TIME");
            String eventSpan = intent.getStringExtra("EVENT_SPAN");
            String graceTime = intent.getStringExtra("EVENT_GRACE_TIME");
            String eventType = intent.getStringExtra("EVENT_TYPE");
            String eventFor = intent.getStringExtra("EVENT_FOR");
            String photoUrl = intent.getStringExtra("EVENT_PHOTO_URL");
            String reason = intent.getStringExtra("EVENT_REJECTION_REASON");

            // Set event data to views
            setEventDataToViews(name, description, venue, startDate, endDate, startTime, endTime,
                    eventSpan, graceTime, eventType, eventFor, photoUrl, eventStatus, reason);
        }
    }

    private void setEventDataToViews(String name, String description, String venue,
                                     String startDate, String endDate, String startTime, String endTime,
                                     String eventSpan, String graceTime, String eventType,
                                     String eventFor, String photoUrl, String status, String reason) {

        Log.d(TAG, "setEventDataToViews: Setting extracted data to views");

        // Set event name
        if (name != null && !name.isEmpty()) {
            eventNameText.setText(name);
        }

        // Set event description
        if (description != null && !description.isEmpty()) {
            eventDescriptionText.setText(description);
        }

        // Format and set start date in YYYY-MM-DD format to match end date
        if (startDate != null && !startDate.isEmpty()) {
            try {
                // Try to parse the date if it's in a different format
                SimpleDateFormat inputFormat;
                if (startDate.contains(",")) {
                    // Format like "May 05, 2025"
                    inputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                } else if (startDate.contains("/")) {
                    // Format like "05/05/2025"
                    inputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                } else {
                    // Assume it's already in YYYY-MM-DD format
                    inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                }

                Date date = inputFormat.parse(startDate);
                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String formattedStartDate = outputFormat.format(date);
                    startDateText.setText(formattedStartDate);
                } else {
                    startDateText.setText(startDate); // Fallback to original
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing start date: " + e.getMessage());
                startDateText.setText(startDate); // Fallback to original
            }
        }

        // Always show venue section
        venueText.setVisibility(View.VISIBLE);
        venueLabel.setVisibility(View.VISIBLE);

        // Set venue text
        if (venue != null && !venue.isEmpty() && !venue.equalsIgnoreCase("")) {
            venueText.setText(venue);
        } else {
            // Set a default value when venue is not specified
            venueText.setText("Not specified");
        }

        // Handle single/multi-day event display
        if (startDate != null && endDate != null && startDate.equals(endDate)) {
            // Single day event
            endDateLabel.setVisibility(View.GONE);
            endDateText.setVisibility(View.GONE);
        } else {
            // Multi-day event
            endDateLabel.setVisibility(View.VISIBLE);
            endDateText.setVisibility(View.VISIBLE);
            if (endDate != null && !endDate.isEmpty()) {
                endDateText.setText(endDate);
            } else {
                endDateText.setText("Not specified");
            }
        }

        // Format and set start time with AM/PM
        if (startTime != null && !startTime.isEmpty()) {
            // Check if time already contains AM/PM
            if (!startTime.contains("AM") && !startTime.contains("PM") &&
                    !startTime.contains("am") && !startTime.contains("pm")) {
                try {
                    // Try to parse hour to determine AM/PM
                    String[] timeParts = startTime.split(":");
                    if (timeParts.length > 0) {
                        int hour = Integer.parseInt(timeParts[0]);
                        if (hour >= 12) {
                            startTime += " PM";
                        } else {
                            startTime += " AM";
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting start time: " + e.getMessage());
                }
            }
            startTimeText.setText(startTime);
        } else {
            startTimeText.setText("Not specified");
        }

        // Format and set end time with AM/PM
        if (endTime != null && !endTime.isEmpty() && !endTime.equalsIgnoreCase("")) {
            // Check if time already contains AM/PM
            if (!endTime.contains("AM") && !endTime.contains("PM") &&
                    !endTime.contains("am") && !endTime.contains("pm")) {
                try {
                    // Try to parse hour to determine AM/PM
                    String[] timeParts = endTime.split(":");
                    if (timeParts.length > 0) {
                        int hour = Integer.parseInt(timeParts[0]);
                        if (hour >= 12) {
                            endTime += " PM";
                        } else {
                            endTime += " AM";
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting end time: " + e.getMessage());
                }
            }
            endTimeText.setText(endTime);
        } else {
            endTimeText.setText("Not specified");
        }

        // Set additional event details
        if (eventSpan != null && !eventSpan.isEmpty()) {
            eventSpanText.setText(eventSpan);
        } else {
            eventSpanText.setText("Single-day");
        }

        if (graceTime != null && !graceTime.isEmpty() && !graceTime.equalsIgnoreCase("null")) {
            graceTimeText.setText(graceTime);
        } else {
            graceTimeText.setText("none");
        }

        if (eventType != null && !eventType.isEmpty()) {
            eventTypeText.setText(eventType);
        } else {
            eventTypeText.setText("Other");
        }

        if (eventFor != null && !eventFor.isEmpty()) {
            eventForText.setText(eventFor);
        } else {
            eventForText.setText("All");
        }

        // Load event image
        if (photoUrl != null && !photoUrl.isEmpty()) {
            try {
                Picasso.get().load(photoUrl).placeholder(R.drawable.placeholder_image).into(eventPhotoUrlImage);
            } catch (Exception e) {
                Log.e(TAG, "setEventDataToViews: Error loading image", e);
                eventPhotoUrlImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            eventPhotoUrlImage.setImageResource(R.drawable.placeholder_image);
        }

        // Handle rejection status and reason
        handleRejectionStatus(status, reason);
    }

    /**
     * Handles the display of rejection reason when event status is "rejected"
     */
    private void handleRejectionStatus(String status, String reason) {
        Log.d(TAG, "handleRejectionStatus: Checking if event is rejected");

        // Default hide the rejection container
        reasonContainer.setVisibility(View.GONE);

        // Check if event status is "rejected"
        if (status != null && status.equalsIgnoreCase("rejected")) {
            Log.d(TAG, "handleRejectionStatus: Event is rejected");

            if (reason != null && !reason.isEmpty()) {
                reasonContainer.setVisibility(View.VISIBLE);
                reasonText.setText(reason);
                Log.d(TAG, "handleRejectionStatus: Showing rejection reason: " + reason);
            } else {
                // If no reason provided but status is rejected, show default message
                reasonContainer.setVisibility(View.VISIBLE);
                reasonText.setText("No specific reason provided");
                Log.d(TAG, "handleRejectionStatus: No rejection reason found");
            }
        }
    }
    /**
     * Fetches the rejection reason from Firebase Realtime Database
     * Modified to correctly query the eventProposals node based on eventId
     */
    private void fetchRejectionReasonFromDatabase() {
        Log.d(TAG, "fetchRejectionReasonFromDatabase: Fetching rejection reason for event ID: " + eventId);

        if (eventId != null && !eventId.isEmpty()) {
            // Initialize Firebase Database reference - pointing to eventProposals node
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("eventProposals");

            // Query the database for the specific event by its ID
            databaseRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Try to get the reason field
                        String reasonFromDb = dataSnapshot.child("reason").getValue(String.class);

                        if (reasonFromDb != null && !reasonFromDb.isEmpty()) {
                            reasonContainer.setVisibility(View.VISIBLE);
                            reasonText.setText(reasonFromDb);
                            Log.d(TAG, "fetchRejectionReasonFromDatabase: Reason loaded from database: " + reasonFromDb);
                        } else {
                            Log.d(TAG, "fetchRejectionReasonFromDatabase: No rejection reason found in database");
                            // Even if no reason, show container with default message
                            reasonContainer.setVisibility(View.VISIBLE);
                            reasonText.setText("No specific reason provided");
                        }
                    } else {
                        Log.d(TAG, "fetchRejectionReasonFromDatabase: Event not found in database");
                        // Try alternative path as fallback - directly with event ID
                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                        rootRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot fallbackSnapshot) {
                                if (fallbackSnapshot.exists()) {
                                    String fallbackReason = fallbackSnapshot.child("reason").getValue(String.class);
                                    if (fallbackReason != null && !fallbackReason.isEmpty()) {
                                        reasonContainer.setVisibility(View.VISIBLE);
                                        reasonText.setText(fallbackReason);
                                        Log.d(TAG, "Fallback: Reason loaded from database: " + fallbackReason);
                                    } else {
                                        // As another fallback, try to get the reason directly from the event name
                                        tryFetchByEventName();
                                    }
                                } else {
                                    // As another fallback, try to get the reason directly from the event name
                                    tryFetchByEventName();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "Fallback query cancelled", databaseError.toException());
                                tryFetchByEventName();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "fetchRejectionReasonFromDatabase: Database error: ", databaseError.toException());
                    reasonContainer.setVisibility(View.GONE);
                }
            });
        } else if (eventName != null && !eventName.isEmpty()) {
            // If eventId is null but eventName is available, try with event name
            tryFetchByEventName();
        } else {
            Log.e(TAG, "fetchRejectionReasonFromDatabase: Both event ID and name are null or empty");
            reasonContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Additional method to try fetching rejection reason by event name
     * Used as a fallback when eventId-based query fails
     */
    private void tryFetchByEventName() {
        if (eventName == null || eventName.isEmpty()) {
            Log.e(TAG, "tryFetchByEventName: Event name is null or empty");
            reasonContainer.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "tryFetchByEventName: Trying to fetch rejection reason for event name: " + eventName);

        // Initialize Firebase Database reference
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // Query the eventProposals node for matching eventName
        DatabaseReference proposalsRef = rootRef.child("eventProposals");

        Query query = proposalsRef.orderByChild("eventName").equalTo(eventName);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean reasonFound = false;

                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    String status = eventSnapshot.child("status").getValue(String.class);
                    if (status != null && status.equalsIgnoreCase("rejected")) {
                        String reasonFromDb = eventSnapshot.child("reason").getValue(String.class);
                        if (reasonFromDb != null && !reasonFromDb.isEmpty()) {
                            reasonContainer.setVisibility(View.VISIBLE);
                            reasonText.setText(reasonFromDb);
                            reasonFound = true;
                            Log.d(TAG, "tryFetchByEventName: Reason found: " + reasonFromDb);
                            break;
                        }
                    }
                }

                if (!reasonFound) {
                    // Last attempt - try direct path with event name
                    rootRef.child(eventName).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot directSnapshot) {
                            if (directSnapshot.exists()) {
                                String directReason = directSnapshot.child("reason").getValue(String.class);
                                if (directReason != null && !directReason.isEmpty()) {
                                    reasonContainer.setVisibility(View.VISIBLE);
                                    reasonText.setText(directReason);
                                    Log.d(TAG, "Direct path: Reason found: " + directReason);
                                } else {
                                    // Show default message
                                    reasonContainer.setVisibility(View.VISIBLE);
                                    reasonText.setText("No specific reason provided");
                                    Log.d(TAG, "No reason found after all attempts");
                                }
                            } else {
                                // Show default message
                                reasonContainer.setVisibility(View.VISIBLE);
                                reasonText.setText("No specific reason provided");
                                Log.d(TAG, "No reason found after all attempts");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Direct path query cancelled", databaseError.toException());
                            reasonContainer.setVisibility(View.VISIBLE);
                            reasonText.setText("No specific reason provided");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "tryFetchByEventName: Database error: ", databaseError.toException());
                reasonContainer.setVisibility(View.GONE);
            }
        });
    }
}