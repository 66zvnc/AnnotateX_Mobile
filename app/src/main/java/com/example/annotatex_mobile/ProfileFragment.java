package com.example.annotatex_mobile;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.example.annotatex_mobile.utils.LanguageUtils;

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

        // Add language option
        LinearLayout languageOption = view.findViewById(R.id.languageOption);
        TextView currentLanguageText = view.findViewById(R.id.currentLanguageText);

        // Load and display current language
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String currentLanguage = prefs.getString("selected_language", "English");
        currentLanguageText.setText(currentLanguage);

        languageOption.setOnClickListener(v -> showLanguageDialog());

        return view;
    }

    private void setupNavigationListeners(View view) {
        view.findViewById(R.id.settingsOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsProfileActivity.class)));
        view.findViewById(R.id.helpOption).setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpCenterProfileActivity.class)));
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
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLanguagePreference(String selectedLanguage) {
        // Save the selection to SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        prefs.edit().putString("selected_language", selectedLanguage).apply();

        // Update the app locale using LanguageUtils
        Context updatedContext = LanguageUtils.updateLocale(requireContext(), selectedLanguage);
        
        // Update the UI text
        TextView currentLanguageText = requireView().findViewById(R.id.currentLanguageText);
        currentLanguageText.setText(selectedLanguage);
        
        // Show success message
        Toast.makeText(requireContext(), R.string.language_updated_success, Toast.LENGTH_SHORT).show();
        
        // Recreate activity to apply changes
        requireActivity().recreate();
    }

    private void showLanguageDialog() {
        Dialog languageDialog = new Dialog(requireContext());
        languageDialog.setContentView(R.layout.dialog_language_selection);
        
        // Set dialog window attributes
        Window window = languageDialog.getWindow();
        if (window != null) {
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Initialize views
        ImageButton backButton = languageDialog.findViewById(R.id.backButton);
        RecyclerView otherLanguagesRecyclerView = languageDialog.findViewById(R.id.otherLanguagesRecyclerView);
        
        // Set up the RecyclerView for other languages
        List<Language> otherLanguages = Arrays.asList(
            new Language("Bulgarian", R.drawable.flag_bg),
            new Language("German", R.drawable.flag_de),
            new Language("Russian", R.drawable.flag_ru),
            new Language("French", R.drawable.flag_fr),
            new Language("Spanish", R.drawable.flag_es)
        );
        
        LanguageAdapter adapter = new LanguageAdapter(otherLanguages, selectedLanguage -> {
            saveLanguagePreference(selectedLanguage);
            languageDialog.dismiss();
        });
        
        otherLanguagesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        otherLanguagesRecyclerView.setAdapter(adapter);

        // Set click listeners
        backButton.setOnClickListener(v -> languageDialog.dismiss());
        
        languageDialog.findViewById(R.id.englishUsOption).setOnClickListener(v -> {
            saveLanguagePreference("English (US)");
            languageDialog.dismiss();
        });
        
        languageDialog.findViewById(R.id.englishUkOption).setOnClickListener(v -> {
            saveLanguagePreference("English (UK)");
            languageDialog.dismiss();
        });

        languageDialog.show();
    }
}
