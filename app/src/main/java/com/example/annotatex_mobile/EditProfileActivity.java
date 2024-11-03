package com.example.annotatex_mobile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private EditText fullNameEditText, usernameEditText, titleEditText, phoneEditText, locationEditText;
    private Button saveButton;

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

        loadProfileData();

        saveButton.setOnClickListener(v -> saveProfileData());
    }

    private void loadProfileData() {

    }

    private void saveProfileData() {
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

}
