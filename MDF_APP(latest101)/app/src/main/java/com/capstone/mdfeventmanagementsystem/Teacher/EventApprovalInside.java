package com.capstone.mdfeventmanagementsystem.Teacher;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private CardView proposalCardView;
    private Button viewProposalButton;
    private Button resubmitEventButton; // New button for resubmitting events

    // Labels for visibility control
    private TextView endDateLabel;
    private TextView venueLabel;

    // Event data
    private String eventId;
    private String eventStatus;
    private String eventName;
    private String proposalDocUrl;
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

        // Set up proposal document button click listener
        setupProposalButtonListener();

        // Set up resubmit button click listener
        setupResubmitButtonListener();
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
        proposalCardView = findViewById(R.id.proposalCardView);
        viewProposalButton = findViewById(R.id.viewProposalButton);
        resubmitEventButton = findViewById(R.id.resubmitEventButton); // Initialize the resubmit button

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

            // Check if status is rejected to show resubmit button
            if (eventStatus != null && eventStatus.equalsIgnoreCase("rejected")) {
                resubmitEventButton.setVisibility(View.VISIBLE);
                Log.d(TAG, "receiveIntentData: Event is rejected, showing resubmit button");
            }
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

        // Update event status from database
        eventStatus = status;

        // Get the proposal document URL
        proposalDocUrl = dataSnapshot.child("eventProposal").getValue(String.class);

        // Check if proposal document URL exists
        if (proposalDocUrl != null && !proposalDocUrl.isEmpty()) {
            proposalCardView.setVisibility(View.VISIBLE);
            Log.d(TAG, "displayEventDataFromDatabase: Found proposal document URL: " + proposalDocUrl);
        } else {
            // Try alternative field name
            proposalDocUrl = dataSnapshot.child("eventProposalUrl").getValue(String.class);
            if (proposalDocUrl != null && !proposalDocUrl.isEmpty()) {
                proposalCardView.setVisibility(View.VISIBLE);
                Log.d(TAG, "displayEventDataFromDatabase: Found proposal document URL from alternative field: " + proposalDocUrl);
            } else {
                proposalCardView.setVisibility(View.GONE);
                Log.d(TAG, "displayEventDataFromDatabase: No proposal document URL found");
            }
        }

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
            proposalDocUrl = intent.getStringExtra("EVENT_PROPOSAL_URL");

            // Check if proposal URL exists
            if (proposalDocUrl != null && !proposalDocUrl.isEmpty()) {
                proposalCardView.setVisibility(View.VISIBLE);
                Log.d(TAG, "displayEventDataFromIntent: Found proposal document URL from intent: " + proposalDocUrl);
            } else {
                proposalCardView.setVisibility(View.GONE);
                Log.d(TAG, "displayEventDataFromIntent: No proposal document URL found in intent");
            }

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
     * and shows the resubmit button
     */
    private void handleRejectionStatus(String status, String reason) {
        Log.d(TAG, "handleRejectionStatus: Checking if event is rejected");

        // Default hide the rejection container
        reasonContainer.setVisibility(View.GONE);

        // Default hide the resubmit button
        resubmitEventButton.setVisibility(View.GONE);

        // Check if event status is "rejected"
        if (status != null && status.equalsIgnoreCase("rejected")) {
            Log.d(TAG, "handleRejectionStatus: Event is rejected");

            // Show the resubmit button for rejected events
            resubmitEventButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "handleRejectionStatus: Showing resubmit button");

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
     * Set up the click listener for the proposal document button
     */
    private void setupProposalButtonListener() {
        viewProposalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProposalDocument();
            }
        });
    }

    /**
     * Set up the click listener for the resubmit event button
     */
    private void setupResubmitButtonListener() {
        resubmitEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToCreateEventActivity();
            }
        });
    }

    /**
     * Navigate to TeacherCreateEventActivity with the event data for resubmission
     */
    private void navigateToCreateEventActivity() {
        Log.d(TAG, "navigateToCreateEventActivity: Navigating to TeacherCreateEventActivity for resubmission");

        Intent intent = new Intent(EventApprovalInside.this, TeacherCreateEventActivity.class);

        // Pass event data to pre-fill the form
        intent.putExtra("IS_RESUBMISSION", true);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("EVENT_NAME", eventNameText.getText().toString());
        intent.putExtra("EVENT_DESCRIPTION", eventDescriptionText.getText().toString());
        intent.putExtra("EVENT_VENUE", venueText.getText().toString());
        intent.putExtra("EVENT_START_DATE", startDateText.getText().toString());
        intent.putExtra("EVENT_END_DATE", endDateText.getText().toString());
        intent.putExtra("EVENT_START_TIME", startTimeText.getText().toString());
        intent.putExtra("EVENT_END_TIME", endTimeText.getText().toString());
        intent.putExtra("EVENT_SPAN", eventSpanText.getText().toString());
        intent.putExtra("EVENT_GRACE_TIME", graceTimeText.getText().toString());
        intent.putExtra("EVENT_TYPE", eventTypeText.getText().toString());
        intent.putExtra("EVENT_FOR", eventForText.getText().toString());
        intent.putExtra("EVENT_PROPOSAL_URL", proposalDocUrl);

        // Start the activity
        startActivity(intent);

        // Show a toast message
        Toast.makeText(EventApprovalInside.this,
                "Resubmitting event: " + eventNameText.getText().toString(),
                Toast.LENGTH_SHORT).show();
    }


    /**
     * Open the proposal document in a browser or PDF viewer
     */
    private void openProposalDocument() {
        Log.d(TAG, "openProposalDocument: Attempting to open proposal document");

        if (proposalDocUrl != null && !proposalDocUrl.isEmpty()) {
            try {
                // Create an intent to view the document
                Intent intent = new Intent(Intent.ACTION_VIEW);

                // Ensure the URL has proper encoding
                String encodedUrl = proposalDocUrl;
                if (!proposalDocUrl.startsWith("http://") && !proposalDocUrl.startsWith("https://")) {
                    // Add https if missing
                    encodedUrl = "https://" + proposalDocUrl;
                }

                // Set the proper MIME type based on file extension
                String mimeType = getMimeTypeFromUrl(encodedUrl);
                if (mimeType != null) {
                    intent.setDataAndType(Uri.parse(encodedUrl), mimeType);
                    Log.d(TAG, "openProposalDocument: Setting MIME type: " + mimeType);
                } else {
                    intent.setData(Uri.parse(encodedUrl));
                }

                // Add flags to start activity in a new task
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Try to open with specific app first
                try {
                    startActivity(intent);
                    Log.d(TAG, "openProposalDocument: Opening document with specific handler: " + encodedUrl);
                    return;
                } catch (ActivityNotFoundException specificAppNotFound) {
                    Log.d(TAG, "openProposalDocument: No specific app found, trying browser");
                }

                // If no specific app to handle the document, try with browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(encodedUrl));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(browserIntent);
                    Log.d(TAG, "openProposalDocument: Opening in browser: " + encodedUrl);
                } catch (ActivityNotFoundException browserNotFound) {
                    Toast.makeText(EventApprovalInside.this,
                            "No application found to open this document. Please install an appropriate viewer or browser.",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "openProposalDocument: No application found to handle URL at all");
                }

            } catch (Exception e) {
                Toast.makeText(EventApprovalInside.this,
                        "Error opening document: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "openProposalDocument: Error opening document", e);
            }
        } else {
            // No proposal URL available
            Toast.makeText(EventApprovalInside.this,
                    "No proposal document available for this event",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "openProposalDocument: No proposal document URL available");

            // Try fetching the proposal URL again from Firebase
            fetchProposalUrlFromFirebase();
        }
    }

    /**
     * Get MIME type based on file extension in URL
     * @param url The document URL
     * @return The MIME type string or null if unknown
     */
    private String getMimeTypeFromUrl(String url) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null && !extension.isEmpty()) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType != null && !mimeType.isEmpty()) {
                return mimeType;
            }
        }

        // Handle common document types that might not be properly detected
        if (url.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (url.toLowerCase().endsWith(".doc") || url.toLowerCase().endsWith(".docx")) {
            return "application/msword";
        } else if (url.toLowerCase().endsWith(".xls") || url.toLowerCase().endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (url.toLowerCase().endsWith(".ppt") || url.toLowerCase().endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (url.toLowerCase().endsWith(".txt")) {
            return "text/plain";
        }
        // If we can't determine the type, return null to let the system decide
        return null;
    }
    /**
     * Fetch the proposal URL from Firebase if it wasn't found initially
     */
    private void fetchProposalUrlFromFirebase() {
        Log.d(TAG, "fetchProposalUrlFromFirebase: Attempting to fetch proposal URL from Firebase");

        if (eventId != null && !eventId.isEmpty()) {
            eventProposalsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Try different potential field names for the proposal URL
                        String url = dataSnapshot.child("eventProposal").getValue(String.class);

                        if (url == null || url.isEmpty()) {
                            url = dataSnapshot.child("eventProposalUrl").getValue(String.class);
                        }

                        if (url == null || url.isEmpty()) {
                            url = dataSnapshot.child("proposalUrl").getValue(String.class);
                        }

                        if (url == null || url.isEmpty()) {
                            url = dataSnapshot.child("proposalDocUrl").getValue(String.class);
                        }

                        if (url != null && !url.isEmpty()) {
                            proposalDocUrl = url;
                            proposalCardView.setVisibility(View.VISIBLE);
                            Log.d(TAG, "fetchProposalUrlFromFirebase: Found proposal URL: " + proposalDocUrl);

                            // Try opening the document again
                            openProposalDocument();
                        } else {
                            Log.d(TAG, "fetchProposalUrlFromFirebase: No proposal URL found in database");
                            Toast.makeText(EventApprovalInside.this,
                                    "No proposal document found for this event",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "fetchProposalUrlFromFirebase: Event not found in database");
                        Toast.makeText(EventApprovalInside.this,
                                "Could not find event information",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "fetchProposalUrlFromFirebase: Database error", databaseError.toException());
                    Toast.makeText(EventApprovalInside.this,
                            "Database error: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else if (eventName != null && !eventName.isEmpty()) {
            // Try finding the event by name if ID is not available
            Query query = eventProposalsRef.orderByChild("eventName").equalTo(eventName);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        // Get the first matching event
                        DataSnapshot firstEvent = dataSnapshot.getChildren().iterator().next();

                        // Try different potential field names for the proposal URL
                        String url = firstEvent.child("eventProposal").getValue(String.class);

                        if (url == null || url.isEmpty()) {
                            url = firstEvent.child("eventProposalUrl").getValue(String.class);
                        }

                        if (url == null || url.isEmpty()) {
                            url = firstEvent.child("proposalUrl").getValue(String.class);
                        }

                        if (url == null || url.isEmpty()) {
                            url = firstEvent.child("proposalDocUrl").getValue(String.class);
                        }

                        if (url != null && !url.isEmpty()) {
                            proposalDocUrl = url;
                            proposalCardView.setVisibility(View.VISIBLE);
                            Log.d(TAG, "fetchProposalUrlFromFirebase: Found proposal URL by name search: " + proposalDocUrl);

                            // Try opening the document again
                            openProposalDocument();
                        } else {
                            Log.d(TAG, "fetchProposalUrlFromFirebase: No proposal URL found by name search");
                            Toast.makeText(EventApprovalInside.this,
                                    "No proposal document found for this event",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "fetchProposalUrlFromFirebase: No event found by name: " + eventName);
                        Toast.makeText(EventApprovalInside.this,
                                "Could not find event information",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "fetchProposalUrlFromFirebase: Database error in name query", databaseError.toException());
                    Toast.makeText(EventApprovalInside.this,
                            "Database error: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "fetchProposalUrlFromFirebase: No event ID or name available");
            Toast.makeText(EventApprovalInside.this,
                    "Event information is incomplete",
                    Toast.LENGTH_SHORT).show();
        }
    }
}