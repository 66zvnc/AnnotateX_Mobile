package com.example.annotatex_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PrivacyActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PrivacyPrefs";
    private static final String KEY_SYNC_CONTACTS = "syncContacts";
    private static final String KEY_SYNC_FACEBOOK_FRIENDS = "syncFacebookFriends";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        Switch syncContactsSwitch = findViewById(R.id.syncContactsSwitch);
        Switch syncFacebookSwitch = findViewById(R.id.syncFacebookSwitch);
        ImageView goBackButton = findViewById(R.id.goBackButton);

        // Load preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean syncContacts = preferences.getBoolean(KEY_SYNC_CONTACTS, false);
        boolean syncFacebookFriends = preferences.getBoolean(KEY_SYNC_FACEBOOK_FRIENDS, false);

        // Set the switches' initial state
        syncContactsSwitch.setChecked(syncContacts);
        syncFacebookSwitch.setChecked(syncFacebookFriends);

        // Set listeners for the switches
        syncContactsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SYNC_CONTACTS, isChecked);
            editor.apply();
        });

        syncFacebookSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SYNC_FACEBOOK_FRIENDS, isChecked);
            editor.apply();
        });

        // Go Back button functionality
        goBackButton.setOnClickListener(v -> {
            // Navigate back to SettingsActivity
            Intent intent = new Intent(PrivacyActivity.this, SettingsProfileActivity.class);
            startActivity(intent);
            finish(); // Finish the current activity
        });
    }
}
