package com.capstone.mdfeventmanagementsystem.Utilities;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChangeHistoryManager {
    private static final String TAG = "ChangeHistoryManager";
    private static final String CHANGE_HISTORY_KEY = "change_history";
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long HISTORY_RETENTION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    // Class to store change history
    public static class ChangeHistoryItem {
        String node; // e.g., "events", "eventProposals", "students/certificates"
        String eventId;
        String changeType; // e.g., "added", "changed", "removed"
        long timestamp;
        String details; // JSON or string representation of changed data

        ChangeHistoryItem(String node, String eventId, String changeType, long timestamp, String details) {
            this.node = node;
            this.eventId = eventId;
            this.changeType = changeType;
            this.timestamp = timestamp;
            this.details = details;
        }
    }

    public ChangeHistoryManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.gson = new Gson();
    }

    public void saveChangeToHistory(String node, String eventId, String changeType, long timestamp, String details) {
        List<ChangeHistoryItem> changeHistory = getChangeHistory();
        changeHistory.add(0, new ChangeHistoryItem(node, eventId, changeType, timestamp, details));

        // Limit history size
        if (changeHistory.size() > MAX_HISTORY_SIZE) {
            changeHistory = changeHistory.subList(0, MAX_HISTORY_SIZE);
        }

        // Save to SharedPreferences
        String json = gson.toJson(changeHistory);
        sharedPreferences.edit().putString(CHANGE_HISTORY_KEY, json).apply();
        Log.d(TAG, "Saved change to history: node=" + node + ", eventId=" + eventId + ", changeType=" + changeType);
    }

    public List<ChangeHistoryItem> getChangeHistory() {
        String json = sharedPreferences.getString(CHANGE_HISTORY_KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<ChangeHistoryItem>>(){}.getType();
        try {
            List<ChangeHistoryItem> history = gson.fromJson(json, type);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse change history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void cleanChangeHistory() {
        List<ChangeHistoryItem> history = getChangeHistory();
        long cutoff = System.currentTimeMillis() - HISTORY_RETENTION_MS;
        history.removeIf(item -> item.timestamp < cutoff);
        String json = gson.toJson(history);
        sharedPreferences.edit().putString(CHANGE_HISTORY_KEY, json).apply();
        Log.d(TAG, "Cleaned change history, new size: " + history.size());
    }
}
