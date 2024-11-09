package com.example.annotatex_mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
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

        // Load preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean syncContacts = preferences.getBoolean(KEY_SYNC_CONTACTS, false);
        boolean syncFacebookFriends = preferences.getBoolean(KEY_SYNC_FACEBOOK_FRIENDS, false);

        // Set the switches' initial state
        syncContactsSwitch.setChecked(syncContacts);
        syncFacebookSwitch.setChecked(syncFacebookFriends);

        // Set listeners for the switches
        syncContactsSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SYNC_CONTACTS, isChecked);
            editor.apply();
        });

        syncFacebookSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SYNC_FACEBOOK_FRIENDS, isChecked);
            editor.apply();
        });
    }
}
