package com.example.annotatex_mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddGroupMemberActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String groupId;
    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private List<Friend> friendsList;
    private Map<String, String> selectedFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_member);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        groupId = getIntent().getStringExtra("groupId");

        if (groupId == null) {
            Toast.makeText(this, "Error: Group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        friendsList = new ArrayList<>();
        selectedFriends = new HashMap<>();

        // Set up RecyclerView
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new FriendsAdapter(this, friendsList) {
            @Override
            public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
                Friend friend = friendsList.get(position);

                holder.nameTextView.setText(friend.getName());
                holder.statusTextView.setText(friend.getStatus());

                // Load friend's profile picture
                loadFriendProfilePicture(friend.getId(), holder.profileImageView);

                // Set up selection logic
                holder.itemView.setOnClickListener(v -> {
                    if (selectedFriends.containsKey(friend.getId())) {
                        selectedFriends.remove(friend.getId());
                        holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    } else {
                        selectedFriends.put(friend.getId(), friend.getName());
                        holder.itemView.setBackgroundColor(getResources().getColor(R.color.selected_background));
                    }
                });
            }
        };
        friendsRecyclerView.setAdapter(friendsAdapter);

        // Set up back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Fetch friends
        fetchFriends();

        // Add button click handler
        Button addMembersButton = findViewById(R.id.addMembersButton);
        addMembersButton.setOnClickListener(v -> {
            if (selectedFriends.isEmpty()) {
                Toast.makeText(this, "Please select friends to add", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add all selected friends to the group
            for (String friendId : selectedFriends.keySet()) {
                firestore.collection("groups")
                        .document(groupId)
                        .update("members", FieldValue.arrayUnion(friendId))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Members added successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add members", Toast.LENGTH_SHORT).show();
                            Log.e("AddGroupMember", "Error adding members", e);
                        });
            }
        });

        // Add search functionality
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFriends(newText);
                return true;
            }
        });
    }

    private void loadFriendProfilePicture(String friendId, ImageView imageView) {
        firestore.collection("users").document(friendId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .circleCrop()
                            .into(imageView);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e("AddGroupMember", "Error loading friend profile picture", e);
                imageView.setImageResource(R.drawable.ic_default_profile);
            });
    }

    private void fetchFriends() {
        String currentUserId = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    friendsList.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Friend friend = new Friend();
                        friend.setId(document.getId());
                        friend.setName(document.getString("name"));
                        friend.setProfileImageUrl(document.getString("profileImageUrl"));
                        friend.setStatus("Online"); // Or get actual status
                        friendsList.add(friend);
                    }
                    friendsAdapter.notifyDataSetChanged();
                    
                    if (friendsList.isEmpty()) {
                        Toast.makeText(this, "No friends found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading friends", Toast.LENGTH_SHORT).show();
                    Log.e("AddGroupMember", "Error loading friends", e);
                });
    }

    private void filterFriends(String query) {
        List<Friend> filteredList = new ArrayList<>();
        for (Friend friend : friendsList) {
            if (friend.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(friend);
            }
        }
        friendsAdapter.updateFriendsList(filteredList);
    }
} 