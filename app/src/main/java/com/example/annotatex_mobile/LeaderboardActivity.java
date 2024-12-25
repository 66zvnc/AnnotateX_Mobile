package com.example.annotatex_mobile;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LeaderboardActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private String userId;
    private TextView booksCompleted, currentRank;
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter leaderboardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_goal);

        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        loadUserStats();
        setupLeaderboard();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        booksCompleted = findViewById(R.id.booksCompleted);
        currentRank = findViewById(R.id.currentRank);
        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadUserStats() {
        // First, ensure current user exists in Firestore
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String currentUserName = currentUserEmail != null ? currentUserEmail.split("@")[0] : "Anonymous";

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", currentUserName);
        userData.put("booksCompleted", 0); // Default value
        userData.put("email", currentUserEmail);
        
        // Create or update user document
        firestore.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // After ensuring user exists, get their completed books count
                    firestore.collection("books")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("status", "completed")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                int completed = querySnapshot.size();
                                
                                // Update the user's booksCompleted count
                                firestore.collection("users")
                                        .document(userId)
                                        .update("booksCompleted", completed)
                                        .addOnSuccessListener(updateVoid -> {
                                            booksCompleted.setText(String.valueOf(completed));
                                            updateUserRank(completed);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Leaderboard", "Error updating user's books count", e);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error creating/updating user", e);
                });
    }

    private void updateUserRank(int userBooks) {
        firestore.collection("users")
                .orderBy("booksCompleted", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rank = 1;
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (doc.getId().equals(userId)) {
                            currentRank.setText("#" + rank);
                            break;
                        }
                        rank++;
                    }
                });
    }

    private void setupLeaderboard() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // First get all users except current user
        firestore.collection("users")
                .get()
                .addOnSuccessListener(allUsersSnapshot -> {
                    if (allUsersSnapshot.isEmpty()) {
                        Toast.makeText(this, "No other users found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Add random stats to users
                    for (DocumentSnapshot doc : allUsersSnapshot) {
                        String docUserId = doc.getId();
                        if (!docUserId.equals(currentUserId)) {  // Skip current user
                            // Generate random number between 5 and 50 for books completed
                            int randomBooks = (int) (Math.random() * 46) + 5;
                            
                            Map<String, Object> userData = new HashMap<>();
                            // Use email as name if name is not available
                            String name = doc.getString("name");
                            if (name == null || name.isEmpty()) {
                                String email = doc.getString("email");
                                name = email != null ? email.split("@")[0] : "Anonymous Reader";
                            }
                            
                            userData.put("name", name);
                            userData.put("booksCompleted", randomBooks);
                            userData.put("email", doc.getString("email"));
                            userData.put("profileImageUrl", doc.getString("profileImageUrl"));

                            firestore.collection("users")
                                    .document(docUserId)
                                    .set(userData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d("Leaderboard", "Updated user stats: " + docUserId));
                        }
                    }

                    // Then get top 10 users for leaderboard
                    firestore.collection("users")
                            .orderBy("booksCompleted", Query.Direction.DESCENDING)
                            .limit(10)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<LeaderboardItem> leaderboardItems = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot) {
                                    String docUserId = doc.getId();
                                    if (!docUserId.equals(currentUserId)) {  // Skip current user
                                        Log.d("Leaderboard", "User found: " + doc.getString("name") + 
                                                           " Books: " + doc.getLong("booksCompleted"));
                                        
                                        String name = doc.getString("name");
                                        Long booksCompletedLong = doc.getLong("booksCompleted");
                                        String profileImageUrl = doc.getString("profileImageUrl");
                                        
                                        name = (name != null) ? name : "Anonymous Reader";
                                        int booksCompleted = (booksCompletedLong != null) ? 
                                                           booksCompletedLong.intValue() : 0;
                                        
                                        leaderboardItems.add(new LeaderboardItem(
                                            name,
                                            booksCompleted,
                                            profileImageUrl
                                        ));
                                    }
                                }

                                if (leaderboardItems.isEmpty()) {
                                    Toast.makeText(this, "No users found in leaderboard", 
                                                 Toast.LENGTH_SHORT).show();
                                } else {
                                    leaderboardAdapter = new LeaderboardAdapter(leaderboardItems, this);
                                    leaderboardRecyclerView.setAdapter(leaderboardAdapter);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Leaderboard", "Error fetching leaderboard data", e);
                                Toast.makeText(this, "Failed to load leaderboard", 
                                             Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error fetching users", e);
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }
} 