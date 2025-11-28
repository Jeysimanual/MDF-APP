package com.capstone.mdfeventmanagementsystem.Utilities;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import android.content.Context;

import java.util.concurrent.TimeUnit;

public class WorkManagerAllowRegistration {

    /**
     * Starts the background task to check registration status for events.
     */
    public static void startPeriodicRegistrationCheck(Context context) {
        // Create the periodic work request for the worker (check every 1 hour)
        PeriodicWorkRequest registrationCheckRequest =
                new PeriodicWorkRequest.Builder(RegistrationAllowedCheckerWorker.class, 1, TimeUnit.HOURS)
                        .build();

        // Enqueue the worker to start the background task periodically
        WorkManager.getInstance(context).enqueue(registrationCheckRequest);
    }
}
