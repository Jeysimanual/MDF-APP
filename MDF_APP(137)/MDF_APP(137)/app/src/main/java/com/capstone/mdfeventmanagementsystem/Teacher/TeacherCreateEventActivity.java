package com.capstone.mdfeventmanagementsystem.Teacher;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Utilities.BaseActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherCreateEventActivity extends BaseActivity {

    private ConstraintLayout page1, page2, page3;
    private Button nextPg1, nextPg2, backPg2, backPg3, createButton, chooseFileButton;
    private int currentPage = 1;
    private TextView fileNameLabel;
    private MaterialCardView uploadCoverPhotoCard;
    private ImageView uploadCoverPhoto;

    // Form fields for page 1
    private EditText eventNameField;
    private EditText eventDescriptionField;
    private Spinner eventTypeSpinner;
    private Spinner eventForSpinner;
    // Form fields for page 2
    private EditText venueField;
    private RadioButton radioSingleDayEvent, radioMultiDayEvent;
    private EditText startDateField, endDateField;
    private EditText startTimeField, endTimeField;
    private Spinner graceTimeSpinner;

    private TextView specifyEventTypeLabel;
    private TextView specifyEventTypeAsterisk;
    private EditText specifyEventTypeField;

    private Spinner targetParticipantsSpinner;
    private TextView customParticipantsLabel;
    private TextView customParticipantsAsterisk;
    private EditText customParticipantsField;

    // Grace Time Custom Fields
    private TextView customGraceTimeLabel;
    private TextView customGraceTimeAsterisk;
    private EditText customGraceTimeField;

    // Uri for the selected image and document
    private Uri coverPhotoUri = null;
    private Uri proposalFileUri = null;
    private String proposalFileName = "";

    // Firebase Storage
    private StorageReference storageRef;

    // Activity Result Launchers for file selection
    private ActivityResultLauncher<Intent> coverPhotoLauncher;
    private ActivityResultLauncher<String> proposalFileLauncher;

    // Data storage for event information
    private Map<String, Object> eventData = new HashMap<>();

    // Firebase
    private FirebaseDatabase database;
    private DatabaseReference eventProposalRef;

    // Edit mode flag
    private boolean isEditing = false;
    private boolean isProposal = true; // Added to track if editing a proposal
    private String eventId;

    private LinearLayout selectedYearLevelsContainer;
    private List<String> selectedYearLevels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_create_event);

        // Initialize Firebase
        initializeFirebase();

        // Initialize createButton
        createButton = findViewById(R.id.createButton);

        // Initialize page 1 form fields
        initializePage1Fields();

        // Initialize page 2 form fields
        initializePage2Fields();

        // Initialize page 3 views
        uploadCoverPhotoCard = findViewById(R.id.uploadCoverPhotoCard);
        uploadCoverPhoto = findViewById(R.id.uploadCoverPhoto);
        chooseFileButton = findViewById(R.id.chooseFileButton);
        fileNameLabel = findViewById(R.id.fileNameLabel);
        TextView proposalLabel = findViewById(R.id.proposalLabel);
        TextView proposalAsterisk = findViewById(R.id.proposalAsterisk);
        ConstraintLayout fileUploadContainer = findViewById(R.id.fileUploadContainer);

        // Check if this is a resubmission or edit
        checkForResubmissionOrEdit();

        // Log the state of isEditing and isProposal
        Log.d("TeacherCreateEvent", "onCreate after checkForResubmissionOrEdit: isEditing=" + isEditing + ", isProposal=" + isProposal);

        // Handle visibility of proposal fields
        if (isEditing && !isProposal) {
            // Editing an approved event (in events node)
            Log.d("TeacherCreateEvent", "Hiding proposal fields for approved event edit");
            if (proposalLabel != null) {
                proposalLabel.setVisibility(View.GONE);
                Log.d("TeacherCreateEvent", "proposalLabel set to GONE");
            } else {
                Log.e("TeacherCreateEvent", "proposalLabel is null");
            }
            if (proposalAsterisk != null) {
                proposalAsterisk.setVisibility(View.GONE);
                Log.d("TeacherCreateEvent", "proposalAsterisk set to GONE");
            } else {
                Log.e("TeacherCreateEvent", "proposalAsterisk is null");
            }
            if (fileUploadContainer != null) {
                fileUploadContainer.setVisibility(View.GONE);
                Log.d("TeacherCreateEvent", "fileUploadContainer set to GONE");
            } else {
                Log.e("TeacherCreateEvent", "fileUploadContainer is null");
            }
            if (chooseFileButton != null) {
                chooseFileButton.setVisibility(View.GONE);
                Log.d("TeacherCreateEvent", "chooseFileButton set to GONE");
            } else {
                Log.e("TeacherCreateEvent", "chooseFileButton is null");
            }
            if (fileNameLabel != null) {
                fileNameLabel.setVisibility(View.GONE);
                Log.d("TeacherCreateEvent", "fileNameLabel set to GONE");
            } else {
                Log.e("TeacherCreateEvent", "fileNameLabel is null");
            }
            eventData.remove("eventProposal");
            Log.d("TeacherCreateEvent", "Removed eventProposal from eventData");
        } else {
            // Creating a new event or editing a proposal
            Log.d("TeacherCreateEvent", "Showing proposal fields: isEditing=" + isEditing + ", isProposal=" + isProposal);
            if (proposalLabel != null) {
                proposalLabel.setVisibility(View.VISIBLE);
                Log.d("TeacherCreateEvent", "proposalLabel set to VISIBLE");
            } else {
                Log.e("TeacherCreateEvent", "proposalLabel is null");
            }
            if (proposalAsterisk != null) {
                proposalAsterisk.setVisibility(View.VISIBLE);
                Log.d("TeacherCreateEvent", "proposalAsterisk set to VISIBLE");
            } else {
                Log.e("TeacherCreateEvent", "proposalAsterisk is null");
            }
            if (fileUploadContainer != null) {
                fileUploadContainer.setVisibility(View.VISIBLE);
                Log.d("TeacherCreateEvent", "fileUploadContainer set to VISIBLE");
            } else {
                Log.e("TeacherCreateEvent", "fileUploadContainer is null");
            }
            if (chooseFileButton != null) {
                chooseFileButton.setVisibility(View.VISIBLE);
                Log.d("TeacherCreateEvent", "chooseFileButton set to VISIBLE");
            } else {
                Log.e("TeacherCreateEvent", "chooseFileButton is null");
            }
            if (fileNameLabel != null) {
                fileNameLabel.setVisibility(View.VISIBLE);
                Log.d("TeacherCreateEvent", "fileNameLabel set to VISIBLE");
            } else {
                Log.e("TeacherCreateEvent", "fileNameLabel is null");
            }
        }

        // Initialize other views
        page1 = findViewById(R.id.page1);
        page2 = findViewById(R.id.page2);
        page3 = findViewById(R.id.page3);

        nextPg1 = findViewById(R.id.nextButton1);
        nextPg2 = findViewById(R.id.nextPg3);
        backPg2 = findViewById(R.id.backPg1);
        backPg3 = findViewById(R.id.backPg2);

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Set up click listeners for page 3
        uploadCoverPhotoCard.setOnClickListener(v -> openGallery());
        if (chooseFileButton != null) {
            chooseFileButton.setOnClickListener(v -> openFileChooser());
        } else {
            Log.e("TeacherCreateEvent", "chooseFileButton is null when setting click listener");
        }
        if (coverPhotoUri == null && !eventData.containsKey("eventPhotoUrl")) {
            uploadCoverPhoto.setImageResource(R.drawable.ic_add); // Default icon
        }
        uploadCoverPhoto.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Ensure icon or image is visible

        setupPageNavigation();
        setupBottomNavigation();
        showPage(1); // Show first page initially

    }
    @Override
    public void onBackPressed() {
        // Check if any meaningful data is entered
        boolean hasData = !eventNameField.getText().toString().trim().isEmpty() ||
                !eventDescriptionField.getText().toString().trim().isEmpty() ||
                !selectedYearLevels.isEmpty() ||
                coverPhotoUri != null ||
                proposalFileUri != null ||
                !startDateField.getText().toString().trim().isEmpty();

        if (hasData) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Leave Event Creation?")
                    .setMessage("You have unsaved changes. Are you sure you want to leave?")
                    .setPositiveButton("Leave", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Stay", null)
                    .setCancelable(false)
                    .show();
        } else {
            // No data entered → safe to go back
            super.onBackPressed();
        }
    }

    private void checkForResubmissionOrEdit() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("IS_EDITING", false)) {
                isEditing = true;
                isProposal = intent.getBooleanExtra("IS_PROPOSAL", false);
                Log.d("TeacherCreateEvent", "checkForResubmissionOrEdit: isEditing=" + isEditing + ", isProposal=" + isProposal);
                createButton.setText(isProposal ? "Update" : "Update");
                eventId = intent.getStringExtra("EVENT_ID");
                populateFieldsForEdit(intent);
            } else if (intent.getBooleanExtra("IS_RESUBMISSION", false)) {
                createButton.setText("Resubmit Event");
                isProposal = true;
                Log.d("TeacherCreateEvent", "checkForResubmissionOrEdit: Resubmission, isProposal=true");
                populateFieldsForResubmission(intent);
            } else {
                // New event creation, ensure proposal fields are visible
                isEditing = false;
                isProposal = true; // New events require a proposal
                Log.d("TeacherCreateEvent", "checkForResubmissionOrEdit: New event, isEditing=false, isProposal=true");
                createButton.setText("Create Event");
            }
        }
    }

    // Remove initializePage3Fields method as it's redundant
    // All page 3 initialization is now handled in onCreate

    // Rest of the methods remain unchanged
    private void populateFieldsForEdit(Intent intent) {
        // Get the event details from the intent
        eventId = intent.getStringExtra("EVENT_ID");
        String eventName = intent.getStringExtra("EVENT_NAME");
        String eventDescription = intent.getStringExtra("EVENT_DESCRIPTION");
        String venue = intent.getStringExtra("EVENT_VENUE");
        String startDate = intent.getStringExtra("EVENT_START_DATE");
        String endDate = intent.getStringExtra("EVENT_END_DATE");
        String startTime = intent.getStringExtra("EVENT_START_TIME");
        String endTime = intent.getStringExtra("EVENT_END_TIME");
        String eventSpan = intent.getStringExtra("EVENT_SPAN");
        String graceTime = intent.getStringExtra("EVENT_GRACE_TIME");
        String eventType = intent.getStringExtra("EVENT_TYPE");
        String eventFor = intent.getStringExtra("EVENT_FOR");
        String proposalUrl = intent.getStringExtra("EVENT_PROPOSAL_URL");
        String photoUrl = intent.getStringExtra("EVENT_PHOTO_URL");
        String targetParticipant = intent.getStringExtra("TARGET_PARTICIPANTS");

        // Store the eventId for later use when saving
        eventData.put("eventId", eventId);

        // Populate Page 1 fields
        populatePage1Fields(eventName, eventDescription, eventType, eventFor, targetParticipant);

        // Populate Page 2 fields
        populatePage2Fields(venue, startDate, endDate, startTime, endTime, eventSpan, graceTime);

        // For Page 3, handle existing files
        if (proposalUrl != null && !proposalUrl.isEmpty()) {
            eventData.put("eventProposal", proposalUrl);
            // Display the existing proposal file name
            String fileName = proposalUrl.substring(proposalUrl.lastIndexOf('/') + 1);
            fileNameLabel.setText(fileName);
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            eventData.put("eventPhotoUrl", photoUrl);
            // Load the existing cover photo
            Glide.with(this)
                    .load(photoUrl)
                    .centerCrop()
                    .into(uploadCoverPhoto);
        } else {
            // If no photo URL, set default icon
            uploadCoverPhoto.setImageResource(R.drawable.ic_add);
        }
    }

    private void populateFieldsForResubmission(Intent intent) {
        // Get the event details from the intent
        String eventId = intent.getStringExtra("EVENT_ID");
        String eventName = intent.getStringExtra("EVENT_NAME");
        String eventDescription = intent.getStringExtra("EVENT_DESCRIPTION");
        String venue = intent.getStringExtra("EVENT_VENUE");
        String startDate = intent.getStringExtra("EVENT_START_DATE");
        String endDate = intent.getStringExtra("EVENT_END_DATE");
        String startTime = intent.getStringExtra("EVENT_START_TIME");
        String endTime = intent.getStringExtra("EVENT_END_TIME");
        String eventSpan = intent.getStringExtra("EVENT_SPAN");
        String graceTime = intent.getStringExtra("EVENT_GRACE_TIME");
        String eventType = intent.getStringExtra("EVENT_TYPE");
        String eventFor = intent.getStringExtra("EVENT_FOR");
        String proposalUrl = intent.getStringExtra("EVENT_PROPOSAL_URL");
        String targetParticipant = intent.getStringExtra("TARGET_PARTICIPANTS");

        // Store the eventId for later use when saving
        eventData.put("originalEventId", eventId);

        // Populate Page 1 fields
        populatePage1Fields(eventName, eventDescription, eventType, eventFor, targetParticipant);

        // Populate Page 2 fields
        populatePage2Fields(venue, startDate, endDate, startTime, endTime, eventSpan, graceTime);

        // For Page 3, if there's a proposal URL, we need to handle it differently
        if (proposalUrl != null && !proposalUrl.isEmpty()) {
            eventData.put("existingProposalUrl", proposalUrl);
            // Display the existing proposal file name
            String fileName = proposalUrl.substring(proposalUrl.lastIndexOf('/') + 1);
            fileNameLabel.setText(fileName);
        }
    }

    private void populatePage1Fields(String eventName, String eventDescription, String eventType, String eventFor, String targetParticipants) {
        // Log input parameters
        Log.d("populatePage1Fields", "Input - eventName: " + eventName);
        Log.d("populatePage1Fields", "Input - eventDescription: " + eventDescription);
        Log.d("populatePage1Fields", "Input - eventType: " + eventType);
        Log.d("populatePage1Fields", "Input - eventFor: " + eventFor);
        Log.d("populatePage1Fields", "Input - targetParticipants: " + targetParticipants);

        // Set event name and description
        if (eventName != null) {
            eventNameField.setText(eventName);
            eventData.put("eventName", eventName);
            Log.d("populatePage1Fields", "Set eventNameField to: " + eventName);
        }

        if (eventDescription != null) {
            eventDescriptionField.setText(eventDescription);
            eventData.put("eventDescription", eventDescription);
            Log.d("populatePage1Fields", "Set eventDescriptionField to: " + eventDescription);
        }

        // Set event type spinner
        if (eventType != null) {
            String[] eventTypes = {"Seminar", "Off-Campus Activity", "Sports Event", "Other"};
            boolean foundMatchingType = false;

            Log.d("populatePage1Fields", "Checking eventType: " + eventType + " against predefined types");
            for (int i = 0; i < eventTypes.length; i++) {
                if (eventTypes[i].equalsIgnoreCase(eventType)) {
                    eventTypeSpinner.setSelection(i);
                    eventData.put("eventType", eventType);
                    foundMatchingType = true;
                    Log.d("populatePage1Fields", "Found matching type: " + eventType + " at index " + i);
                    break;
                }
            }

            if (!foundMatchingType) {
                Log.d("populatePage1Fields", "No match found, handling as custom type");
                specifyEventTypeLabel.setVisibility(View.VISIBLE);
                specifyEventTypeAsterisk.setVisibility(View.VISIBLE);
                specifyEventTypeField.setVisibility(View.VISIBLE);
                Log.d("populatePage1Fields", "Setting specifyEventTypeField to: " + eventType);
                specifyEventTypeField.setText(eventType);
                eventTypeSpinner.setSelection(3); // Set to "Other" after setting text
                eventData.put("eventType", eventType);
                Log.d("populatePage1Fields", "Set spinner to Other after setting text");
            } else {
                specifyEventTypeLabel.setVisibility(View.GONE);
                specifyEventTypeAsterisk.setVisibility(View.GONE);
                specifyEventTypeField.setVisibility(View.GONE);
                Log.d("populatePage1Fields", "No custom type, hiding specifyEventTypeField");
            }
        } else {
            Log.d("populatePage1Fields", "eventType is null, skipping event type population");
        }

        // Set event for (multiple year levels)
        if (eventFor != null) {
            String[] gradeOptions = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "All Year Level"};
            String[] selectedGrades = eventFor.split(",");
            selectedYearLevels.clear();
            for (String grade : selectedGrades) {
                String formattedGrade = grade.trim().replace("-", " ");
                if (grade.equals("All")) {
                    formattedGrade = "All Year Level";
                }
                for (String option : gradeOptions) {
                    if (option.equals(formattedGrade)) {
                        selectedYearLevels.add(formattedGrade);
                        break;
                    }
                }
            }
            updateSelectedYearLevelsUI();
            eventData.put("eventFor", eventFor);
            Log.d("populatePage1Fields", "Set eventFor to: " + eventFor);
        }

        // Set target number of participants spinner
        if (targetParticipants != null) {
            String[] participantOptions = {"None", "10", "20", "30", "40", "50", "100", "Custom"};
            boolean foundMatchingOption = false;

            Log.d("populatePage1Fields", "Checking targetParticipants: " + targetParticipants);
            for (int i = 0; i < participantOptions.length; i++) {
                String option = participantOptions[i];
                if (option.equals("None") && targetParticipants.equalsIgnoreCase("none")) {
                    targetParticipantsSpinner.setSelection(i);
                    eventData.put("targetParticipant", "none");
                    foundMatchingOption = true;
                    Log.d("populatePage1Fields", "Set targetParticipant to none at index " + i);
                    break;
                } else if (option.equals(targetParticipants)) {
                    targetParticipantsSpinner.setSelection(i);
                    eventData.put("targetParticipant", targetParticipants);
                    foundMatchingOption = true;
                    Log.d("populatePage1Fields", "Set targetParticipant to: " + targetParticipants + " at index " + i);
                    break;
                }
            }

            if (!foundMatchingOption) {
                targetParticipantsSpinner.setSelection(7);
                customParticipantsLabel.setVisibility(View.VISIBLE);
                customParticipantsAsterisk.setVisibility(View.VISIBLE);
                customParticipantsField.setVisibility(View.VISIBLE);
                Log.d("populatePage1Fields", "Setting customParticipantsField to: " + targetParticipants);
                customParticipantsField.setText(targetParticipants);
                eventData.put("targetParticipant", targetParticipants);
                Log.d("populatePage1Fields", "Set to Custom after setting text");
            }
        }
    }

    private void populatePage2Fields(String venue, String startDate, String endDate, String startTime, String endTime, String eventSpan, String graceTime) {
        // Set venue
        if (venue != null) {
            venueField.setText(venue);
            eventData.put("venue", venue);
        }

        // Set event span radio buttons
        if (eventSpan != null) {
            if (eventSpan.equals("single-day")) {
                radioSingleDayEvent.setChecked(true);
                eventData.put("eventSpan", "single-day");
            } else if (eventSpan.equals("multi-day")) {
                radioMultiDayEvent.setChecked(true);
                eventData.put("eventSpan", "multi-day");
            }
        }

        // Handle startDate
        if (startDate != null && !startDate.isEmpty()) {
            try {
                SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);

                // Try parsing as storage format (yyyy-MM-dd) or display format (MM-dd-yyyy)
                Date date;
                if (startDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    // If it matches display format, use it directly
                    date = displayFormat.parse(startDate);
                    Log.d("StartDateParsing", "Parsed startDate as display format: " + startDate);
                } else {
                    // Try storage format
                    try {
                        date = storageFormat.parse(startDate);
                        Log.d("StartDateParsing", "Parsed startDate as storage format: " + startDate);
                    } catch (ParseException e) {
                        // Fallback to display format
                        date = displayFormat.parse(startDate);
                        Log.d("StartDateParsing", "Fallback parsed startDate as display format: " + startDate);
                    }
                }

                // Format for display and storage
                String displayDate = displayFormat.format(date);
                String storageDate = storageFormat.format(date);

                // Set the display date in the field
                startDateField.setText(displayDate);
                // Store the storage format in eventData
                eventData.put("startDate", storageDate);

                // If in edit mode, ensure the startDate is retained and displayed
                if (isEditing) {
                    startDateField.setText(displayDate); // Explicitly set to ensure display
                    eventData.put("startDate", storageDate); // Ensure eventData is updated
                    Log.d("StartDateEditMode", "Edit mode: Set startDateField to " + displayDate);
                }
            } catch (ParseException e) {
                // Fallback: set the raw startDate and log the error
                Log.e("StartDateParsing", "Error parsing start date: " + startDate, e);
                startDateField.setText(startDate); // Set raw startDate as fallback
                eventData.put("startDate", startDate);
                // If in edit mode, ensure the raw startDate is displayed
                if (isEditing) {
                    startDateField.setText(startDate); // Explicitly set to ensure display
                    Log.d("StartDateEditMode", "Edit mode fallback: Set startDateField to raw " + startDate);
                }
            }
        } else {
            // Log if startDate is null or empty
            Log.w("StartDateParsing", "startDate is null or empty");
            if (isEditing) {
                startDateField.setText(""); // Clear field in edit mode if no startDate
                eventData.remove("startDate"); // Remove from eventData
            }
        }

        // Handle endDate
        if (endDate != null) {
            try {
                // Check if the date is already in the display format
                if (endDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    endDateField.setText(endDate);

                    // Convert to storage format for eventData
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                    SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date date = displayFormat.parse(endDate);
                    String storageDate = storageFormat.format(date);
                    eventData.put("endDate", storageDate);
                } else {
                    // Assuming it's in the storage format
                    SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                    Date date = storageFormat.parse(endDate);
                    String displayDate = displayFormat.format(date);
                    endDateField.setText(displayDate);
                    eventData.put("endDate", endDate);
                }
            } catch (Exception e) {
                // Handle parsing error
                endDateField.setText(endDate);
                eventData.put("endDate", endDate);
            }
        }

        // Set times
        if (startTime != null) {
            startTimeField.setText(startTime);
            eventData.put("startTime", startTime);
        }

        if (endTime != null) {
            endTimeField.setText(endTime);
            eventData.put("endTime", endTime);
        }

        // Set grace time spinner and handle custom values + DEBUG LOGS
        if (graceTime != null) {
            Log.d("GRACE_TIME_DEBUG", "=== GRACE TIME POPULATE START ===");
            Log.d("GRACE_TIME_DEBUG", "Saved graceTime from Firebase: '" + graceTime + "'");

            String[] graceTimeOptions = {"None", "15 min", "30 min", "60 min", "120 min", "Custom"};

            boolean matched = false;

            if (graceTime.equals("none")) {
                Log.d("GRACE_TIME_DEBUG", "Matched 'none' → setting spinner to 0 (None)");
                graceTimeSpinner.setSelection(0);
                matched = true;
            } else if (graceTime.equals("15")) {
                Log.d("GRACE_TIME_DEBUG", "Matched '15' → setting spinner to 1");
                graceTimeSpinner.setSelection(1);
                matched = true;
            } else if (graceTime.equals("30")) {
                Log.d("GRACE_TIME_DEBUG", "Matched '30' → setting spinner to 2");
                graceTimeSpinner.setSelection(2);
                matched = true;
            } else if (graceTime.equals("60")) {
                Log.d("GRACE_TIME_DEBUG", "Matched '60' → setting spinner to 3");
                graceTimeSpinner.setSelection(3);
                matched = true;
            } else if (graceTime.equals("120")) {
                Log.d("GRACE_TIME_DEBUG", "Matched '120' → setting spinner to 4");
                graceTimeSpinner.setSelection(4);
                matched = true;
            } else {
                // CUSTOM VALUE
                Log.d("GRACE_TIME_DEBUG", "NO predefined match → treating as CUSTOM: " + graceTime);
                try {
                    int customMins = Integer.parseInt(graceTime);
                    Log.d("GRACE_TIME_DEBUG", "Parsed custom minutes: " + customMins);

                    graceTimeSpinner.setSelection(5); // "Custom"
                    Log.d("GRACE_TIME_DEBUG", "Spinner set to Custom (index 5)");

                    // Force show and fill custom field
                    customGraceTimeLabel.setVisibility(View.VISIBLE);
                    customGraceTimeAsterisk.setVisibility(View.VISIBLE);
                    customGraceTimeField.setVisibility(View.VISIBLE);
                    customGraceTimeField.setText(String.valueOf(customMins));

                    Log.d("GRACE_TIME_DEBUG", "Custom field VISIBLE + setText(" + customMins + ")");
                    eventData.put("graceTime", String.valueOf(customMins));

                } catch (NumberFormatException e) {
                    Log.e("GRACE_TIME_DEBUG", "Failed to parse graceTime as number: " + graceTime, e);
                    graceTimeSpinner.setSelection(0);
                }
                matched = true;
            }

            if (!matched) {
                Log.w("GRACE_TIME_DEBUG", "No match found at all → forcing 'None'");
                graceTimeSpinner.setSelection(0);
            }

            Log.d("GRACE_TIME_DEBUG", "=== GRACE TIME POPULATE END ===\n");
        }
    }

    private void initializeFirebase() {
        database = FirebaseDatabase.getInstance();
        eventProposalRef = database.getReference("eventProposals");
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeActivityResultLaunchers() {
        // Launcher for cover photo selection
        coverPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        coverPhotoUri = result.getData().getData();

                        // Display the selected image in the ImageView
                        if (coverPhotoUri != null) {
                            uploadCoverPhoto.setImageResource(0); // Clear default icon
                            Glide.with(this)
                                    .load(coverPhotoUri)
                                    .centerCrop() // Ensures the image fits well
                                    .into(uploadCoverPhoto);
                        } else {
                            uploadCoverPhoto.setImageResource(R.drawable.ic_add); // Revert to default icon
                        }
                    }
                });

        // Launcher for proposal file selection
        proposalFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String fileExtension = getFileExtension(uri);
                        if (fileExtension != null && (fileExtension.equalsIgnoreCase("pdf") ||
                                fileExtension.equalsIgnoreCase("doc") ||
                                fileExtension.equalsIgnoreCase("docx"))) {
                            proposalFileUri = uri;

                            // Get file name from URI
                            String fileName = getFileNameFromUri(uri);
                            proposalFileName = fileName;

                            // Display the file name
                            fileNameLabel.setText(fileName);
                        } else {
                            Toast.makeText(this, "Please select a .pdf, .doc, or .docx file", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            try (android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    private void initializePage1Fields() {
        // Initialize EditText fields
        eventNameField = findViewById(R.id.eventName);
        eventDescriptionField = findViewById(R.id.eventDescription);

        // Initialize the specify event type fields
        specifyEventTypeLabel = findViewById(R.id.specifyEventTypeLabel);
        specifyEventTypeAsterisk = findViewById(R.id.specifyEventTypeAsterisk);
        specifyEventTypeField = findViewById(R.id.specifyEventTypeField);

        // Initialize and setup Event Type Spinner
        eventTypeSpinner = findViewById(R.id.eventTypeSpinner);
        setupEventTypeSpinner();

        // Initialize and setup Event For Spinner
        eventForSpinner = findViewById(R.id.eventForSpinner);
        selectedYearLevelsContainer = findViewById(R.id.selectedYearLevelsContainer);
        setupEventForSpinner();

        // Initialize and setup Target Number of Participants Spinner
        targetParticipantsSpinner = findViewById(R.id.targetParticipantsSpinner);
        customParticipantsLabel = findViewById(R.id.customParticipantsLabel);
        customParticipantsAsterisk = findViewById(R.id.customParticipantsAsterisk);
        customParticipantsField = findViewById(R.id.customParticipantsField);
        setupTargetParticipantsSpinner();

        // Add character limit counters and input restrictions
        setupInputFieldRestrictions();
    }

    private void setupInputFieldRestrictions() {
        // Character counter for eventNameField (max 200 characters)
        final TextView eventNameCharCount = findViewById(R.id.eventNameCharCount);
        eventNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                eventNameCharCount.setText(length + "/200");
                if (length > 200) {
                    eventNameField.setError("Event name cannot exceed 200 characters");
                    eventNameCharCount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    eventNameField.setError(null);
                    eventNameCharCount.setTextColor(getResources().getColor(R.color.gray));
                }
            }
        });

        // Character counter for eventDescriptionField (max 500 characters)
        final TextView eventDescriptionCharCount = findViewById(R.id.eventDescriptionCharCount);
        eventDescriptionField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                eventDescriptionCharCount.setText(length + "/500");
                if (length > 500) {
                    eventDescriptionField.setError("Event description cannot exceed 500 characters");
                    eventDescriptionCharCount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    eventDescriptionField.setError(null);
                    eventDescriptionCharCount.setTextColor(getResources().getColor(R.color.gray));
                }
            }
        });

        // Restrict customParticipantsField to 3 digits (max 999)
        customParticipantsField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.isEmpty()) {
                    try {
                        int value = Integer.parseInt(input);
                        if (value > 999) {
                            customParticipantsField.setText("999");
                            customParticipantsField.setSelection(3);
                            customParticipantsField.setError("Maximum participants is 999");
                        } else if (value <= 0) {
                            customParticipantsField.setError("Number must be greater than 0");
                        } else {
                            customParticipantsField.setError(null);
                        }
                    } catch (NumberFormatException e) {
                        customParticipantsField.setError("Please enter a valid number");
                    }
                } else {
                    customParticipantsField.setError(null);
                }
            }
        });
    }

    private void setupTargetParticipantsSpinner() {
        String[] participantOptions = {"None", "10", "20", "30", "40", "50", "100", "Custom"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                participantOptions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetParticipantsSpinner.setAdapter(adapter);

        targetParticipantsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = participantOptions[position];
                if (selectedOption.equals("Custom")) {
                    customParticipantsLabel.setVisibility(View.VISIBLE);
                    customParticipantsAsterisk.setVisibility(View.VISIBLE);
                    customParticipantsField.setVisibility(View.VISIBLE);
                } else {
                    customParticipantsLabel.setVisibility(View.GONE);
                    customParticipantsAsterisk.setVisibility(View.GONE);
                    customParticipantsField.setVisibility(View.GONE);
                    eventData.put("targetParticipant", selectedOption.equals("None") ? "none" : selectedOption);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                eventData.put("targetParticipant", "none");
            }
        });
    }

    private void initializePage2Fields() {
        // Initialize Venue field
        venueField = findViewById(R.id.venue);

        // Initialize Event Type Radio Buttons
        radioSingleDayEvent = findViewById(R.id.radioSingleDayEvent);
        radioMultiDayEvent = findViewById(R.id.radioMultiDayEvent);


        // Set default selection
        radioSingleDayEvent.setChecked(true);
        eventData.put("eventSpan", "single-day");

        // Add listeners to radio buttons
        radioSingleDayEvent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                eventData.put("eventSpan", "single-day");
                // If single day is selected, make the end date match the start date
                if (startDateField.getText().toString().length() > 0) {
                    endDateField.setText(startDateField.getText().toString());
                }
            }
        });

        radioMultiDayEvent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                eventData.put("eventSpan", "multi-day");
            }
        });

        // Initialize Date pickers
        startDateField = findViewById(R.id.startDate);
        endDateField = findViewById(R.id.endDate);
        setupDatePicker(startDateField, "Start Date");
        setupDatePicker(endDateField, "End Date");

        // Initialize Time pickers
        startTimeField = findViewById(R.id.startTime);
        endTimeField = findViewById(R.id.endTime);
        setupTimePicker(startTimeField, "Start Time");
        setupTimePicker(endTimeField, "End Time");

        // Initialize custom grace time fields
        // Initialize and setup Grace Time Spinner
        graceTimeSpinner = findViewById(R.id.graceTimeSpinner);
        customGraceTimeLabel = findViewById(R.id.customGraceTimeLabel);
        customGraceTimeAsterisk = findViewById(R.id.customGraceTimeAsterisk);
        customGraceTimeField = findViewById(R.id.customGraceTimeField);
        setupGraceTimeSpinner();
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};
        galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        coverPhotoLauncher.launch(galleryIntent);
    }

    private void openFileChooser() {
        // Only allow PDF, DOC, and DOCX files
        proposalFileLauncher.launch("application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private String getFileExtension(Uri uri) {
        String extension;
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        extension = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
        return extension;
    }

    private void setupDatePicker(final EditText dateField, final String hint) {
        dateField.setHint(hint);

        dateField.setOnClickListener(v -> {
            // Get Current Date
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Create calendar for selected date
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                        // Create calendar for today (with time set to beginning of day)
                        Calendar todayCalendar = Calendar.getInstance();
                        todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                        todayCalendar.set(Calendar.MINUTE, 0);
                        todayCalendar.set(Calendar.SECOND, 0);
                        todayCalendar.set(Calendar.MILLISECOND, 0);

                        // Check if selected date is in the past
                        if (selectedCalendar.before(todayCalendar)) {
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Selected date is invalid. You can't choose a past date.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // For end date, check if it's before start date
                        if (dateField.getId() == R.id.endDate && !startDateField.getText().toString().isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                                Date startDate = sdf.parse(startDateField.getText().toString());
                                Calendar startCalendar = Calendar.getInstance();
                                startCalendar.setTime(startDate);

                                // Reset time portion to compare dates only
                                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                startCalendar.set(Calendar.MINUTE, 0);
                                startCalendar.set(Calendar.SECOND, 0);
                                startCalendar.set(Calendar.MILLISECOND, 0);

                                if (selectedCalendar.before(startCalendar)) {
                                    Toast.makeText(TeacherCreateEventActivity.this,
                                            "End date cannot be before start date. Please select a valid date.",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (ParseException e) {
                                Log.e("DateValidation", "Error parsing start date", e);
                            }
                        }

                        // Format the date
                        SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                        String displayDate = displayFormat.format(selectedCalendar.getTime());

                        SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        String storageDate = storageFormat.format(selectedCalendar.getTime());

                        // Check for date conflicts only if this is the start date (to avoid duplicate checks)
                        if (dateField.getId() == R.id.startDate) {
                            checkDateAvailability(storageDate, selectedCalendar, displayDate, dateField);
                        } else {
                            // For end date, just set it directly
                            setDateToField(dateField, displayDate, storageDate);
                        }
                    }, year, month, day);

            datePickerDialog.show();
        });
    }

    private void setDateToField(EditText dateField, String displayDate, String storageDate) {
        dateField.setText(displayDate);

        if (dateField.getId() == R.id.startDate && radioSingleDayEvent.isChecked()) {
            endDateField.setText(displayDate);
            eventData.put("endDate", storageDate);
        }

        if (dateField.getId() == R.id.startDate) {
            eventData.put("startDate", storageDate);
        } else if (dateField.getId() == R.id.endDate) {
            eventData.put("endDate", storageDate);
        }
    }

    private void checkDateAvailability(String storageDate, Calendar selectedCalendar, String displayDate, EditText dateField) {
        String formattedGrades = String.join(",", selectedYearLevels).replace("All Year Level", "All").replace(" ", "-");

        // Reference to both eventProposals and events nodes
        DatabaseReference proposalsRef = database.getReference("eventProposals");
        DatabaseReference eventsRef = database.getReference("events");

        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        todayCalendar.set(Calendar.MINUTE, 0);
        todayCalendar.set(Calendar.SECOND, 0);
        todayCalendar.set(Calendar.MILLISECOND, 0);

        // Validate that the selected date is not in the past
        if (selectedCalendar.before(todayCalendar)) {
            Toast.makeText(TeacherCreateEventActivity.this,
                    "Selected date is invalid. You can't choose a past date.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check eventProposals for conflicts
        proposalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot proposalsSnapshot) {
                // Check events for conflicts
                eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot eventsSnapshot) {
                        boolean hasConflict = false;
                        String conflictMessage = "Selected date has a scheduling conflict";

                        // Check events node
                        for (DataSnapshot eventSnapshot : eventsSnapshot.getChildren()) {
                            String eventIdFromDB = eventSnapshot.child("eventId").getValue(String.class);
                            // Skip the current event if editing
                            if (isEditing && eventId != null && eventId.equals(eventIdFromDB)) {
                                continue;
                            }
                            // Skip if this is a resubmission of the same event
                            if (eventData.containsKey("originalEventId") &&
                                    eventData.get("originalEventId").equals(eventIdFromDB)) {
                                continue;
                            }

                            String eventFor = eventSnapshot.child("eventFor").getValue(String.class);
                            if (eventFor != null) {
                                String[] eventGrades = eventFor.split(",");
                                for (String eventGrade : eventGrades) {
                                    if (formattedGrades.contains(eventGrade) || eventGrade.equals("All") || formattedGrades.contains("All")) {
                                        String eventSpan = eventSnapshot.child("eventSpan").getValue(String.class);
                                        String existingStartDate = eventSnapshot.child("startDate").getValue(String.class);
                                        String existingEndDate = eventSnapshot.child("endDate").getValue(String.class);

                                        if (existingStartDate != null) {
                                            try {
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                                Date selectedDate = sdf.parse(storageDate);
                                                Date startDate = sdf.parse(existingStartDate);
                                                Date endDate = (existingEndDate != null) ? sdf.parse(existingEndDate) : startDate;

                                                if ("single-day".equals(eventSpan)) {
                                                    if (selectedDate.equals(startDate)) {
                                                        hasConflict = true;
                                                        conflictMessage = "Selected date conflicts with a single-day event on " + existingStartDate;
                                                        break;
                                                    }
                                                } else if ("multi-day".equals(eventSpan)) {
                                                    if (selectedDate.equals(startDate) || (selectedDate.after(startDate) && selectedDate.before(endDate)) || selectedDate.equals(endDate)) {
                                                        hasConflict = true;
                                                        conflictMessage = "Selected date conflicts with a multi-day event (" + existingStartDate + " to " + existingEndDate + ")";
                                                        break;
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Log.e("DateConflict", "Error parsing dates in events node", e);
                                            }
                                        }
                                    }
                                }
                            }
                            if (hasConflict) break;
                        }

                        // Check eventProposals node
                        for (DataSnapshot eventSnapshot : proposalsSnapshot.getChildren()) {
                            String eventIdFromDB = eventSnapshot.child("eventId").getValue(String.class);
                            // Skip the current event if editing
                            if (isEditing && eventId != null && eventId.equals(eventIdFromDB)) {
                                continue;
                            }
                            // Skip if this is a resubmission of the same event
                            if (eventData.containsKey("originalEventId") &&
                                    eventData.get("originalEventId").equals(eventIdFromDB)) {
                                continue;
                            }

                            String eventFor = eventSnapshot.child("eventFor").getValue(String.class);
                            if (eventFor != null) {
                                String[] eventGrades = eventFor.split(",");
                                for (String eventGrade : eventGrades) {
                                    if (formattedGrades.contains(eventGrade) || eventGrade.equals("All") || formattedGrades.contains("All")) {
                                        String eventSpan = eventSnapshot.child("eventSpan").getValue(String.class);

                                        if ("single-day".equals(eventSpan)) {
                                            String existingStartDate = eventSnapshot.child("startDate").getValue(String.class);
                                            if (existingStartDate != null && existingStartDate.equals(storageDate)) {
                                                hasConflict = true;
                                                conflictMessage = "Selected date has a scheduling conflict with a pending proposal on " + existingStartDate;
                                                break;
                                            }
                                        } else if ("multi-day".equals(eventSpan)) {
                                            String existingStartDate = eventSnapshot.child("startDate").getValue(String.class);
                                            String existingEndDate = eventSnapshot.child("endDate").getValue(String.class);

                                            if (existingStartDate != null && existingEndDate != null) {
                                                try {
                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                                    Date rangeStart = sdf.parse(existingStartDate);
                                                    Date rangeEnd = sdf.parse(existingEndDate);
                                                    Date selectedDate = sdf.parse(storageDate);

                                                    if ((selectedDate.equals(rangeStart) || selectedDate.after(rangeStart)) &&
                                                            (selectedDate.equals(rangeEnd) || selectedDate.before(rangeEnd))) {
                                                        hasConflict = true;
                                                        conflictMessage = "Selected date conflicts with a pending multi-day proposal (" +
                                                                existingStartDate + " to " + existingEndDate + ")";
                                                        break;
                                                    }
                                                } catch (Exception e) {
                                                    Log.e("DateConflict", "Error parsing dates in proposals node", e);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (hasConflict) break;
                        }

                        if (hasConflict) {
                            Toast.makeText(TeacherCreateEventActivity.this, conflictMessage, Toast.LENGTH_LONG).show();
                        } else {
                            setDateToField(dateField, displayDate, storageDate);

                            if (isSameDay(selectedCalendar, Calendar.getInstance())) {
                                validateTimeForCurrentDate();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Error checking events availability: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TeacherCreateEventActivity.this,
                        "Error checking proposals availability: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    private void validateTimeForCurrentDate() {
        startTimeField.setText("");
        endTimeField.setText("");

        updateTimePickerForCurrentDate();

        Toast.makeText(TeacherCreateEventActivity.this,
                "You're creating an event for today. Please select a time in the future.",
                Toast.LENGTH_LONG).show();
    }

    private void updateTimePickerForCurrentDate() {
        startTimeField.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        boolean isCurrentDate = false;
                        String startDateStr = startDateField.getText().toString();
                        if (!startDateStr.isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                                Date selectedDate = sdf.parse(startDateStr);
                                Calendar selectedCal = Calendar.getInstance();
                                selectedCal.setTime(selectedDate);

                                Calendar todayCal = Calendar.getInstance();
                                isCurrentDate = isSameDay(selectedCal, todayCal);
                            } catch (Exception e) {
                                Log.e("TimeValidation", "Error parsing date", e);
                            }
                        }

                        if (isCurrentDate) {
                            Calendar selectedTime = Calendar.getInstance();
                            selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                            selectedTime.set(Calendar.MINUTE, selectedMinute);

                            Calendar currentTime = Calendar.getInstance();

                            if (selectedTime.before(currentTime)) {
                                Toast.makeText(TeacherCreateEventActivity.this,
                                        "Don't select a time in the past. Please choose a present time.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        Calendar timeCal = Calendar.getInstance();
                        timeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(Calendar.MINUTE, selectedMinute);
                        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String displayTime = displayTimeFormat.format(timeCal.getTime());

                        SimpleDateFormat storageTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        String storageTime = storageTimeFormat.format(timeCal.getTime());

                        startTimeField.setText(displayTime);
                        eventData.put("startTime", storageTime);

                        if (endTimeField.getText().toString().isEmpty()) {
                            Calendar endTime = Calendar.getInstance();
                            endTime.set(Calendar.HOUR_OF_DAY, selectedHour + 1);
                            endTime.set(Calendar.MINUTE, selectedMinute);

                            String endDisplayTime = displayTimeFormat.format(endTime.getTime());
                            String endStorageTime = storageTimeFormat.format(endTime.getTime());

                            endTimeField.setText(endDisplayTime);
                            eventData.put("endTime", endStorageTime);
                        }
                    }, currentHour, currentMinute, false);

            timePickerDialog.show();
        });

        endTimeField.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Default to 1 hour after start time if start time is set
            if (!startTimeField.getText().toString().trim().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    Date start = sdf.parse(startTimeField.getText().toString());
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(start);
                    startCal.add(Calendar.HOUR_OF_DAY, 1); // Suggest minimum valid time
                    hour = startCal.get(Calendar.HOUR_OF_DAY);
                    minute = startCal.get(Calendar.MINUTE);
                } catch (Exception ignored) {}
            }

            new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
                if (startTimeField.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please select start time first", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    SimpleDateFormat displayFmt = new SimpleDateFormat("hh:mm a", Locale.US);
                    SimpleDateFormat storageFmt = new SimpleDateFormat("HH:mm", Locale.US);

                    Date startDate = displayFmt.parse(startTimeField.getText().toString());

                    Calendar selectedEndCal = Calendar.getInstance();
                    selectedEndCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedEndCal.set(Calendar.MINUTE, selectedMinute);

                    Calendar minEndCal = Calendar.getInstance();
                    minEndCal.setTime(startDate);
                    minEndCal.add(Calendar.HOUR_OF_DAY, 1); // Minimum 1 hour later

                    if (selectedEndCal.before(minEndCal)) {
                        Toast.makeText(this, "End time must be at least 1 hour after start time", Toast.LENGTH_LONG).show();
                        return; // Block selection
                    }

                    String displayTime = displayFmt.format(selectedEndCal.getTime());
                    String storageTime = storageFmt.format(selectedEndCal.getTime());

                    endTimeField.setText(displayTime);
                    eventData.put("endTime", storageTime);

                } catch (ParseException e) {
                    Toast.makeText(this, "Error parsing time", Toast.LENGTH_SHORT).show();
                }
            }, hour, minute, false).show();
        });
    }

    private void setupTimePicker(final EditText timeField, final String hint) {
        timeField.setHint(hint);

        timeField.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        calendar.set(Calendar.MINUTE, selectedMinute);
                        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String displayTime = displayTimeFormat.format(calendar.getTime());

                        SimpleDateFormat storageTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        String storageTime = storageTimeFormat.format(calendar.getTime());

                        timeField.setText(displayTime);

                        if (timeField.getId() == R.id.startTime) {
                            eventData.put("startTime", storageTime);
                        } else if (timeField.getId() == R.id.endTime) {
                            eventData.put("endTime", storageTime);
                        }
                    }, hour, minute, false);

            timePickerDialog.show();
        });
    }

    private void setupGraceTimeSpinner() {
        String[] graceTimeOptions = {"None", "15 min", "30 min", "60 min", "120 min", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                graceTimeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        graceTimeSpinner.setAdapter(adapter);

        graceTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = graceTimeOptions[position];

                if (selectedOption.equals("Custom")) {
                    customGraceTimeLabel.setVisibility(View.VISIBLE);
                    customGraceTimeAsterisk.setVisibility(View.VISIBLE);
                    customGraceTimeField.setVisibility(View.VISIBLE);
                    eventData.put("graceTime", "custom"); // temporary
                } else {
                    customGraceTimeLabel.setVisibility(View.GONE);
                    customGraceTimeAsterisk.setVisibility(View.GONE);
                    customGraceTimeField.setVisibility(View.GONE);

                    String minutes = "none";
                    if (!selectedOption.equals("None")) {
                        minutes = selectedOption.split(" ")[0]; // "60", "120", etc.
                    }

                    // Block 60 min and above if event is exactly 1 hour
                    if (isEventDurationExactlyOneHour() &&
                            ("60".equals(minutes) || "120".equals(minutes))) {

                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Grace time of 60 minutes or more is not allowed for 1-hour events. Please choose below 60 min or use Custom.",
                                Toast.LENGTH_LONG).show();

                        graceTimeSpinner.setSelection(0); // reset to "None"
                        eventData.put("graceTime", "none");
                    } else {
                        eventData.put("graceTime", minutes);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                eventData.put("graceTime", "none");
            }
        });

        // Real-time validation for custom field
        customGraceTimeField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (input.isEmpty()) {
                    customGraceTimeField.setError(null);
                    eventData.put("graceTime", "none");
                    return;
                }

                try {
                    int minutes = Integer.parseInt(input);

                    if (minutes <= 0) {
                        customGraceTimeField.setError("Must be greater than 0");
                        eventData.put("graceTime", "none");
                    }
                    else if (isEventDurationExactlyOneHour() && minutes >= 60) {
                        customGraceTimeField.setError("60 minutes or more not allowed for 1-hour events");
                        // Do NOT save invalid value
                    }
                    else if (minutes > 999) {
                        customGraceTimeField.setError("Maximum 999 minutes");
                        customGraceTimeField.setText("999");
                        customGraceTimeField.setSelection(3);
                    }
                    else {
                        customGraceTimeField.setError(null);
                        eventData.put("graceTime", String.valueOf(minutes));
                    }
                } catch (NumberFormatException e) {
                    customGraceTimeField.setError("Invalid number");
                    eventData.put("graceTime", "none");
                }
            }
        });
    }

    // Add this helper method (replace the old one)
    private boolean isEventDurationExactlyOneHour() {
        String startTimeStr = startTimeField.getText().toString().trim();
        String endTimeStr = endTimeField.getText().toString().trim();

        if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date start = sdf.parse(startTimeStr);
            Date end = sdf.parse(endTimeStr);

            if (start != null && end != null) {
                long diffInMillis = end.getTime() - start.getTime();
                long diffInMinutes = diffInMillis / (1000 * 60);

                // Handle case where end time is next day (e.g., 11:00 PM → 1:00 AM)
                if (diffInMillis < 0) {
                    diffInMillis += 24 * 60 * 60 * 1000; // add 24 hours
                    diffInMinutes = diffInMillis / (1000 * 60);
                }

                return diffInMinutes == 60;
            }
        } catch (Exception e) {
            Log.e("GraceTime", "Error parsing time for 1-hour check", e);
        }
        return false;
    }

    private boolean validatePage2() {
        boolean isValid = true;

        String venue = venueField.getText().toString().trim();
        if (venue.isEmpty()) {
            venueField.setError("Venue is required");
            isValid = false;
        } else {
            eventData.put("venue", venue);
        }

        String startDate = startDateField.getText().toString().trim();
        if (startDate.isEmpty()) {
            startDateField.setError("Start date is required");
            isValid = false;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                Date dateSelected = sdf.parse(startDate);

                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.setTime(dateSelected);

                Calendar todayCalendar = Calendar.getInstance();
                todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                todayCalendar.set(Calendar.MINUTE, 0);
                todayCalendar.set(Calendar.SECOND, 0);
                todayCalendar.set(Calendar.MILLISECOND, 0);

                if (selectedCalendar.before(todayCalendar)) {
                    startDateField.setError("Selected date is invalid");
                    Toast.makeText(this, "Selected date is invalid. You can't choose a past date.", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }

                if (isSameDay(selectedCalendar, Calendar.getInstance())) {
                    if (!validateTimeForCurrentDay()) {
                        isValid = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String endDate = endDateField.getText().toString().trim();
        if (endDate.isEmpty()) {
            endDateField.setError("End date is required");
            isValid = false;
        } else {
            if (radioMultiDayEvent.isChecked()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                    Date startDateObj = sdf.parse(startDate);
                    Date endDateObj = sdf.parse(endDate);

                    if (endDateObj.before(startDateObj)) {
                        endDateField.setError("End date must be after start date");
                        Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                        isValid = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String startTime = startTimeField.getText().toString().trim();
        if (startTime.isEmpty()) {
            startTimeField.setError("Start time is required");
            isValid = false;
        }

        String endTime = endTimeField.getText().toString().trim();
        if (endTime.isEmpty()) {
            endTimeField.setError("End time is required");
            isValid = false;
        }

        // ADD THIS WHOLE BLOCK HERE – ENFORCES 1-HOUR MINIMUM DURATION
        String startTimeStr = startTimeField.getText().toString().trim();
        String endTimeStr = endTimeField.getText().toString().trim();

        if (!startTimeStr.isEmpty() && !endTimeStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                Date startTimeDate = sdf.parse(startTimeStr);
                Date endTimeDate = sdf.parse(endTimeStr);

                Calendar startCal = Calendar.getInstance();
                startCal.setTime(startTimeDate);

                Calendar endCal = Calendar.getInstance();
                endCal.setTime(endTimeDate);

                // Add 1 hour to start time for comparison
                Calendar minimumEndTime = (Calendar) startCal.clone();
                minimumEndTime.add(Calendar.HOUR_OF_DAY, 1);

                if (endCal.before(minimumEndTime)) {
                    endTimeField.setError("End time must be at least 1 hour after start time");
                    Toast.makeText(this, "End time must be at least 1 hour after start time", Toast.LENGTH_LONG).show();
                    isValid = false;
                } else {
                    endTimeField.setError(null); // Clear error if valid
                }
            } catch (ParseException e) {
                Log.e("TimeValidation", "Error parsing time for 1-hour check", e);
                endTimeField.setError("Invalid time format");
                isValid = false;
            }
        }

// Validate custom grace time if visible
        if (customGraceTimeField.getVisibility() == View.VISIBLE) {
            String customGrace = customGraceTimeField.getText().toString().trim();

            if (customGrace.isEmpty()) {
                customGraceTimeField.setError("Please enter grace time in minutes");
                isValid = false;
            } else {
                try {
                    int mins = Integer.parseInt(customGrace);

                    if (mins <= 0) {
                        customGraceTimeField.setError("Must be greater than 0");
                        isValid = false;
                    }
                    // Block 60 minutes AND ANYTHING ABOVE when event is exactly 1 hour
                    else if (isEventDurationExactlyOneHour() && mins >= 60) {
                        customGraceTimeField.setError("Grace time of 60 minutes or more is not allowed for 1-hour events");
                        Toast.makeText(this,
                                "Grace time of 60 minutes or more is not allowed when the event duration is exactly 1 hour.\n" +
                                        "Please choose None, 15 min, 30 min, or a value below 60.",
                                Toast.LENGTH_LONG).show();
                        isValid = false;
                    }
                    else if (mins > 999) {
                        customGraceTimeField.setError("Maximum 999 minutes allowed");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    customGraceTimeField.setError("Please enter a valid number");
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private boolean validateTimeForCurrentDay() {
        String startTime = startTimeField.getText().toString().trim();
        if (startTime.isEmpty()) {
            return true;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date selectedStartTime = sdf.parse(startTime);

            Calendar selectedTimeCal = Calendar.getInstance();
            selectedTimeCal.setTime(selectedStartTime);

            Calendar nowCal = Calendar.getInstance();
            nowCal.set(Calendar.HOUR_OF_DAY, selectedTimeCal.get(Calendar.HOUR_OF_DAY));
            nowCal.set(Calendar.MINUTE, selectedTimeCal.get(Calendar.MINUTE));

            Calendar currentTimeCal = Calendar.getInstance();

            if (nowCal.before(currentTimeCal)) {
                startTimeField.setError("Cannot select a past time");
                Toast.makeText(this,
                        "Cannot select a past time for today's event. Please choose a future time.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            Log.e("TimeValidation", "Error validating time for current day", e);
        }

        return true;
    }

    private boolean validatePage3() {
        boolean isValid = true;

        if (coverPhotoUri == null && !eventData.containsKey("eventPhotoUrl")) {
            Toast.makeText(this, "Please upload a cover photo", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Only validate proposal file if not editing an event in the events node
        if (isProposal && proposalFileUri == null && !eventData.containsKey("eventProposal")) {
            Toast.makeText(this, "Please upload an event proposal (.pdf, .doc, or .docx)", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (proposalFileUri != null) {
            String fileExtension = getFileExtension(proposalFileUri);
            if (fileExtension == null || !(fileExtension.equalsIgnoreCase("pdf") ||
                    fileExtension.equalsIgnoreCase("doc") ||
                    fileExtension.equalsIgnoreCase("docx"))) {
                Toast.makeText(this, "Invalid file type. Please upload a .pdf, .doc, or .docx file", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }

    private void setupEventTypeSpinner() {
        String[] eventTypes = {"Seminar", "Off-Campus Activity", "Sports Event", "Other"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                eventTypes
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        eventTypeSpinner.setAdapter(adapter);

        eventTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedEventType = eventTypes[position];
                if (selectedEventType.equals("Other")) {
                    specifyEventTypeLabel.setVisibility(View.VISIBLE);
                    specifyEventTypeAsterisk.setVisibility(View.VISIBLE);
                    specifyEventTypeField.setVisibility(View.VISIBLE);
                } else {
                    specifyEventTypeLabel.setVisibility(View.GONE);
                    specifyEventTypeAsterisk.setVisibility(View.GONE);
                    specifyEventTypeField.setVisibility(View.GONE);
                    eventData.put("eventType", selectedEventType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupEventForSpinner() {
        String[] gradeOptions = {"--Select Grade Level--", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "All Year Level"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                gradeOptions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        eventForSpinner.setAdapter(adapter);

        eventForSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGrade = gradeOptions[position];
                if (!selectedGrade.equals("--Select Grade Level--")) {
                    if (selectedGrade.equals("All Year Level")) {
                        // Clear all other selections and add only "All Year Level"
                        selectedYearLevels.clear();
                        selectedYearLevels.add(selectedGrade);
                    } else {
                        // Remove "All Year Level" if present and add the selected grade
                        selectedYearLevels.remove("All Year Level");
                        if (!selectedYearLevels.contains(selectedGrade)) {
                            selectedYearLevels.add(selectedGrade);
                        }
                        // Check if all grades (7-12) are selected
                        String[] allGrades = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};
                        boolean hasAllGrades = true;
                        for (String grade : allGrades) {
                            if (!selectedYearLevels.contains(grade)) {
                                hasAllGrades = false;
                                break;
                            }
                        }
                        if (hasAllGrades) {
                            selectedYearLevels.clear();
                            selectedYearLevels.add("All Year Level");
                        }
                    }
                    updateSelectedYearLevelsUI();

                    // Clear start date if not in edit mode
                    if (!isEditing) {
                        String currentStartDate = startDateField.getText().toString().trim();
                        if (!currentStartDate.isEmpty()) {
                            startDateField.setText("");
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Please reselect the event date to check availability for this grade level",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                // Reset spinner to prevent immediate re-selection
                eventForSpinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateSelectedYearLevelsUI() {
        selectedYearLevelsContainer.removeAllViews();
        if (!selectedYearLevels.isEmpty()) {
            selectedYearLevelsContainer.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (String yearLevel : selectedYearLevels) {
                View yearLevelView = inflater.inflate(R.layout.item_selected_year_level, selectedYearLevelsContainer, false);
                TextView yearLevelText = yearLevelView.findViewById(R.id.yearLevelText);
                yearLevelText.setText(yearLevel);

                View removeButton = yearLevelView.findViewById(R.id.removeYearLevelButton);
                removeButton.setOnClickListener(v -> {
                    selectedYearLevels.remove(yearLevel);
                    updateSelectedYearLevelsUI();
                    if (!isEditing && !startDateField.getText().toString().trim().isEmpty()) {
                        startDateField.setText("");
                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Please reselect the event date to check availability for the updated grade levels",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                selectedYearLevelsContainer.addView(yearLevelView);
            }
        } else {
            selectedYearLevelsContainer.setVisibility(View.GONE);
        }
    }

    private void setupPageNavigation() {
        nextPg1.setOnClickListener(v -> {
            if (validatePage1()) {
                savePageOneData();
                showPage(2);
            }
        });

        backPg2.setOnClickListener(v -> showPage(1));

        nextPg2.setOnClickListener(v -> {
            if (validatePage2()) {
                showPage(3);
            }
        });

        backPg3.setOnClickListener(v -> showPage(2));

        createButton.setOnClickListener(v -> {
            if (validatePage3()) {
                uploadFilesAndSaveEvent();
            }
        });
    }

    private boolean validatePage1() {
        boolean isValid = true;

        String eventName = eventNameField.getText().toString().trim();
        if (eventName.isEmpty()) {
            eventNameField.setError("Event name is required");
            isValid = false;
        }

        String eventDescription = eventDescriptionField.getText().toString().trim();
        if (eventDescription.isEmpty()) {
            eventDescriptionField.setError("Event description is required");
            isValid = false;
        }

        String selectedEventType = eventTypeSpinner.getSelectedItem().toString();
        if (selectedEventType.equals("Other")) {
            String specifiedType = specifyEventTypeField.getText().toString().trim();
            if (specifiedType.isEmpty()) {
                specifyEventTypeField.setError("Please specify the event type");
                isValid = false;
            }
        }

        if (selectedYearLevels.isEmpty()) {
            Toast.makeText(this, "Please select at least one year level", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            String formattedGrades = String.join(",", selectedYearLevels).replace("All Year Level", "All").replace(" ", "-");
            eventData.put("eventFor", formattedGrades);
        }

        String selectedTargetParticipants = targetParticipantsSpinner.getSelectedItem().toString();
        if (selectedTargetParticipants.equals("Custom")) {
            String customNumber = customParticipantsField.getText().toString().trim();
            if (customNumber.isEmpty()) {
                customParticipantsField.setError("Please enter the number of participants");
                isValid = false;
            } else {
                try {
                    int number = Integer.parseInt(customNumber);
                    if (number <= 0) {
                        customParticipantsField.setError("Number must be greater than 0");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    customParticipantsField.setError("Please enter a valid number");
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private void savePageOneData() {
        eventData.put("eventName", eventNameField.getText().toString().trim());
        eventData.put("eventDescription", eventDescriptionField.getText().toString().trim());

        String selectedEventType = eventTypeSpinner.getSelectedItem().toString();
        if (selectedEventType.equals("Other")) {
            String specifiedType = specifyEventTypeField.getText().toString().trim();
            eventData.put("eventType", specifiedType);
        } else {
            eventData.put("eventType", selectedEventType);
        }

        // Format grades as "Grade-7,Grade-8" for eventFor
        String formattedGrades = String.join(",", selectedYearLevels)
                .replace("All Year Level", "All")
                .replace(" ", "-");
        eventData.put("eventFor", formattedGrades);

        String selectedTargetParticipants = targetParticipantsSpinner.getSelectedItem().toString();
        if (selectedTargetParticipants.equals("Custom")) {
            String customNumber = customParticipantsField.getText().toString().trim();
            eventData.put("targetParticipant", customNumber);
        } else {
            eventData.put("targetParticipant", selectedTargetParticipants.toLowerCase());
        }

        eventData.put("createdAt", System.currentTimeMillis());
        eventData.put("scanPermission", false); // Add scanPermission field

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            findTeacherIdByEmail(userEmail);
        } else {
            Toast.makeText(this, "You must be logged in to create an event", Toast.LENGTH_SHORT).show();
        }
    }

    private void findTeacherIdByEmail(String email) {
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("teachers");

        teachersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                        String teacherId = teacherSnapshot.getKey();
                        eventData.put("createdBy", teacherId);
                        break;
                    }
                } else {
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Could not find your teacher profile. Please contact support.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TeacherCreateEventActivity.this,
                        "Error finding teacher profile: " + databaseError.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadFilesAndSaveEvent() {
        // Initialize ProgressBar
        ProgressBar createProgressBar = findViewById(R.id.createProgressBar);

        // Show ProgressBar and disable button
        createProgressBar.setVisibility(View.VISIBLE);
        createButton.setVisibility(View.GONE);
        createButton.setEnabled(false);
        Toast.makeText(this, isEditing ? "Updating event..." : "Uploading files and creating event...", Toast.LENGTH_SHORT).show();

        if (!eventData.containsKey("createdBy")) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("teachers");
                teachersRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                                String teacherId = teacherSnapshot.getKey();
                                eventData.put("createdBy", teacherId);
                                proceedWithUpload();
                            }
                        } else {
                            createProgressBar.setVisibility(View.GONE);
                            createButton.setVisibility(View.VISIBLE);
                            createButton.setEnabled(true);
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Could not find your teacher profile. Please contact support.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        createProgressBar.setVisibility(View.GONE);
                        createButton.setVisibility(View.VISIBLE);
                        createButton.setEnabled(true);
                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Error finding teacher profile: " + databaseError.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                createProgressBar.setVisibility(View.GONE);
                createButton.setVisibility(View.VISIBLE);
                createButton.setEnabled(true);
                Toast.makeText(this, "You must be logged in to create an event", Toast.LENGTH_SHORT).show();
            }
        } else {
            proceedWithUpload();
        }
    }

    private void proceedWithUpload() {
        if (isEditing) {
            // Update existing event
            updateEvent();
        } else {
            // Handle create or resubmission
            boolean isResubmission = eventData.containsKey("originalEventId");
            String eventId;

            if (isResubmission) {
                eventId = eventProposalRef.push().getKey();
                if (eventId == null) {
                    createButton.setEnabled(true);
                    Toast.makeText(this, "Error creating event", Toast.LENGTH_SHORT).show();
                    return;
                }
                eventData.put("resubmissionOf", eventData.get("originalEventId"));
                eventData.remove("originalEventId");
            } else {
                eventId = eventProposalRef.push().getKey();
                if (eventId == null) {
                    createButton.setEnabled(true);
                    Toast.makeText(this, "Error creating event", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            eventData.put("eventId", eventId);
            eventData.put("registrationAllowed", false);
            eventData.put("status", "pending");

            long currentTimeMillis = System.currentTimeMillis();
            eventData.put("timestamp", currentTimeMillis);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            String currentDate = dateFormat.format(new Date(currentTimeMillis));
            eventData.put("dateCreated", currentDate);

            if (coverPhotoUri != null) {
                String eventName = eventData.containsKey("eventName") ?
                        eventData.get("eventName").toString() :
                        "event_" + eventId;
                uploadCoverPhoto(eventId, eventName);
            } else if (isResubmission) {
                uploadEventProposal(eventId);
            } else {
                createButton.setEnabled(true);
                Toast.makeText(this, "Please upload a cover photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateEvent() {
        // Update timestamps
        long currentTimeMillis = System.currentTimeMillis();
        eventData.put("timestamp", currentTimeMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        String currentDate = dateFormat.format(new Date(currentTimeMillis));
        eventData.put("dateCreated", currentDate);

        // Ensure date fields are in storage format (yyyy-MM-dd)
        String startDateDisplay = startDateField.getText().toString().trim();
        String endDateDisplay = endDateField.getText().toString().trim();
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            if (!startDateDisplay.isEmpty()) {
                Date startDate = displayFormat.parse(startDateDisplay);
                eventData.put("startDate", storageFormat.format(startDate));
            }
            if (!endDateDisplay.isEmpty()) {
                Date endDate = displayFormat.parse(endDateDisplay);
                eventData.put("endDate", storageFormat.format(endDate));
            }
        } catch (ParseException e) {
            Log.e("DateParsing", "Error parsing dates: " + e.getMessage());
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            createButton.setEnabled(true);
            return;
        }

        // Check if a new cover photo is selected
        if (coverPhotoUri != null) {
            String eventName = eventData.containsKey("eventName") ?
                    eventData.get("eventName").toString() : "event_" + eventId;
            String originalFileName = getOriginalFileName(coverPhotoUri);
            if (originalFileName == null || originalFileName.isEmpty()) {
                originalFileName = "photo." + getFileExtension(coverPhotoUri);
            }
            StorageReference coverPhotoRef = storageRef.child(eventName + "/" + originalFileName);

            // Upload new cover photo
            String finalOriginalFileName = originalFileName;
            coverPhotoRef.putFile(coverPhotoUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        coverPhotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            eventData.put("eventPhotoUrl", uri.toString());
                            eventData.put("eventPhotoPath", eventName + "/" + finalOriginalFileName);
                            // Proceed with proposal handling after photo upload
                            handleProposalUpdate();
                        });
                    })
                    .addOnFailureListener(e -> {
                        createButton.setEnabled(true);
                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Failed to upload cover photo: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No new cover photo selected, proceed with proposal handling
            handleProposalUpdate();
        }
    }

    private void handleProposalUpdate() {
        if (isProposal && proposalFileUri != null) {
            String eventName = eventData.containsKey("eventName") ?
                    eventData.get("eventName").toString() : "event_" + eventId;
            StorageReference proposalRef = storageRef.child(eventName + "/event_proposal/" + proposalFileName);

            proposalRef.putFile(proposalFileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        proposalRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            eventData.put("eventProposal", uri.toString());
                            // Proceed with updating the event after proposal upload
                            proceedWithEventUpdate();
                        });
                    })
                    .addOnFailureListener(e -> {
                        createButton.setEnabled(true);
                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Failed to upload event proposal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No new proposal file or not a proposal, proceed with updating the event
            proceedWithEventUpdate();
        }
    }

    private void proceedWithEventUpdate() {
        ProgressBar createProgressBar = findViewById(R.id.createProgressBar);
        // Check if the event exists in eventProposals or events
        DatabaseReference eventProposalsRef = database.getReference("eventProposals").child(eventId);
        eventProposalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Event is a proposal, update in eventProposals node
                    eventData.put("status", "pending"); // Ensure it remains pending
                    eventProposalsRef.updateChildren(eventData, (databaseError, databaseReference) -> {
                        createProgressBar.setVisibility(View.GONE);
                        createButton.setVisibility(View.VISIBLE);
                        createButton.setEnabled(true);
                        if (databaseError == null) {
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Event proposal updated successfully",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(TeacherCreateEventActivity.this, TeacherEvents.class));
                            finish();
                        } else {
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Failed to update event proposal: " + databaseError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Event is not in eventProposals, assume it's in events (approved event)
                    deleteStudentTickets(eventId);
                    DatabaseReference eventRef = database.getReference("events").child(eventId);
                    eventRef.updateChildren(eventData, (databaseError, databaseReference) -> {
                        createProgressBar.setVisibility(View.GONE);
                        createButton.setVisibility(View.VISIBLE);
                        createButton.setEnabled(true);
                        if (databaseError == null) {
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Event updated successfully",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(TeacherCreateEventActivity.this, TeacherEvents.class));
                            finish();
                        } else {
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Failed to update event: " + databaseError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                createProgressBar.setVisibility(View.GONE);
                createButton.setVisibility(View.VISIBLE);
                createButton.setEnabled(true);
                Toast.makeText(TeacherCreateEventActivity.this,
                        "Error checking event status: " + databaseError.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteStudentTickets(String eventId) {
        DatabaseReference studentsRef = database.getReference("students");
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot studentSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot ticketsSnapshot = studentSnapshot.child("tickets");
                    if (ticketsSnapshot.exists()) {
                        for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                            String ticketId = ticketSnapshot.getKey();
                            if (ticketId != null && ticketId.equals(eventId)) {
                                ticketSnapshot.getRef().removeValue()
                                        .addOnSuccessListener(aVoid -> Log.d("TicketDeletion", "Ticket deleted for event: " + eventId))
                                        .addOnFailureListener(e -> Log.e("TicketDeletion", "Failed to delete ticket: " + e.getMessage()));
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TicketDeletion", "Error deleting tickets: " + databaseError.getMessage());
            }
        });
    }

    private void uploadCoverPhoto(String eventId, String eventName) {
        String originalFileName = getOriginalFileName(coverPhotoUri);
        if (originalFileName == null || originalFileName.isEmpty()) {
            originalFileName = "photo." + getFileExtension(coverPhotoUri);
        }

        StorageReference coverPhotoRef = storageRef.child(eventName + "/" + originalFileName);

        String finalOriginalFileName = originalFileName;
        coverPhotoRef.putFile(coverPhotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    coverPhotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        eventData.put("eventPhotoUrl", uri.toString());
                        eventData.put("eventPhotoPath", eventName + "/" + finalOriginalFileName);
                        uploadEventProposal(eventId);
                    });
                })
                .addOnFailureListener(e -> {
                    createButton.setEnabled(true);
                    Toast.makeText(TeacherCreateEventActivity.this, "Failed to upload cover photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getOriginalFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("FileNameError", "Error getting original filename", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadEventProposal(String eventId) {
        ProgressBar createProgressBar = findViewById(R.id.createProgressBar);
        if (proposalFileUri != null) {
            String eventName = eventData.get("eventName").toString();
            StorageReference proposalRef = storageRef.child(eventName + "/event_proposal/" + proposalFileName);

            proposalRef.putFile(proposalFileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        proposalRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            eventData.put("eventProposal", uri.toString());
                            saveEventToFirebase(eventId);
                        });
                    })
                    .addOnFailureListener(e -> {
                        createProgressBar.setVisibility(View.GONE);
                        createButton.setVisibility(View.VISIBLE);
                        createButton.setEnabled(true);
                        Toast.makeText(TeacherCreateEventActivity.this, "Failed to upload event proposal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else if (eventData.containsKey("eventProposal") || eventData.containsKey("existingProposalUrl")) {
            if (eventData.containsKey("existingProposalUrl")) {
                eventData.put("eventProposal", eventData.get("existingProposalUrl"));
                eventData.remove("existingProposalUrl");
            }
            saveEventToFirebase(eventId);
        } else {
            createProgressBar.setVisibility(View.GONE);
            createButton.setVisibility(View.VISIBLE);
            createButton.setEnabled(true);
            Toast.makeText(this, "Please upload an event proposal", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEventToFirebase(String eventId) {
        ProgressBar createProgressBar = findViewById(R.id.createProgressBar);
        boolean isResubmission = eventData.containsKey("resubmissionOf");
        final String originalEventId = isResubmission ? eventData.get("resubmissionOf").toString() : null;

        if (isResubmission) {
            eventData.put("isResubmission", true);
        }

        // Ensure eventFor is formatted correctly
        String formattedGrades = String.join(",", selectedYearLevels)
                .replace("All Year Level", "All")
                .replace(" ", "-");
        eventData.put("eventFor", formattedGrades);
        eventData.put("scanPermission", false); // Ensure scanPermission is included

        // Ensure date fields are in storage format (yyyy-MM-dd)
        String startDateDisplay = startDateField.getText().toString().trim();
        String endDateDisplay = endDateField.getText().toString().trim();
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            if (!startDateDisplay.isEmpty()) {
                Date startDate = displayFormat.parse(startDateDisplay);
                eventData.put("startDate", storageFormat.format(startDate));
            }
            if (!endDateDisplay.isEmpty()) {
                Date endDate = displayFormat.parse(endDateDisplay);
                eventData.put("endDate", storageFormat.format(endDate));
            }
        } catch (ParseException e) {
            Log.e("DateParsing", "Error parsing dates: " + e.getMessage());
            createProgressBar.setVisibility(View.GONE);
            createButton.setVisibility(View.VISIBLE);
            createButton.setEnabled(true);
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditing) {
            // Update existing event
            updateEvent();
        } else {
            // Save new event or resubmission
            eventProposalRef.child(eventId).setValue(eventData, (databaseError, databaseReference) -> {
                createProgressBar.setVisibility(View.GONE);
                createButton.setVisibility(View.VISIBLE);
                createButton.setEnabled(true);
                if (databaseError == null) {
                    String message = isResubmission ? "Event resubmitted successfully" : "Event created successfully";
                    Toast.makeText(TeacherCreateEventActivity.this, message, Toast.LENGTH_SHORT).show();

                    if (isResubmission && originalEventId != null) {
                        deleteRejectedEvent(originalEventId);
                    }

                    startActivity(new Intent(TeacherCreateEventActivity.this, TeacherEvents.class));
                    finish();
                } else {
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Failed to " + (isResubmission ? "resubmit" : "create") + " event: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void deleteRejectedEvent(String originalEventId) {
        if (originalEventId == null || originalEventId.isEmpty()) {
            return;
        }

        DatabaseReference rejectedEventRef = database.getReference("eventProposals").child(originalEventId);
        rejectedEventRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("DeleteEvent", "Rejected event deleted successfully");
            } else {
                Log.e("DeleteEvent", "Failed to delete rejected event", task.getException());
            }
        });
    }

    private void showPage(int page) {
        currentPage = page;
        page1.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        page2.setVisibility(page == 2 ? View.VISIBLE : View.GONE);
        page3.setVisibility(page == 3 ? View.VISIBLE : View.GONE);
        updateProgressIndicator(page);
    }

    private void updateProgressIndicator(int page) {
        TextView step1 = findViewById(R.id.stepBasicInfo);
        TextView step2 = findViewById(R.id.stepSchedule);
        TextView step3 = findViewById(R.id.stepMedia);

        View connector1 = findViewById(R.id.connector1);
        View connector2 = findViewById(R.id.connector2);

        step1.setBackgroundResource(R.drawable.progress_bar_background);
        step2.setBackgroundResource(R.drawable.progress_bar_background);
        step3.setBackgroundResource(R.drawable.progress_bar_background);

        step1.setTextColor(getResources().getColor(R.color.gray));
        step2.setTextColor(getResources().getColor(R.color.gray));
        step3.setTextColor(getResources().getColor(R.color.gray));

        connector1.setBackgroundColor(getResources().getColor(R.color.light_gray));
        connector2.setBackgroundColor(getResources().getColor(R.color.light_gray));

        if (page >= 1) {
            step1.setBackgroundResource(R.drawable.progress_bar_active);
            step1.setTextColor(getResources().getColor(android.R.color.white));
        }
        if (page >= 2) {
            step2.setBackgroundResource(R.drawable.progress_bar_active);
            step2.setTextColor(getResources().getColor(android.R.color.white));
            connector1.setBackgroundColor(getResources().getColor(R.color.primary));
        }
        if (page >= 3) {
            step3.setBackgroundResource(R.drawable.progress_bar_active);
            step3.setTextColor(getResources().getColor(android.R.color.white));
            connector2.setBackgroundColor(getResources().getColor(R.color.primary));
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);
        bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

        View fabScan = findViewById(R.id.fab_create);
        if (fabScan != null) {
            fabScan.setSelected(true);
        }

        bottomNavigationView.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_UNLABELED);
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home_teacher) {
                startActivity(new Intent(this, TeacherDashboard.class));
            } else if (itemId == R.id.nav_event_teacher) {
                startActivity(new Intent(this, TeacherEvents.class));
            } else if (itemId == R.id.nav_scan_teacher) {
                startActivity(new Intent(this, TeacherScanning.class));
            } else if (itemId == R.id.nav_profile_teacher) {
                startActivity(new Intent(this, TeacherProfile.class));
            }
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
}