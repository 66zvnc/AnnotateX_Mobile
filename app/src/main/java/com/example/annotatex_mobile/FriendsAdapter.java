package com.example.annotatex_mobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private final List<Friend> friendsList;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public FriendsAdapter(Context context, List<Friend> friendsList) {
        this.context = context;
        this.friendsList = friendsList;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendsList.get(position);

        // Set friend's name and status
        holder.nameTextView.setText(friend.getName());
        holder.statusTextView.setText(friend.getStatus());

        // Load profile image using Glide
        if (friend.getProfileImageUrl() != null && !friend.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(friend.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_default_profile);
        }

        // Handle delete button click
        holder.deleteFriendButton.setOnClickListener(v -> deleteFriend(friend, holder.getAdapterPosition()));
    }

    private void deleteFriend(Friend friend, int position) {
        String currentUserId = auth.getCurrentUser().getUid();

        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if position is still valid before attempting to delete
        if (position < 0 || position >= friendsList.size()) {
            return;
        }

        // Delete the friend from the current user's friends list
        firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friend.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove the friend from the list and update RecyclerView
                    if (position < friendsList.size()) {
                        friendsList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, friendsList.size());
                        Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show());

        // Delete the current user from the friend's friends list
        firestore.collection("users")
                .document(friend.getId())
                .collection("friends")
                .document(currentUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Optional: Notify if removal from friend's list was successful
                    Toast.makeText(context, "Removed from friend's list", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update friend's list", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView, deleteFriendButton;
        TextView nameTextView, statusTextView;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            deleteFriendButton = itemView.findViewById(R.id.deleteFriendButton);
        }
    }
}
