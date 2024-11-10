package com.example.annotatex_mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchUsersActivity extends AppCompatActivity {

    private static final String TAG = "SearchUsersActivity";
    private RecyclerView usersRecyclerView;
    private UsersAdapter usersAdapter;
    private List<Friend> usersList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        usersList = new ArrayList<>();
        usersAdapter = new UsersAdapter(this, usersList);
        usersRecyclerView.setAdapter(usersAdapter);

        // Initialize SearchView
        searchView = findViewById(R.id.searchView);
        if (searchView == null) {
            Log.e(TAG, "SearchView is null");
            return;
        }

        setupSearchView();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUsers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 2) {
                    searchUsers(newText);
                } else {
                    usersList.clear();
                    usersAdapter.updateList(usersList);
                }
                return true;
            }
        });
    }

    private void searchUsers(String query) {
        Log.d(TAG, "Searching for users with query: " + query);
        if (query.isEmpty()) {
            usersList.clear();
            usersAdapter.updateList(usersList);
            return;
        }

        firestore.collection("users")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Friend> filteredList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String username = document.getString("username");
                        String fullName = document.getString("fullName");
                        String profileImageUrl = document.getString("profileImageUrl");
                        String status = document.getString("status");

                        Friend user = new Friend(id, fullName != null ? fullName : username, profileImageUrl, status);
                        if (user.isValid()) {
                            filteredList.add(user);
                        }
                    }
                    usersAdapter.updateList(filteredList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching users", e));
    }

}
