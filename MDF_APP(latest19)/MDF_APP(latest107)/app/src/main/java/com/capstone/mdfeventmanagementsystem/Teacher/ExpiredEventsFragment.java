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
import com.capstone.mdfeventmanagementsystem.Adapters.EventAdapterTeacher;
import com.capstone.mdfeventmanagementsystem.R;
import com.capstone.mdfeventmanagementsystem.Student.Event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExpiredEventsFragment extends Fragment {
    private static final String TAG = "ExpiredEventsFragment";
    private RecyclerView recyclerView;
    private EventAdapterTeacher eventAdapterTeacher;
    private List<Event> expiredEventList;
    private TextView noEventMessage;
    private EventAdapterTeacher.OnEventClickListener onEventClickListener;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ExpiredEventsFragment() {
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
        return inflater.inflate(R.layout.fragment_expired_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewExpiredEvents);
        noEventMessage = view.findViewById(R.id.noExpiredEventMessage);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expiredEventList = new ArrayList<>();

        eventAdapterTeacher = new EventAdapterTeacher(getContext(), expiredEventList, onEventClickListener);
        recyclerView.setAdapter(eventAdapterTeacher);

        // Initially show the no events message
        noEventMessage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    public void updateExpiredEvents(List<Event> expiredEvents) {
        if (expiredEventList == null) {
            expiredEventList = new ArrayList<>();
        }

        // Clear and add all expired events
        expiredEventList.clear();
        expiredEventList.addAll(expiredEvents);

        // Log for debugging
        Log.d(TAG, "Updating expired events list with " + expiredEventList.size() + " events");

        // Update UI based on whether we have expired events
        if (expiredEventList.isEmpty()) {
            if (noEventMessage != null) {
                Log.d(TAG, "No expired events found, showing message");
                noEventMessage.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (noEventMessage != null) {
                Log.d(TAG, "Expired events found, hiding message");
                noEventMessage.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
        }

        // Update adapter
        if (eventAdapterTeacher != null) {
            eventAdapterTeacher.updateEventList(expiredEventList);
            eventAdapterTeacher.notifyDataSetChanged();
        }
    }
}