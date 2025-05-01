package com.capstone.mdfeventmanagementsystem.Teacher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapter;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActiveEventsFragment extends Fragment {
    private static final String TAG = "ActiveEventsFragment";
    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private List<Event> activeEventList;
    private TextView noEventMessage;
    private EventAdapter.OnEventClickListener onEventClickListener;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ActiveEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof EventAdapter.OnEventClickListener) {
            onEventClickListener = (EventAdapter.OnEventClickListener) context;
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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        activeEventList = new ArrayList<>();

        eventAdapter = new EventAdapter(getContext(), activeEventList, onEventClickListener);
        recyclerView.setAdapter(eventAdapter);
    }

    public void updateActiveEvents(List<Event> activeEvents) {
        if (activeEventList == null) {
            activeEventList = new ArrayList<>();
        }

        // Clear and add all active events
        activeEventList.clear();
        activeEventList.addAll(activeEvents);

        // Log for debugging
        Log.d(TAG, "Updating active events list with " + activeEventList.size() + " events");

        // Update UI based on whether we have active events
        if (activeEventList.isEmpty()) {
            if (noEventMessage != null) {
                Log.d(TAG, "No active events found, showing message");
                noEventMessage.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (noEventMessage != null) {
                Log.d(TAG, "Active events found, hiding message");
                noEventMessage.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }

        // Update adapter
        if (eventAdapter != null) {
            eventAdapter.updateEventList(activeEventList);
            eventAdapter.notifyDataSetChanged();
        }
    }
}