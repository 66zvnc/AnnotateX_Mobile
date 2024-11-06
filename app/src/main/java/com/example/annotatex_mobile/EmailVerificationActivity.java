package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification); // Ensure this layout exists

        auth = FirebaseAuth.getInstance();
        Button doneButton = findViewById(R.id.doneButton);
        TextView verificationMessage = findViewById(R.id.verificationMessage);

        // Display the message
        verificationMessage.setText("Please verify your email address to continue.");

        doneButton.setOnClickListener(v -> checkEmailVerification());
    }

    private void checkEmailVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Reload user information to check for verification
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        // Email is verified, proceed to the app
                        Intent intent = new Intent(EmailVerificationActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Show a message that the email is not verified
                        Toast.makeText(this, "Your email is not verified yet.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to check email verification status.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
