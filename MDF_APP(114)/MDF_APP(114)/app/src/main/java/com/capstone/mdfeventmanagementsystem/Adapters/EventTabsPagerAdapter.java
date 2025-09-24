package com.capstone.mdfeventmanagementsystem.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.capstone.mdfeventmanagementsystem.Teacher.EventDetailsFragment;
import com.capstone.mdfeventmanagementsystem.Teacher.ParticipantsFragment;

public class EventTabsPagerAdapter extends FragmentStateAdapter {
    private static final int NUM_TABS = 2;
    private String eventId;
    private String eventDescription;
    private String eventPhotoUrl;
    private String eventFor;

    public EventTabsPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                                 String eventId,
                                 String eventDescription,
                                 String eventPhotoUrl,
                                 String eventFor) {
        super(fragmentActivity);
        this.eventId = eventId;
        this.eventDescription = eventDescription;
        this.eventPhotoUrl = eventPhotoUrl;
        this.eventFor = eventFor;

    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return EventDetailsFragment.newInstance(eventDescription, eventPhotoUrl, eventId, eventFor);
            case 1:
                return ParticipantsFragment.newInstance(eventId);
            default:
                return EventDetailsFragment.newInstance(eventDescription, eventPhotoUrl, eventId, eventFor);
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }


}