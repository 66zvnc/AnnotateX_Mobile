package com.example.annotatex_mobile;

import static android.content.ContentValues.TAG;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private List<Friend> friendsList;

    private FirebaseAuth auth;
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        friendsList = new ArrayList<>();

        friendsAdapter = new FriendsAdapter(requireContext(), friendsList);
        friendsRecyclerView.setAdapter(friendsAdapter);

        // Set up the "Add Friend" icon
        ImageView addFriendIcon = view.findViewById(R.id.addFriendIcon);
        addFriendIcon.setOnClickListener(v -> {
            // Open the SearchUsersActivity
            startActivity(new Intent(getContext(), SearchUsersActivity.class));
        });

        return view;
    }

    private void addFriend(Friend user) {
        String currentUserId = auth.getCurrentUser().getUid();

        if (user != null && user.isValid() && !user.getId().equals(currentUserId)) {

            firestore.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .document(user.getId())
                    .set(user)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Friend added successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding friend", e));
        }
    }


}
