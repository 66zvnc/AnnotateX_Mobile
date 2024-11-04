package com.example.annotatex_mobile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private ImageView profileImageView;
    private Uri imageUri;
    private EditText fullNameEditText, usernameEditText, titleEditText, phoneEditText;
    private Button changeProfilePictureButton, saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_edit_profile);

        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        profileImageView = findViewById(R.id.profileImageView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        titleEditText = findViewById(R.id.titleEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);
        saveButton = findViewById(R.id.saveButton);

        loadProfileData();

        changeProfilePictureButton.setOnClickListener(v -> openGallery());
        saveButton.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.contains("profileImageUrl")) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_default_profile).into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                        }

                        if (documentSnapshot.contains("fullName")) {
                            fullNameEditText.setText(documentSnapshot.getString("fullName"));
                        }
                        if (documentSnapshot.contains("username")) {
                            usernameEditText.setText(documentSnapshot.getString("username"));
                        }
                        if (documentSnapshot.contains("title")) {
                            titleEditText.setText(documentSnapshot.getString("title"));
                        }
                        if (documentSnapshot.contains("phone")) {
                            phoneEditText.setText(documentSnapshot.getString("phone"));
                        }
                    })
                    .addOnFailureListener(e -> showToast("Failed to load profile data"));
        }
    }

    private void saveProfileData() {
        if (user != null) {
            String fullName = fullNameEditText.getText().toString();
            String username = usernameEditText.getText().toString();
            String title = titleEditText.getText().toString();
            String phone = phoneEditText.getText().toString();

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("fullName", fullName);
            profileData.put("username", username);
            profileData.put("title", title);
            profileData.put("phone", phone);

            // Save profile data to Firestore
            firestore.collection("users").document(user.getUid())
                    .set(profileData)
                    .addOnSuccessListener(aVoid -> showToast("Profile updated successfully"))
                    .addOnFailureListener(e -> showToast("Failed to update profile data"));

            // If there is a new profile picture, upload it to Firebase Storage
            if (imageUri != null) {
                uploadProfilePicture();
            }
        } else {
            showToast("User not logged in");
        }
    }

    private void uploadProfilePicture() {
        StorageReference profilePicRef = storage.getReference().child("profilePic/" + user.getUid() + ".jpg");

        profilePicRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Update Firestore with the new profile picture URL
                    firestore.collection("users").document(user.getUid())
                            .update("profileImageUrl", uri.toString())
                            .addOnSuccessListener(aVoid -> showToast("Profile picture updated successfully"))
                            .addOnFailureListener(e -> showToast("Failed to save profile picture URL"));
                }))
                .addOnFailureListener(e -> showToast("Failed to upload profile picture"));
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);  // Display the selected image
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

