package com.example.annotatex_mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private EditText fullNameEditText, usernameEditText, titleEditText, phoneEditText;
    private Button saveButton, changeProfilePictureButton;
    private Uri imageUri;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_edit_profile);

        profileImageView = findViewById(R.id.profileImageView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        titleEditText = findViewById(R.id.titleEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        user = auth.getCurrentUser();

        loadProfileData();

        changeProfilePictureButton.setOnClickListener(v -> showImageUploadOptions());
        saveButton.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        if (user != null) {
            loadProfileImage();

            // Load profile information from Firestore
            DocumentReference userDoc = firestore.collection("users").document(user.getUid());
            userDoc.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    fullNameEditText.setText(documentSnapshot.getString("fullName"));
                    usernameEditText.setText(documentSnapshot.getString("username"));
                    titleEditText.setText(documentSnapshot.getString("title"));
                    phoneEditText.setText(documentSnapshot.getString("phone"));
                }
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage() {
        if (user != null) {
            StorageReference profilePicRef = storage.getReference().child("profilePic/" + user.getUid() + ".jpg");
            profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.ic_default_profile)
                        .into(profileImageView);
            }).addOnFailureListener(e -> {
                profileImageView.setImageResource(R.drawable.ic_default_profile);
                Toast.makeText(this, "Profile image not found.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveProfileData() {
        if (user != null) {
            String fullName = fullNameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String title = titleEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();

            UserProfile profileData = new UserProfile(fullName, username, title, phone);

            DocumentReference userDoc = firestore.collection("users").document(user.getUid());
            userDoc.set(profileData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void showImageUploadOptions() {
        PopupMenu popupMenu = new PopupMenu(this, changeProfilePictureButton);
        popupMenu.getMenuInflater().inflate(R.menu.image_upload_options, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.option_take_photo) {
                openCamera();
                return true;
            } else if (itemId == R.id.option_upload_gallery) {
                openGallery();
                return true;
            } else if (itemId == R.id.option_upload_files) {
                openFileManager();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    handleImage(bitmap);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    profileImageView.setImageURI(imageUri);
                    uploadImageToFirebase();
                }
            }
    );

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void handleImage(Bitmap bitmap) {
        profileImageView.setImageBitmap(bitmap);
        imageUri = getImageUriFromBitmap(bitmap);
        uploadImageToFirebase();
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase() {
        if (user != null && imageUri != null) {
            StorageReference profilePicRef = storage.getReference().child("profilePic/" + user.getUid() + ".jpg");

            profilePicRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        DocumentReference userDoc = firestore.collection("users").document(user.getUid());
                        userDoc.update("profileImageUrl", uri.toString())
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save profile picture URL", Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "User not authenticated or image URI is null.", Toast.LENGTH_SHORT).show();
        }
    }

    private static class UserProfile {
        String fullName;
        String username;
        String title;
        String phone;

        UserProfile(String fullName, String username, String title, String phone) {
            this.fullName = fullName;
            this.username = username;
            this.title = title;
            this.phone = phone;
        }
    }
}
