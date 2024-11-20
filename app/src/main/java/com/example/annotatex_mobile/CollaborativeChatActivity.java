package com.example.annotatex_mobile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        addBookButton.setOnClickListener(v -> fetchBooksAndOpenDialog());
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
     * Fetch books from the global "books" collection based on the current user's userId
     * and display them in the selection dialog.
     */
    private void fetchBooksAndOpenDialog() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("books")
                .whereEqualTo("userId", currentUserId) // Filter books by userId
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Book book = documentSnapshot.toObject(Book.class);
                        book.setId(documentSnapshot.getId()); // Set the book ID
                        books.add(book);
                    }

                    // Add preloaded books if no user-uploaded books are found
                    if (books.isEmpty()) {
                        addPreloadedBooks(books);
                    }

                    // Pass the context and books list to the dialog
                    BookSelectionDialog dialog = new BookSelectionDialog(this, books);
                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch books", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Add preloaded books to the list if no user-uploaded books exist.
     */
    private void addPreloadedBooks(List<Book> books) {
        books.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
        books.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
        books.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        books.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
        books.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));
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
