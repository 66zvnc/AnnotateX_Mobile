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
        adapter = new LibraryAdapter(getContext(), bookList, this);

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

    /**
     * Load books from Firestore and add preloaded books.
     */
    private void loadBooksFromFirestore() {
        CollectionReference booksCollection = firestore.collection("books");
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId != null) {
            booksCollection.whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    bookList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String coverUrl = document.getString("coverUrl");
                        String description = document.getString("description");
                        String id = document.getId();
                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        bookList.add(book);
                    }
                    addPreloadedBooks(); // Add predefined books
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                }
            });
        } else {
            addPreloadedBooks(); // Add predefined books if the user is not logged in
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Add predefined books to the list.
     */
    private void addPreloadedBooks() {
        Log.d(TAG, "Adding preloaded books.");
        bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
        bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
        bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
        bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));
        bookList.add(new Book(R.drawable.book_6, "url_to_pdf_6", "Stripped", "Brenda M. Gonzalez", "Living and loving after trauma."));
        bookList.add(new Book(R.drawable.book_7, "url_to_pdf_7", "12 Rules for Life", "Jordan B. Peterson", "An antidote to chaos."));
        bookList.add(new Book(R.drawable.book_8, "url_to_pdf_8", "Readistan", "Shah Rukh Nadeem", "Summaries of the best books."));
        bookList.add(new Book(R.drawable.book_9, "url_to_pdf_9", "Reclaim Your Heart", "Yasmin Mogahed", "Free yourself from life's shackles."));
        bookList.add(new Book(R.drawable.book_10, "url_to_pdf_10", "Lost Islamic History", "Firas Alkhateeb", "Muslim civilization through the ages."));
    }

    @Override
    public void onPdfClick(Book book) {
        // Handle book selection
        if (getActivity() instanceof CollaborativeChatActivity) {
            ((CollaborativeChatActivity) getActivity()).addCollaborativeBook(book);
        }
        getParentFragmentManager().popBackStack(); // Close the fragment after selection
    }

    @Override
    public void onPdfClick(String pdfUrl) {
        // Handle PDF URL click if necessary
    }

    /**
     * Adds spacing between items in the RecyclerView.
     */
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
