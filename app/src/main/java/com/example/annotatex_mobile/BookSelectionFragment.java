package com.example.annotatex_mobile;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookSelectionFragment extends Fragment implements LibraryAdapter.OnPdfClickListener {

    private static final String TAG = "BookSelectionFragment";

    private RecyclerView bookSelectionRecyclerView;
    private LibraryAdapter adapter;
    private List<Book> bookList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private OnBookSelectedListener bookSelectedListener;

    public static BookSelectionFragment newInstance(OnBookSelectedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("BookSelectedListener cannot be null");
        }
        BookSelectionFragment fragment = new BookSelectionFragment();
        fragment.bookSelectedListener = listener;
        return fragment;
    }

    public void setBookSelectedListener(OnBookSelectedListener listener) {
        this.bookSelectedListener = listener;
        if (listener == null) {
            Log.w(TAG, "BookSelectedListener is being set to null");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_selection, container, false);

        // Initialize Firebase services
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize book list and adapter
        bookList = new ArrayList<>();
        adapter = new LibraryAdapter(getContext(), bookList, this, true);

        // Configure RecyclerView
        bookSelectionRecyclerView = view.findViewById(R.id.bookSelectionRecyclerView);
        int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2; // Adjust for tablets/phones
        bookSelectionRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columns));
        bookSelectionRecyclerView.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.recycler_view_item_spacing)));
        bookSelectionRecyclerView.setAdapter(adapter);

        // Load books into the list
        loadBooksFromFirestore();

        return view;
    }

    private void loadBooksFromFirestore() {
        Log.d(TAG, "Fetching books from Firestore...");
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e(TAG, "No user logged in");
            return;
        }

        firestore.collection("books")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String coverUrl = document.getString("coverUrl");
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");

                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        bookList.add(book);
                        Log.d(TAG, "Added book: " + book.getTitle());
                    }
                    adapter.updateBooks(bookList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading books", e);
                    Toast.makeText(getContext(), "Failed to load books", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onPdfClick(Book book) {
        Log.d(TAG, "Book selected: " + book.getTitle());
        if (bookSelectedListener != null) {
            bookSelectedListener.onBookSelected(book);
        } else {
            Log.e(TAG, "BookSelectedListener is null");
            Toast.makeText(getContext(), "Unable to select book at this time", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPdfClick(String pdfUrl) {

    }

    private static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;
            if (parent.getChildLayoutPosition(view) < 2) {
                outRect.top = space;
            }
        }
    }

    public interface OnBookSelectedListener {
        void onBookSelected(Book book);
    }
}
