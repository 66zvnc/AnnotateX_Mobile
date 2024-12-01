package com.example.annotatex_mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendSelectionDialog {

    public interface FriendSelectionListener {
        void onFriendsSelected(Map<String, String> selectedFriends); // Map of friendId to friendName
    }

    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final List<String> preselectedFriendIds; // List of friends to preselect

    public FriendSelectionDialog(Context context, List<String> preselectedFriendIds) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.preselectedFriendIds = preselectedFriendIds != null ? preselectedFriendIds : new ArrayList<>();
    }

    public void show(FriendSelectionListener listener) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(currentUserId).collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Friend> friends = new ArrayList<>();
                    Map<String, String> selectedFriends = new HashMap<>();

                    queryDocumentSnapshots.forEach(doc -> {
                        Friend friend = new Friend(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getString("profileImageUrl"),
                                doc.getString("status"),
                                doc.getBoolean("removed") != null && doc.getBoolean("removed")
                        );
                        friends.add(friend);

                        // Automatically select preselected friends
                        if (preselectedFriendIds.contains(friend.getId())) {
                            selectedFriends.put(friend.getId(), friend.getName());
                        }
                    });

                    if (friends.isEmpty()) {
                        Toast.makeText(context, "No friends available to share with", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Inflate the dialog layout
                    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_friend_selection, null);

                    RecyclerView recyclerView = dialogView.findViewById(R.id.friendsRecyclerView);
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));

                    // Use FriendsAdapter to display friends in the dialog
                    FriendsAdapter adapter = new FriendsAdapter(context, friends) {
                        @Override
                        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
                            super.onBindViewHolder(holder, position);

                            Friend currentFriend = friends.get(position);

                            // Highlight the user if selected, reset to normal if not
                            if (selectedFriends.containsKey(currentFriend.getId())) {
                                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.selected_background));
                            } else {
                                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                            }

                            // Add click listener for each friend to toggle selection
                            holder.itemView.setOnClickListener(v -> {
                                if (selectedFriends.containsKey(currentFriend.getId())) {
                                    // Deselect the friend
                                    selectedFriends.remove(currentFriend.getId());
                                    holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                                } else {
                                    // Select the friend
                                    selectedFriends.put(currentFriend.getId(), currentFriend.getName());
                                    holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.selected_background));
                                }
                            });
                        }
                    };
                    recyclerView.setAdapter(adapter);

                    // Create the dialog
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Select Friends to Collaborate With")
                            .setView(dialogView)
                            .setCancelable(true)
                            .create();

                    // Add a confirm button to complete the selection
                    Button confirmButton = dialogView.findViewById(R.id.confirmButton);
                    confirmButton.setOnClickListener(v -> {
                        if (selectedFriends.isEmpty()) {
                            Toast.makeText(context, "No friends selected", Toast.LENGTH_SHORT).show();
                        } else {
                            listener.onFriendsSelected(selectedFriends);
                            Toast.makeText(context, "Selected friends: " + selectedFriends.size(), Toast.LENGTH_SHORT).show();
                            dialog.dismiss(); // Close the dialog
                        }
                    });

                    // Show the dialog
                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
    }
}
