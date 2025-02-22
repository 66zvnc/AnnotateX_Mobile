package com.example.annotatex_mobile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private TextView userName, userHandle;
    private ImageView profileImage;
    private TextView booksCount, localRank, pointsCount;
    private TextView bookTitle, authorName;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        
        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Load user profile
        loadUserProfile();

        // Load other data
        loadUserStats();
        loadContinueReading();
        loadShelfBooks();
    }

    private void initializeViews() {
        userName = findViewById(R.id.userName);
        userHandle = findViewById(R.id.userHandle);
        profileImage = findViewById(R.id.profileImage);
        booksCount = findViewById(R.id.booksCount);
        localRank = findViewById(R.id.localRank);
        pointsCount = findViewById(R.id.pointsCount);
        bookTitle = findViewById(R.id.bookTitle);
        authorName = findViewById(R.id.authorName);
        backButton = findViewById(R.id.backButton);
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("fullName      ");
                        String username = document.getString("username");
                        String profileImageUrl = document.getString("profileImageUrl");

                        userName.setText(name);
                        userHandle.setText("@" + username);

                        // Load profile image using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .into(profileImage);
                        }
                    }
                });
    }

    private void loadUserStats() {
        // TODO: Implement loading user statistics from Firebase
    }

    private void loadContinueReading() {
        // TODO: Implement loading current book from Firebase
    }

    private void loadShelfBooks() {
        // TODO: Implement loading shelf books from Firebase
    }
} 