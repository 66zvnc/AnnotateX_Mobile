package com.example.annotatex_mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesFragment extends Fragment {

    private RecyclerView friendRequestsRecyclerView;
    private ActivitiesAdapter activitiesAdapter;
    private List<FriendRequest> activitiesList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activities, container, false);
        requireActivity().setTitle("Activities");

        // Initialize Firestore and FirebaseAuth
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView);
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        activitiesList = new ArrayList<>();
        activitiesAdapter = new ActivitiesAdapter(requireContext(), activitiesList);
        friendRequestsRecyclerView.setAdapter(activitiesAdapter);

        // Start listening for friend requests
        listenForFriendRequests();

        return view;
    }

    private void listenForFriendRequests() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) return;

        listenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) return;

                    activitiesList.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String senderId = document.getString("senderId");
                        String senderName = document.getString("senderName");
                        String receiverId = document.getString("receiverId");
                        long timestamp = document.getLong("timestamp");

                        FriendRequest request = new FriendRequest(senderId, senderName, receiverId, timestamp);
                        activitiesList.add(request);
                    }

                    // Notify the adapter to update the UI
                    activitiesAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up Firestore listener
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
