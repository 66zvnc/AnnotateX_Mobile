package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        goBackButton.setOnClickListener(v -> finish());

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

        // Set up RecyclerView with a GridLayoutManager
        int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2; // Adjust for tablets/phones
        collaborativeBooksRecyclerView.setLayoutManager(new GridLayoutManager(this, columns));
        collaborativeBooksRecyclerView.setAdapter(adapter);

        // Load collaborative books
        loadCollaborativeBooks();

        // Add functionality to the "Add Book" button
        ImageView addBookButton = findViewById(R.id.addBookButton);
        addBookButton.setOnClickListener(v -> openBookSelectionFragment());

        // Add SearchView functionality
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBooks(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBooks(newText);
                return false;
            }
        });
    }

    private void filterBooks(String query) {
        List<Book> filteredList = new ArrayList<>();
        for (Book book : collaborativeBooksList) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(book);
            }
        }
        adapter.updateBooks(filteredList);
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
                    adapter.updateBooks(collaborativeBooksList);
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
        intent.putExtra("book", book);
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
                    adapter.updateBooks(collaborativeBooksList);
                    Toast.makeText(this, "Collaboration stopped", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to stop collaboration", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    public void addCollaborativeBook(Book book) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null || book == null) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add the current user as a collaborator
        List<String> collaborators = book.getCollaborators() != null ? new ArrayList<>(book.getCollaborators()) : new ArrayList<>();
        if (!collaborators.contains(currentUserId)) {
            collaborators.add(currentUserId);
        }

        // Add collaborators from the selected friend
        firestore.collection("users").document(currentUserId).collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot friendDoc : queryDocumentSnapshots) {
                        String friendId = friendDoc.getId();
                        if (!collaborators.contains(friendId)) {
                            collaborators.add(friendId);
                        }
                    }

                    // Update the book's collaborators
                    book.setCollaborators(collaborators);

                    // Save the collaborative book for each collaborator
                    for (String collaboratorId : collaborators) {
                        firestore.collection("users")
                                .document(collaboratorId)
                                .collection("collaborativeBooks")
                                .document(book.getId())
                                .set(book)
                                .addOnSuccessListener(aVoid -> Log.d("Collaboration", "Book added for collaborator: " + collaboratorId))
                                .addOnFailureListener(e -> Log.e("Collaboration", "Failed to add book for collaborator: " + collaboratorId, e));
                    }

                    Toast.makeText(this, "Collaborative book added successfully", Toast.LENGTH_SHORT).show();
                    loadCollaborativeBooks(); // Refresh the local library
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch friends for collaboration", Toast.LENGTH_SHORT).show();
                    Log.e("Collaboration", "Error fetching friends", e);
                });
    }

}
