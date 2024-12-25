package com.example.annotatex_mobile;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

        addExampleUsers();

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
        firestore.collection("books")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int completed = querySnapshot.size();
                    booksCompleted.setText(String.valueOf(completed));
                    updateUserRank(completed);
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
        firestore.collection("users")
                .orderBy("booksCompleted", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<LeaderboardItem> leaderboardItems = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        leaderboardItems.add(new LeaderboardItem(
                            doc.getString("name"),
                            doc.getLong("booksCompleted").intValue(),
                            doc.getString("profileImageUrl")
                        ));
                    }
                    leaderboardAdapter = new LeaderboardAdapter(leaderboardItems);
                    leaderboardRecyclerView.setAdapter(leaderboardAdapter);
                });
    }

    private void addExampleUsers() {
        List<Map<String, Object>> exampleUsers = new ArrayList<>();
        
        // Example user data
        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "Sarah Johnson");
        user1.put("booksCompleted", 45);
        user1.put("profileImageUrl", "https://example.com/sarah.jpg");
        user1.put("email", "sarah.j@example.com");
        
        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Michael Chen");
        user2.put("booksCompleted", 38);
        user2.put("profileImageUrl", "https://example.com/michael.jpg");
        user2.put("email", "m.chen@example.com");
        
        Map<String, Object> user3 = new HashMap<>();
        user3.put("name", "Emma Davis");
        user3.put("booksCompleted", 32);
        user3.put("profileImageUrl", "https://example.com/emma.jpg");
        user3.put("email", "emma.d@example.com");
        
        Map<String, Object> user4 = new HashMap<>();
        user4.put("name", "James Wilson");
        user4.put("booksCompleted", 29);
        user4.put("profileImageUrl", "https://example.com/james.jpg");
        user4.put("email", "j.wilson@example.com");
        
        Map<String, Object> user5 = new HashMap<>();
        user5.put("name", "Sophia Martinez");
        user5.put("booksCompleted", 27);
        user5.put("profileImageUrl", "https://example.com/sophia.jpg");
        user5.put("email", "s.martinez@example.com");

        exampleUsers.add(user1);
        exampleUsers.add(user2);
        exampleUsers.add(user3);
        exampleUsers.add(user4);
        exampleUsers.add(user5);

        // Add users to Firestore
        for (Map<String, Object> user : exampleUsers) {
            firestore.collection("users")
                    .document() // Firestore will generate a random ID
                    .set(user)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Leaderboard", "Example user added successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Leaderboard", "Error adding example user", e);
                    });
        }
    }
} 