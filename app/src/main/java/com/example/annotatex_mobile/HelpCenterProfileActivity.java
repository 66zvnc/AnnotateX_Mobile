package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HelpCenterProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile_help_center);

        // Set up the "Go Back" button functionality
        ImageView goBackButton = findViewById(R.id.goBackButton);
        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(HelpCenterProfileActivity.this, MainActivity.class);
            intent.putExtra("navigateTo", "ProfileFragment");
            startActivity(intent);
            finish();
        });
    }
}
