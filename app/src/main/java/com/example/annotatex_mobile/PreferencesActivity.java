package com.example.annotatex_mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class PreferencesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_SUGGEST_BOOKS = "suggestBooks";
    private Switch suggestBooksSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        suggestBooksSwitch = findViewById(R.id.suggestBooksSwitch);

        // Load preference
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean suggestBooks = preferences.getBoolean(KEY_SUGGEST_BOOKS, true);
        suggestBooksSwitch.setChecked(suggestBooks);

        suggestBooksSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_SUGGEST_BOOKS, isChecked);
            editor.apply();
        });
    }
}
