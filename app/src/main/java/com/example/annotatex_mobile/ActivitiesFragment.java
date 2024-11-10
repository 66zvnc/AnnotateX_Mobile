package com.example.annotatex_mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
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

    private static final String CHANNEL_ID = "friend_requests_channel";
    private RecyclerView friendRequestsRecyclerView;
    private ActivitiesAdapter activitiesAdapter;
    private List<String> activitiesList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activities, container, false);
        requireActivity().setTitle("Activities");

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView);
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        activitiesList = new ArrayList<>();
        activitiesAdapter = new ActivitiesAdapter(requireContext(), activitiesList);
        friendRequestsRecyclerView.setAdapter(activitiesAdapter);

        // Set up notifications channel
        createNotificationChannel();

        // Start listening for friend requests
        listenForFriendRequests();

        return view;
    }

    private void listenForFriendRequests() {
        String currentUserId = auth.getCurrentUser().getUid();

        listenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) return;

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String senderName = document.getString("senderName");
                        if (senderName != null) {
                            String notificationMessage = senderName + " sent you a friend request!";
                            activitiesList.add(notificationMessage);
                            activitiesAdapter.notifyDataSetChanged();

                            // Show system notification
                            showSystemNotification(notificationMessage);
                        }
                    }
                });
    }


    private void showSystemNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(requireContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Friend Request")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Friend Requests";
            String description = "Notifications for friend requests";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
