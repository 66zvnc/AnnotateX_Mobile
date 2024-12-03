package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private List<Friend> friendsList;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listenerRegistration;
    private RecyclerView groupsRecyclerView;
    private GroupAdapter groupAdapter;
    private List<Group> groupsList;
    private ListenerRegistration groupsListenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        friendsList = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(requireContext(), friendsList);
        friendsRecyclerView.setAdapter(friendsAdapter);

        // Initialize groups RecyclerView
        groupsRecyclerView = view.findViewById(R.id.groupsRecyclerView);
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        groupsList = new ArrayList<>();
        groupAdapter = new GroupAdapter(requireContext(), groupsList, group -> {
            // Handle group click - navigate to group chat
            Intent intent = new Intent(getContext(), GroupChatActivity.class);
            intent.putExtra("groupId", group.getId());
            intent.putExtra("groupName", group.getName());
            startActivity(intent);
        });
        groupsRecyclerView.setAdapter(groupAdapter);

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

        return view;
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
                    friendsAdapter.notifyDataSetChanged();
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
                    groupAdapter.notifyDataSetChanged();
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
        friendsAdapter.notifyDataSetChanged();
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
}
