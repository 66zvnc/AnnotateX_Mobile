package com.example.annotatex_mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EditProfileFragment extends DialogFragment {

    private ImageView profileImageView;
    private EditText fullNameEditText, usernameEditText, titleEditText, phoneEditText, locationEditText;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Remove title bar for full-screen appearance
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        profileImageView = view.findViewById(R.id.profileImageView);
        fullNameEditText = view.findViewById(R.id.fullNameEditText);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        titleEditText = view.findViewById(R.id.titleEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        saveButton = view.findViewById(R.id.saveButton);

        loadProfileData();

        saveButton.setOnClickListener(v -> saveProfileData());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set dialog to full screen
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void loadProfileData() {
        // Load data from SharedPreferences or Firebase
    }

    private void saveProfileData() {
        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}
