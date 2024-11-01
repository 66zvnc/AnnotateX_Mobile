package com.example.annotatex_mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class EditProfileFragment extends Fragment {

    private ImageView profileImageView;
    private EditText fullNameEditText, usernameEditText, titleEditText, phoneEditText, locationEditText;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        profileImageView = view.findViewById(R.id.profileImageView);
        fullNameEditText = view.findViewById(R.id.fullNameEditText);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        titleEditText = view.findViewById(R.id.titleEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        saveButton = view.findViewById(R.id.saveButton);

        // Load existing profile data if needed
        loadProfileData();

        // Set up save button listener
        saveButton.setOnClickListener(v -> saveProfileData());

        return view;
    }

    private void loadProfileData() {
        // Load data from SharedPreferences or Firebase
        // Set existing values to EditTexts if available
    }

    private void saveProfileData() {
        // Save the updated profile data
        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}
