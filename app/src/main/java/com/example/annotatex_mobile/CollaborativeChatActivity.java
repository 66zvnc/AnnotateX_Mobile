package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollaborativeChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String friendId;

    private RecyclerView collaborativeBooksRecyclerView;
    private CollaborativeBooksAdapter adapter;
    private List<Book> collaborativeBooksList;

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

        // RecyclerView for collaborative books
        collaborativeBooksRecyclerView = findViewById(R.id.collaborativeBooksRecyclerView);

        // Get friend/user ID passed from the previous activity
        friendId = getIntent().getStringExtra("friendId");

        if (friendId != null) {
            loadUserProfile(friendId, profileImageView, nameTextView);
        }

        // Set up "Go Back" button functionality
        goBackButton.setOnClickListener(v -> finish()); // Close the activity and go back

        // Initialize RecyclerView
        collaborativeBooksList = new ArrayList<>();
        adapter = new CollaborativeBooksAdapter(this, collaborativeBooksList, new CollaborativeBooksAdapter.OnBookInteractionListener() {
            @Override
            public void onViewDetails(Book book) {
                openBookDetails(book);
            }

            @Override
            public void onStopCollab(Book book) {
                stopCollaboration(book);
            }
        });

        // Set up RecyclerView with a GridLayoutManager for grid-style display
        int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2; // 3 columns for tablets, 2 for phones
        collaborativeBooksRecyclerView.setLayoutManager(new GridLayoutManager(this, columns));
        collaborativeBooksRecyclerView.setAdapter(adapter);

        // Load collaborative books
        loadCollaborativeBooks();

        // Add functionality to the "Add Book" button
        ImageView addBookButton = findViewById(R.id.addBookButton);
        addBookButton.setOnClickListener(v -> openBookSelectionFragment());
    }

    private void loadUserProfile(String userId, ImageView profileImageView, TextView nameTextView) {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("username");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Set profile name and image
                        nameTextView.setText(userName != null ? userName : "Unknown User");
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
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void loadCollaborativeBooks() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null || friendId == null) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch collaborative books shared by the current user and the selected friend
        firestore.collection("users").document(currentUserId)
                .collection("collaborativeBooks")
                .whereArrayContains("collaborators", friendId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    collaborativeBooksList.clear();
                    collaborativeBooksList.addAll(queryDocumentSnapshots.toObjects(Book.class));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load collaborative books", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void openBookSelectionFragment() {
        collaborativeBooksRecyclerView.setVisibility(View.GONE);
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

        BookSelectionFragment fragment = new BookSelectionFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openBookDetails(Book book) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("book", book); // Assuming `Book` implements `Serializable`
        startActivity(intent);
    }

    private void stopCollaboration(Book book) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(currentUserId)
                .collection("collaborativeBooks")
                .document(book.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    collaborativeBooksList.remove(book);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Collaboration stopped", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to stop collaboration", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    public void addCollaborativeBook(Book book) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null || friendId == null) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add collaborators to the book
        book.setCollaborators(Arrays.asList(currentUserId, friendId));

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
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Collaborative book added", Toast.LENGTH_SHORT).show();
                                loadCollaborativeBooks(); // Refresh the RecyclerView
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to add collaborative book to friend's library", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add collaborative book to your library", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

}
