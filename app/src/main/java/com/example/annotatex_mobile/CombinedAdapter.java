package com.example.annotatex_mobile;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CombinedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_GROUP = 0;
    private static final int TYPE_FRIEND = 1;

    private List<Object> items;
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public CombinedAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        if (viewType == TYPE_GROUP) {
            View groupView = inflater.inflate(R.layout.item_group, parent, false);
            return new GroupViewHolder(groupView);
        } else { // TYPE_FRIEND
            View friendView = inflater.inflate(R.layout.item_friend, parent, false);
            return new FriendViewHolder(friendView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_GROUP) {
            GroupViewHolder groupHolder = (GroupViewHolder) holder;
            Group group = (Group) items.get(position);
            groupHolder.groupNameTextView.setText(group.getName());
            groupHolder.memberCountTextView.setText(group.getMembers().size() + " Members");
            
            // Add long click listener for groups
            groupHolder.itemView.setOnLongClickListener(v -> {
                showGroupPopupMenu(v, group);
                return true;
            });

            // Load member images
            if (!group.getMembers().isEmpty()) {
                // Hide all images initially
                groupHolder.memberImage1.setVisibility(View.GONE);
                groupHolder.memberImage2.setVisibility(View.GONE);

                // Randomly select up to 2 members to display
                List<String> memberIds = group.getMembers();
                int displayCount = Math.min(2, memberIds.size());
                
                for (int i = 0; i < displayCount; i++) {
                    String memberId = memberIds.get(i);
                    ImageView imageView = (i == 0) ? groupHolder.memberImage1 : groupHolder.memberImage2;
                    imageView.setVisibility(View.VISIBLE);

                    // Load member profile image
                    firestore.collection("users").document(memberId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(context)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .circleCrop()
                                    .into(imageView);
                            } else {
                                imageView.setImageResource(R.drawable.ic_default_profile);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CombinedAdapter", "Error loading member profile image", e);
                            imageView.setImageResource(R.drawable.ic_default_profile);
                        });
                }
            }

            // Handle group click
            groupHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, GroupChatActivity.class);
                intent.putExtra("groupId", group.getId());
                intent.putExtra("groupName", group.getName());
                context.startActivity(intent);
            });
        } else {
            FriendViewHolder friendHolder = (FriendViewHolder) holder;
            Friend friend = (Friend) items.get(position);
            friendHolder.nameTextView.setText(friend.getName());
            friendHolder.statusTextView.setText(friend.getStatus());

            // Load profile image
            if (friend.getProfileImageUrl() != null && !friend.getProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(friend.getProfileImageUrl())
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .circleCrop()
                        .into(friendHolder.profileImageView);
            } else {
                friendHolder.profileImageView.setImageResource(R.drawable.ic_default_profile);
            }

            // Handle friend click
            friendHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, CollaborativeChatActivity.class);
                intent.putExtra("friendId", friend.getId());
                context.startActivity(intent);
            });

            // Handle long press menu
            friendHolder.itemView.setOnLongClickListener(v -> {
                showFriendPopupMenu(v, friend, position);
                return true;
            });
        }
    }

    private void showFriendPopupMenu(View anchor, Friend friend, int position) {
        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.inflate(R.menu.friend_options_menu);

        // Only show view profile and remove friend options
        MenuItem removeFriendItem = popupMenu.getMenu().findItem(R.id.menu_remove_friend);
        MenuItem deleteCollabItem = popupMenu.getMenu().findItem(R.id.menu_delete_collab);
        
        removeFriendItem.setVisible(true);
        deleteCollabItem.setVisible(false); // Never show delete collab option

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_view_profile) {
                viewProfile(friend);
                return true;
            } else if (item.getItemId() == R.id.menu_remove_friend) {
                deleteFriend(friend, position);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    // Friend action methods (copied from FriendsAdapter)
    private void viewProfile(Friend friend) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra("friendId", friend.getId());
        context.startActivity(intent);
    }

    private void deleteFriend(Friend friend, int position) {
        String currentUserId = auth.getCurrentUser().getUid();
        String friendId = friend.getId();

        // Delete from current user's friends collection
        firestore.collection("users").document(currentUserId)
                .collection("friends").document(friendId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete from friend's collection
                    firestore.collection("users").document(friendId)
                            .collection("friends").document(currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                // Remove friend from the list
                                for (int i = 0; i < items.size(); i++) {
                                    if (items.get(i) instanceof Friend) {
                                        Friend f = (Friend) items.get(i);
                                        if (f.getId().equals(friend.getId())) {
                                            items.remove(i);
                                            notifyItemRemoved(i);
                                            break;
                                        }
                                    }
                                }
                                Log.d("CombinedAdapter", "Friend removed successfully");
                            })
                            .addOnFailureListener(e -> Log.e("CombinedAdapter", "Error removing from friend's list", e));
                })
                .addOnFailureListener(e -> Log.e("CombinedAdapter", "Error removing friend", e));
    }

    private void showGroupPopupMenu(View anchor, Group group) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.inflate(R.menu.group_long_press_menu);

        String currentUserId = auth.getCurrentUser().getUid();
        boolean isAdmin = group.getCreatedBy() != null && 
                        group.getCreatedBy().equals(currentUserId);

        // Show/hide delete option based on admin status
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete_group);
        deleteItem.setVisible(isAdmin);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_group) {
                showDeleteGroupConfirmation(group);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showDeleteGroupConfirmation(Group group) {
        new AlertDialog.Builder(context)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                FirebaseFirestore.getInstance()
                    .collection("groups")
                    .document(group.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Find and remove the group from items list
                        for (int i = 0; i < items.size(); i++) {
                            if (items.get(i) instanceof Group && 
                                ((Group) items.get(i)).getId().equals(group.getId())) {
                                items.remove(i);
                                notifyItemRemoved(i);
                                break;
                            }
                        }
                        Toast.makeText(context, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete group: " + e.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof Group) {
            return TYPE_GROUP;
        } else {
            return TYPE_FRIEND;
        }
    }

    // ViewHolder classes
    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView, memberCountTextView;
        ImageView memberImage1, memberImage2;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            memberCountTextView = itemView.findViewById(R.id.memberCountTextView);
            memberImage1 = itemView.findViewById(R.id.memberImage1);
            memberImage2 = itemView.findViewById(R.id.memberImage2);
        }
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView, statusTextView;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }

    // Add this method to update both groups and friends
    public void updateItems(List<Group> groups, List<Friend> friends) {
        items.clear();
        if (groups != null) {
            items.addAll(groups);
        }
        if (friends != null) {
            items.addAll(friends);
        }
        notifyDataSetChanged();
    }
} 