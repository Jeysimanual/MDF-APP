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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_approval_inside);

        Log.d(TAG, "onCreate: Initializing EventApprovalInside activity");

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

        // Display event data
        displayEventData();
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

    private void displayEventData() {
        Log.d(TAG, "displayEventData: Setting event data to views");

        Intent intent = getIntent();
        if (intent != null) {
            // Set text to views
            String eventName = intent.getStringExtra("EVENT_NAME");
            if (eventName != null && !eventName.isEmpty()) {
                eventNameText.setText(eventName);
            }

            String eventDescription = intent.getStringExtra("EVENT_DESCRIPTION");
            if (eventDescription != null && !eventDescription.isEmpty()) {
                eventDescriptionText.setText(eventDescription);
            }

            // Get start date, end date, and venue
            String startDate = intent.getStringExtra("EVENT_START_DATE");
            String endDate = intent.getStringExtra("EVENT_END_DATE");
            String venue = intent.getStringExtra("EVENT_VENUE");

            // Handle start date
            if (startDate != null && !startDate.isEmpty()) {
                startDateText.setText(startDate);
            }

            // Handle venue - make sure it's visible and set
            if (venue != null && !venue.isEmpty() && !venue.equalsIgnoreCase("linga")) {
                venueText.setText(venue);
                venueText.setVisibility(View.VISIBLE);
                venueLabel.setVisibility(View.VISIBLE);
            } else {
                venueText.setVisibility(View.GONE);
                venueLabel.setVisibility(View.GONE);
            }

            // Check if event is a single day event
            if (startDate != null && endDate != null && startDate.equals(endDate)) {
                // It's a single day event, hide the end date section
                endDateLabel.setVisibility(View.GONE);
                endDateText.setVisibility(View.GONE);
            } else {
                // It's a multi-day event, show both dates
                endDateLabel.setVisibility(View.VISIBLE);
                endDateText.setVisibility(View.VISIBLE);
                if (endDate != null && !endDate.isEmpty()) {
                    endDateText.setText(endDate);
                } else {
                    endDateText.setText("Not specified");
                }
            }

            // Set time values
            String startTime = intent.getStringExtra("EVENT_START_TIME");
            String endTime = intent.getStringExtra("EVENT_END_TIME");

            if (startTime != null && !startTime.isEmpty()) {
                startTimeText.setText(startTime);
            } else {
                startTimeText.setText("Not specified");
            }

            if (endTime != null && !endTime.isEmpty() && !endTime.equalsIgnoreCase("linga")) {
                endTimeText.setText(endTime);
            } else {
                endTimeText.setText("Not specified");
            }

            // Additional fields
            String eventSpan = intent.getStringExtra("EVENT_SPAN");
            if (eventSpan != null && !eventSpan.isEmpty()) {
                eventSpanText.setText(eventSpan);
            } else {
                eventSpanText.setText("Single-day");
            }

            String graceTime = intent.getStringExtra("EVENT_GRACE_TIME");
            if (graceTime != null && !graceTime.isEmpty()) {
                graceTimeText.setText(graceTime);
            } else {
                graceTimeText.setText("0");
            }

            String eventType = intent.getStringExtra("EVENT_TYPE");
            if (eventType != null && !eventType.isEmpty()) {
                eventTypeText.setText(eventType);
            } else {
                eventTypeText.setText("Other");
            }

            String eventFor = intent.getStringExtra("EVENT_FOR");
            if (eventFor != null && !eventFor.isEmpty()) {
                eventForText.setText(eventFor);
            } else {
                eventForText.setText("All");
            }

            // Load image if available
            String photoUrl = intent.getStringExtra("EVENT_PHOTO_URL");
            if (photoUrl != null && !photoUrl.isEmpty()) {
                try {
                    Picasso.get().load(photoUrl).placeholder(R.drawable.placeholder_image).into(eventPhotoUrlImage);
                } catch (Exception e) {
                    Log.e(TAG, "displayEventData: Error loading image", e);
                    eventPhotoUrlImage.setImageResource(R.drawable.placeholder_image);
                }
            } else {
                eventPhotoUrlImage.setImageResource(R.drawable.placeholder_image);
            }

            // Handle event rejection status and reason
            handleEventRejectionStatus();
        }
    }

    /**
     * Handles the display of rejection reason when event status is "rejected"
     * Shows the rejection container with the reason fetched from the database/intent
     */
    private void handleEventRejectionStatus() {
        Log.d(TAG, "handleEventRejectionStatus: Checking if event is rejected");

        // Default hide the rejection container
        reasonContainer.setVisibility(View.GONE);

        // Check if event status is "rejected"
        if (eventStatus != null && eventStatus.equalsIgnoreCase("rejected")) {
            Log.d(TAG, "handleEventRejectionStatus: Event is rejected, showing reason container");

            // First check if rejection reason is in intent
            Intent intent = getIntent();
            String reasonOfRejection = intent.getStringExtra("EVENT_REJECTION_REASON");

            // If reason is in intent, display it
            if (reasonOfRejection != null && !reasonOfRejection.isEmpty()) {
                reasonContainer.setVisibility(View.VISIBLE);
                reasonText.setText(reasonOfRejection);
                Log.d(TAG, "handleEventRejectionStatus: Rejection reason set from intent: " + reasonOfRejection);
            } else {
                // If not in intent, fetch from Firebase
                fetchRejectionReasonFromDatabase();
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