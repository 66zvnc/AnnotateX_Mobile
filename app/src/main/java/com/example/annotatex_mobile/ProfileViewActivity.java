package com.example.annotatex_mobile;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private TextView fullName, username;
    private ImageView profileImage;
    private TextView booksCount, localRank, pointsCount;
    private RecyclerView shelfRecyclerView;
    private ImageButton backButton;

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
        loadShelfBooks();
    }

    private void initializeViews() {
        fullName = findViewById(R.id.fullName);
        username = findViewById(R.id.username);
        profileImage = findViewById(R.id.profileImage);
        backButton = findViewById(R.id.backButton);
        shelfRecyclerView = findViewById(R.id.shelfRecyclerView);
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userFullName = documentSnapshot.getString("fullName");
                    String userUsername = documentSnapshot.getString("username");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                    // Update UI with null checks
                    if (fullName != null && userFullName != null) {
                        fullName.setText(userFullName);
                    }
                    if (username != null && userUsername != null) {
                        username.setText("@" + userUsername);
                    }
                    if (profileImage != null && profileImageUrl != null) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .into(profileImage);
                    }
                } else {
                    Log.d("ProfileView", "No user document found");
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ProfileView", "Error loading profile", e);
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadShelfBooks() {
        // Initialize RecyclerView with GridLayoutManager
        if (shelfRecyclerView != null) {
            shelfRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            // Add your adapter initialization and data loading here
        }
    }
} 