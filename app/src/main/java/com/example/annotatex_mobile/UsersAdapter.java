package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<Friend> usersList;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public UsersAdapter(Context context, List<Friend> usersList) {
        this.context = context;
        this.usersList = usersList;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Friend user = usersList.get(position);
        holder.nameTextView.setText(user.getName());
        holder.statusTextView.setText(user.getStatus());

        // Load profile image using Glide
        Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_profile)
                .into(holder.profileImageView);

        // Check if a friend request has already been sent
        checkFriendRequestStatus(user, holder);

        // Set up click listener for the "Add Friend" button
        holder.addFriendButton.setOnClickListener(v -> sendFriendRequest(user, holder));
    }

    private void checkFriendRequestStatus(Friend user, UserViewHolder holder) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Check if a friend request already exists
        firestore.collection("users")
                .document(user.getId())
                .collection("friendRequests")
                .document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        holder.addFriendButton.setEnabled(false);
                        holder.addFriendButton.setText("Requested");
                    }
                });
    }

    private void sendFriendRequest(Friend user, UserViewHolder holder) {
        String currentUserId = auth.getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch current user's full name from Firestore
        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String senderName = documentSnapshot.getString("fullName");
                    if (senderName == null || senderName.isEmpty()) {
                        senderName = "Unknown User"; // Fallback if fullName is not found
                    }

                    // Create a timestamp for when the request was sent
                    long timestamp = System.currentTimeMillis();

                    // Create a FriendRequest object
                    FriendRequest friendRequest = new FriendRequest(
                            currentUserId,
                            senderName,
                            user.getId(),
                            timestamp
                    );

                    // Send friend request to the selected user
                    firestore.collection("users")
                            .document(user.getId())
                            .collection("friendRequests")
                            .document(currentUserId)
                            .set(friendRequest)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show();
                                holder.addFriendButton.setEnabled(false);
                                holder.addFriendButton.setText("Requested");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to send friend request", Toast.LENGTH_SHORT).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to fetch user information", Toast.LENGTH_SHORT).show();
                });
    }




    @Override
    public int getItemCount() {
        return usersList.size();
    }

    // Method to update the list of users
    public void updateList(List<Friend> newUsersList) {
        usersList.clear();
        usersList.addAll(newUsersList);
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView, statusTextView;
        Button addFriendButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            addFriendButton = itemView.findViewById(R.id.addFriendButton);
        }
    }
}
