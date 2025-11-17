package com.capstone.mdfeventmanagementsystem.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.capstone.mdfeventmanagementsystem.Teacher.ActiveEventsFragment;
import com.capstone.mdfeventmanagementsystem.Teacher.ExpiredEventsFragment;

public class EventsPagerAdapter extends FragmentStateAdapter {
    private static final int NUM_PAGES = 2;
    private final ActiveEventsFragment activeEventsFragment;
    private final ExpiredEventsFragment expiredEventsFragment;

    public EventsPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                              ActiveEventsFragment activeEventsFragment,
                              ExpiredEventsFragment expiredEventsFragment) {
        super(fragmentActivity);
        this.activeEventsFragment = activeEventsFragment;
        this.expiredEventsFragment = expiredEventsFragment;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return activeEventsFragment;
        } else {
            return expiredEventsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}