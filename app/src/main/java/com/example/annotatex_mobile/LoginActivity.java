package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailField, passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return;
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, PdfViewerFragment.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish(); // Close LoginActivity to prevent going back to it
                }
            } else {
                Toast.makeText(LoginActivity.this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
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

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    Toast.makeText(LoginActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, PdfViewerFragment.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish(); // Close LoginActivity to prevent going back to it
                }
            } else {
                Toast.makeText(LoginActivity.this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
