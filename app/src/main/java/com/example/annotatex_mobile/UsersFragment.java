package com.example.annotatex_mobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private RecyclerView usersRecyclerView;
    private UsersAdapter usersAdapter;
    private List<Friend> usersList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_search_users, container, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        usersRecyclerView = view.findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        usersList = new ArrayList<>();
        usersAdapter = new UsersAdapter(requireContext(), usersList);
        usersRecyclerView.setAdapter(usersAdapter);

        loadUsers();

        return view;
    }

    private void loadUsers() {
        CollectionReference usersRef = firestore.collection("users");
        String currentUserId = auth.getCurrentUser().getUid();

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                usersList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String userId = document.getId();
                    if (!userId.equals(currentUserId)) {
                        String name = document.getString("name");
                        String profileImageUrl = document.getString("profileImageUrl");
                        usersList.add(new Friend(userId, name, profileImageUrl, "Available", false));
                    }
                }
                usersAdapter.notifyDataSetChanged();
            }
        });
    }
}
