package com.capstone.mdfeventmanagementsystem.Teacher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActiveEventsFragment extends Fragment {
    private static final String TAG = "ActiveEventsFragment";
    private RecyclerView recyclerView;
    private EventAdapterTeacher eventAdapterTeacher;
    private List<Event> allActiveEvents; // Original full list of events
    private List<Event> filteredActiveEvents; // Filtered list to display
    private TextView noEventMessage;
    private ImageView filterImageView;
    private EventAdapterTeacher.OnEventClickListener onEventClickListener;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Filter variables
    private String selectedEventType = "All";
    private String selectedEventFor = "All";
    private LocalDate filterStartDate = null;
    private LocalDate filterEndDate = null;

    public ActiveEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof EventAdapterTeacher.OnEventClickListener) {
            onEventClickListener = (EventAdapterTeacher.OnEventClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnEventClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewActiveEvents);
        noEventMessage = view.findViewById(R.id.noActiveEventMessage);

        // Get filter icon from parent activity
        if (getActivity() != null) {
            filterImageView = getActivity().findViewById(R.id.filter);
            if (filterImageView != null) {
                filterImageView.setOnClickListener(v -> showFilterDialog());
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allActiveEvents = new ArrayList<>();
        filteredActiveEvents = new ArrayList<>();

        eventAdapterTeacher = new EventAdapterTeacher(getContext(), filteredActiveEvents, onEventClickListener);
        recyclerView.setAdapter(eventAdapterTeacher);
    }

    /**
     * Shows the filter dialog with chip groups for filtering events
     */
    private void showFilterDialog() {
        if (getContext() == null) return;

        // Create custom dialog
        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_filter_teacher);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Get references to the chip groups
        ChipGroup chipGroupEventType = dialog.findViewById(R.id.chipGroupEventType);
        ChipGroup chipGroupEventFor = dialog.findViewById(R.id.chipGroupEventFor);

        // Get references to individual chips
        Chip chipEventTypeAll = dialog.findViewById(R.id.chipEventTypeAll);
        Chip chipSeminar = dialog.findViewById(R.id.chipSeminar);
        Chip chipOffCampus = dialog.findViewById(R.id.chipOffCampus);
        Chip chipSports = dialog.findViewById(R.id.chipSports);
        Chip chipOther = dialog.findViewById(R.id.chipOther);

        Chip chipAll = dialog.findViewById(R.id.chipAll);
        Chip chipGrade7 = dialog.findViewById(R.id.chipGrade7);
        Chip chipGrade8 = dialog.findViewById(R.id.chipGrade8);
        Chip chipGrade9 = dialog.findViewById(R.id.chipGrade9);
        Chip chipGrade10 = dialog.findViewById(R.id.chipGrade10);
        Chip chipGrade11 = dialog.findViewById(R.id.chipGrade11);
        Chip chipGrade12 = dialog.findViewById(R.id.chipGrade12);

        // Set the initial state based on current selections
        // For Event Type
        switch (selectedEventType) {
            case "All":
                chipEventTypeAll.setChecked(true);
                break;
            case "Seminar":
                chipSeminar.setChecked(true);
                break;
            case "Off-Campus Activity":
                chipOffCampus.setChecked(true);
                break;
            case "Sports Event":
                chipSports.setChecked(true);
                break;
            case "Other":
                chipOther.setChecked(true);
                break;
        }

        // For Event For
        switch (selectedEventFor) {
            case "All":
                chipAll.setChecked(true);
                break;
            case "Grade-7":
                chipGrade7.setChecked(true);
                break;
            case "Grade-8":
                chipGrade8.setChecked(true);
                break;
            case "Grade-9":
                chipGrade9.setChecked(true);
                break;
            case "Grade-10":
                chipGrade10.setChecked(true);
                break;
            case "Grade-11":
                chipGrade11.setChecked(true);
                break;
            case "Grade-12":
                chipGrade12.setChecked(true);
                break;
        }

        // Initialize buttons with custom styling
        Button buttonClear = dialog.findViewById(R.id.buttonClear);
        Button buttonApply = dialog.findViewById(R.id.buttonApply);

        // Add animations to chips
        addAnimationToChips(chipGroupEventType);
        addAnimationToChips(chipGroupEventFor);

        // Clear button
        buttonClear.setOnClickListener(v -> {
            // Reset all filters to default values with animation
            buttonClear.animate().alpha(0.7f).setDuration(100).withEndAction(() -> {
                buttonClear.animate().alpha(1.0f).setDuration(100);

                selectedEventType = "All";
                selectedEventFor = "All";
                filterStartDate = null;
                filterEndDate = null;

                // Select the "All" chips
                chipEventTypeAll.setChecked(true);
                chipAll.setChecked(true);

                // Apply the reset filters immediately
                applyFilters();
                dialog.dismiss();

                // Show toast to inform user
                Toast.makeText(getContext(), "Filters have been reset", Toast.LENGTH_SHORT).show();
            }).start();
        });

        // Apply button
        buttonApply.setOnClickListener(v -> {
            // Add animation
            buttonApply.animate().alpha(0.7f).setDuration(100).withEndAction(() -> {
                buttonApply.animate().alpha(1.0f).setDuration(100);

                // Get selected values from chips
                int eventTypeCheckedId = chipGroupEventType.getCheckedChipId();
                int eventForCheckedId = chipGroupEventFor.getCheckedChipId();

                // Get the selected event type
                if (eventTypeCheckedId == R.id.chipEventTypeAll) {
                    selectedEventType = "All";
                } else if (eventTypeCheckedId == R.id.chipSeminar) {
                    selectedEventType = "Seminar";
                } else if (eventTypeCheckedId == R.id.chipOffCampus) {
                    selectedEventType = "Off-Campus Activity";
                } else if (eventTypeCheckedId == R.id.chipSports) {
                    selectedEventType = "Sports Event";
                } else if (eventTypeCheckedId == R.id.chipOther) {
                    selectedEventType = "Other";
                }

                // Get the selected event for
                if (eventForCheckedId == R.id.chipAll) {
                    selectedEventFor = "All";
                } else if (eventForCheckedId == R.id.chipGrade7) {
                    selectedEventFor = "Grade-7";
                } else if (eventForCheckedId == R.id.chipGrade8) {
                    selectedEventFor = "Grade-8";
                } else if (eventForCheckedId == R.id.chipGrade9) {
                    selectedEventFor = "Grade-9";
                } else if (eventForCheckedId == R.id.chipGrade10) {
                    selectedEventFor = "Grade-10";
                } else if (eventForCheckedId == R.id.chipGrade11) {
                    selectedEventFor = "Grade-11";
                } else if (eventForCheckedId == R.id.chipGrade12) {
                    selectedEventFor = "Grade-12";
                }

                // Apply filters
                applyFilters();
                dialog.dismiss();
            }).start();
        });

        dialog.show();
    }

    /**
     * Add click animation to all chips in a chip group
     */
    private void addAnimationToChips(ChipGroup chipGroup) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chip = chipGroup.getChildAt(i);
            if (chip instanceof Chip) {
                chip.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            break;
                    }
                    return false;
                });
            }
        }
    }

    /**
     * Apply filters to the event list
     */
    private void applyFilters() {
        if (allActiveEvents == null) {
            return;
        }

        Log.d(TAG, "Applying filters - Type: " + selectedEventType + ", For: " + selectedEventFor);
        filteredActiveEvents.clear();

        for (Event event : allActiveEvents) {
            boolean matchesFilters = true;

            // Filter by event type
            if (!selectedEventType.equals("All") && !event.getEventType().equalsIgnoreCase(selectedEventType)) {
                matchesFilters = false;
            }

            // Filter by event for
            if (!selectedEventFor.equals("All") && !event.getEventFor().equalsIgnoreCase(selectedEventFor)) {
                matchesFilters = false;
            }

            // Filter by date range - implement if needed
            // Currently not implementing date range filtering but the variables are here if needed

            if (matchesFilters) {
                filteredActiveEvents.add(event);
            }
        }

        // Update UI based on filtered results
        updateUI();
    }

    /**
     * Updates UI based on filtered events list
     */
    private void updateUI() {
        // Update adapter with filtered list
        if (eventAdapterTeacher != null) {
            eventAdapterTeacher.updateEventList(filteredActiveEvents);
            eventAdapterTeacher.notifyDataSetChanged();
        }

        // Show appropriate message if no events match filters
        if (filteredActiveEvents.isEmpty()) {
            updateNoEventsMessage();
            if (noEventMessage != null) {
                noEventMessage.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (noEventMessage != null) {
                noEventMessage.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }

        Log.d(TAG, "UI updated with " + filteredActiveEvents.size() + " events");
    }

    /**
     * Updates the no events message based on current filters
     */
    private void updateNoEventsMessage() {
        if (noEventMessage == null) return;

        StringBuilder message = new StringBuilder("No events found");

        // Add filter information to the message
        if (!selectedEventType.equals("All") || !selectedEventFor.equals("All")) {
            message.append(" for ");

            // Add event type information if filtered
            if (!selectedEventType.equals("All")) {
                message.append(selectedEventType);

                // Add "and" if both filters are applied
                if (!selectedEventFor.equals("All")) {
                    message.append(" and ");
                }
            }

            // Add event for information if filtered
            if (!selectedEventFor.equals("All")) {
                message.append(selectedEventFor);
            }
        }

        message.append(".");
        noEventMessage.setText(message.toString());
    }

    /**
     * Updates the active events list and applies any current filters
     */
    public void updateActiveEvents(List<Event> activeEvents) {
        if (allActiveEvents == null) {
            allActiveEvents = new ArrayList<>();
        }

        // Clear and add all active events to the full list
        allActiveEvents.clear();
        allActiveEvents.addAll(activeEvents);

        // Log for debugging
        Log.d(TAG, "Received " + allActiveEvents.size() + " active events, applying filters");

        // Apply current filters
        applyFilters();
    }
}