package com.example.annotatex_mobile;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollaborativeChatActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String friendId;

    private RecyclerView collaborativeBooksRecyclerView;
    private CollaborativeBooksAdapter adapter;
    private List<Book> collaborativeBooksList;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageInput;
    private ImageButton sendButton;

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
        EditText searchView = findViewById(R.id.searchView);
        searchView.setHint("Search Books");
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterBooks(searchView.getText().toString());
                return true;
            }
            return false;
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initialize chat components
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Set up chat RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        
        chatAdapter = new ChatAdapter(auth.getCurrentUser().getUid());
        chatRecyclerView.setAdapter(chatAdapter);

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Clear any existing messages before loading
        chatAdapter.clearMessages();
        
        // Start listening for messages
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
        String currentUserId = auth.getCurrentUser().getUid();
        
        Log.d("CollaborativeChatActivity", "Loading books for user: " + currentUserId);
        
        firestore.collection("users")
                .document(currentUserId)
                .collection("collaborativeBooks")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    collaborativeBooksList.clear();
                    Log.d("CollaborativeChatActivity", "Found " + querySnapshot.size() + " books");
                    
                    for (DocumentSnapshot document : querySnapshot) {
                        Map<String, Object> data = document.getData();
                        Log.d("CollaborativeChatActivity", "Raw document data: " + data);

                        String id = document.getId();
                        String coverUrl = document.getString("coverUrl");
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String userId = document.getString("userId");

                        Log.d("CollaborativeChatActivity", String.format(
                            "Processing book: %s\nCover URL: %s\nID: %s", 
                            title, coverUrl, id
                        ));

                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        collaborativeBooksList.add(book);
                    }
                    
                    adapter.updateBooks(collaborativeBooksList);
                })
                .addOnFailureListener(e -> {
                    Log.e("CollaborativeChatActivity", "Error loading books", e);
                    Toast.makeText(this, "Failed to load collaborative books", Toast.LENGTH_SHORT).show();
                });
    }

    private void openBookSelectionFragment() {
        collaborativeBooksRecyclerView.setVisibility(View.GONE);
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);

        BookSelectionFragment fragment = BookSelectionFragment.newInstance(book -> {
            // Show confirmation dialog
            new AlertDialog.Builder(CollaborativeChatActivity.this)
                .setTitle("Add Book")
                .setMessage("Do you want to collaborate on \"" + book.getTitle() + "\" with this friend?")
                .setPositiveButton("Add", (dialog, which) -> {
                    addCollaborativeBook(book);
                    dialog.dismiss();
                    // Return to the books list view
                    getSupportFragmentManager().popBackStack();
                    collaborativeBooksRecyclerView.setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container).setVisibility(View.GONE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
        });

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
        String currentUserId = auth.getCurrentUser().getUid();

        if (currentUserId == null || friendId == null || book == null) {
            Toast.makeText(this, "Invalid data for collaboration", Toast.LENGTH_SHORT).show();
            return;
        }

        // First check if the book already exists
        firestore.collection("users")
                .document(currentUserId)
                .collection("collaborativeBooks")
                .document(book.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Book bookToSave;
                    if (documentSnapshot.exists()) {
                        bookToSave = documentSnapshot.toObject(Book.class);
                        if (bookToSave == null) {
                            bookToSave = new Book(book);
                        }
                    } else {
                        bookToSave = new Book(book);
                    }

                    // Log original book data
                    Log.d("CollaborativeChatActivity", "Original book data:");
                    Log.d("CollaborativeChatActivity", "Title: " + book.getTitle());
                    Log.d("CollaborativeChatActivity", "Cover URL: " + book.getCoverImageUrl());
                    Log.d("CollaborativeChatActivity", "ID: " + book.getId());

                    // Prepare book data for saving
                    Map<String, Object> bookData = new HashMap<>();
                    bookData.put("id", bookToSave.getId());
                    bookData.put("coverUrl", book.getCoverImageUrl()); // Use original book's cover URL
                    bookData.put("pdfUrl", bookToSave.getPdfUrl());
                    bookData.put("title", bookToSave.getTitle());
                    bookData.put("author", bookToSave.getAuthor());
                    bookData.put("description", bookToSave.getDescription());
                    bookData.put("userId", currentUserId);
                    
                    // Set up collaborators
                    List<String> collaborators = new ArrayList<>();
                    collaborators.add(currentUserId);
                    collaborators.add(friendId);
                    bookData.put("collaborators", collaborators);

                    // Set up collaborations
                    Map<String, String> collaborations = new HashMap<>();
                    collaborations.put(currentUserId, "INDIVIDUAL");
                    collaborations.put(friendId, "INDIVIDUAL");
                    bookData.put("collaborations", collaborations);
                    
                    bookData.put("timestamp", System.currentTimeMillis());

                    Log.d("CollaborativeChatActivity", "Saving collaborative book data: " + bookData);

                    // Save for both users
                    saveBookForBothUsers(book, bookData, currentUserId, friendId);
                });
    }

    private void saveBookForBothUsers(Book originalBook, Map<String, Object> bookData, String currentUserId, String friendId) {
        // Save for current user
        firestore.collection("users")
                .document(currentUserId)
                .collection("collaborativeBooks")
                .document(originalBook.getId())
                .set(bookData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CollaborativeChatActivity", "Book saved for current user");
                    
                    // Save for friend
                    firestore.collection("users")
                            .document(friendId)
                            .collection("collaborativeBooks")
                            .document(originalBook.getId())
                            .set(bookData)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("CollaborativeChatActivity", "Book saved for collaborator");
                                Toast.makeText(this, "Book successfully added to collaboration!", 
                                    Toast.LENGTH_SHORT).show();
                                loadCollaborativeBooks();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CollaborativeChatActivity", "Failed to save book for collaborator", e);
                                Toast.makeText(this, "Failed to share with collaborator", 
                                    Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        String currentUserId = auth.getCurrentUser().getUid();
        String currentUserName = auth.getCurrentUser().getDisplayName();

        Message message = new Message(currentUserId, currentUserName, content);
        
        String chatId = currentUserId.compareTo(friendId) < 0 
            ? currentUserId + "_" + friendId 
            : friendId + "_" + currentUserId;

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    message.setId(documentReference.getId());  // Set the message ID
                    messageInput.setText("");
                    Log.d(TAG, "Message sent successfully with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForMessages() {
        if (friendId == null) return;

        String chatId = auth.getCurrentUser().getUid().compareTo(friendId) < 0 
            ? auth.getCurrentUser().getUid() + "_" + friendId 
            : friendId + "_" + auth.getCurrentUser().getUid();

        // First load existing messages
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            message.setId(doc.getId());  // Set the message ID
                            chatAdapter.addMessage(message);
                            // Mark received messages as seen
                            if (!message.getSenderId().equals(auth.getCurrentUser().getUid())) {
                                markMessageAsSeen(chatId, doc.getId());
                            }
                        }
                    }
                    chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                });

        // Then listen for new messages
        firestore.collection("chats")
                .document(chatId)
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
                                    message.setId(dc.getDocument().getId());  // Set the message ID
                                    chatAdapter.addMessage(message);
                                    // Mark new received messages as seen
                                    if (!message.getSenderId().equals(auth.getCurrentUser().getUid())) {
                                        markMessageAsSeen(chatId, dc.getDocument().getId());
                                    }
                                    chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                                }
                            } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                Message updatedMessage = dc.getDocument().toObject(Message.class);
                                if (updatedMessage != null) {
                                    updatedMessage.setId(dc.getDocument().getId());  // Set the message ID
                                    updateMessageInAdapter(updatedMessage);
                                }
                            }
                        }
                    }
                });
    }

    private void markMessageAsSeen(String chatId, String messageId) {
        String currentUserId = auth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("seen", true);
        updates.put("seenBy." + currentUserId, System.currentTimeMillis());

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Error marking message as seen", e));
    }

    private void updateMessageInAdapter(Message updatedMessage) {
        if (updatedMessage == null || updatedMessage.getId() == null) {
            Log.e(TAG, "Updated message or message ID is null");
            return;
        }

        for (int i = 0; i < chatAdapter.getItemCount(); i++) {
            Message message = chatAdapter.getMessage(i);
            if (message != null && message.getId() != null && 
                message.getId().equals(updatedMessage.getId())) {
                message.setSeen(updatedMessage.isSeen());
                chatAdapter.notifyItemChanged(i);
                break;
            }
        }
    }
}
