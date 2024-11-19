package com.example.annotatex_mobile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class CollaborativeChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaborative_chat);

        // Initialize your UI components (Profile Image, User Name, etc.)
        ImageView profileImageView = findViewById(R.id.profileImageView);
        TextView nameTextView = findViewById(R.id.nameTextView);
        Button goBackButton = findViewById(R.id.goBackButton);

        // Set up the profile image, name, and other UI logic here

        goBackButton.setOnClickListener(v -> {
            // Handle the 'Go Back' button logic (finish or navigate to the previous screen)
            onBackPressed();
        });
    }
}
