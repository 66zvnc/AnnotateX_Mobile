package com.example.annotatex_mobile;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class HelpSupportActivity extends AppCompatActivity {
    private EditText bugDescriptionInput;
    private EditText expectedBehaviorInput;
    private EditText moreInformationInput;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        // Initialize views
        bugDescriptionInput = findViewById(R.id.bugDescriptionInput);
        expectedBehaviorInput = findViewById(R.id.expectedBehaviorInput);
        moreInformationInput = findViewById(R.id.moreInformationInput);
        backButton = findViewById(R.id.backButton);

        // Set up back button click listener
        backButton.setOnClickListener(v -> finish());
    }
} 