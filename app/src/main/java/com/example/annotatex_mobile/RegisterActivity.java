package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EditText emailField, usernameField, fullnameField, passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        emailField = findViewById(R.id.email);
        usernameField = findViewById(R.id.username);
        fullnameField = findViewById(R.id.fullname);
        passwordField = findViewById(R.id.password);
        Button registerButton = findViewById(R.id.register_button);
        TextView loginLink = findViewById(R.id.login_link);
        ImageView googleSignup = findViewById(R.id.google_signup);

        // Register button click listener
        registerButton.setOnClickListener(v -> registerUser());

        // Login link click listener
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Google signup click listener
        googleSignup.setOnClickListener(v -> {
            // Implement Google sign up logic
            Toast.makeText(this, "Google sign up clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String fullname = fullnameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            usernameField.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(fullname)) {
            fullnameField.setError("Full name is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters");
            return;
        }

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Create user profile in Firestore
                            Map<String, Object> userProfile = new HashMap<>();
                            userProfile.put("username", username);
                            userProfile.put("fullname", fullname);
                            userProfile.put("email", email);

                            firestore.collection("users").document(user.getUid())
                                    .set(userProfile)
                                    .addOnSuccessListener(aVoid -> {
                                        // Send email verification
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(RegisterActivity.this, 
                                                                "Registration successful! Please check your email to verify.", 
                                                                Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(RegisterActivity.this, EmailVerificationActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this,
                                            "Error creating user profile: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 