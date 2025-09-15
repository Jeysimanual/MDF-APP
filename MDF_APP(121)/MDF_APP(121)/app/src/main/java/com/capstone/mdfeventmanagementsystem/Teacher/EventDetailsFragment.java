package com.capstone.mdfeventmanagementsystem.Teacher;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.capstone.mdfeventmanagementsystem.Adapters.CoordinatorAdapter;
import com.capstone.mdfeventmanagementsystem.Adapters.StudentAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Button showCoordinatorsBtn;
    private Button addCoordinatorBtn;
    private CardView registrationCard;
    private SwitchCompat registrationSwitch;
    private TextView registrationStatusText;
    private DatabaseReference eventRef;
    private ImageButton editEventButton;
    private boolean canEditEvent = false;
    private boolean isEventCreator = false;
    private String targetParticipant = null;
    private ValueEventListener coordinatorListener;
    private ValueEventListener scanPermissionListener;

    public EventDetailsFragment() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        addCoordinatorBtn = view.findViewById(R.id.addCoordinatorsBtn);
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

        if (photoImageView != null) {
            photoImageView.setOnClickListener(v -> showImagePopup(photoUrl));
        }

        if (editEventButton != null) {
            editEventButton.setOnClickListener(v -> handleEditButtonClick());
            editEventButton.setVisibility(View.GONE);
        }

        if (addCoordinatorBtn != null) {
            addCoordinatorBtn.setOnClickListener(v -> showAddCoordinatorDialog());
            addCoordinatorBtn.setVisibility(View.VISIBLE);
        }

        if (showCoordinatorsBtn != null) {
            showCoordinatorsBtn.setOnClickListener(v -> showCoordinatorsDialog());
        }

        if (eventUID != null && !eventUID.isEmpty()) {
            eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);
            getEventDetails(eventUID);
            getTicketCount(eventUID);
            getTargetParticipant(eventUID);
            getTotalCoordinators(eventUID);
            setupRegistrationControl();
            checkEditPermission();
            checkCreatorPermission();
        } else {
            Log.e("EventDetailsFragment", "Event UID is null or empty, cannot proceed with loading event details.");
        }
    }

    private void showAddCoordinatorDialog() {
        if (!isAdded()) return;

        // Get the current teacher's ID
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference teacherRef = FirebaseDatabase.getInstance().getReference("teachers").child(teacherId);

        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                // Check if teacher is a year-level advisor and get section
                String yearLevelAdvisor = snapshot.child("year_level_advisor").getValue(String.class);
                String teacherSection = snapshot.child("section").getValue(String.class);
                if (yearLevelAdvisor == null || yearLevelAdvisor.isEmpty()) {
                    String reason = yearLevelAdvisor == null ? "year_level_advisor is null" : "year_level_advisor is empty";
                    Log.w("AddCoordinator", "Teacher is not a year-level advisor. Reason: " + reason + ", teacherId: " + teacherId);
                    Toast.makeText(requireContext(), "You are not authorized to add coordinators.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (teacherSection == null || teacherSection.isEmpty()) {
                    String reason = teacherSection == null ? "section is null" : "section is empty";
                    Log.w("AddCoordinator", "Teacher has no valid section. Reason: " + reason + ", teacherId: " + teacherId);
                    Toast.makeText(requireContext(), "You are not assigned to a valid section.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("AddCoordinator", "Teacher's year_level_advisor: " + yearLevelAdvisor + ", section: " + teacherSection + ", teacherId: " + teacherId);

                // Determine if the teacher's advised year level is included in eventFor
                boolean isAuthorized = false;
                if (eventForValue != null && !eventForValue.isEmpty()) {
                    if (eventForValue.equalsIgnoreCase("All")) {
                        isAuthorized = true;
                    } else {
                        String advisorGrade = "Grade-" + yearLevelAdvisor;
                        String[] eventGrades = eventForValue.split(",");
                        for (String grade : eventGrades) {
                            if (grade.trim().equalsIgnoreCase(advisorGrade)) {
                                isAuthorized = true;
                                break;
                            }
                        }
                    }
                }

                if (!isAuthorized) {
                    Log.w("AddCoordinator", "Teacher's advised year level (" + yearLevelAdvisor + ") does not match eventFor: " + eventForValue + ", teacherId: " + teacherId);
                    Toast.makeText(requireContext(), "You can only add assistant for Grade " + yearLevelAdvisor + " events.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Proceed with the dialog setup
                Dialog dialog = new Dialog(requireContext());
                dialog.setContentView(R.layout.dialog_add_coordinator);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                RecyclerView studentsRecyclerView = dialog.findViewById(R.id.studentsRecyclerView);
                ProgressBar loadingProgressBar = dialog.findViewById(R.id.loadingProgressBar);
                TextView emptyTextView = dialog.findViewById(R.id.emptyTextView);
                ImageButton cancelButton = dialog.findViewById(R.id.cancelButton);
                EditText searchEditText = dialog.findViewById(R.id.searchEditText);
                ImageButton clearSearchButton = dialog.findViewById(R.id.clearSearchButton);

                studentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                StudentAdapter studentAdapter = new StudentAdapter((studentId, studentName) ->
                        addStudentAsCoordinator(studentId, studentName));
                studentsRecyclerView.setAdapter(studentAdapter);

                // Store the full list of students for filtering
                List<StudentAdapter.StudentItem> fullStudentList = new ArrayList<>();

                // Determine eligible grades from eventForValue
                Set<String> eligibleGrades = new HashSet<>();
                if (eventForValue != null && !eventForValue.isEmpty()) {
                    if (eventForValue.equalsIgnoreCase("All")) {
                        eligibleGrades.addAll(Arrays.asList("7", "8", "9", "10", "11", "12"));
                    } else {
                        String[] grades = eventForValue.split(",");
                        for (String grade : grades) {
                            String gradeNumber = grade.replace("Grade-", "").trim();
                            if (!gradeNumber.isEmpty()) {
                                eligibleGrades.add(gradeNumber);
                            }
                        }
                    }
                } else {
                    Log.w("AddCoordinator", "eventForValue is null or empty, defaulting to no students, teacherId: " + teacherId);
                    loadingProgressBar.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    emptyTextView.setText("No eligible students found for this event.");
                    return;
                }

                // Clear search button functionality
                clearSearchButton.setVisibility(View.GONE); // Initially hidden
                clearSearchButton.setOnClickListener(v -> {
                    searchEditText.setText("");
                    clearSearchButton.setVisibility(View.GONE);
                });

                // Search functionality
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String query = s.toString().trim().toLowerCase();
                        clearSearchButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                        List<StudentAdapter.StudentItem> filteredList = new ArrayList<>();
                        Map<String, List<StudentAdapter.StudentItem>> studentsByGrade = new HashMap<>();

                        for (StudentAdapter.StudentItem item : fullStudentList) {
                            if (!item.isHeader() && item.getDisplayName() != null && item.getDisplayName().toLowerCase().contains(query)) {
                                String grade = item.getYearLevel();
                                if (!studentsByGrade.containsKey(grade)) {
                                    studentsByGrade.put(grade, new ArrayList<>());
                                }
                                studentsByGrade.get(grade).add(item);
                            }
                        }

                        // Add headers for grades with matching students
                        List<String> sortedGrades = new ArrayList<>(studentsByGrade.keySet());
                        Collections.sort(sortedGrades, (g1, g2) -> {
                            try {
                                return Integer.parseInt(g1) - Integer.parseInt(g2);
                            } catch (NumberFormatException e) {
                                return g1.compareTo(g2);
                            }
                        });

                        for (String grade : sortedGrades) {
                            filteredList.add(new StudentAdapter.StudentItem(grade, teacherSection));
                            filteredList.addAll(studentsByGrade.get(grade));
                        }

                        studentAdapter.setData(filteredList);
                        studentsRecyclerView.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
                        emptyTextView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                        emptyTextView.setText(filteredList.isEmpty() ? "No students match your search in section " + teacherSection + "." : "");
                    }
                });

                DatabaseReference coordinatorsRef = eventRef.child("eventCoordinators");
                ValueEventListener coordinatorListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot coordinatorSnapshot) {
                        if (!isAdded()) return;

                        List<String> existingCoordinatorIds = new ArrayList<>();
                        for (DataSnapshot snapshot : coordinatorSnapshot.getChildren()) {
                            if (snapshot.getValue(Boolean.class) == true) {
                                existingCoordinatorIds.add(snapshot.getKey());
                            }
                        }

                        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
                        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!isAdded()) return;

                                List<StudentAdapter.StudentItem> students = new ArrayList<>();
                                Map<String, List<StudentAdapter.StudentItem>> studentsByGrade = new HashMap<>();

                                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                    String studentId = studentSnapshot.getKey();
                                    if (existingCoordinatorIds.contains(studentId)) continue;

                                    String firstName = studentSnapshot.child("firstName").getValue(String.class);
                                    String lastName = studentSnapshot.child("lastName").getValue(String.class);
                                    String middleName = studentSnapshot.child("middleName").getValue(String.class);
                                    String yearLevel = studentSnapshot.child("yearLevel").getValue(String.class);
                                    String studentSection = studentSnapshot.child("section").getValue(String.class);

                                    if (firstName == null || lastName == null || yearLevel == null || studentSection == null) {
                                        Log.d("AddCoordinator", "Skipping student " + studentId + ": missing data (firstName=" + firstName + ", lastName=" + lastName + ", yearLevel=" + yearLevel + ", section=" + studentSection + ")");
                                        continue;
                                    }

                                    if (!studentSection.equalsIgnoreCase(teacherSection)) {
                                        Log.d("AddCoordinator", "Skipping student " + studentId + ": section (" + studentSection + ") does not match teacher's section (" + teacherSection + ")");
                                        continue;
                                    }

                                    if (!eventForValue.equalsIgnoreCase("All") && !eligibleGrades.contains(yearLevel)) {
                                        Log.d("AddCoordinator", "Skipping student " + studentId + ": yearLevel (" + yearLevel + ") not in eligible grades: " + eligibleGrades);
                                        continue;
                                    }

                                    String displayName = lastName + ", " + firstName +
                                            (middleName != null && !middleName.isEmpty() ? " " + middleName.charAt(0) + "." : "");
                                    StudentAdapter.StudentItem student = new StudentAdapter.StudentItem(studentId, displayName, yearLevel);
                                    students.add(student);

                                    if (!studentsByGrade.containsKey(yearLevel)) {
                                        studentsByGrade.put(yearLevel, new ArrayList<>());
                                    }
                                    studentsByGrade.get(yearLevel).add(student);
                                }

                                List<String> sortedGrades = new ArrayList<>(studentsByGrade.keySet());
                                Collections.sort(sortedGrades, (g1, g2) -> {
                                    try {
                                        return Integer.parseInt(g1) - Integer.parseInt(g2);
                                    } catch (NumberFormatException e) {
                                        return g1.compareTo(g2);
                                    }
                                });

                                List<StudentAdapter.StudentItem> groupedData = new ArrayList<>();
                                for (String grade : sortedGrades) {
                                    if (!studentsByGrade.get(grade).isEmpty()) {
                                        groupedData.add(new StudentAdapter.StudentItem(grade, teacherSection));
                                        groupedData.addAll(studentsByGrade.get(grade));
                                    }
                                }

                                fullStudentList.clear();
                                fullStudentList.addAll(groupedData);

                                loadingProgressBar.setVisibility(View.GONE);
                                if (groupedData.isEmpty()) {
                                    emptyTextView.setVisibility(View.VISIBLE);
                                    studentsRecyclerView.setVisibility(View.GONE);
                                    emptyTextView.setText("No eligible students found in your section (" + teacherSection + ") for this event.");
                                } else {
                                    emptyTextView.setVisibility(View.GONE);
                                    studentsRecyclerView.setVisibility(View.VISIBLE);
                                    studentAdapter.setData(groupedData);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (!isAdded()) return;
                                loadingProgressBar.setVisibility(View.GONE);
                                emptyTextView.setVisibility(View.VISIBLE);
                                emptyTextView.setText("Error loading students: " + error.getMessage());
                                Log.e("AddCoordinator", "Error fetching students: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        loadingProgressBar.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText("Error loading coordinators: " + error.getMessage());
                        Log.e("AddCoordinator", "Error fetching coordinators: " + error.getMessage());
                    }
                };

                coordinatorsRef.addValueEventListener(coordinatorListener);

                dialog.setOnDismissListener(d -> {
                    coordinatorsRef.removeEventListener(coordinatorListener);
                    Log.d("AddCoordinator", "Coordinator listener removed on dialog dismiss");
                });

                cancelButton.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e("AddCoordinator", "Error fetching teacher data: " + error.getMessage() + ", teacherId: " + teacherId);
                Toast.makeText(requireContext(), "Error checking advisor status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addStudentAsCoordinator(String studentId, String studentName) {
        if (!isAdded() || eventRef == null) return;

        DatabaseReference coordinatorsRef = eventRef.child("eventCoordinators").child(studentId);
        coordinatorsRef.setValue(true)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), studentName + " added as coordinator", Toast.LENGTH_SHORT).show();
                        getTotalCoordinators(eventUID);
                    } else {
                        Toast.makeText(requireContext(), "Failed to add " + studentName + " as coordinator", Toast.LENGTH_SHORT).show();
                        Log.e("AddCoordinator", "Error adding coordinator: " + task.getException());
                    }
                });
    }

    private void showCoordinatorsDialog() {
        if (eventUID == null || eventUID.isEmpty() || !isAdded()) {
            Toast.makeText(getContext(), "Event ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CoordinatorDebug", "Starting showCoordinatorsDialog for event ID: " + eventUID);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_show_coordinator, null);
        builder.setView(dialogView);

        RecyclerView coordinatorsRecyclerView = dialogView.findViewById(R.id.coordinatorsRecyclerView);
        TextView emptyTextView = dialogView.findViewById(R.id.emptyTextView);
        ImageButton closeButton = dialogView.findViewById(R.id.closeButton);
        ProgressBar loadingProgressBar = dialogView.findViewById(R.id.loadingProgressBar);
        SwitchCompat allowRegistrationSwitch = dialogView.findViewById(R.id.allowRegistrationSwitch);
        TextView scanStatusText = dialogView.findViewById(R.id.scanStatusText);

        if (coordinatorsRecyclerView == null || emptyTextView == null || closeButton == null || loadingProgressBar == null || allowRegistrationSwitch == null || scanStatusText == null) {
            Log.e("CoordinatorDebug", "One or more dialog views are null");
            Toast.makeText(getContext(), "Error setting up dialog", Toast.LENGTH_SHORT).show();
            return;
        }

        List<CoordinatorAdapter.CoordinatorItem> coordinatorItems = new ArrayList<>();
        CoordinatorAdapter coordinatorAdapter = new CoordinatorAdapter((studentId, studentName) -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to remove " + studentName + " as an assistant for this event?")
                    .setPositiveButton("Yes", (dialog, which) -> removeCoordinatorFromEvent(studentId, studentName))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        coordinatorsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coordinatorsRecyclerView.setAdapter(coordinatorAdapter);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        DatabaseReference eventRef = FirebaseDatabase.getInstance().getReference("events").child(eventUID);

        // Fetch and set scanPermission for the switch
        scanPermissionListener = eventRef.child("scanPermission").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Boolean isScanPermissionAllowed = snapshot.getValue(Boolean.class);
                if (isScanPermissionAllowed == null) {
                    isScanPermissionAllowed = false;
                }
                allowRegistrationSwitch.setOnCheckedChangeListener(null);
                allowRegistrationSwitch.setChecked(isScanPermissionAllowed);
                scanStatusText.setText(isScanPermissionAllowed ? "Ticket scanning is currently ENABLED" : "Ticket scanning is currently DISABLED");
                scanStatusText.setTextColor(getResources().getColor(isScanPermissionAllowed ? R.color.green : R.color.red));
                setScanPermissionSwitchListener(allowRegistrationSwitch);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Log.e("FirebaseError", "Failed to read scan permission status: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load scan permission status", Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch coordinators and their names
        coordinatorListener = eventRef.child("eventCoordinators").orderByValue().equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return;
                coordinatorItems.clear();
                List<String> coordinatorIds = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String studentId = snapshot.getKey();
                    if (studentId != null) {
                        coordinatorIds.add(studentId);
                    }
                }
                Log.d("CoordinatorDebug", "Fetched coordinator IDs: " + coordinatorIds);

                if (coordinatorIds.isEmpty()) {
                    loadingProgressBar.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    coordinatorsRecyclerView.setVisibility(View.GONE);
                    coordinatorAdapter.setData(coordinatorItems);
                    emptyTextView.setText("No student assistants added.");
                    return;
                }

                // Fetch student names from the students node
                DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("students");
                studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                        if (!isAdded()) return;
                        Map<String, Map<String, List<CoordinatorAdapter.CoordinatorItem>>> coordinatorsByGradeAndSection = new HashMap<>();

                        for (String studentId : coordinatorIds) {
                            DataSnapshot studentData = studentSnapshot.child(studentId);
                            String firstName = studentData.child("firstName").getValue(String.class);
                            String lastName = studentData.child("lastName").getValue(String.class);
                            String middleName = studentData.child("middleName").getValue(String.class);
                            String yearLevel = studentData.child("yearLevel").getValue(String.class);
                            String section = studentData.child("section").getValue(String.class);

                            if (firstName == null || lastName == null || yearLevel == null || section == null) {
                                Log.d("CoordinatorDebug", "Skipping student " + studentId + ": missing data (firstName=" + firstName + ", lastName=" + lastName + ", yearLevel=" + yearLevel + ", section=" + section + ")");
                                continue;
                            }

                            String displayName = lastName + ", " + firstName +
                                    (middleName != null && !middleName.isEmpty() ? " " + middleName.charAt(0) + "." : "");
                            CoordinatorAdapter.CoordinatorItem coordinatorItem = new CoordinatorAdapter.CoordinatorItem(studentId, displayName, yearLevel);

                            if (!coordinatorsByGradeAndSection.containsKey(yearLevel)) {
                                coordinatorsByGradeAndSection.put(yearLevel, new HashMap<>());
                            }
                            if (!coordinatorsByGradeAndSection.get(yearLevel).containsKey(section)) {
                                coordinatorsByGradeAndSection.get(yearLevel).put(section, new ArrayList<>());
                            }
                            coordinatorsByGradeAndSection.get(yearLevel).get(section).add(coordinatorItem);
                        }

                        // Sort grades and sections, create grouped data with headers
                        List<String> sortedGrades = new ArrayList<>(coordinatorsByGradeAndSection.keySet());
                        Collections.sort(sortedGrades, (g1, g2) -> {
                            try {
                                return Integer.parseInt(g1) - Integer.parseInt(g2);
                            } catch (NumberFormatException e) {
                                return g1.compareTo(g2);
                            }
                        });

                        coordinatorItems.clear();
                        for (String grade : sortedGrades) {
                            Map<String, List<CoordinatorAdapter.CoordinatorItem>> sectionsMap = coordinatorsByGradeAndSection.get(grade);
                            List<String> sortedSections = new ArrayList<>(sectionsMap.keySet());
                            Collections.sort(sortedSections);
                            for (String section : sortedSections) {
                                if (!sectionsMap.get(section).isEmpty()) {
                                    coordinatorItems.add(new CoordinatorAdapter.CoordinatorItem(grade, section));
                                    coordinatorItems.addAll(sectionsMap.get(section));
                                }
                            }
                        }

                        loadingProgressBar.setVisibility(View.GONE);
                        if (coordinatorItems.isEmpty()) {
                            emptyTextView.setVisibility(View.VISIBLE);
                            coordinatorsRecyclerView.setVisibility(View.GONE);
                            emptyTextView.setText("No student assistants added.");
                        } else {
                            emptyTextView.setVisibility(View.GONE);
                            coordinatorsRecyclerView.setVisibility(View.VISIBLE);
                        }
                        coordinatorAdapter.setData(coordinatorItems);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded()) return;
                        Log.e("FirebaseError", "Error fetching student names: " + error.getMessage());
                        loadingProgressBar.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText("Error loading student assistants");
                        coordinatorsRecyclerView.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error loading student assistant list", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isAdded()) return;
                Log.e("FirebaseError", "Error fetching coordinators: " + databaseError.getMessage());
                loadingProgressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                emptyTextView.setText("Error loading student assistants");
                coordinatorsRecyclerView.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error loading student assistant list", Toast.LENGTH_SHORT).show();
            }
        });

        closeButton.setOnClickListener(v -> {
            // Remove listeners when closing the dialog
            if (coordinatorListener != null) {
                eventRef.child("eventCoordinators").removeEventListener(coordinatorListener);
                coordinatorListener = null;
            }
            if (scanPermissionListener != null) {
                eventRef.child("scanPermission").removeEventListener(scanPermissionListener);
                scanPermissionListener = null;
            }
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialogInterface -> {
            // Ensure listeners are removed if dialog is dismissed
            if (coordinatorListener != null) {
                eventRef.child("eventCoordinators").removeEventListener(coordinatorListener);
                coordinatorListener = null;
            }
            if (scanPermissionListener != null) {
                eventRef.child("scanPermission").removeEventListener(scanPermissionListener);
                scanPermissionListener = null;
            }
        });

        dialog.show();
    }

    private void setScanPermissionSwitchListener(SwitchCompat allowRegistrationSwitch) {
        if (!isAdded() || allowRegistrationSwitch == null) return;
        allowRegistrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showEnableScanPermissionConfirmationDialog(allowRegistrationSwitch);
            } else {
                showDisableScanPermissionConfirmationDialog(allowRegistrationSwitch);
            }
        });
    }

    private void showEnableScanPermissionConfirmationDialog(SwitchCompat allowRegistrationSwitch) {
        if (!isAdded()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Enable Ticket Scanning")
                .setMessage("Are you sure you want to enable ticket scanning for this event?")
                .setPositiveButton("Yes", (dialog, which) -> updateScanPermissionStatus(true, allowRegistrationSwitch))
                .setNegativeButton("No", (dialog, which) -> {
                    allowRegistrationSwitch.setOnCheckedChangeListener(null);
                    allowRegistrationSwitch.setChecked(false);
                    setScanPermissionSwitchListener(allowRegistrationSwitch);
                })
                .setCancelable(false)
                .show();
    }

    private void showDisableScanPermissionConfirmationDialog(SwitchCompat allowRegistrationSwitch) {
        if (!isAdded()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Disable Ticket Scanning")
                .setMessage("Are you sure you want to disable ticket scanning for this event?")
                .setPositiveButton("Yes", (dialog, which) -> updateScanPermissionStatus(false, allowRegistrationSwitch))
                .setNegativeButton("No", (dialog, which) -> {
                    allowRegistrationSwitch.setOnCheckedChangeListener(null);
                    allowRegistrationSwitch.setChecked(true);
                    setScanPermissionSwitchListener(allowRegistrationSwitch);
                })
                .setCancelable(false)
                .show();
    }

    private void updateScanPermissionStatus(boolean isAllowed, SwitchCompat allowRegistrationSwitch) {
        if (!isAdded() || eventRef == null) return;
        eventRef.child("scanPermission").setValue(isAllowed)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        String message = isAllowed ? "Ticket scanning is now enabled for this event" : "Ticket scanning is now disabled for this event";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update ticket scanning status", Toast.LENGTH_SHORT).show();
                        allowRegistrationSwitch.setOnCheckedChangeListener(null);
                        allowRegistrationSwitch.setChecked(!isAllowed);
                        setScanPermissionSwitchListener(allowRegistrationSwitch);
                    }
                });
    }

    private void removeCoordinatorFromEvent(String studentId, String studentName) {
        if (!isAdded()) return;
        if (eventUID == null || eventUID.isEmpty()) {
            Toast.makeText(getContext(), "Error: Event ID is invalid!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference eventRef = FirebaseDatabase.getInstance()
                .getReference("events")
                .child(eventUID)
                .child("eventCoordinators");
        eventRef.child(studentId).removeValue().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), studentName + " removed successfully", Toast.LENGTH_SHORT).show();
                getTotalCoordinators(eventUID); // Update total coordinators count
            } else {
                Toast.makeText(getContext(), "Error removing " + studentName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePopup(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(getContext(), "No image available", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        ImageView fullScreenImageView = dialog.findViewById(R.id.fullScreenImageView);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(fullScreenImageView);
        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleEditButtonClick() {
        if (!isAdded()) return;

        if (canEditEvent) {
            if (!areTextViewsInitialized()) {
                Toast.makeText(getContext(), "Event details are not fully loaded. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

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
            intent.putExtra("TARGET_PARTICIPANTS", targetParticipant);
            startActivity(intent);
        } else {
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
                if (!isAdded()) return;
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
                                Log.d("CreatorPermission", "Teacher is the creator, showing edit button.");
                            }
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
                                Log.d("CreatorPermission", "Teacher is not the creator, hiding edit button.");
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
                            editEventButton.setImageResource(R.drawable.ic_edit);
                            editEventButton.setVisibility(View.GONE);
                            Log.d("CreatorPermission", "No 'createdBy' node found, hiding edit button.");
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

        checkCreatorPermission();
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
        if (!isAdded()) return;
        if (isRegistrationAllowed) {
            registrationStatusText.setText("Registration is currently OPEN");
            registrationStatusText.setTextColor(getResources().getColor(R.color.green));
        } else {
            registrationStatusText.setText("Registration is currently CLOSED");
            registrationStatusText.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private void setRegistrationSwitchListener() {
        if (!isAdded() || registrationSwitch == null) return;
        registrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showOpenRegistrationConfirmationDialog();
            } else {
                showCloseRegistrationConfirmationDialog();
            }
        });
    }

    private void showOpenRegistrationConfirmationDialog() {
        if (!isAdded()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Open Registration")
                .setMessage("Are you sure you want to open registration for this event?")
                .setPositiveButton("Yes", (dialog, which) -> updateRegistrationStatus(true))
                .setNegativeButton("No", (dialog, which) -> {
                    registrationSwitch.setOnCheckedChangeListener(null);
                    registrationSwitch.setChecked(false);
                    setRegistrationSwitchListener();
                })
                .setCancelable(false)
                .show();
    }

    private void showCloseRegistrationConfirmationDialog() {
        if (!isAdded()) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Close Registration")
                .setMessage("Are you sure you want to close registration for this event?")
                .setPositiveButton("Yes", (dialog, which) -> updateRegistrationStatus(false))
                .setNegativeButton("No", (dialog, which) -> {
                    registrationSwitch.setOnCheckedChangeListener(null);
                    registrationSwitch.setChecked(true);
                    setRegistrationSwitchListener();
                })
                .setCancelable(false)
                .show();
    }

    private void updateRegistrationStatus(boolean isAllowed) {
        if (!isAdded() || eventRef == null) return;
        eventRef.child("registrationAllowed").setValue(isAllowed)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        String message = isAllowed ? "Registration is now open for this event" : "Registration is now closed for this event";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update registration status", Toast.LENGTH_SHORT).show();
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
                if (!isAdded()) return;
                if (dataSnapshot.exists()) {
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
                            if (startTime != null && !startTime.isEmpty()) {
                                startTimeTextView.setText(convertTo12HourFormat(startTime));
                            } else {
                                startTimeTextView.setText("N/A");
                            }
                        }
                        if (endTimeTextView != null) {
                            String endTime = dataSnapshot.child("endTime").getValue(String.class);
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
                            if (eventFor != null && !eventFor.isEmpty()) {
                                eventForValue = eventFor; // Update eventForValue if fetched from database
                            }
                        }
                        if ((description == null || description.isEmpty()) && descriptionTextView != null) {
                            String descriptionFromDb = dataSnapshot.child("description").getValue(String.class);
                            if (descriptionFromDb != null && !descriptionFromDb.isEmpty()) {
                                descriptionTextView.setText(descriptionFromDb);
                            }
                        }
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
                if (isAdded()) {
                    Log.e("FirebaseError", "Error fetching event details: " + databaseError.getMessage());
                }
            }
        });
    }

    private void checkEventStartTimeAndUpdateRegistration(String startDate, String startTime) {
        if (!isAdded() || registrationCard == null) return;
        try {
            Log.d("EventStartTime", "Processing event start: Date=" + startDate + ", Time=" + startTime);
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

            int hour, minute;
            if (startTime.contains("AM") || startTime.contains("PM")) {
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

            if (currentTime.compareTo(eventStartTime) >= 0) {
                Log.d("EditButtonControl", "Event has started, hiding edit button");
                editEventButton.setVisibility(View.GONE);
            } else {
                Log.d("EditButtonControl", "Event not started, applying registration logic");
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
            }
        } catch (Exception e) {
            Log.e("EditButtonControl", "Error parsing date/time: " + e.getMessage(), e);
        }
    }

    private void toggleEditButtonBasedOnRegistration(boolean isRegistrationAllowed) {
        if (!isAdded() || editEventButton == null) return;
        if (!isEventCreator && !canEditEvent) {
            Log.d("EditButtonControl", "Teacher is not the creator and edit request not approved, edit button remains hidden.");
            editEventButton.setVisibility(View.GONE);
            return;
        }

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
                                Log.d("EditButtonControl", "Event not started, applying registration logic");
                                if (isRegistrationAllowed) {
                                    Log.d("EditButtonControl", "Registration is open, hiding edit button");
                                    editEventButton.setVisibility(View.GONE);
                                } else {
                                    Log.d("EditButtonControl", "Registration is closed, showing edit button");
                                    editEventButton.setVisibility(View.VISIBLE);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EditButtonControl", "Error parsing date/time: " + e.getMessage(), e);
                            if (isRegistrationAllowed) {
                                editEventButton.setVisibility(View.GONE);
                            } else {
                                editEventButton.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
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
            return time24Hour;
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

                if (isAdded() && ticketGeneratedTextView != null) {
                    String displayText;
                    if ("none".equalsIgnoreCase(targetParticipant)) {
                        displayText = String.valueOf(ticketCount);
                    } else if (targetParticipant != null && !targetParticipant.isEmpty() && !targetParticipant.equals("none")) {
                        displayText = ticketCount + "/" + targetParticipant;
                    } else {
                        displayText = String.valueOf(ticketCount);
                    }
                    ticketGeneratedTextView.setText(displayText);
                }
                Log.d("TicketCount", "Final ticket count: " + ticketCount);

                if (targetParticipant != null && !targetParticipant.isEmpty() && !targetParticipant.equals("none") && eventRef != null) {
                    try {
                        int targetParticipantValue = Integer.parseInt(targetParticipant);
                        if (ticketCount >= targetParticipantValue) {
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
                    } catch (NumberFormatException e) {
                        Log.e("TicketCount", "Invalid targetParticipant format: " + targetParticipant, e);
                    }
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
                    String value = snapshot.getValue(String.class);
                    if (value != null) {
                        targetParticipant = value;
                        Log.d("TargetParticipant", "Fetched targetParticipant: " + targetParticipant);
                    } else {
                        targetParticipant = null;
                        Log.d("TargetParticipant", "Invalid targetParticipant format: " + value);
                    }
                    getTicketCount(eventId);
                } else {
                    targetParticipant = null;
                    Log.d("TargetParticipant", "No targetParticipant found for event: " + eventId);
                    getTicketCount(eventId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TargetParticipant", "Error fetching target participant: " + error.getMessage());
                targetParticipant = null;
                if (isAdded()) {
                    getTicketCount(eventId);
                }
            }
        });
    }

    private void getTotalCoordinators(String eventId) {
        if (eventId == null || eventId.isEmpty() || totalCoordinatorTextView == null) {
            Log.e("OrganizerDetails", "Cannot fetch coordinators: eventId is null or empty or view not found");
            if (isAdded() && totalCoordinatorTextView != null) {
                totalCoordinatorTextView.setText("Event ID is invalid");
            }
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("events").child(eventId).child("eventCoordinators");
        coordinatorListener = ref.orderByValue().equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    long coordinatorCount = snapshot.getChildrenCount();
                    totalCoordinatorTextView.setText(coordinatorCount + " Student Assistant(s)");
                } else {
                    totalCoordinatorTextView.setText("0 Student Assistant(s)");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventRef != null) {
            if (coordinatorListener != null) {
                eventRef.child("eventCoordinators").removeEventListener(coordinatorListener);
                coordinatorListener = null;
                Log.d("EventDetailsFragment", "Coordinator listener removed in onDestroyView");
            }
            if (scanPermissionListener != null) {
                eventRef.child("scanPermission").removeEventListener(scanPermissionListener);
                scanPermissionListener = null;
                Log.d("EventDetailsFragment", "Scan permission listener removed in onDestroyView");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("EventDetailsFragment", "Fragment destroyed");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("EventDetailsFragment", "Fragment detached");
    }
}