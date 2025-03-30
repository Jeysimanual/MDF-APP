package com.capstone.mdfeventmanagementsystem;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class MDFEventApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
