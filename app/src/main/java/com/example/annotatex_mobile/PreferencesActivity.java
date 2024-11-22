package com.example.annotatex_mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class PreferencesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_SUGGEST_BOOKS = "suggestBooks";
    private Switch suggestBooksSwitch;
    private ImageView goBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Initialize UI components
        suggestBooksSwitch = findViewById(R.id.suggestBooksSwitch);
        goBackButton = findViewById(R.id.goBackButton);

        // Load preference
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean suggestBooks = preferences.getBoolean(KEY_SUGGEST_BOOKS, true);
        suggestBooksSwitch.setChecked(suggestBooks);

        suggestBooksSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SUGGEST_BOOKS, isChecked);
            editor.apply();
        });

        // Handle "Go Back" button click
        goBackButton.setOnClickListener(v -> {
            // Close the activity to return to the previous screen
            finish();
        });
    }
}
