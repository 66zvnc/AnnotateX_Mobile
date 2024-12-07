package com.example.annotatex_mobile;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.DocumentChange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String groupId;
    private String groupName;
    private boolean isAdmin;

    private RecyclerView collaborativeBooksRecyclerView;
    private CollaborativeBooksAdapter adapter;
    private List<Book> collaborativeBooksList;

    private ImageView groupPhotoImage;
    private View memberImagesContainer;
    private ImageView memberImage1;
    private ImageView memberImage2;
    private TextView groupNameTextView;
    private ImageView goBackButton;
    private ImageView addBookButton;
    private ImageView groupInfoButton;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageInput;
    private ImageButton sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        groupPhotoImage = findViewById(R.id.groupPhotoImage);
        memberImagesContainer = findViewById(R.id.memberImagesContainer);
        memberImage1 = findViewById(R.id.memberImage1);
        memberImage2 = findViewById(R.id.memberImage2);
        groupNameTextView = findViewById(R.id.nameTextView);
        goBackButton = findViewById(R.id.goBackButton);
        addBookButton = findViewById(R.id.addBookButton);
        groupInfoButton = findViewById(R.id.groupInfoButton);

        // Initialize chat views
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

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
            intent.putExtra("isAdmin", isAdmin);
            startActivity(intent);
        });

        // Load group details including photo
        if (groupId != null) {
            loadGroupDetails();
        }

        // Set up go back button click listener
        goBackButton.setOnClickListener(v -> {
            finish(); // This will close the current activity and return to the previous one
        });

        // Set up chat RecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(auth.getCurrentUser().getUid());
        chatRecyclerView.setAdapter(chatAdapter);

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Load and listen for messages
        listenForMessages();
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
                        // Set admin status (still needed for other admin functions)
                        String currentUserId = auth.getCurrentUser().getUid();
                        isAdmin = group.getCreatedBy() != null && group.getCreatedBy().equals(currentUserId);
                        
                        groupNameTextView.setText(group.getName());
                        
                        // Check if group has a photo
                        String photoUrl = group.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // Show group photo and hide member grid
                            groupPhotoImage.setVisibility(View.VISIBLE);
                            memberImagesContainer.setVisibility(View.GONE);
                            
                            // Load group photo
                            Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .circleCrop()
                                .into(groupPhotoImage);
                        } else {
                            // Show member grid and hide group photo
                            groupPhotoImage.setVisibility(View.GONE);
                            memberImagesContainer.setVisibility(View.VISIBLE);
                            loadRandomMemberImages(group.getMembers());
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading group details", e);
                Toast.makeText(this, "Failed to load group details", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadRandomMemberImages(List<String> memberIds) {
        // Reset images
        memberImage1.setVisibility(View.GONE);
        memberImage2.setVisibility(View.GONE);

        if (memberIds.isEmpty()) return;

        // Randomly select 2 members
        List<String> shuffledMembers = new ArrayList<>(memberIds);
        Collections.shuffle(shuffledMembers);
        int displayCount = Math.min(2, shuffledMembers.size());
        List<String> selectedMembers = shuffledMembers.subList(0, displayCount);

        // Batch fetch user data
        firestore.collection("users")
            .whereIn(FieldPath.documentId(), selectedMembers)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                ImageView[] memberImages = {memberImage1, memberImage2};
                int index = 0;
                
                for (DocumentSnapshot document : querySnapshot) {
                    if (index >= displayCount) break;
                    
                    String profileImageUrl = document.getString("profileImageUrl");
                    ImageView imageView = memberImages[index++];
                    imageView.setVisibility(View.VISIBLE);

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .circleCrop()
                            .into(imageView);
                    } else {
                        imageView.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading member profile images", e);
                memberImage1.setImageResource(R.drawable.ic_default_profile);
                memberImage2.setImageResource(R.drawable.ic_default_profile);
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

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        String currentUserId = auth.getCurrentUser().getUid();
        String currentUserName = auth.getCurrentUser().getDisplayName();

        Message message = new Message(currentUserId, currentUserName, content);
        
        firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    Log.d(TAG, "Message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForMessages() {
        // First load existing messages
        firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            message.setId(doc.getId());
                            chatAdapter.addMessage(message);
                            // Mark received messages as seen
                            if (!message.getSenderId().equals(auth.getCurrentUser().getUid())) {
                                markMessageAsSeen(message.getId());
                            }
                        }
                    }
                    chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                });

        // Then listen for real-time updates
        firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for messages", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                if (message != null) {
                                    message.setId(dc.getDocument().getId());
                                    chatAdapter.addMessage(message);
                                    if (!message.getSenderId().equals(auth.getCurrentUser().getUid())) {
                                        markMessageAsSeen(message.getId());
                                    }
                                    chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                                }
                            }
                        }
                    }
                });
    }

    private void markMessageAsSeen(String messageId) {
        firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .document(messageId)
                .update("seen", true)
                .addOnFailureListener(e -> Log.e(TAG, "Error marking message as seen", e));
    }
}

