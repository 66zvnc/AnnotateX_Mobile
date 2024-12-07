package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class FriendsFragment extends Fragment {

    private RecyclerView combinedRecyclerView;
    private CombinedAdapter combinedAdapter;
    private List<Friend> friendsList;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private List<Group> groupsList;
    private ListenerRegistration groupsListenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        combinedRecyclerView = view.findViewById(R.id.combinedRecyclerView);
        combinedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        combinedAdapter = new CombinedAdapter(requireContext());
        combinedRecyclerView.setAdapter(combinedAdapter);

        // Start listening for real-time updates
        listenForFriendUpdates();
        listenForGroupUpdates();

        // Set up the "Add Friend" icon
        ImageView addFriendIcon = view.findViewById(R.id.addFriendIcon);
        addFriendIcon.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SearchUsersActivity.class));
        });

        // Set up the "Create Group" icon
        ImageView createGroupIcon = view.findViewById(R.id.addGroupIcon);
        createGroupIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), GroupCreationActivity.class);
            startActivity(intent);
        });

        // Set up SearchView
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterItems(newText);
                return true;
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize lists if they haven't been initialized
        if (friendsList == null) {
            friendsList = new ArrayList<>();
        }
        if (groupsList == null) {
            groupsList = new ArrayList<>();
        }
    }

    private void listenForFriendUpdates() {
        String currentUserId = auth.getCurrentUser().getUid();

        listenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        Log.e(TAG, "Error listening for friend updates", e);
                        return;
                    }

                    friendsList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Friend updatedFriend = document.toObject(Friend.class);
                        boolean found = false;

                        for (Friend friend : friendsList) {
                            if (friend.getId().equals(updatedFriend.getId())) {
                                friend.update(updatedFriend);
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            friendsList.add(updatedFriend);
                        }
                    }
                    updateLists();
                });
    }

    private void listenForGroupUpdates() {
        String currentUserId = auth.getCurrentUser().getUid();

        groupsListenerRegistration = firestore.collection("groups")
                .whereArrayContains("members", currentUserId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        Log.e(TAG, "Error listening for group updates", e);
                        return;
                    }

                    groupsList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Group group = document.toObject(Group.class);
                        group.setId(document.getId());
                        groupsList.add(group);
                        Log.d(TAG, "Found group: " + group.getName() + " with ID: " + group.getId());
                    }
                    updateLists();
                });
    }

    private void handleFriendUpdates(QuerySnapshot querySnapshot) {
        for (DocumentChange change : querySnapshot.getDocumentChanges()) {
            DocumentSnapshot document = change.getDocument();
            Friend updatedFriend = document.toObject(Friend.class);

            switch (change.getType()) {
                case ADDED:
                    // Add a new friend to the list
                    addOrUpdateFriend(updatedFriend);
                    break;
                case MODIFIED:
                    // Update the existing friend's information
                    updateFriend(updatedFriend);
                    break;
                case REMOVED:
                    // Remove the friend from the list and show "Add Friend" button
                    removeFriend(updatedFriend);
                    break;
            }
        }

        // Notify the adapter of data changes
        combinedAdapter.notifyDataSetChanged();
    }

    private void addOrUpdateFriend(Friend updatedFriend) {
        boolean friendExists = false;
        for (Friend friend : friendsList) {
            if (friend.getId().equals(updatedFriend.getId())) {
                friend.update(updatedFriend);
                friendExists = true;
                break;
            }
        }
        if (!friendExists) {
            friendsList.add(updatedFriend);
        }
    }

    private void updateFriend(Friend updatedFriend) {
        for (int i = 0; i < friendsList.size(); i++) {
            if (friendsList.get(i).getId().equals(updatedFriend.getId())) {
                friendsList.get(i).update(updatedFriend);
                break;
            }
        }
    }

    private void removeFriend(Friend removedFriend) {
        for (int i = 0; i < friendsList.size(); i++) {
            if (friendsList.get(i).getId().equals(removedFriend.getId())) {
                friendsList.get(i).setRemoved(true);
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        if (groupsListenerRegistration != null) {
            groupsListenerRegistration.remove();
        }
    }

    private void updateLists() {
        combinedAdapter.updateItems(groupsList, friendsList);
    }

    private void filterItems(String query) {
        if (query == null || query.isEmpty()) {
            // If query is empty, show all items
            combinedAdapter.updateItems(groupsList, friendsList);
            return;
        }

        query = query.toLowerCase().trim();

        // Filter groups
        List<Group> filteredGroups = new ArrayList<>();
        if (groupsList != null) {
            for (Group group : groupsList) {
                if (group.getName().toLowerCase().contains(query)) {
                    filteredGroups.add(group);
                }
            }
        }

        // Filter friends
        List<Friend> filteredFriends = new ArrayList<>();
        if (friendsList != null) {
            for (Friend friend : friendsList) {
                if (friend.getName().toLowerCase().contains(query)) {
                    filteredFriends.add(friend);
                }
            }
        }

        // Update adapter with filtered lists
        combinedAdapter.updateItems(filteredGroups, filteredFriends);
    }
}
