package com.example.annotatex_mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "friend_requests_channel";
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration friendRequestListener;
    private Fragment activeFragment; // Keep track of the active fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase App
        FirebaseApp.initializeApp(this);

        // Initialize Play Integrity for App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Set up bottom navigation view
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Prevent reloading the same fragment
            if (itemId == R.id.nav_library && !(activeFragment instanceof LibraryFragment)) {
                selectedFragment = new LibraryFragment();
            } else if (itemId == R.id.nav_categories && !(activeFragment instanceof CategoriesFragment)) {
                selectedFragment = new CategoriesFragment();
            } else if (itemId == R.id.nav_add_book && !(activeFragment instanceof PdfViewerFragment)) {
                selectedFragment = new PdfViewerFragment();
            } else if (itemId == R.id.nav_friends && !(activeFragment instanceof FriendsFragment)) {
                selectedFragment = new FriendsFragment();
            } else if (itemId == R.id.nav_profile && !(activeFragment instanceof ProfileFragment)) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                // Replace the fragment and update the active fragment reference
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                activeFragment = selectedFragment;
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            handleIntentNavigation(getIntent());
        }

        // Set up the notification channel for friend requests
        createNotificationChannel();

        // Start listening for friend requests
        listenForFriendRequests();
    }

    private void listenForFriendRequests() {
        String currentUserId = auth.getCurrentUser().getUid();

        friendRequestListener = firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) return;

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String senderName = document.getString("senderName");
                        if (senderName != null && !senderName.isEmpty()) {
                            showSystemNotification(senderName + " sent you a friend request!");
                        }
                    }
                });
    }

    private void showSystemNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("notification_type", "friend_request"); // Pass notification type
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Friend Request")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

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

    private void openNotificationsFragment() {
        Fragment notificationsFragment = new NotificationsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, notificationsFragment)
                .commit();
        activeFragment = notificationsFragment; // Update active fragment
    }

    private void handleIntentNavigation(Intent intent) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (intent != null && "friend_request".equals(intent.getStringExtra("notification_type"))) {
            openNotificationsFragment(); // Navigate to NotificationsFragment
        } else if (intent != null && "ProfileFragment".equals(intent.getStringExtra("navigateTo"))) {
            // Handle navigation back to ProfileFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_library); // Default tab
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentNavigation(intent);
    }

    @Override
    public void onBackPressed() {
        if (activeFragment instanceof ProfileFragment) {
            super.onBackPressed(); // Exit the app if on the ProfileFragment
        } else {
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            bottomNav.setSelectedItemId(R.id.nav_profile);
            Fragment profileFragment = new ProfileFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .commit();
            activeFragment = profileFragment; // Set active fragment to ProfileFragment
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendRequestListener != null) {
            friendRequestListener.remove();
        }
    }
}
