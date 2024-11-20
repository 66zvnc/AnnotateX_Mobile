package com.example.annotatex_mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendSelectionDialog {

    public interface FriendSelectionListener {
        void onFriendSelected(String friendId, String friendName);
    }

    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FriendSelectionDialog(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void show(FriendSelectionListener listener) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(currentUserId).collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> friendIds = new ArrayList<>();
                    List<String> friendNames = new ArrayList<>();

                    queryDocumentSnapshots.forEach(doc -> {
                        friendIds.add(doc.getId());
                        String name = doc.getString("name");
                        friendNames.add(name != null ? name : "Unknown Friend");
                    });

                    if (friendNames.isEmpty()) {
                        Toast.makeText(context, "No friends available to share with", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Select a Friend");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, friendNames);
                    builder.setAdapter(adapter, (dialog, which) -> {
                        String selectedFriendId = friendIds.get(which);
                        String selectedFriendName = friendNames.get(which);
                        listener.onFriendSelected(selectedFriendId, selectedFriendName);
                    });

                    builder.setCancelable(true);
                    builder.create().show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
    }
}
