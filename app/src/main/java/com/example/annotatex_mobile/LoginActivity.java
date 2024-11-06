package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private EditText emailField, passwordField;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);
        LinearLayout googleLoginButton = findViewById(R.id.google_login_button);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> registerUser());
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    navigateToHome(user.getUid());
                }
            } else {
                Toast.makeText(LoginActivity.this, "Google login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser() {
        String identifier = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(identifier)) {
            emailField.setError("Username or Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return;
        }

        if (identifier.contains("@")) {
            // Login with email
            signInWithEmail(identifier, password);
        } else {
            // Login with username, fetch associated email from Firestore
            firestore.collection("users")
                    .whereEqualTo("username", identifier)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            String email = task.getResult().getDocuments().get(0).getString("email");
                            if (email != null) {
                                signInWithEmail(email, password);
                            } else {
                                emailField.setError("Email not found for this username");
                            }
                        } else {
                            emailField.setError("Username not found");
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void signInWithEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    // Check if email is verified
                    if (user.isEmailVerified()) {
                        String userId = user.getUid();
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToHome(userId);
                    } else {
                        // Redirect to email verification screen
                        Toast.makeText(LoginActivity.this, "Please verify your email before logging in.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                        startActivity(intent);
                        finish();
                    }
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
                    // Send verification email
                    user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                        if (verificationTask.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Registration successful! Please check your email to verify.", Toast.LENGTH_SHORT).show();
                            // Redirect to EmailVerificationActivity
                            Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                            startActivity(intent);
                            finish(); // Close the LoginActivity
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to send verification email: " + Objects.requireNonNull(verificationTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(LoginActivity.this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome(String userId) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }
}
