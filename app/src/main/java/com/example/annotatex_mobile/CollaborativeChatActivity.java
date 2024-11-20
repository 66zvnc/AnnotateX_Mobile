package com.example.annotatex_mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CollaborativeChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String friendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaborative_chat);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        ImageView profileImageView = findViewById(R.id.profileImageView);
        TextView nameTextView = findViewById(R.id.nameTextView);
        ImageView goBackButton = findViewById(R.id.goBackButton);

        // Get friend/user ID passed from the previous activity
        friendId = getIntent().getStringExtra("friendId");

        if (friendId != null) {
            loadUserProfile(friendId, profileImageView, nameTextView);
        }

        // Set up "Go Back" button functionality
        goBackButton.setOnClickListener(v -> onBackPressed());

        // Add functionality to the "Add Book" button
        ImageView addBookButton = findViewById(R.id.addBookButton);
        addBookButton.setOnClickListener(v -> openBookSelectionFragment());
    }

    /**
     * Method to load the user's profile from Firestore.
     */
    private void loadUserProfile(String userId, ImageView profileImageView, TextView nameTextView) {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("username");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Set profile name and image
                        nameTextView.setText(userName);
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Open the BookSelectionFragment to allow the user to select a book.
     */
    private void openBookSelectionFragment() {
        BookSelectionFragment fragment = new BookSelectionFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Save the cover image locally for faster loading and offline access.
     */
    private void saveCoverImageLocally(Context context, Book book) {
        if (book.hasUrlCover()) {
            try {
                String imageUrl = book.getCoverImageUrl();
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                File file = new File(context.getFilesDir(), book.getId() + ".png");
                try (FileOutputStream output = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                }

                book.setCoverImageLocalPath(file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a collaborative book for both the current user and their friend.
     */
    public void addCollaborativeBook(Book book) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null || friendId == null) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add the collaborative book to the current user's library
        firestore.collection("users")
                .document(currentUserId)
                .collection("collaborativeBooks")
                .document(book.getId())
                .set(book)
                .addOnSuccessListener(aVoid -> {
                    // Add the collaborative book to the friend's library
                    firestore.collection("users")
                            .document(friendId)
                            .collection("collaborativeBooks")
                            .document(book.getId())
                            .set(book)
                            .addOnSuccessListener(aVoid1 -> Toast.makeText(this, "Collaborative book added", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to add collaborative book to friend's library", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add collaborative book to your library", Toast.LENGTH_SHORT).show());
    }
}
