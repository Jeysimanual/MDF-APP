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
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.storage.UploadTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_create_event);

        // Initialize Firebase
        initializeFirebase();

        // Initialize views
        page1 = findViewById(R.id.page1);
        page2 = findViewById(R.id.page2);
        page3 = findViewById(R.id.page3);

        nextPg1 = findViewById(R.id.nextButton1);
        nextPg2 = findViewById(R.id.nextPg3);
        backPg2 = findViewById(R.id.backPg1);
        backPg3 = findViewById(R.id.backPg2);
        createButton = findViewById(R.id.createButton);

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Initialize page 1 form fields
        initializePage1Fields();
        // Initialize page 2 form fields
        initializePage2Fields();
        // Initialize page 3 form fields
        initializePage3Fields();
        // Check if this is a resubmission
        checkForResubmission();


        setupPageNavigation();
        setupBottomNavigation();
        showPage(1); // Show first page initially
    }
    private void checkForResubmission() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("IS_RESUBMISSION", false)) {
            // This is a resubmission, populate the fields with the passed data
            populateFieldsForResubmission(intent);
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

        // Store the eventId for later use when saving
        eventData.put("originalEventId", eventId);

        // Populate Page 1 fields
        populatePage1Fields(eventName, eventDescription, eventType, eventFor);

        // Populate Page 2 fields
        populatePage2Fields(venue, startDate, endDate, startTime, endTime, eventSpan, graceTime);

        // For Page 3, if there's a proposal URL, we need to handle it differently
        if (proposalUrl != null && !proposalUrl.isEmpty()) {
            eventData.put("existingProposalUrl", proposalUrl);
        }

        // Adjust the create button text to indicate resubmission
        createButton.setText("Resubmit Event");
    }

    private void populatePage1Fields(String eventName, String eventDescription, String eventType, String eventFor) {
        // Set event name and description
        if (eventName != null) {
            eventNameField.setText(eventName);
            eventData.put("eventName", eventName);
        }

        if (eventDescription != null) {
            eventDescriptionField.setText(eventDescription);
            eventData.put("eventDescription", eventDescription);
        }

        // Set event type spinner
        if (eventType != null) {
            String[] eventTypes = {"Seminar", "Off-Campus Activity", "Sports Event", "Other"};
            boolean foundMatchingType = false;

            for (int i = 0; i < eventTypes.length; i++) {
                if (eventTypes[i].equals(eventType)) {
                    eventTypeSpinner.setSelection(i);
                    eventData.put("eventType", eventType);
                    foundMatchingType = true;
                    break;
                }
            }

            // If no matching type is found, select "Other" and set the specified type
            if (!foundMatchingType) {
                eventTypeSpinner.setSelection(3); // "Other" is at index 3
                specifyEventTypeField.setText(eventType);
                specifyEventTypeLabel.setVisibility(View.VISIBLE);
                specifyEventTypeAsterisk.setVisibility(View.VISIBLE);
                specifyEventTypeField.setVisibility(View.VISIBLE);
                eventData.put("eventType", eventType);
            }
        }

        // Set event for spinner
        if (eventFor != null) {
            String[] gradeOptions = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "All Year Level"};

            // Convert format back from "Grade-7" to "Grade 7" if needed
            String formattedEventFor = eventFor.replace("-", " ");

            // Special case for "All"
            if (eventFor.equals("All")) {
                formattedEventFor = "All Year Level";
            }

            for (int i = 0; i < gradeOptions.length; i++) {
                if (gradeOptions[i].equals(formattedEventFor)) {
                    eventForSpinner.setSelection(i);
                    break;
                }
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

        // Convert date format from display format (MM-dd-yyyy) to storage format (yyyy-MM-dd)
        // and vice versa as needed
        if (startDate != null) {
            try {
                // Check if the date is already in the display format
                if (startDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    startDateField.setText(startDate);

                    // Convert to storage format for eventData
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                    SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date date = displayFormat.parse(startDate);
                    String storageDate = storageFormat.format(date);
                    eventData.put("startDate", storageDate);
                } else {
                    // Assuming it's in the storage format
                    SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                    Date date = storageFormat.parse(startDate);
                    String displayDate = displayFormat.format(date);
                    startDateField.setText(displayDate);
                    eventData.put("startDate", startDate);
                }
            } catch (Exception e) {
                // Handle parsing error
                startDateField.setText(startDate);
                eventData.put("startDate", startDate);
            }
        }

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

        // Set grace time spinner
        if (graceTime != null) {
            String[] graceTimeOptions = {"None", "15 min", "30 min", "60 min", "120 min"};

            // Find the appropriate spinner option
            int selectionIndex = 0;
            if (graceTime.equals("none")) {
                selectionIndex = 0;
            } else {
                for (int i = 1; i < graceTimeOptions.length; i++) {
                    if (graceTimeOptions[i].startsWith(graceTime)) {
                        selectionIndex = i;
                        break;
                    }
                }
            }

            graceTimeSpinner.setSelection(selectionIndex);
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
                            uploadCoverPhoto.setImageResource(0); // Clear existing image
                            Glide.with(this)
                                    .load(coverPhotoUri)
                                    .centerCrop()
                                    .into(uploadCoverPhoto);
                        }
                    }
                });

        // Launcher for proposal file selection
        proposalFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        proposalFileUri = uri;

                        // Get file name from URI
                        String fileName = getFileNameFromUri(uri);
                        proposalFileName = fileName;

                        // Display the file name
                        fileNameLabel.setText(fileName);
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
        setupEventForSpinner();
    }

    // In the initializePage2Fields() method, change the eventType to eventSpan
    private void initializePage2Fields() {
        // Initialize Venue field
        venueField = findViewById(R.id.venue);

        // Initialize Event Type Radio Buttons
        radioSingleDayEvent = findViewById(R.id.radioSingleDayEvent);
        radioMultiDayEvent = findViewById(R.id.radioMultiDayEvent);

        // Set default selection
        radioSingleDayEvent.setChecked(true);
        eventData.put("eventSpan", "single-day");  // Changed from eventType to eventSpan

        // Add listeners to radio buttons
        radioSingleDayEvent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                eventData.put("eventSpan", "single-day");  // Changed from eventType to eventSpan
                // If single day is selected, make the end date match the start date
                if (startDateField.getText().toString().length() > 0) {
                    endDateField.setText(startDateField.getText().toString());
                }
            }
        });

        radioMultiDayEvent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                eventData.put("eventSpan", "multi-day");  // Changed from eventType to eventSpan
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

        // Initialize and setup Grace Time Spinner
        graceTimeSpinner = findViewById(R.id.graceTimeSpinner);
        setupGraceTimeSpinner();
    }

    private void initializePage3Fields() {
        // Initialize views for Page 3
        uploadCoverPhotoCard = findViewById(R.id.uploadCoverPhotoCard);
        uploadCoverPhoto = findViewById(R.id.uploadCoverPhoto);
        chooseFileButton = findViewById(R.id.chooseFileButton);
        fileNameLabel = findViewById(R.id.fileNameLabel);

        // Set up click listener for cover photo upload
        uploadCoverPhotoCard.setOnClickListener(v -> openGallery());

        // Set up click listener for file chooser
        chooseFileButton.setOnClickListener(v -> openFileChooser());
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};
        galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        coverPhotoLauncher.launch(galleryIntent);
    }

    private void openFileChooser() {
        // Only allow PDF and DOC files
        proposalFileLauncher.launch("application/*");
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
                        // Format for display (keep the original format for UI)
                        SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                        String displayDate = displayFormat.format(selectedCalendar.getTime());

                        // Format for storage (use yyyy-MM-dd format)
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
        // Set the selected date to the field (for display purposes)
        dateField.setText(displayDate);

        // If this is the start date and single day event is selected,
        // set the end date to the same value
        if (dateField.getId() == R.id.startDate && radioSingleDayEvent.isChecked()) {
            endDateField.setText(displayDate);
            eventData.put("endDate", storageDate);
        }

        // Store date information in the required format
        if (dateField.getId() == R.id.startDate) {
            eventData.put("startDate", storageDate);
        } else if (dateField.getId() == R.id.endDate) {
            eventData.put("endDate", storageDate);
        }
    }
    private void checkDateAvailability(String storageDate, Calendar selectedCalendar, String displayDate, EditText dateField) {
        // Get the selected grade level from the spinner
        String selectedGrade = eventForSpinner.getSelectedItem().toString();
        String formattedGrade = selectedGrade.equals("All Year Level") ? "All" : selectedGrade.replace(" ", "-");

        DatabaseReference eventsRef = database.getReference("eventProposals");

        // First check if this is a valid date (not in the past)
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        todayCalendar.set(Calendar.MINUTE, 0);
        todayCalendar.set(Calendar.SECOND, 0);
        todayCalendar.set(Calendar.MILLISECOND, 0);

        if (selectedCalendar.before(todayCalendar)) {
            Toast.makeText(TeacherCreateEventActivity.this,
                    "Selected date is invalid. You can't choose a past date.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for multi-day event conflicts
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasConflict = false;
                String conflictMessage = "Selected date has a scheduling conflict";

                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    // Skip if this is a resubmission of the same event
                    String eventId = eventSnapshot.child("eventId").getValue(String.class);
                    if (eventData.containsKey("originalEventId") &&
                            eventData.get("originalEventId").equals(eventId)) {
                        continue;
                    }

                    // Get the event's grade level
                    String eventFor = eventSnapshot.child("eventFor").getValue(String.class);

                    // If there's no grade level match (including "All"), skip this event
                    if (!("All".equals(formattedGrade) || "All".equals(eventFor) || formattedGrade.equals(eventFor))) {
                        continue;
                    }

                    // Get event type (single or multi-day)
                    String eventSpan = eventSnapshot.child("eventSpan").getValue(String.class);

                    // For single-day events, just check the start date
                    if ("single-day".equals(eventSpan)) {
                        String existingStartDate = eventSnapshot.child("startDate").getValue(String.class);
                        if (existingStartDate != null && existingStartDate.equals(storageDate)) {
                            hasConflict = true;
                            break;
                        }
                    }
                    // For multi-day events, check if the selected date falls within the event's range
                    else if ("multi-day".equals(eventSpan)) {
                        String existingStartDate = eventSnapshot.child("startDate").getValue(String.class);
                        String existingEndDate = eventSnapshot.child("endDate").getValue(String.class);

                        if (existingStartDate != null && existingEndDate != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                Date rangeStart = sdf.parse(existingStartDate);
                                Date rangeEnd = sdf.parse(existingEndDate);
                                Date selectedDate = sdf.parse(storageDate);

                                // Check if the selected date falls within the multi-day event range
                                if ((selectedDate.equals(rangeStart) || selectedDate.after(rangeStart)) &&
                                        (selectedDate.equals(rangeEnd) || selectedDate.before(rangeEnd))) {
                                    hasConflict = true;
                                    conflictMessage = "Selected date conflicts with an existing multi-day event (" +
                                            existingStartDate + " to " + existingEndDate + ")";
                                    break;
                                }
                            } catch (Exception e) {
                                Log.e("DateConflict", "Error parsing dates", e);
                            }
                        }
                    }
                }

                if (hasConflict) {
                    // Show error message for conflict
                    Toast.makeText(TeacherCreateEventActivity.this, conflictMessage, Toast.LENGTH_LONG).show();
                } else {
                    // No conflict, set the date
                    setDateToField(dateField, displayDate, storageDate);

                    // If this is for the current date, validate the time fields
                    if (isSameDay(selectedCalendar, Calendar.getInstance())) {
                        validateTimeForCurrentDate();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TeacherCreateEventActivity.this,
                        "Error checking date availability: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper function to check if two Calendar instances represent the same day
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    private void validateTimeForCurrentDate() {
        // Clear any existing times to force validation
        startTimeField.setText("");
        endTimeField.setText("");

        // Update time picker behavior for current date
        updateTimePickerForCurrentDate();

        // Show a toast to inform the user
        Toast.makeText(TeacherCreateEventActivity.this,
                "You're creating an event for today. Please select a time in the future.",
                Toast.LENGTH_LONG).show();
    }
    // Update the time picker to enforce future time selection for current date
    private void updateTimePickerForCurrentDate() {
        // Update the existing time picker setup
        startTimeField.setOnClickListener(v -> {
            // Get Current Time
            final Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

            // Time Picker Dialog with validation for current date
            TimePickerDialog timePickerDialog = new TimePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        // Check if this event is for the current date
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

                        // For current date events, validate that time is in the future
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

                        // Format the time for display (12-hour format with AM/PM)
                        Calendar timeCal = Calendar.getInstance();
                        timeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(Calendar.MINUTE, selectedMinute);
                        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String displayTime = displayTimeFormat.format(timeCal.getTime());

                        // Format the time for storage (24-hour format without AM/PM)
                        SimpleDateFormat storageTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        String storageTime = storageTimeFormat.format(timeCal.getTime());

                        // Set the selected time to the field (for display)
                        startTimeField.setText(displayTime);

                        // Store time information
                        eventData.put("startTime", storageTime);

                        // If end time is empty or before start time, update it
                        if (endTimeField.getText().toString().isEmpty()) {
                            // Default end time to start time + 1 hour
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

        // Also update end time picker
        endTimeField.setOnClickListener(v -> {
            // Get Current Time or Start Time if set
            final Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

            // If start time is set, use it as minimum
            String startTimeStr = startTimeField.getText().toString();
            if (!startTimeStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    Date startTime = sdf.parse(startTimeStr);
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startTime);

                    currentHour = startCal.get(Calendar.HOUR_OF_DAY);
                    currentMinute = startCal.get(Calendar.MINUTE);
                } catch (Exception e) {
                    Log.e("TimeValidation", "Error parsing start time", e);
                }
            }

            // Time Picker Dialog with validation
            TimePickerDialog timePickerDialog = new TimePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        // Check if selected end time is after start time
                        String startTimeText = startTimeField.getText().toString();
                        if (!startTimeText.isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);

                                Date startTime = sdf.parse(startTimeText);

                                Calendar selectedEndTime = Calendar.getInstance();
                                selectedEndTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                                selectedEndTime.set(Calendar.MINUTE, selectedMinute);

                                Calendar startTimeCal = Calendar.getInstance();
                                startTimeCal.setTime(startTime);

                                if (selectedEndTime.before(startTimeCal) ||
                                        (selectedEndTime.get(Calendar.HOUR_OF_DAY) == startTimeCal.get(Calendar.HOUR_OF_DAY) &&
                                                selectedEndTime.get(Calendar.MINUTE) == startTimeCal.get(Calendar.MINUTE))) {
                                    Toast.makeText(TeacherCreateEventActivity.this,
                                            "End time must be after start time",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (Exception e) {
                                Log.e("TimeValidation", "Error validating times", e);
                            }
                        }

                        // Format the time
                        Calendar timeCal = Calendar.getInstance();
                        timeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(Calendar.MINUTE, selectedMinute);

                        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String displayTime = displayTimeFormat.format(timeCal.getTime());

                        SimpleDateFormat storageTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        String storageTime = storageTimeFormat.format(timeCal.getTime());

                        // Set the selected time
                        endTimeField.setText(displayTime);
                        eventData.put("endTime", storageTime);
                    }, currentHour, currentMinute, false);

            timePickerDialog.show();
        });
    }
    private void setupTimePicker(final EditText timeField, final String hint) {
        timeField.setHint(hint);

        timeField.setOnClickListener(v -> {
            // Get Current Time
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Time Picker Dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        // Format the time for display (12-hour format with AM/PM)
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        calendar.set(Calendar.MINUTE, selectedMinute);
                        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String displayTime = displayTimeFormat.format(calendar.getTime());

                        // Format the time for storage (24-hour format without AM/PM)
                        SimpleDateFormat storageTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        String storageTime = storageTimeFormat.format(calendar.getTime());

                        // Set the selected time to the field (for display)
                        timeField.setText(displayTime);

                        // Store time information (without AM/PM)
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
        // Create array of grace time options
        String[] graceTimeOptions = {"None", "15 min", "30 min", "60 min", "120 min"};

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                graceTimeOptions
        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        graceTimeSpinner.setAdapter(adapter);

        // Set listener for selection
        graceTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = graceTimeOptions[position];

                // Store the grace time value
                if (selectedOption.equals("None")) {
                    eventData.put("graceTime", "none");
                } else {
                    // Extract the number but store as string
                    String graceTimeMinutes = selectedOption.split(" ")[0];
                    eventData.put("graceTime", graceTimeMinutes);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to "none" instead of 0 minutes
                eventData.put("graceTime", "none");
            }
        });
    }

    // 4. Also modify validatePage2() to check date validity before proceeding
    private boolean validatePage2() {
        boolean isValid = true;

        // Validate Venue
        String venue = venueField.getText().toString().trim();
        if (venue.isEmpty()) {
            venueField.setError("Venue is required");
            isValid = false;
        } else {
            eventData.put("venue", venue);
        }

        // Validate Event Dates
        String startDate = startDateField.getText().toString().trim();
        if (startDate.isEmpty()) {
            startDateField.setError("Start date is required");
            isValid = false;
        } else {
            // Check if the start date is in the past
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

                // If this is the current date, perform additional time validation
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
            // For multi-day events, check if end date is after start date
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

        // Validate Event Times
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

        // Grace time is always selected because spinner has a default selection

        return isValid;
    }

    private boolean validateTimeForCurrentDay() {
        String startTime = startTimeField.getText().toString().trim();
        if (startTime.isEmpty()) {
            return true; // Skip validation if no time is set yet
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date selectedStartTime = sdf.parse(startTime);

            Calendar selectedTimeCal = Calendar.getInstance();
            selectedTimeCal.setTime(selectedStartTime);

            // Set the selected hour and minute, but keep today's date
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

        // Validate Cover Photo
        if (coverPhotoUri == null) {
            Toast.makeText(this, "Please upload a cover photo", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate Proposal File
        if (proposalFileUri == null) {
            Toast.makeText(this, "Please upload an event proposal", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void setupEventTypeSpinner() {
        // Create array of event types
        String[] eventTypes = {"Seminar", "Off-Campus Activity", "Sports Event", "Other"};

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                eventTypes
        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        eventTypeSpinner.setAdapter(adapter);

        // Set listener for selection
        eventTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedEventType = eventTypes[position];

                // Check if "Other" is selected
                if (selectedEventType.equals("Other")) {
                    // Show the specify event type fields
                    specifyEventTypeLabel.setVisibility(View.VISIBLE);
                    specifyEventTypeAsterisk.setVisibility(View.VISIBLE);
                    specifyEventTypeField.setVisibility(View.VISIBLE);

                    // Clear any previous text
                    specifyEventTypeField.setText("");

                    // Don't store the event type yet, wait for the user to specify it
                } else {
                    // Hide the specify event type fields
                    specifyEventTypeLabel.setVisibility(View.GONE);
                    specifyEventTypeAsterisk.setVisibility(View.GONE);
                    specifyEventTypeField.setVisibility(View.GONE);

                    // Store the selected event type
                    eventData.put("eventType", selectedEventType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    private void setupEventForSpinner() {
        // Create array of grade levels
        String[] gradeOptions = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12","All Year Level"};

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                gradeOptions
        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        eventForSpinner.setAdapter(adapter);

        // Set listener for selection
        eventForSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get selected text
                String selectedGrade = gradeOptions[position];

                // Check if "All Year Level" is selected
                if (selectedGrade.equals("All Year Level")) {
                    // Store as "All" as requested
                    eventData.put("eventFor", "All");
                } else {
                    // Convert to required format (e.g., "Grade 7" to "Grade-7")
                    String formattedGrade = selectedGrade.replace(" ", "-");
                    // Save to event data
                    eventData.put("eventFor", formattedGrade);
                }

                // If a start date is already selected, clear it to force revalidation with new grade level
                String currentStartDate = startDateField.getText().toString().trim();
                if (!currentStartDate.isEmpty()) {
                    // Clear the field to prompt reselection
                    startDateField.setText("");
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Please reselect the event date to check availability for this grade level",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupPageNavigation() {
        // Page 1 -> Next Button
        nextPg1.setOnClickListener(v -> {
            if (validatePage1()) {
                savePageOneData();
                showPage(2);
            }
        });

        // Page 2 -> Back Button
        backPg2.setOnClickListener(v -> showPage(1));

        // Page 2 -> Next Button
        nextPg2.setOnClickListener(v -> {
            if (validatePage2()) {
                showPage(3);
            }
        });

        // Page 3 -> Back Button
        backPg3.setOnClickListener(v -> showPage(2));

        // Page 3 -> Create Button
        createButton.setOnClickListener(v -> {
            if (validatePage3()) {
                uploadFilesAndSaveEvent();
            }
        });
    }

    private boolean validatePage1() {
        boolean isValid = true;

        // Validate Event Name
        String eventName = eventNameField.getText().toString().trim();
        if (eventName.isEmpty()) {
            eventNameField.setError("Event name is required");
            isValid = false;
        }

        // Validate Event Description
        String eventDescription = eventDescriptionField.getText().toString().trim();
        if (eventDescription.isEmpty()) {
            eventDescriptionField.setError("Event description is required");
            isValid = false;
        }

        // Validate Specified Event Type if "Other" is selected
        String selectedEventType = eventTypeSpinner.getSelectedItem().toString();
        if (selectedEventType.equals("Other")) {
            String specifiedType = specifyEventTypeField.getText().toString().trim();
            if (specifiedType.isEmpty()) {
                specifyEventTypeField.setError("Please specify the event type");
                isValid = false;
            }
        }

        return isValid;
    }
    private void savePageOneData() {
        // Save EditText values
        eventData.put("eventName", eventNameField.getText().toString().trim());
        eventData.put("eventDescription", eventDescriptionField.getText().toString().trim());

        // Handle event type based on spinner selection
        String selectedEventType = eventTypeSpinner.getSelectedItem().toString();
        if (selectedEventType.equals("Other")) {
            // Use the specified event type
            String specifiedType = specifyEventTypeField.getText().toString().trim();
            eventData.put("eventType", specifiedType);
        }
        // If not "Other", eventType was already set in the spinner listener

        // Event For is already saved in the spinner listener

        // Add timestamp
        eventData.put("createdAt", System.currentTimeMillis());

        // Instead of using Firebase Auth UID, we'll use the teacher's ID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            // Query the teachers node to find the teacher with matching email
            findTeacherIdByEmail(userEmail);
        } else {
            // Handle the case when the user is not authenticated
            Toast.makeText(this, "You must be logged in to create an event", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method to find a teacher's ID by their email address
     * @param email The email of the logged-in teacher
     */
    private void findTeacherIdByEmail(String email) {
        DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("teachers");

        teachersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // We found the teacher with this email
                    for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                        // Get the teacher's ID (key)
                        String teacherId = teacherSnapshot.getKey();
                        // Add the teacher ID to the event data
                        eventData.put("createdBy", teacherId);
                        break; // Just take the first match (should be only one)
                    }
                } else {
                    // No teacher found with this email
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
        // Show loading or disable buttons
        createButton.setEnabled(false);

        // Show a progress message
        Toast.makeText(this, "Uploading files and creating event...", Toast.LENGTH_SHORT).show();

        // Check if we have the teacherId in eventData
        if (!eventData.containsKey("createdBy")) {
            // Try to get it again before continuing
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                // Query the teachers node to find the teacher with matching email
                DatabaseReference teachersRef = FirebaseDatabase.getInstance().getReference("teachers");
                teachersRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot teacherSnapshot : dataSnapshot.getChildren()) {
                                String teacherId = teacherSnapshot.getKey();
                                eventData.put("createdBy", teacherId);
                                // Now continue with the upload
                                proceedWithUpload();
                            }
                        } else {
                            // No teacher found with this email
                            createButton.setEnabled(true);
                            Toast.makeText(TeacherCreateEventActivity.this,
                                    "Could not find your teacher profile. Please contact support.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        createButton.setEnabled(true);
                        Toast.makeText(TeacherCreateEventActivity.this,
                                "Error finding teacher profile: " + databaseError.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // User not authenticated
                createButton.setEnabled(true);
                Toast.makeText(this, "You must be logged in to create an event", Toast.LENGTH_SHORT).show();
            }
        } else {
            // We already have the teacher ID, proceed with upload
            proceedWithUpload();
        }
    }
    private void proceedWithUpload() {
        // Determine if this is a resubmission
        boolean isResubmission = eventData.containsKey("originalEventId");
        String eventId;

        if (isResubmission) {
            // For resubmission, we create a new event entry but link it to the original
            eventId = eventProposalRef.push().getKey();

            if (eventId == null) {
                // Error generating ID
                createButton.setEnabled(true);
                Toast.makeText(this, "Error creating event", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add reference to the original event
            eventData.put("resubmissionOf", eventData.get("originalEventId"));
            // Remove the temporary originalEventId from eventData
            eventData.remove("originalEventId");
        } else {
            // Generate a unique key for the new event
            eventId = eventProposalRef.push().getKey();

            if (eventId == null) {
                // Error generating ID
                createButton.setEnabled(true);
                Toast.makeText(this, "Error creating event", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Add the ID to the event data
        eventData.put("eventId", eventId);

        // Set the required default values
        eventData.put("registrationAllowed", false);
        eventData.put("status", "pending");

        // Add timestamps
        long currentTimeMillis = System.currentTimeMillis();
        eventData.put("timestamp", currentTimeMillis);

        // Format current date as string
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        String currentDate = dateFormat.format(new Date(currentTimeMillis));
        eventData.put("dateCreated", currentDate);

        // Upload cover photo to Firebase Storage
        if (coverPhotoUri != null) {
            String eventName = eventData.containsKey("eventName") ?
                    eventData.get("eventName").toString() :
                    "event_" + eventId;

            uploadCoverPhoto(eventId, eventName);
        } else if (isResubmission) {
            // For resubmission without a new cover photo, we proceed to proposal upload
            // but we keep the existing photo URL if available
            uploadEventProposal(eventId);
        } else {
            // No cover photo for a new event, check with the user
            createButton.setEnabled(true);
            Toast.makeText(this, "Please upload a cover photo", Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadCoverPhoto(String eventId, String eventName) {
        // Get the original filename from the URI
        String originalFileName = getOriginalFileName(coverPhotoUri);

        // If we couldn't get the original filename, fall back to a default name with extension
        if (originalFileName == null || originalFileName.isEmpty()) {
            originalFileName = "photo." + getFileExtension(coverPhotoUri);
        }

        // Create a reference to the file location in Firebase Storage using eventName as directory
        // The path will be: eventName/originalFileName
        StorageReference coverPhotoRef = storageRef.child(eventName + "/" + originalFileName);

        // Upload the file
        String finalOriginalFileName = originalFileName;
        coverPhotoRef.putFile(coverPhotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    coverPhotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Add the download URL to the event data
                        eventData.put("eventPhotoUrl", uri.toString());

                        // Store the photo path for easier reference
                        eventData.put("eventPhotoPath", eventName + "/" + finalOriginalFileName);

                        // Now upload the event proposal
                        uploadEventProposal(eventId);
                    });
                })
                .addOnFailureListener(e -> {
                    // Enable the create button again
                    createButton.setEnabled(true);
                    Toast.makeText(TeacherCreateEventActivity.this, "Failed to upload cover photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Method to get the original filename from a URI
     * @param uri The URI of the file
     * @return The original filename or null if it couldn't be determined
     */
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
            // If content resolver approach failed, try getting filename from path
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadEventProposal(String eventId) {
        if (proposalFileUri != null) {
            // Get the event name from your event data
            String eventName = eventData.get("eventName").toString();

            // Create a reference to the file location in Firebase Storage
            // Path structure: eventName/event_proposal/file.pdf
            StorageReference proposalRef = storageRef.child(eventName + "/event_proposal/" + proposalFileName);

            // Upload the file
            proposalRef.putFile(proposalFileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL
                        proposalRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Add the download URL to the event data
                            eventData.put("eventProposal", uri.toString());

                            // Finally save the event data to the database
                            saveEventToFirebase(eventId);
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Enable the create button again
                        createButton.setEnabled(true);
                        Toast.makeText(TeacherCreateEventActivity.this, "Failed to upload event proposal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else if (eventData.containsKey("existingProposalUrl")) {
            // For resubmission where we're keeping the existing proposal document
            eventData.put("eventProposal", eventData.get("existingProposalUrl"));
            eventData.remove("existingProposalUrl"); // Remove the temporary field
            saveEventToFirebase(eventId);
        } else {
            // No proposal file, ask the user to upload one
            createButton.setEnabled(true);
            Toast.makeText(this, "Please upload an event proposal", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEventToFirebase(String eventId) {
        // Check if this is a resubmission
        boolean isResubmission = eventData.containsKey("resubmissionOf");
        final String originalEventId = isResubmission ? eventData.get("resubmissionOf").toString() : null;

        // For resubmissions, add additional data
        if (isResubmission) {
            eventData.put("isResubmission", true);
        }

        // Save to Firebase
        eventProposalRef.child(eventId).setValue(eventData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                // Re-enable buttons
                createButton.setEnabled(true);

                if (databaseError == null) {
                    // Success
                    String message = isResubmission ?
                            "Event resubmitted successfully" :
                            "Event created successfully";

                    Toast.makeText(TeacherCreateEventActivity.this, message, Toast.LENGTH_SHORT).show();

                    // If this is a resubmission, delete the original rejected event
                    if (isResubmission && originalEventId != null) {
                        deleteRejectedEvent(originalEventId);
                    }

                    // Navigate to events list or dashboard
                    startActivity(new Intent(TeacherCreateEventActivity.this, TeacherEvents.class));
                    finish();
                } else {
                    // Failed
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Failed to " + (isResubmission ? "resubmit" : "create") + " event: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Deletes the rejected event from Firebase when a resubmission is created
     * @param originalEventId The ID of the original rejected event that's being resubmitted
     */
    private void deleteRejectedEvent(String originalEventId) {
        if (originalEventId == null || originalEventId.isEmpty()) {
            return;
        }

        // Get reference to the rejected event in the eventProposals collection
        DatabaseReference rejectedEventRef = database.getReference("eventProposals").child(originalEventId);

        // Delete the rejected event
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

        // Show correct page
        page1.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        page2.setVisibility(page == 2 ? View.VISIBLE : View.GONE);
        page3.setVisibility(page == 3 ? View.VISIBLE : View.GONE);

        // Update progress indicator
        updateProgressIndicator(page);
    }

    private void updateProgressIndicator(int page) {
        TextView step1 = findViewById(R.id.stepBasicInfo);
        TextView step2 = findViewById(R.id.stepSchedule);
        TextView step3 = findViewById(R.id.stepMedia);

        View connector1 = findViewById(R.id.connector1);
        View connector2 = findViewById(R.id.connector2);

        // Reset all steps
        step1.setBackgroundResource(R.drawable.progress_bar_background);
        step2.setBackgroundResource(R.drawable.progress_bar_background);
        step3.setBackgroundResource(R.drawable.progress_bar_background);

        step1.setTextColor(getResources().getColor(R.color.gray));
        step2.setTextColor(getResources().getColor(R.color.gray));
        step3.setTextColor(getResources().getColor(R.color.gray));

        connector1.setBackgroundColor(getResources().getColor(R.color.light_gray)); // default
        connector2.setBackgroundColor(getResources().getColor(R.color.light_gray)); // default

        // Update steps and lines based on current page
        if (page >= 1) {
            step1.setBackgroundResource(R.drawable.progress_bar_active);
            step1.setTextColor(getResources().getColor(android.R.color.white));
        }
        if (page >= 2) {
            step2.setBackgroundResource(R.drawable.progress_bar_active);
            step2.setTextColor(getResources().getColor(android.R.color.white));
            connector1.setBackgroundColor(getResources().getColor(R.color.primary)); // blue
        }
        if (page >= 3) {
            step3.setBackgroundResource(R.drawable.progress_bar_active);
            step3.setTextColor(getResources().getColor(android.R.color.white));
            connector2.setBackgroundColor(getResources().getColor(R.color.primary)); // blue
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_teacher);

        // Deselect all items
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