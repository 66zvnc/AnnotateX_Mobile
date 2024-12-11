package com.example.annotatex_mobile;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

        // Load contacts by default
        loadContacts();

        // Set up search functionality
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadContacts(); // Show contacts when search is empty
                } else {
                    searchUsers(newText); // Show search results when typing
                }
                return true;
            }
        });
    }

    private void loadContacts() {
        ContentResolver contentResolver = getContentResolver();
        List<Friend> contactsList = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 100);
            return;
        }

        try (Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {

            if (cursor != null) {
                int nameColumnIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberColumnIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int photoUriColumnIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);

                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameColumnIndex);
                    String phoneNumber = cursor.getString(numberColumnIndex);
                    String photoUri = cursor.getString(photoUriColumnIndex);
                    
                    if (name != null && !name.trim().isEmpty() && 
                        phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                        Friend contact = new Friend(
                            phoneNumber,  // Using phone number as ID
                            name,         // Contact name
                            photoUri,     // Contact photo URI
                            phoneNumber,  // Using phone number as status/secondary text
                            false
                        );
                        contactsList.add(contact);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading contacts", e);
            Toast.makeText(this, "Error loading contacts: " + e.getMessage(),
                         Toast.LENGTH_LONG).show();
        }

        // Update the adapter with contacts
        usersAdapter.updateList(contactsList);
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

                        Friend user = new Friend(id, fullName != null ? fullName : username, profileImageUrl, status,false);
                        if (user.isValid()) {
                            filteredList.add(user);
                        }
                    }
                    usersAdapter.updateList(filteredList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching users", e));
    }

}
