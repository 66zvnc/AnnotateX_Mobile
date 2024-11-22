package com.example.annotatex_mobile;

import android.Manifest;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsActivity extends AppCompatActivity {

    private Switch switchNotifications, switchPhotos, switchFiles, switchContacts;
    private ImageView goBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        // Initialize UI components
        switchNotifications = findViewById(R.id.switchNotifications);
        switchPhotos = findViewById(R.id.switchPhotos);
        switchFiles = findViewById(R.id.switchFiles);
        switchContacts = findViewById(R.id.switchContacts);
        goBackButton = findViewById(R.id.goBackButton);

        // Load current preferences
        loadPreferences();

        // Set listeners for switches
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableNotifications();
            } else {
                disableNotifications();
            }
            savePreferences();
        });

        switchPhotos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestPhotoPermission();
            } else {
                Toast.makeText(this, "Photo access disabled", Toast.LENGTH_SHORT).show();
            }
            savePreferences();
        });

        switchFiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestFilePermission();
            } else {
                Toast.makeText(this, "File access disabled", Toast.LENGTH_SHORT).show();
            }
            savePreferences();
        });

        switchContacts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestPermission(Manifest.permission.READ_CONTACTS, "Contacts");
            } else {
                Toast.makeText(this, "Contacts access disabled", Toast.LENGTH_SHORT).show();
            }
            savePreferences();
        });

        // Set listener for the Go Back button
        goBackButton.setOnClickListener(v -> {
            // Navigate back to the ProfileSettingsFragment
            finish(); // Closes the activity and returns to the previous one
        });
    }

    private void enableNotifications() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null && notificationManager.areNotificationsEnabled()) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enable notifications in system settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void disableNotifications() {
        Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
    }

    private void requestPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            requestPermission(Manifest.permission.READ_MEDIA_IMAGES, "Photos");
        } else {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "Photos");
        }
    }

    private void requestFilePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            requestPermission(Manifest.permission.READ_MEDIA_IMAGES, "Files");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "Files");
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Files");
        }
    }

    private void requestPermission(String permission, String type) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, type + " access already granted", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
        }
    }

    private void loadPreferences() {
        boolean allowNotifications = getSharedPreferences("AppPermissions", MODE_PRIVATE)
                .getBoolean("AllowNotifications", false);
        boolean allowPhotos = getSharedPreferences("AppPermissions", MODE_PRIVATE)
                .getBoolean("AllowPhotos", false);
        boolean allowFiles = getSharedPreferences("AppPermissions", MODE_PRIVATE)
                .getBoolean("AllowFiles", false);
        boolean allowContacts = getSharedPreferences("AppPermissions", MODE_PRIVATE)
                .getBoolean("AllowContacts", false);

        switchNotifications.setChecked(allowNotifications);
        switchPhotos.setChecked(allowPhotos);
        switchFiles.setChecked(allowFiles);
        switchContacts.setChecked(allowContacts);
    }

    private void savePreferences() {
        getSharedPreferences("AppPermissions", MODE_PRIVATE)
                .edit()
                .putBoolean("AllowNotifications", switchNotifications.isChecked())
                .putBoolean("AllowPhotos", switchPhotos.isChecked())
                .putBoolean("AllowFiles", switchFiles.isChecked())
                .putBoolean("AllowContacts", switchContacts.isChecked())
                .apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, permissions[i] + " permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, permissions[i] + " permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
