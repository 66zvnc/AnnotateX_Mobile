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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class GroupDetailsActivity extends AppCompatActivity {
    private static final String TAG = "GroupDetailsActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String groupId;

    private TextView groupNameText;
    private TextView memberCountText;
    private LinearLayout membersContainer;
    private Button leaveGroupButton;
    private ImageView groupPhotoImage;
    private ImageView editGroupPhotoButton;
    private FirebaseStorage storage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();

        // Get groupId from intent
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Error: Group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        groupNameText = findViewById(R.id.groupNameText);
        memberCountText = findViewById(R.id.memberCountText);
        membersContainer = findViewById(R.id.membersContainer);
        leaveGroupButton = findViewById(R.id.leaveGroupButton);
        groupPhotoImage = findViewById(R.id.groupPhotoImage);
        editGroupPhotoButton = findViewById(R.id.editGroupPhotoButton);
        
        // Set up back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Set up image picker
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadGroupPhoto(imageUri);
                    }
                }
            }
        );

        // Set up edit photo button
        editGroupPhotoButton.setOnClickListener(v -> openImagePicker());

        // Load group details
        loadGroupDetails();

        // Set up leave group button
        leaveGroupButton.setOnClickListener(v -> showLeaveGroupConfirmation());
    }

    private void loadGroupDetails() {
        firestore.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null) {
                        groupNameText.setText(group.getName());
                        memberCountText.setText(group.getMembers().size() + " Members");
                        
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
        int membersPerRow = 3;
        
        // Calculate how many rows we need
        int totalRows = (int) Math.ceil(memberIds.size() / (float) membersPerRow);
        
        for (int row = 0; row < totalRows; row++) {
            // Create a new row
            LinearLayout currentRow = new LinearLayout(this);
            currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
            currentRow.setOrientation(LinearLayout.HORIZONTAL);
            currentRow.setGravity(Gravity.CENTER);
            currentRow.setPadding(0, 0, 0, 16); // Add some vertical spacing between rows
            
            // Calculate start and end indices for this row
            int startIndex = row * membersPerRow;
            int endIndex = Math.min((row + 1) * membersPerRow, memberIds.size());
            
            // Add member views to this row
            for (int i = startIndex; i < endIndex; i++) {
                String memberId = memberIds.get(i);
                View memberView = getLayoutInflater().inflate(R.layout.item_group_member, currentRow, false);
                
                // Set equal width for each member in the row
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                );
                memberView.setLayoutParams(params);
                
                ImageView profileImage = memberView.findViewById(R.id.memberProfileImage);
                TextView nameText = memberView.findViewById(R.id.memberNameText);

                // Load member details
                firestore.collection("users").document(memberId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            nameText.setText(username != null ? username : "User");

                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .circleCrop()
                                    .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.ic_default_profile);
                            }
                        } else {
                            nameText.setText("Unknown User");
                            profileImage.setImageResource(R.drawable.ic_default_profile);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading member details", e);
                        nameText.setText("Unknown User");
                        profileImage.setImageResource(R.drawable.ic_default_profile);
                    });

                currentRow.addView(memberView);
            }
            
            membersContainer.addView(currentRow);
        }
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
} 