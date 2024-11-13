package com.example.annotatex_mobile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

        // Handle long press to show the popup menu
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(holder.itemView, friend, position);
            return true;
        });
    }

    private void showPopupMenu(View anchor, Friend friend, int position) {
        // Create a PopupMenu anchored to the view
        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.inflate(R.menu.friend_options_menu);

        // Set up menu item click listeners
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_view_profile) {
                viewProfile(friend);
                return true;
            } else if (item.getItemId() == R.id.menu_remove_friend) {
                deleteFriend(friend, position);
                return true;
            } else if (item.getItemId() == R.id.menu_block_friend) {
                blockFriend(friend);
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    private void viewProfile(Friend friend) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra("friendId", friend.getId());
        context.startActivity(intent);
    }

    private void deleteFriend(Friend friend, int position) {
        String currentUserId = auth.getCurrentUser().getUid();

        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete friend from user's list
        firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friend.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    friendsList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show());
    }

    private void blockFriend(Friend friend) {
        Toast.makeText(context, "Friend blocked (feature coming soon)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView, statusTextView;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }
}
