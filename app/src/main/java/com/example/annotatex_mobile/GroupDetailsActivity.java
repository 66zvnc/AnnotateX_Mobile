package com.example.annotatex_mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupDetailsActivity extends AppCompatActivity {
    private static final String TAG = "GroupDetailsActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String groupId;
    private boolean isAdmin = false;

    private TextView groupNameText;
    private TextView memberCountText;
    private LinearLayout membersContainer;
    private Button leaveGroupButton;
    private ImageView groupPhotoImage;
    private ImageView editGroupPhotoButton;
    private FirebaseStorage storage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView addMemberButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Get groupId and admin status from intent
        groupId = getIntent().getStringExtra("groupId");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        if (groupId == null) {
            Toast.makeText(this, "Error: Group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();
        
        // Verify admin status and update UI
        verifyAdminStatus();
    }

    private void initializeViews() {
        groupNameText = findViewById(R.id.groupNameText);
        memberCountText = findViewById(R.id.memberCountText);
        membersContainer = findViewById(R.id.membersContainer);
        leaveGroupButton = findViewById(R.id.leaveGroupButton);
        groupPhotoImage = findViewById(R.id.groupPhotoImage);
        editGroupPhotoButton = findViewById(R.id.editGroupPhotoButton);
        addMemberButton = findViewById(R.id.addMemberButton);

        // Set up click listeners
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        editGroupPhotoButton.setOnClickListener(v -> openImagePicker());
        addMemberButton.setOnClickListener(v -> openAddMemberActivity());
        leaveGroupButton.setOnClickListener(v -> showLeaveGroupConfirmation());
    }

    private void verifyAdminStatus() {
        String currentUserId = auth.getCurrentUser().getUid();
        
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String createdBy = documentSnapshot.getString("createdBy");
                    isAdmin = createdBy != null && createdBy.equals(currentUserId);
                    
                    // Update UI based on admin status
                    updateUIForAdminStatus();
                    
                    // Load the rest of the group details
                    loadGroupDetails();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error verifying admin status", e);
                Toast.makeText(this, "Error loading group details", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateUIForAdminStatus() {
        // Update visibility of admin-only controls
        editGroupPhotoButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        addMemberButton.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "Admin status updated - isAdmin: " + isAdmin);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroupDetails(); // Refresh the members list
    }

    private void loadGroupDetails() {
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null) {
                        // Set admin status (still needed for other admin functions)
                        String currentUserId = auth.getCurrentUser().getUid();
                        isAdmin = group.getCreatedBy() != null && group.getCreatedBy().equals(currentUserId);
                        
                        // Update UI elements
                        groupNameText.setText(group.getName());
                        memberCountText.setText(group.getMembers().size() + " Members");
                        
                        // Show edit photo button for all members
                        editGroupPhotoButton.setVisibility(View.VISIBLE);
                        
                        // Show add member button only for admin
                        addMemberButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                        
                        // Load group photo if available
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            loadGroupPhoto(photoUrl);
                        }
                        
                        loadMembers(group.getMembers());
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading group details", e);
                Toast.makeText(this, "Failed to load group details", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadMembers(List<String> memberIds) {
        membersContainer.removeAllViews();
        String currentUserId = auth.getCurrentUser().getUid();
        
        // Pre-inflate all views to avoid multiple inflations
        List<View> memberViews = new ArrayList<>();
        Map<String, View> memberViewMap = new HashMap<>();
        
        for (String memberId : memberIds) {
            View memberView = getLayoutInflater().inflate(R.layout.item_group_member, membersContainer, false);
            memberViews.add(memberView);
            memberViewMap.put(memberId, memberView);
            
            ImageView profileImage = memberView.findViewById(R.id.memberProfileImage);
            TextView nameText = memberView.findViewById(R.id.memberNameText);
            ImageView removeButton = memberView.findViewById(R.id.removeMemberButton);

            // Debug log to check admin status
            Log.d(TAG, "isAdmin: " + isAdmin + ", currentUserId: " + currentUserId + ", memberId: " + memberId);

            // Show remove button for admin (except for themselves)
            if (isAdmin && !memberId.equals(currentUserId)) {
                removeButton.setVisibility(View.VISIBLE);
                removeButton.setOnClickListener(v -> showRemoveMemberDialog(memberId));
            } else {
                removeButton.setVisibility(View.GONE);
            }
            
            membersContainer.addView(memberView);
        }

        // First, verify admin status when loading group details
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String createdBy = documentSnapshot.getString("createdBy");
                    // Update admin status based on group creator
                    isAdmin = createdBy != null && createdBy.equals(currentUserId);
                    Log.d(TAG, "Group creator: " + createdBy + ", Current user: " + currentUserId + ", isAdmin: " + isAdmin);
                }
                
                // Then load member details
                firestore.collection("users")
                    .whereIn(FieldPath.documentId(), memberIds)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (DocumentSnapshot document : querySnapshot) {
                            String memberId = document.getId();
                            View memberView = memberViewMap.get(memberId);
                            if (memberView != null) {
                                ImageView profileImage = memberView.findViewById(R.id.memberProfileImage);
                                TextView nameText = memberView.findViewById(R.id.memberNameText);
                                ImageView removeButton = memberView.findViewById(R.id.removeMemberButton);
                                
                                String username = document.getString("username");
                                String profileImageUrl = document.getString("profileImageUrl");

                                nameText.setText(username != null ? username : "User");

                                // Update remove button visibility again after confirming admin status
                                if (isAdmin && !memberId.equals(currentUserId)) {
                                    removeButton.setVisibility(View.VISIBLE);
                                    removeButton.setOnClickListener(v -> showRemoveMemberDialog(memberId));
                                }

                                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                    Glide.with(GroupDetailsActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_default_profile)
                                        .error(R.drawable.ic_default_profile)
                                        .circleCrop()
                                        .into(profileImage);
                                } else {
                                    profileImage.setImageResource(R.drawable.ic_default_profile);
                                }
                            }
                        }
                    });
            });

        // Show/hide add member button based on admin status
        addMemberButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    private void showRemoveMemberDialog(String memberId) {
        firestore.collection("users").document(memberId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String username = documentSnapshot.getString("username");
                new AlertDialog.Builder(this)
                    .setTitle("Remove Member")
                    .setMessage("Are you sure you want to remove " + (username != null ? username : "this member") + " from the group?")
                    .setPositiveButton("Remove", (dialog, which) -> removeMember(memberId))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
    }

    private void removeMember(String memberId) {
        firestore.collection("groups").document(groupId)
            .update("members", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show();
                loadGroupDetails(); // Reload the members list
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error removing member", e);
                Toast.makeText(this, "Failed to remove member", Toast.LENGTH_SHORT).show();
            });
    }

    private void showLeaveGroupConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void leaveGroup() {
        String currentUserId = auth.getCurrentUser().getUid();
        firestore.collection("groups").document(groupId)
            .update("members", FieldValue.arrayRemove(currentUserId))
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Left group successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error leaving group", e);
                Toast.makeText(this, "Failed to leave group", Toast.LENGTH_SHORT).show();
            });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadGroupPhoto(Uri imageUri) {
        // Show loading indicator
        // You might want to add a ProgressBar in your layout
        Toast.makeText(this, "Uploading group photo...", Toast.LENGTH_SHORT).show();

        // Create a storage reference
        StorageReference photoRef = storage.getReference()
            .child("group_photos")
            .child(groupId)
            .child("group_photo.jpg");

        // Upload file
        photoRef.putFile(imageUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return photoRef.getDownloadUrl();
            })
            .addOnSuccessListener(downloadUri -> {
                // Update group document with new photo URL
                firestore.collection("groups").document(groupId)
                    .update("photoUrl", downloadUri.toString())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Group photo updated", Toast.LENGTH_SHORT).show();
                        // Load the new image
                        loadGroupPhoto(downloadUri.toString());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating group photo URL", e);
                        Toast.makeText(this, "Failed to update group photo", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error uploading group photo", e);
                Toast.makeText(this, "Failed to upload group photo", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadGroupPhoto(String photoUrl) {
        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_default_profile)
            .error(R.drawable.ic_default_profile)
            .circleCrop()
            .into(groupPhotoImage);
    }

    private void openAddMemberActivity() {
        Intent intent = new Intent(this, AddGroupMemberActivity.class);
        intent.putExtra("groupId", groupId);
        startActivity(intent);
    }

    private void displayMember(String memberId, String username) {
        View memberView = getLayoutInflater().inflate(R.layout.item_group_member, membersContainer, false);
        TextView nameText = memberView.findViewById(R.id.memberNameText);
        ImageView removeButton = memberView.findViewById(R.id.removeMemberButton);

        nameText.setText(username != null ? username : "Loading...");
        
        // Only show remove button if user is admin
        removeButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        
        // Set up remove button click listener (only for admin)
        if (isAdmin) {
            removeButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Remove Member")
                    .setMessage("Are you sure you want to remove " + (username != null ? username : "this member") + " from the group?")
                    .setPositiveButton("Remove", (dialog, which) -> removeMember(memberId))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

        membersContainer.addView(memberView);
    }
} 