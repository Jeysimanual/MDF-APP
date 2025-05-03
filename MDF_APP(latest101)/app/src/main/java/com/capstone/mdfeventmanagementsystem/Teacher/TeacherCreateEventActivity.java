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
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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

        setupPageNavigation();
        setupBottomNavigation();
        showPage(1); // Show first page initially
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
                        // Format the date
                        calendar.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                        String formattedDate = dateFormat.format(calendar.getTime());

                        // Set the selected date to the field
                        dateField.setText(formattedDate);

                        // If this is the start date and single day event is selected,
                        // set the end date to the same value
                        if (dateField.getId() == R.id.startDate && radioSingleDayEvent.isChecked()) {
                            endDateField.setText(formattedDate);
                        }

                        // Store date information
                        if (dateField.getId() == R.id.startDate) {
                            eventData.put("startDate", formattedDate);
                        } else if (dateField.getId() == R.id.endDate) {
                            eventData.put("endDate", formattedDate);
                        }
                    }, year, month, day);

            datePickerDialog.show();
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
                        // Format the time
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        calendar.set(Calendar.MINUTE, selectedMinute);
                        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                        String formattedTime = timeFormat.format(calendar.getTime());

                        // Set the selected time to the field
                        timeField.setText(formattedTime);

                        // Store time information
                        if (timeField.getId() == R.id.startTime) {
                            eventData.put("startTime", formattedTime);
                        } else if (timeField.getId() == R.id.endTime) {
                            eventData.put("endTime", formattedTime);
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

                // Extract numeric value (0 for None, or the minutes value)
                int graceTimeMinutes;
                if (selectedOption.equals("None")) {
                    graceTimeMinutes = 0;
                } else {
                    // Extract the number from format like "15 min"
                    graceTimeMinutes = Integer.parseInt(selectedOption.split(" ")[0]);
                }

                // Save to event data
                eventData.put("graceTime", graceTimeMinutes);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to 0 minutes (None)
                eventData.put("graceTime", 0);
            }
        });
    }

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
        }

        String endDate = endDateField.getText().toString().trim();
        if (endDate.isEmpty()) {
            endDateField.setError("End date is required");
            isValid = false;
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
                eventData.put("eventType", selectedEventType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupEventForSpinner() {
        // Create array of grade levels
        String[] gradeOptions = {"Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};

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

                // Convert to required format (e.g., "Grade 7" to "Grade-7")
                String formattedGrade = selectedGrade.replace(" ", "-");

                // Save to event data
                eventData.put("eventFor", formattedGrade);
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

        // No need to validate spinners as they always have a selection

        return isValid;
    }

    private void savePageOneData() {
        // Save EditText values
        eventData.put("eventName", eventNameField.getText().toString().trim());
        eventData.put("eventDescription", eventDescriptionField.getText().toString().trim());

        // Event Type and Event For are already saved in the spinner listeners

        // Add timestamp and status
        eventData.put("createdAt", System.currentTimeMillis());

        // Add the creator's ID if user is authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            eventData.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
    }

    private void uploadFilesAndSaveEvent() {
        // Show loading or disable buttons
        createButton.setEnabled(false);

        // Show a progress message
        Toast.makeText(this, "Uploading files and creating event...", Toast.LENGTH_SHORT).show();

        // Generate a unique key for the new event
        String eventId = eventProposalRef.push().getKey();

        if (eventId == null) {
            // Error generating ID
            createButton.setEnabled(true);
            Toast.makeText(this, "Error creating event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add the ID to the event data
        eventData.put("eventId", eventId);

        // Set the required default values
        eventData.put("registrationAllowed", false);
        eventData.put("status", "pending");

        // Add timestamps
        long currentTimeMillis = System.currentTimeMillis();
        eventData.put("timestamp", currentTimeMillis);

        // Format current date as string - Updated format to "Month DD, YYYY"
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        String currentDate = dateFormat.format(new Date(currentTimeMillis));
        eventData.put("dateCreated", currentDate);

        // Upload cover photo to Firebase Storage
        if (coverPhotoUri != null) {
            // Extract event name from the eventData map or use a default value
            String eventName = eventData.containsKey("eventName") ?
                    eventData.get("eventName").toString() :
                    "event_" + eventId;

            uploadCoverPhoto(eventId, eventName);
        } else {
            // No cover photo, continue with event proposal
            uploadEventProposal(eventId);
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
        } else {
            // No proposal file, just save the event data
            saveEventToFirebase(eventId);
        }
    }

    private void saveEventToFirebase(String eventId) {
        // Save to Firebase
        eventProposalRef.child(eventId).setValue(eventData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                // Re-enable buttons
                createButton.setEnabled(true);

                if (databaseError == null) {
                    // Success
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Event created successfully", Toast.LENGTH_SHORT).show();

                    // Navigate to events list or dashboard
                    startActivity(new Intent(TeacherCreateEventActivity.this, TeacherEvents.class));
                    finish();
                } else {
                    // Failed
                    Toast.makeText(TeacherCreateEventActivity.this,
                            "Failed to create event: " + databaseError.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
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