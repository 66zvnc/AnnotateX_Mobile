package com.example.annotatex_mobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_DARK_MODE = "darkMode";
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile_settings);

        Switch darkModeSwitch = findViewById(R.id.darkModeSwitch);
        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);
        Button preferencesButton = findViewById(R.id.preferencesButton);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Load dark mode preference
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        darkModeSwitch.setChecked(isDarkMode);
        applyDarkMode(isDarkMode);

        // Dark mode switch listener
        darkModeSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();
            applyDarkMode(isChecked);
        });

        // Delete account button listener with confirmation dialog
        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Preferences button listener
        preferencesButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsProfileActivity.this, PreferencesActivity.class);
            startActivity(intent);
        });
    }

    private void applyDarkMode(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes, Im sure", (dialog, which) -> deleteAccount())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            firestore.collection("users").document(userId).delete()
                    .addOnSuccessListener(aVoid -> {
                        user.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(SettingsProfileActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(SettingsProfileActivity.this, LoginActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(SettingsProfileActivity.this, "Failed to delete account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SettingsProfileActivity.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No user is currently logged in.", Toast.LENGTH_SHORT).show();
        }
    }
}
