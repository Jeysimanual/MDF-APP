package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RegistrationAllowedCheckerWorker extends Worker {

    private static final String TAG = "regWorker";

    public RegistrationAllowedCheckerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: Worker started to check registration status...");

        final boolean[] success = {false};

        // Use RegistrationAllowedChecker to check and disable expired registrations
        RegistrationAllowedChecker.checkAndDisableExpiredRegistrations(() -> {
            Log.d(TAG, "checkAndDisableExpiredRegistrations completed.");
            success[0] = true;
        });

        // Wait for the task to complete
        try {
            // Simulate waiting for async task completion
            Thread.sleep(5000); // Adjust as necessary based on how long the check takes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Worker interrupted: " + e.getMessage());
        }

        // Return success or retry based on the result
        return success[0] ? Result.success() : Result.retry();
    }
}
