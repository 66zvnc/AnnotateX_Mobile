package com.example.annotatex_mobile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupCreationActivity extends AppCompatActivity {

    private EditText groupNameEditText;
    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<Friend> friendsList;
    private Map<String, String> selectedFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creation);

        // Initialize views
        groupNameEditText = findViewById(R.id.groupNameEditText);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        Button createGroupButton = findViewById(R.id.createGroupButton);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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

        // Fetch friends and display in RecyclerView
        fetchFriends();

        // Handle group creation
        createGroupButton.setOnClickListener(v -> createGroup());

        // Set up search functionality
        setupSearch();
    }

    private void fetchFriends() {
        String currentUserId = auth.getCurrentUser().getUid();

        firestore.collection("users").document(currentUserId).collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    friendsList.clear();
                    querySnapshot.forEach(doc -> {
                        Friend friend = doc.toObject(Friend.class);
                        friend.setId(doc.getId());
                        friendsList.add(friend);
                    });
                    friendsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load friends", Toast.LENGTH_SHORT).show());
    }

    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(groupName)) {
            groupNameEditText.setError("Group name is required");
            return;
        }

        if (selectedFriends.isEmpty()) {
            Toast.makeText(this, "Select at least one friend to create a group", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        List<String> memberIds = new ArrayList<>(selectedFriends.keySet());
        memberIds.add(currentUserId); // Add current user to the group

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", groupName);
        groupData.put("members", memberIds);
        groupData.put("createdAt", System.currentTimeMillis());
        groupData.put("createdBy", currentUserId);

        firestore.collection("groups")
                .add(groupData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("GroupCreation", "Group created with ID: " + documentReference.getId());
                    Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupCreation", "Error creating group", e);
                    Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show();
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
                Log.e("GroupCreation", "Error loading friend profile picture", e);
                imageView.setImageResource(R.drawable.ic_default_profile);
            });
    }

    private void setupSearch() {
        EditText searchView = findViewById(R.id.searchView);
        searchView.setHint("Search for friends");
        
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterFriends(searchView.getText().toString());
                return true;
            }
            return false;
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterFriends(String query) {
        List<Friend> filteredList = new ArrayList<>();
        for (Friend friend : friendsList) {
            if (friend.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(friend);
            }
        }
        friendsAdapter.updateFriends(filteredList);
    }
}
