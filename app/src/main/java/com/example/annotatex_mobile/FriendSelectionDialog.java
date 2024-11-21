package com.example.annotatex_mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendSelectionDialog {

    public interface FriendSelectionListener {
        void onFriendSelected(String friendId, String friendName);
    }

    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FriendSelectionDialog(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
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

                    queryDocumentSnapshots.forEach(doc -> {
                        Friend friend = new Friend(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getString("profileImageUrl"),
                                doc.getString("status"),
                                doc.getBoolean("removed") != null && doc.getBoolean("removed")
                        );
                        friends.add(friend);
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

                            // Add click listener for each friend to trigger the selection callback
                            holder.itemView.setOnClickListener(v -> {
                                Friend selectedFriend = friends.get(position);
                                listener.onFriendSelected(selectedFriend.getId(), selectedFriend.getName());
                            });
                        }
                    };
                    recyclerView.setAdapter(adapter);

                    // Build and show the dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Select a Friend to Collaborate With");
                    builder.setView(dialogView);
                    builder.setCancelable(true);
                    builder.create().show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
    }
}
