package com.example.annotatex_mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_DARK_MODE = "darkMode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile_settings);

        Switch darkModeSwitch = findViewById(R.id.darkModeSwitch);

        // Load dark mode preference
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        darkModeSwitch.setChecked(isDarkMode);

        // Apply dark mode based on the saved preference
        applyDarkMode(isDarkMode);

        // Set up the switch listener
        darkModeSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            // Save the dark mode preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();

            // Apply the selected mode
            applyDarkMode(isChecked);
        });
    }

    private void applyDarkMode(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
