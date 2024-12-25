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
    private String actualUserRank = "0";

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
        // Get current user's username from Firestore
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentUsername = documentSnapshot.getString("username");
                    String displayName = currentUsername != null ? currentUsername : "Anonymous";
                    
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", displayName);
                    userData.put("booksCompleted", 25); // Set a middle value for example
                    userData.put("username", currentUsername);
                    
                    // Create or update user document
                    firestore.collection("users")
                            .document(userId)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                booksCompleted.setText("25"); // Update UI with example value
                                updateUserRank(25);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Leaderboard", "Error creating/updating user", e);
                            });
                });
    }

    private void updateUserRank(int userBooks) {
        firestore.collection("users")
                .orderBy("booksCompleted", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rank = 1;
                    int totalUsers = querySnapshot.size();
                    boolean userFound = false;
                    
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (doc.getId().equals(userId)) {
                            userFound = true;
                            actualUserRank = String.valueOf(rank); // Store the actual rank
                            String rankSuffix = getRankSuffix(rank);
                            currentRank.setText(rank + rankSuffix + " / " + totalUsers);
                            break;
                        }
                        rank++;
                    }
                    
                    if (!userFound) {
                        actualUserRank = "N/A";
                        currentRank.setText("N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error getting user rank", e);
                    actualUserRank = "N/A";
                    currentRank.setText("N/A");
                });
    }

    private String getRankSuffix(int rank) {
        if (rank >= 11 && rank <= 13) {
            return "th";
        }
        switch (rank % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
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

                    // Add random stats to users with a distribution around the current user's score
                    for (DocumentSnapshot doc : allUsersSnapshot) {
                        String docUserId = doc.getId();
                        if (!docUserId.equals(currentUserId)) {
                            // Generate random numbers to create a distribution
                            // Some users will have more books, some less than current user
                            int randomBooks;
                            if (Math.random() < 0.5) {
                                // Lower than current user (5-24)
                                randomBooks = (int) (Math.random() * 20) + 5;
                            } else {
                                // Higher than current user (26-45)
                                randomBooks = (int) (Math.random() * 20) + 26;
                            }
                            
                            Map<String, Object> userData = new HashMap<>();
                            String username = doc.getString("username");
                            String name = username != null && !username.isEmpty() ? username : "Anonymous Reader";
                            
                            userData.put("name", name);
                            userData.put("booksCompleted", randomBooks);
                            userData.put("username", username);
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
                                    Log.d("Leaderboard", "User found: " + doc.getString("name") + 
                                                       " Books: " + doc.getLong("booksCompleted"));
                                    
                                    String name = doc.getString("name");
                                    Long booksCompletedLong = doc.getLong("booksCompleted");
                                    String profileImageUrl = doc.getString("profileImageUrl");
                                    
                                    name = (name != null) ? name : "Anonymous Reader";
                                    int booksCompleted = (booksCompletedLong != null) ? 
                                                       booksCompletedLong.intValue() : 0;
                                    
                                    // Create LeaderboardItem with isCurrentUser flag
                                    LeaderboardItem item = new LeaderboardItem(
                                        name,
                                        booksCompleted,
                                        profileImageUrl,
                                        docUserId.equals(currentUserId) // Set isCurrentUser flag
                                    );
                                    leaderboardItems.add(item);
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

    // Add getter method for the actual rank
    public String getCurrentRank() {
        return actualUserRank;
    }
} 