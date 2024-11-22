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
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String KEY_PROFILE_IMAGE_URL = "profileImageUrl";

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
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
        storage = FirebaseStorage.getInstance();
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

        // Set up onClick listeners for the navigation buttons
        view.findViewById(R.id.settingsOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsProfileActivity.class)));
        view.findViewById(R.id.paymentOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), PaymentMethodsProfileActivity.class)));
        view.findViewById(R.id.helpOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpCenterProfileActivity.class)));
        view.findViewById(R.id.privacyPolicyOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), PrivacyPolicyActivity.class)));
        view.findViewById(R.id.logoutOption).setOnClickListener(v -> logoutUser());
        view.findViewById(R.id.editProfileImageIcon).setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));

        return view;
    }

    private void loadFullName(TextView nameTextView) {
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.contains("fullName")) {
                        String fullName = documentSnapshot.getString("fullName");
                        nameTextView.setText(fullName);
                    } else {
                        nameTextView.setText("Your Name"); // Default if no name is set
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load full name", Toast.LENGTH_SHORT).show());
    }

    private void loadUsername(TextView usernameTextView) {
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.contains("username")) {
                        String username = documentSnapshot.getString("username");
                        usernameTextView.setText("@" + username); // Prefix "@" to the username
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load username", Toast.LENGTH_SHORT).show());
    }

    private void createUserDocumentIfNeeded() {
        if (user == null) return;

        DocumentReference userRef = firestore.collection("users").document(user.getUid());

        // Check if the document exists, if not create it with default values
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                userRef.set(new HashMap<>()).addOnSuccessListener(aVoid ->
                                Toast.makeText(getContext(), "User profile created", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Failed to create profile document", Toast.LENGTH_SHORT).show());
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
                        saveProfileImageUrlLocally(profileImageUrl); // Cache for offline use
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_default_profile);
                    }
                })
                .addOnFailureListener(e -> profileImageView.setImageResource(R.drawable.ic_default_profile));
    }

    private void displayProfileImage(String url) {
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_default_profile)
                .into(profileImageView);
    }

    private void saveProfileImageUrlLocally(String url) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PROFILE_IMAGE_URL, url);
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
