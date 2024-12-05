package com.example.annotatex_mobile;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String groupId;
    private String groupName;

    private RecyclerView collaborativeBooksRecyclerView;
    private CollaborativeBooksAdapter adapter;
    private List<Book> collaborativeBooksList;

    private ImageView groupImageView;
    private TextView groupNameTextView;
    private ImageView goBackButton;
    private ImageView addBookButton;
    private ImageView profileImageView;
    private ImageView groupInfoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        profileImageView = findViewById(R.id.profileImageView);
        groupNameTextView = findViewById(R.id.nameTextView);
        goBackButton = findViewById(R.id.goBackButton);
        addBookButton = findViewById(R.id.addBookButton);
        groupInfoButton = findViewById(R.id.groupInfoButton);

        // Get group ID from intent
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Error: Group ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up group info button click listener
        groupInfoButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupDetailsActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });

        // Rest of your initialization code...
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

    private void loadGroupDetails() {
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null) {
                        // Set group name
                        groupNameTextView.setText(group.getName());
                        
                        // Load first member's profile picture as group picture
                        if (group.getMembers() != null && !group.getMembers().isEmpty()) {
                            String firstMemberId = group.getMembers().get(0);
                            loadMemberProfilePicture(firstMemberId);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading group details", e);
                Toast.makeText(this, "Failed to load group details", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadMemberProfilePicture(String memberId) {
        firestore.collection("users").document(memberId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        // Load profile image with Glide
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .circleCrop()
                            .into(profileImageView);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading member profile picture", e);
                profileImageView.setImageResource(R.drawable.ic_default_profile);
            });
    }

    private void loadCollaborativeBooks() {
        firestore.collection("groups")
                .document(groupId)
                .collection("collaborativeBooks")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    collaborativeBooksList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getString("id");
                        String coverUrl = document.getString("coverUrl");
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String userId = document.getString("userId");

                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        collaborativeBooksList.add(book);
                    }
                    adapter.updateBooks(collaborativeBooksList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load books: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("GroupChatActivity", "Error loading books", e);
                });
    }

    private void openBookDetails(Book book) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    private void stopCollaboration(Book book) {
        firestore.collection("groups").document(groupId)
                .collection("collaborativeBooks").document(book.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    collaborativeBooksList.remove(book);
                    adapter.updateBooks(collaborativeBooksList);
                    Toast.makeText(this, "Book removed from collaboration", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove book", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void openBookSelectionFragment() {
        collaborativeBooksRecyclerView.setVisibility(View.GONE);
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

        BookSelectionFragment fragment = new BookSelectionFragment();
        fragment.setBookSelectedListener(new BookSelectionFragment.OnBookSelectedListener() {
            @Override
            public void onBookSelected(Book book) {
                // Show confirmation dialog
                new AlertDialog.Builder(GroupChatActivity.this)
                    .setTitle("Add Book")
                    .setMessage("Do you want to add \"" + book.getTitle() + "\" to the group?")
                    .setPositiveButton("Add", (dialog, which) -> {
                        addBookToGroup(book);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
            }
        });

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    private void addBookToGroup(Book book) {
        if (groupId == null || book == null) {
            Log.e("GroupChatActivity", "Invalid data - groupId: " + groupId + ", book: " + book);
            Toast.makeText(this, "Invalid data for collaboration", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        Log.d("GroupChatActivity", "Adding book: " + book.getTitle() + " to group: " + groupId);

        // Create a map of the book data
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("id", book.getId());
        bookData.put("coverUrl", book.getCoverImageUrl());
        bookData.put("pdfUrl", book.getPdfUrl());
        bookData.put("title", book.getTitle());
        bookData.put("author", book.getAuthor());
        bookData.put("description", book.getDescription());
        bookData.put("userId", book.getUserId());
        bookData.put("addedAt", System.currentTimeMillis());
        bookData.put("addedBy", currentUserId);

        // Add the book to the group's collaborative books collection
        firestore.collection("groups")
                .document(groupId)
                .collection("collaborativeBooks")
                .document(book.getId())
                .set(bookData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("GroupChatActivity", "Successfully added book to group");
                    Toast.makeText(this, "Book added successfully", Toast.LENGTH_SHORT).show();
                    // Return to the books list view
                    getSupportFragmentManager().popBackStack();
                    collaborativeBooksRecyclerView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container).setVisibility(View.GONE);
                    // Refresh the books list
                    loadCollaborativeBooks();
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupChatActivity", "Error adding book to group", e);
                    Toast.makeText(this, "Failed to add book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
