package com.capstone.mdfeventmanagementsystem.Student;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentActiveEventsFragment extends Fragment implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private DatabaseReference databaseReference;
    private TextView noActiveEventsMessage;
    private TextView noEventsFilterMessage;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String selectedEventType = "All";
    private String selectedEventFor = "All";
    private LocalDate filterStartDate = null;
    private LocalDate filterEndDate = null;

    public StudentActiveEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_active_events, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewEvents);
        noActiveEventsMessage = view.findViewById(R.id.noActiveEventsMessage);
        noEventsFilterMessage = view.findViewById(R.id.noEventsFilterMessage);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(getContext(), eventList, this);
        recyclerView.setAdapter(eventAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("events");

        // Get filter values from arguments (if passed)
        Bundle args = getArguments();
        if (args != null) {
            selectedEventType = args.getString("eventType", "All");
            selectedEventFor = args.getString("eventFor", "All");
            String startDate = args.getString("startDate");
            String endDate = args.getString("endDate");
            if (startDate != null) filterStartDate = LocalDate.parse(startDate, DATE_FORMATTER);
            if (endDate != null) filterEndDate = LocalDate.parse(endDate, DATE_FORMATTER);
        }

        loadEvents();
        return view;
    }

    private void loadEvents() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventList.clear();
                LocalDate today = LocalDate.now();
                boolean hasActiveEvents = false;
                boolean hasFilteredEvents = false;

                // First pass: Check if any active events exist (before filters)
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        try {
                            LocalDate eventEndDate = LocalDate.parse(event.getEndDate(), DATE_FORMATTER);
                            if (!eventEndDate.isBefore(today)) {
                                hasActiveEvents = true;
                                break;
                            }
                        } catch (Exception e) {
                            // Handle parsing errors
                        }
                    }
                }

                // Second pass: Apply filters and collect matching events
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null) {
                        try {
                            LocalDate eventEndDate = LocalDate.parse(event.getEndDate(), DATE_FORMATTER);
                            LocalDate eventStartDate = LocalDate.parse(event.getStartDate(), DATE_FORMATTER);
                            boolean isActive = !eventEndDate.isBefore(today);

                            if (isActive) {
                                boolean matchesFilters = true;

                                if (!selectedEventType.equals("All") && !event.getEventType().equalsIgnoreCase(selectedEventType)) {
                                    matchesFilters = false;
                                }

                                if (!selectedEventFor.equals("All")) {
                                    String eventFor = event.getEventFor();
                                    if (eventFor != null) {
                                        String[] eventForGrades = eventFor.split(",\\s*");
                                        boolean gradeMatch = false;
                                        for (String grade : eventForGrades) {
                                            if (grade.trim().equalsIgnoreCase(selectedEventFor) ||
                                                    grade.trim().toLowerCase().contains("all") ||
                                                    grade.trim().toLowerCase().contains("everyone")) {
                                                gradeMatch = true;
                                                break;
                                            }
                                        }
                                        if (!gradeMatch) {
                                            matchesFilters = false;
                                        }
                                    } else {
                                        matchesFilters = false;
                                    }
                                }

                                if (filterStartDate != null && eventEndDate.isBefore(filterStartDate)) {
                                    matchesFilters = false;
                                }
                                if (filterEndDate != null && eventStartDate.isAfter(filterEndDate)) {
                                    matchesFilters = false;
                                }

                                if (matchesFilters) {
                                    event.setEventUID(dataSnapshot.getKey());
                                    eventList.add(event);
                                    hasFilteredEvents = true;
                                }
                            }
                        } catch (Exception e) {
                            // Handle parsing errors
                        }
                    }
                }

                Collections.sort(eventList, (e1, e2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(e1.getStartDate(), DATE_FORMATTER);
                        LocalDate date2 = LocalDate.parse(e2.getStartDate(), DATE_FORMATTER);
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        return 0;
                    }
                });

                eventAdapter.updateEventList(eventList);

                if (hasFilteredEvents) {
                    noActiveEventsMessage.setVisibility(View.GONE);
                    noEventsFilterMessage.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    if (!hasActiveEvents) {
                        noActiveEventsMessage.setVisibility(View.VISIBLE);
                        noEventsFilterMessage.setVisibility(View.GONE);
                    } else {
                        noActiveEventsMessage.setVisibility(View.GONE);
                        updateNoEventsFilterMessage();
                        noEventsFilterMessage.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noActiveEventsMessage.setVisibility(View.GONE);
                noEventsFilterMessage.setText("Error loading events. Please try again.");
                noEventsFilterMessage.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void updateNoEventsFilterMessage() {
        StringBuilder message = new StringBuilder("No active events found");
        if (!selectedEventType.equals("All") || !selectedEventFor.equals("All")) {
            message.append(" for ");
            if (!selectedEventType.equals("All")) {
                message.append(selectedEventType);
                if (!selectedEventFor.equals("All")) {
                    message.append(" and ");
                }
            }
            if (!selectedEventFor.equals("All")) {
                message.append(selectedEventFor);
            }
        }
        message.append(".");
        noEventsFilterMessage.setText(message.toString());
    }

    @Override
    public void onEventClick(Event event) {
        if (getActivity() instanceof StudentDashboard) {
            ((StudentDashboard) getActivity()).onEventClick(event);
        }
    }
}