package com.example.annotatex_mobile;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        CollectionReference booksCollection = firestore.collection("books");
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId != null) {
            booksCollection.whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    bookList.clear(); // Clear the book list
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String coverUrl = document.getString("coverUrl");
                        String description = document.getString("description");
                        String id = document.getId();
                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        bookList.add(book);
                        Log.d(TAG, "Book loaded: " + book.getTitle());
                    }
                    loadCollaborativeBooks(userId);
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                    // If Firestore fails, ensure preloaded books are still added
                    addPreloadedBooks();
                }
            });
        } else {
            // If no user is logged in, add preloaded books directly
            addPreloadedBooks();
        }
    }

    private void loadCollaborativeBooks(String userId) {
        firestore.collection("users")
                .document(userId)
                .collection("collaborativeBooks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Book collaborativeBook = document.toObject(Book.class);
                        if (!bookList.contains(collaborativeBook)) {
                            bookList.add(collaborativeBook);
                            Log.d(TAG, "Collaborative book added: " + collaborativeBook.getTitle());
                        }
                    }
                    addPreloadedBooks(); // Add preloaded books after collaborative books
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching collaborative books: ", e);
                    addPreloadedBooks(); // Ensure preloaded books are added even on failure
                })
                .addOnCompleteListener(task -> adapter.updateBooks(bookList)); // Always update adapter at the end
    }

    private void addPreloadedBooks() {
        Log.d(TAG, "Adding preloaded books...");
        if (!bookList.isEmpty()) {
            Log.d(TAG, "Preloaded books already exist in the list. Skipping...");
            return; // Prevent duplicate additions
        }

        bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
        bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
        bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
        bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));

        adapter.updateBooks(bookList); // Refresh adapter with the updated list
        Log.d(TAG, "Preloaded books added. Total: " + bookList.size());
    }

    @Override
    public void onPdfClick(Book book) {
        Log.d(TAG, "Book selected: " + book.getTitle());
        // Handle book selection
        if (getActivity() instanceof CollaborativeChatActivity) {
            ((CollaborativeChatActivity) getActivity()).addCollaborativeBook(book);
        }
        getParentFragmentManager().popBackStack(); // Close the fragment after selection
    }

    @Override
    public void onPdfClick(String pdfUrl) {
        Log.d(TAG, "PDF URL selected: " + pdfUrl);
        // Handle PDF URL click if necessary
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
}
