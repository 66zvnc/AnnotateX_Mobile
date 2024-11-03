package com.example.annotatex_mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String KEY_PROFILE_IMAGE_URL = "profileImageUrl";

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private ImageView profileImageView;
    private ImageView editProfileImageIcon;
    private Uri imageUri;
    private String profileImageUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        profileImageView = view.findViewById(R.id.profileImageView);
        editProfileImageIcon = view.findViewById(R.id.editProfileImageIcon);
        TextView nameTextView = view.findViewById(R.id.nameTextView);
        TextView emailTextView = view.findViewById(R.id.emailTextView);

        if (user != null) {
            emailTextView.setText(user.getEmail());
            nameTextView.setText(user.getDisplayName() != null ? user.getDisplayName() : "Your Name");
            loadProfileImage();
        } else {
            emailTextView.setText("No user logged in");
        }

        profileImageView.setOnClickListener(v -> showImageUploadOptions());

        // Open each option as a full-screen activity
        editProfileImageIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
        view.findViewById(R.id.settingsOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsProfileActivity.class)));
        view.findViewById(R.id.paymentOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), PaymentMethodsProfileActivity.class)));
        view.findViewById(R.id.helpOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpCenterProfileActivity.class)));
        view.findViewById(R.id.privacyPolicyOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), PrivacyPolicyActivity.class)));
        view.findViewById(R.id.logoutOption).setOnClickListener(v -> logoutUser());

        return view;
    }

    private void showImageUploadOptions() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), profileImageView);
        if (profileImageUrl != null) {
            popupMenu.getMenuInflater().inflate(R.menu.image_replace_remove_options, popupMenu.getMenu());
        } else {
            popupMenu.getMenuInflater().inflate(R.menu.image_upload_options, popupMenu.getMenu());
        }

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
            } else if (itemId == R.id.option_remove_picture) {
                removeProfilePicture();
                return true;
            } else {
                return false;
            }
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
        String path = MediaStore.Images.Media.insertImage(requireActivity().getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase() {
        if (user != null && imageUri != null) {
            String userId = user.getUid();
            StorageReference profilePicRef = storage.getReference().child("profilePic/" + userId + ".jpg");

            profilePicRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveProfileImageUrl(uri.toString());
                        loadProfileImage();
                        Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                    }))
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to upload profile picture", Toast.LENGTH_SHORT).show());
        }
    }

    private void removeProfilePicture() {
        if (user != null) {
            String userId = user.getUid();
            StorageReference profilePicRef = storage.getReference().child("profilePic/" + userId + ".jpg");

            profilePicRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        profileImageUrl = null;
                        profileImageView.setImageResource(R.drawable.ic_default_profile);
                        clearProfileImageUrl();
                        Toast.makeText(getContext(), "Profile picture removed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to remove profile picture", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveProfileImageUrl(String url) {
        profileImageUrl = url;
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .update("profileImageUrl", url)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save profile URL", Toast.LENGTH_SHORT).show());

            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_PROFILE_IMAGE_URL, url);
            editor.apply();
        }
    }

    private void loadProfileImage() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        profileImageUrl = prefs.getString(KEY_PROFILE_IMAGE_URL, null);

        if (profileImageUrl != null) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .into(profileImageView);
        } else if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.contains("profileImageUrl")) {
                            profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_default_profile).into(profileImageView);
                            saveProfileImageUrl(profileImageUrl);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                        }
                    })
                    .addOnFailureListener(e -> profileImageView.setImageResource(R.drawable.ic_default_profile));
        } else {
            profileImageView.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void clearProfileImageUrl() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_PROFILE_IMAGE_URL);
        editor.apply();
    }

    private void logoutUser() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}

