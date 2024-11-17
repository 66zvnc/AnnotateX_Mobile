package com.example.annotatex_mobile;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "friend_requests_channel";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Request notification permission for Android 13+
        askNotificationPermission();

        // Retrieve and save FCM token
        getAndSaveFcmToken();

        // Initialize bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_library);
        }

        // Create notification channel
        createNotificationChannel();
    }

    /**
     * Request notification permission for Android 13+ (API 33+)
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Explain why the permission is needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    Log.d("MainActivity", "Showing rationale for notification permission");
                }

                // Request the permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d("MainActivity", "Notification permission already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted");
            } else {
                Log.d("MainActivity", "Notification permission denied");
            }
        }
    }

    /**
     * Retrieve the FCM token and save it to Firestore
     */
    private void getAndSaveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("MainActivity", "FCM Token: " + token);

                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        String currentUserId = currentUser.getUid();
                        firestore.collection("users")
                                .document(currentUserId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Token saved to Firestore"))
                                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to save token", e));
                    }
                });
    }

    /**
     * Creates a notification channel for friend request notifications
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Friend Requests";
            String description = "Notifications for friend requests";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;

        if (item.getItemId() == R.id.nav_library) {
            selectedFragment = new LibraryFragment();
        } else if (item.getItemId() == R.id.nav_categories) {
            selectedFragment = new CategoriesFragment();
        } else if (item.getItemId() == R.id.nav_add_book) {
            PdfViewerFragment pdfViewerFragment = new PdfViewerFragment();
            pdfViewerFragment.setFirestore(firestore);
            pdfViewerFragment.setShouldLoadPdf(false);
            selectedFragment = pdfViewerFragment;
        } else if (item.getItemId() == R.id.nav_friends) {
            selectedFragment = new FriendsFragment();
        } else if (item.getItemId() == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        } else {
            Log.e("MainActivity", "Unknown navigation item selected");
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        }
        return false;
    };
}
