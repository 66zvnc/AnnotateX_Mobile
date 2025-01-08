package com.example.annotatex_mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import java.util.Collections;

public class LeaderboardFragment extends Fragment {
    private FirebaseFirestore firestore;
    private String userId;
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter leaderboardAdapter;
    private String actualUserRank = "0";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews(view);
        loadUserStats();
        setupLeaderboard();

        // Set up back button
        view.findViewById(R.id.backButton).setOnClickListener(v -> 
            requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void initializeViews(View view) {
        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
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
                    userData.put("username", currentUsername);
                    
                    // Create or update user document
                    firestore.collection("users")
                            .document(userId)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                setupLeaderboard();
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
                            break;
                        }
                        rank++;
                    }
                    
                    if (!userFound) {
                        actualUserRank = "N/A";
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error getting user rank", e);
                    actualUserRank = "N/A";
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
        List<LeaderboardItem> leaderboardItems = new ArrayList<>();
        
        // Get all users at once
        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String userName = doc.getString("username");
                        String profileImageUrl = doc.getString("profileImageUrl");
                        long booksCompleted = doc.getLong("booksCompleted") != null ? 
                                doc.getLong("booksCompleted") : 0;

                        // Check if this is the current user
                        boolean isCurrentUser = doc.getId().equals(currentUserId);

                        LeaderboardItem item = new LeaderboardItem(
                                userName,
                                booksCompleted,
                                profileImageUrl,
                                isCurrentUser
                        );
                        leaderboardItems.add(item);
                    }

                    // Sort the list by books completed in descending order
                    Collections.sort(leaderboardItems, (item1, item2) -> 
                        Long.compare(item2.getBooksRead(), item1.getBooksRead()));

                    // Set up the adapter
                    if (leaderboardItems.isEmpty()) {
                        Toast.makeText(requireContext(), 
                            "No users found in leaderboard", Toast.LENGTH_SHORT).show();
                    } else {
                        leaderboardAdapter = new LeaderboardAdapter(leaderboardItems, requireContext());
                        leaderboardRecyclerView.setAdapter(leaderboardAdapter);
                        
                        // Find current user's rank
                        for (int i = 0; i < leaderboardItems.size(); i++) {
                            if (leaderboardItems.get(i).isCurrentUser()) {
                                int rank = i + 1;
                                actualUserRank = rank + getRankSuffix(rank);
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error fetching users", e);
                    Toast.makeText(requireContext(), 
                        "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
                });
    }

    // Add getter method for the actual rank
    public String getCurrentRank() {
        return actualUserRank;
    }
} 