package com.example.annotatex_mobile;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private Context context;
    private List<Group> groups;
    private OnGroupClickListener listener;
    private FirebaseFirestore firestore;

    public GroupAdapter(Context context, List<Group> groups, OnGroupClickListener listener) {
        this.context = context;
        this.groups = groups;
        this.listener = listener;
        this.firestore = FirebaseFirestore.getInstance();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView;
        TextView memberCountTextView;
        ImageView memberImage1;
        ImageView memberImage2;
        List<ImageView> memberImages;
        View itemView;
        View groupPhotoContainer;
        View memberImagesContainer;
        ImageView groupPhotoImage;

        GroupViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            memberCountTextView = itemView.findViewById(R.id.memberCountTextView);
            memberImage1 = itemView.findViewById(R.id.memberImage1);
            memberImage2 = itemView.findViewById(R.id.memberImage2);
            groupPhotoContainer = itemView.findViewById(R.id.groupPhotoContainer);
            memberImagesContainer = itemView.findViewById(R.id.memberImagesContainer);
            groupPhotoImage = itemView.findViewById(R.id.groupPhotoImage);
            
            memberImages = Arrays.asList(memberImage1, memberImage2);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroupClick(groups.get(position));
                }
            });

            // Add long click listener for the menu
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(v, groups.get(position));
                }
                return true;
            });
        }

        private void showPopupMenu(View view, Group group) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.inflate(R.menu.group_options_menu);

            // Only show delete option if user is the group creator
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            popup.getMenu().findItem(R.id.action_delete_group).setVisible(
                group.getCreatedBy() != null && group.getCreatedBy().equals(currentUserId)
            );

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_group) {
                    deleteGroup(group);
                    return true;
                }
                return false;
            });

            popup.show();
        }

        private void deleteGroup(Group group) {
            new AlertDialog.Builder(context)
                .setTitle("Delete Group")
                .setMessage("Are you sure you want to delete this group?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                        .collection("groups")
                        .document(group.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to delete group", Toast.LENGTH_SHORT).show();
                        });
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        void loadMemberImages(List<String> memberIds) {
            // Hide all images initially
            for (ImageView imageView : memberImages) {
                imageView.setVisibility(View.GONE);
            }

            // If there are members, randomly select 2 to display
            if (!memberIds.isEmpty()) {
                List<String> shuffledMembers = new ArrayList<>(memberIds);
                Collections.shuffle(shuffledMembers);
                int displayCount = Math.min(2, shuffledMembers.size());

                for (int i = 0; i < displayCount; i++) {
                    String memberId = shuffledMembers.get(i);
                    ImageView imageView = memberImages.get(i);
                    imageView.setVisibility(View.VISIBLE);

                    firestore.collection("users").document(memberId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                    Glide.with(context)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_default_profile)
                                        .error(R.drawable.ic_default_profile)
                                        .circleCrop()
                                        .into(imageView);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("GroupAdapter", "Error loading member profile image", e);
                            imageView.setImageResource(R.drawable.ic_default_profile);
                        });
                }
            }
        }
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.groupNameTextView.setText(group.getName());
        holder.memberCountTextView.setText(group.getMembers().size() + " Members");

        // Check if group has a photo
        if (group.getPhotoUrl() != null && !group.getPhotoUrl().isEmpty()) {
            // Show group photo and hide member grid
            holder.groupPhotoContainer.setVisibility(View.VISIBLE);
            holder.memberImagesContainer.setVisibility(View.GONE);
            
            // Load group photo
            Glide.with(context)
                .load(group.getPhotoUrl())
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .circleCrop()
                .into(holder.groupPhotoImage);
        } else {
            // Show member grid and hide group photo
            holder.groupPhotoContainer.setVisibility(View.GONE);
            holder.memberImagesContainer.setVisibility(View.VISIBLE);
            holder.loadMemberImages(group.getMembers());
        }
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }
} 