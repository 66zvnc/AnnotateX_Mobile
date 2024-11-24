package com.example.annotatex_mobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String KEY_PROFILE_IMAGE_URL = "profileImageUrl";

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firestore;
    private ImageView profileImageView;
    private String profileImageUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        profileImageView = view.findViewById(R.id.profileImageView);
        TextView nameTextView = view.findViewById(R.id.nameTextView);
        TextView usernameTextView = view.findViewById(R.id.usernameTextView);

        if (user != null) {
            loadFullName(nameTextView);
            loadUsername(usernameTextView);
            createUserDocumentIfNeeded();
            loadProfileImage();
        } else {
            usernameTextView.setText("No user logged in");
        }

        // Set up onClick listeners for navigation options
        setupNavigationListeners(view);

        return view;
    }

    private void setupNavigationListeners(View view) {
        view.findViewById(R.id.settingsOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsProfileActivity.class)));
        view.findViewById(R.id.paymentOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), ManageSubscriptionActivity.class)));
        view.findViewById(R.id.helpOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpCenterProfileActivity.class)));
        view.findViewById(R.id.privacyPolicyOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), PrivacyPolicyActivity.class)));
        view.findViewById(R.id.logoutOption).setOnClickListener(v -> logoutUser());
        view.findViewById(R.id.editProfileImageIcon).setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
    }

    private void loadFullName(TextView nameTextView) {
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fullName = documentSnapshot.getString("fullName");
                    nameTextView.setText(fullName != null ? fullName : "Your Name");
                })
                .addOnFailureListener(e -> showToast("Failed to load full name"));
    }

    private void loadUsername(TextView usernameTextView) {
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    usernameTextView.setText(username != null ? "@" + username : "@unknown");
                })
                .addOnFailureListener(e -> showToast("Failed to load username"));
    }

    private void createUserDocumentIfNeeded() {
        if (user == null) return;

        DocumentReference userRef = firestore.collection("users").document(user.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                HashMap<String, Object> defaultData = new HashMap<>();
                defaultData.put("fullName", "Your Name");
                defaultData.put("username", "unknown");
                defaultData.put("profileImageUrl", "");
                userRef.set(defaultData)
                        .addOnSuccessListener(aVoid -> showToast("User profile created"))
                        .addOnFailureListener(e -> showToast("Failed to create profile document"));
            }
        });
    }

    private void loadProfileImage() {
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.contains("profileImageUrl")) {
                        profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        displayProfileImage(profileImageUrl);
                        saveProfileImageUrlLocally(profileImageUrl);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_default_profile);
                    }
                })
                .addOnFailureListener(e -> profileImageView.setImageResource(R.drawable.ic_default_profile));
    }

    private void displayProfileImage(String url) {
        if (isAdded()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .circleCrop()
                    .into(profileImageView);
        }
    }

    private void saveProfileImageUrlLocally(String url) {
        if (isAdded()) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_PROFILE_IMAGE_URL, url).apply();
        }
    }

    private void logoutUser() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
