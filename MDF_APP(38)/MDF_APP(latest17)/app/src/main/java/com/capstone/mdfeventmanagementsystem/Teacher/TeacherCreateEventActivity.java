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

    // Edit mode flag
    private boolean isEditing = false;
    private String eventId;

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
        // Check if this is a resubmission or edit
        checkForResubmissionOrEdit();

        setupPageNavigation();
        setupBottomNavigation();
        showPage(1); // Show first page initially
    }

    private void checkForResubmissionOrEdit() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("IS_EDITING", false)) {
                // This is an edit operation
                isEditing = true;
                createButton.setText("Update");
                eventId = intent.getStringExtra("EVENT_ID");
                populateFieldsForEdit(intent);
            } else if (intent.getBooleanExtra("IS_RESUBMISSION", false)) {
                // This is a resubmission
                createButton.setText("Resubmit Event");
                populateFieldsForResubmission(intent);
            }
        }
    }

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

        // Store the eventId for later use when saving
        eventData.put("eventId", eventId);

        // Populate Page 1 fields
        populatePage1Fields(eventName, eventDescription, eventType, eventFor);

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
            // Display the existing proposal file name
            String fileName = proposalUrl.substring(proposalUrl.lastIndexOf('/') + 1);
            fileNameLabel.setText(fileName);
        }
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
        String selectedGrade = eventForSpinner.getSelectedItem().toString();
        String formattedGrade = selectedGrade.equals("All Year Level") ? "All" : selectedGrade.replace(" ", "-");

        DatabaseReference eventsRef = database.getReference("eventProposals");

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

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasConflict = false;
                String conflictMessage = "Selected date has a scheduling conflict";

                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
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

                    if (!("All".equals(formattedGrade) || "All".equals(eventFor) || formattedGrade.equals(eventFor))) {
                        continue;
                    }

                    String eventSpan = eventSnapshot.child("eventSpan").getValue(String.class);

                    if ("single-day".equals(eventSpan)) {
                        String existingStartDate = eventSnapshot.child("startDate").getValue(String.class);
                        if (existingStartDate != null && existingStartDate.equals(storageDate)) {
                            hasConflict = true;
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
                        "Error checking date availability: " + databaseError.getMessage(),
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
            final Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentMinute = calendar.get(Calendar.MINUTE);

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

            TimePickerDialog timePickerDialog = new TimePickerDialog(TeacherCreateEventActivity.this,
                    (view, selectedHour, selectedMinute) -> {
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

                        Calendar timeCal = Calendar.getInstance();
                        timeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                        timeCal.set(Calendar.MINUTE, selectedMinute);

                        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String displayTime = displayTimeFormat.format(timeCal.getTime());

                        SimpleDateFormat storageTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        String storageTime = storageTimeFormat.format(timeCal.getTime());

                        endTimeField.setText(displayTime);
                        eventData.put("endTime", storageTime);
                    }, currentHour, currentMinute, false);

            timePickerDialog.show();
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
        String[] graceTimeOptions = {"None", "15 min", "30 min", "60 min", "120 min"};

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
                if (selectedOption.equals("None")) {
                    eventData.put("graceTime", "none");
                } else {
                    String graceTimeMinutes = selectedOption.split(" ")[0];
                    eventData.put("graceTime", graceTimeMinutes);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                eventData.put("graceTime", "none");
            }
        });
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

        if (proposalFileUri == null && !eventData.containsKey("eventProposal")) {
            Toast.makeText(this, "Please upload an event proposal", Toast.LENGTH_SHORT).show();
            isValid = false;
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
                    specifyEventTypeField.setText("");
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
        String[] gradeOptions = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12", "All Year Level"};

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
                if (selectedGrade.equals("All Year Level")) {
                    eventData.put("eventFor", "All");
                } else {
                    String formattedGrade = selectedGrade.replace(" ", "-");
                    eventData.put("eventFor", formattedGrade);
                }

                String currentStartDate = startDateField.getText().toString().trim();
                if (!currentStartDate.isEmpty()) {
                    startDateField.setText("");
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Please reselect the event date to check availability for this grade level",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
        if

        (eventDescription.isEmpty()) {
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

        return isValid;
    }

    private void savePageOneData() {
        eventData.put("eventName", eventNameField.getText().toString().trim());
        eventData.put("eventDescription", eventDescriptionField.getText().toString().trim());

        String selectedEventType = eventTypeSpinner.getSelectedItem().toString();
        if (selectedEventType.equals("Other")) {
            String specifiedType = specifyEventTypeField.getText().toString().trim();
            eventData.put("eventType", specifiedType);
        }

        eventData.put("createdAt", System.currentTimeMillis());

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

        // Check if a new cover photo is selected
        if (coverPhotoUri != null) {
            String eventName = eventData.get("eventName").toString();
            uploadCoverPhoto(eventId, eventName);
        } else {
            // Keep existing photo URL and proceed to proposal
            uploadEventProposal(eventId);
        }
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
            createButton.setEnabled(true);
            Toast.makeText(this, "Please upload an event proposal", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEventToFirebase(String eventId) {
        boolean isResubmission = eventData.containsKey("resubmissionOf");
        final String originalEventId = isResubmission ? eventData.get("resubmissionOf").toString() : null;

        if (isResubmission) {
            eventData.put("isResubmission", true);
        }

        if (isEditing) {
            // Update existing event in the events node
            DatabaseReference eventRef = database.getReference("events").child(eventId);
            eventRef.updateChildren(eventData, (databaseError, databaseReference) -> {
                createButton.setEnabled(true);
                if (databaseError == null) {
                    Toast.makeText(TeacherCreateEventActivity.this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(TeacherCreateEventActivity.this, TeacherEvents.class));
                    finish();
                } else {
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Failed to update event: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Existing code for create/resubmission
            eventProposalRef.child(eventId).setValue(eventData, (databaseError, databaseReference) -> {
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