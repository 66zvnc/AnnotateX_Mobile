package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private TextView fullName, username;
    private ImageView profileImage;
    private TextView booksCount, localRank, pointsCount;
    private RecyclerView shelfRecyclerView;
    private ImageButton backButton;
    private ImageButton addToShelfButton;

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
        addToShelfButton = findViewById(R.id.addToShelfButton);
        
        // Set up click listener for add button
        addToShelfButton.setOnClickListener(v -> openBookSelection());
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
        String userId = auth.getCurrentUser().getUid();
        
        // Set up RecyclerView if not already done
        if (shelfRecyclerView.getLayoutManager() == null) {
            GridLayoutManager layoutManager = new GridLayoutManager(this, 3); // Change to 3 columns
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return 1;
                }
            });
            shelfRecyclerView.setLayoutManager(layoutManager);
        }
        
        firestore.collection("users")
                .document(userId)
                .collection("shelf")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> shelfBooks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getString("id");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String coverUrl = document.getString("coverUrl");
                        String pdfUrl = document.getString("pdfUrl");
                        String bookUserId = document.getString("userId");
                        
                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, bookUserId);
                        shelfBooks.add(book);
                        Log.d("ProfileView", "Loaded shelf book: " + title);
                    }
                    
                    // Initialize and set adapter if not already done
                    if (shelfRecyclerView.getAdapter() == null) {
                        LibraryAdapter adapter = new LibraryAdapter(this, shelfBooks, null, false);
                        shelfRecyclerView.setAdapter(adapter);
                    } else {
                        ((LibraryAdapter) shelfRecyclerView.getAdapter()).updateBooks(shelfBooks);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileView", "Error loading shelf books", e);
                    Toast.makeText(this, "Failed to load shelf books", Toast.LENGTH_SHORT).show();
                });
    }

    private void openBookSelection() {
        Intent intent = new Intent(this, BookSelectionActivity.class);
        startActivityForResult(intent, ADD_TO_SHELF_REQUEST_CODE);
    }

    private static final int ADD_TO_SHELF_REQUEST_CODE = 100;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_TO_SHELF_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("selected_book")) {
                try {
                    Book selectedBook = data.getParcelableExtra("selected_book", Book.class);
                    if (selectedBook != null) {
                        addBookToShelf(selectedBook);
                    } else {
                        Log.e("ProfileView", "Selected book is null");
                        Toast.makeText(this, "Error: Could not get book data", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("ProfileView", "Error getting parcelable extra", e);
                    Toast.makeText(this, "Error: Could not process book data", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void addBookToShelf(Book book) {
        if (book == null) {
            Log.e("ProfileView", "Cannot add null book to shelf");
            Toast.makeText(this, "Error: Invalid book data", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Log.e("ProfileView", "User ID is null");
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a Map of the book data to store in Firestore
        Map<String, Object> bookData = new HashMap<>();
        // Generate a new unique ID for the shelf book
        String shelfBookId = firestore.collection("users").document(userId).collection("shelf").document().getId();
        
        bookData.put("id", shelfBookId);
        bookData.put("originalBookId", book.getId());
        bookData.put("title", book.getTitle());
        bookData.put("author", book.getAuthor());
        bookData.put("description", book.getDescription());
        bookData.put("coverUrl", book.getCoverImageUrl());
        bookData.put("pdfUrl", book.getPdfUrl());
        bookData.put("userId", userId);
        bookData.put("timestamp", System.currentTimeMillis());
        bookData.put("isPreloaded", book.isPreloaded());

        // Log the data being saved
        Log.d("ProfileView", "Attempting to add book to shelf: " + book.getTitle());
        Log.d("ProfileView", "Book data: " + bookData.toString());

        // Add book to user's shelf collection in Firestore
        firestore.collection("users")
                .document(userId)
                .collection("shelf")
                .document(shelfBookId)  // Use the generated ID
                .set(bookData)  // Use set instead of add
                .addOnSuccessListener(documentReference -> {
                    Log.d("ProfileView", "Book successfully added to shelf: " + shelfBookId);
                    Toast.makeText(ProfileViewActivity.this, "Book added to shelf", Toast.LENGTH_SHORT).show();
                    loadShelfBooks(); // Refresh the shelf display
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileView", "Error adding book to shelf", e);
                    Toast.makeText(ProfileViewActivity.this, "Failed to add book to shelf: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
} 