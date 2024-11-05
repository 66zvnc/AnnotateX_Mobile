package com.example.annotatex_mobile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
    private EditText fullNameEditText, usernameEditText, phoneEditText, emailEditText;
    private Button changeProfilePictureButton, saveButton;
    private boolean hasProfilePicture = false;

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
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);
        saveButton = findViewById(R.id.saveButton);

        loadProfileData();

        changeProfilePictureButton.setOnClickListener(v -> showProfilePictureOptions());
        saveButton.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {
        if (user != null) {
            String email = user.getEmail();
            if (email != null) {
                emailEditText.setText(email);
                emailEditText.setEnabled(false); // Email is non-editable
            }

            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.contains("profileImageUrl")) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_default_profile).into(profileImageView);
                            hasProfilePicture = true;
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                            hasProfilePicture = false;
                        }

                        if (documentSnapshot.contains("fullName")) {
                            fullNameEditText.setText(documentSnapshot.getString("fullName"));
                        }
                        if (documentSnapshot.contains("username")) {
                            usernameEditText.setText(documentSnapshot.getString("username"));
                        }
                        if (documentSnapshot.contains("phone")) {
                            phoneEditText.setText(documentSnapshot.getString("phone"));
                        }
                        if (documentSnapshot.contains("email")) {
                            emailEditText.setText(documentSnapshot.getString("email"));
                        }
                    })
                    .addOnFailureListener(e -> showToast("Failed to load profile data"));
        }
    }

    private void saveProfileData() {
        if (user != null) {
            String fullName = fullNameEditText.getText().toString();
            String username = usernameEditText.getText().toString();
            String phone = phoneEditText.getText().toString();
            String email = user.getEmail();

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("fullName", fullName);
            profileData.put("username", username);
            profileData.put("phone", phone);
            profileData.put("email", email);

            firestore.collection("users").document(user.getUid())
                    .set(profileData)
                    .addOnSuccessListener(aVoid -> showToast("Profile updated successfully"))
                    .addOnFailureListener(e -> showToast("Failed to update profile data"));

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
                    firestore.collection("users").document(user.getUid())
                            .update("profileImageUrl", uri.toString())
                            .addOnSuccessListener(aVoid -> {
                                showToast("Profile picture updated successfully");
                                hasProfilePicture = true;
                            })
                            .addOnFailureListener(e -> showToast("Failed to save profile picture URL"));
                }))
                .addOnFailureListener(e -> showToast("Failed to upload profile picture"));
    }

    private void showProfilePictureOptions() {
        PopupMenu popupMenu = new PopupMenu(this, changeProfilePictureButton);
        MenuInflater inflater = popupMenu.getMenuInflater();

        if (hasProfilePicture) {
            inflater.inflate(R.menu.image_replace_remove_options, popupMenu.getMenu());
        } else {
            inflater.inflate(R.menu.image_upload_options, popupMenu.getMenu());
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.option_take_photo) {
                takePhoto();
                return true;
            } else if (itemId == R.id.option_upload_gallery) {
                openGallery();
                return true;
            } else if (itemId == R.id.option_upload_files) {
                openFilePicker();
                return true;
            } else if (itemId == R.id.option_replace_picture) {
                openGallery();
                return true;
            } else if (itemId == R.id.option_remove_picture) {
                removeProfilePicture();
                return true;
            } else {
                return false;
            }
        });
        popupMenu.show();
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 101);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 102);
    }

    private void removeProfilePicture() {
        profileImageView.setImageResource(R.drawable.ic_default_profile);
        hasProfilePicture = false;

        firestore.collection("users").document(user.getUid())
                .update("profileImageUrl", null)
                .addOnSuccessListener(aVoid -> showToast("Profile picture removed"))
                .addOnFailureListener(e -> showToast("Failed to remove profile picture"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 100 || requestCode == 102) {
                imageUri = data.getData();
                profileImageView.setImageURI(imageUri);
            } else if (requestCode == 101) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUri(photo);
                profileImageView.setImageURI(imageUri);
            }
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
