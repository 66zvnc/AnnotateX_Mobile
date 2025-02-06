package com.example.annotatex_mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_READING_PROGRESS = "reading_progress";
    private static final String KEY_NEW_BOOKS = "new_books";
    private static final String KEY_LEADERBOARDS = "leaderboards";
    private static final String KEY_FRIENDS = "friends";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize views
        ImageView backButton = findViewById(R.id.backButton);
        Switch readingProgressSwitch = findViewById(R.id.readingProgressSwitch);
        Switch newBooksSwitch = findViewById(R.id.newBooksSwitch);
        Switch leaderboardsSwitch = findViewById(R.id.leaderboardsSwitch);
        Switch friendsSwitch = findViewById(R.id.friendsSwitch);

        // Load saved preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        readingProgressSwitch.setChecked(preferences.getBoolean(KEY_READING_PROGRESS, false));
        newBooksSwitch.setChecked(preferences.getBoolean(KEY_NEW_BOOKS, false));
        leaderboardsSwitch.setChecked(preferences.getBoolean(KEY_LEADERBOARDS, false));
        friendsSwitch.setChecked(preferences.getBoolean(KEY_FRIENDS, false));

        // Set up listeners
        backButton.setOnClickListener(v -> finish());

        readingProgressSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            savePreference(KEY_READING_PROGRESS, isChecked));

        newBooksSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            savePreference(KEY_NEW_BOOKS, isChecked));

        leaderboardsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            savePreference(KEY_LEADERBOARDS, isChecked));

        friendsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            savePreference(KEY_FRIENDS, isChecked));
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
} 