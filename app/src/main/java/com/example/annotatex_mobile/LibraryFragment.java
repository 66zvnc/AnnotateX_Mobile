package com.example.annotatex_mobile;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements LibraryAdapter.OnPdfClickListener {

    private static final String TAG = "LibraryFragment";
    private RecyclerView pdfGalleryRecyclerView;
    private LibraryAdapter adapter;
    private List<Book> bookList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private MotionLayout motionLayout;
    private SearchView searchView;
    private boolean isSearchViewVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        // Initialize Firebase services
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize book list and adapter
        bookList = new ArrayList<>();
        adapter = new LibraryAdapter(getContext(), bookList, this, false); // Pass false for library mode

        // Configure RecyclerView
        pdfGalleryRecyclerView = view.findViewById(R.id.pdfGalleryRecyclerView);
        int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2; // Adjust for tablets/phones
        pdfGalleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columns));
        pdfGalleryRecyclerView.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.recycler_view_item_spacing)));
        pdfGalleryRecyclerView.setAdapter(adapter);

        // Configure MotionLayout and SearchView
        motionLayout = view.findViewById(R.id.motionLayout);
        searchView = view.findViewById(R.id.searchView);
        setupSearchView();

        // Scroll listener for hiding/showing the search bar
        setupScrollListener();

        // Add Notifications icon click listener
        setupIcons(view);

        // Load books from Firestore
        loadBooksFromFirestore();

        return view;
    }

    private void setupScrollListener() {
        pdfGalleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0 && isSearchViewVisible) {
                    motionLayout.transitionToEnd(); // Hide search bar
                    isSearchViewVisible = false;
                } else if (dy < 0 && !isSearchViewVisible) {
                    motionLayout.transitionToStart(); // Show search bar
                    isSearchViewVisible = true;
                }
            }
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBooks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBooks(newText);
                return true;
            }
        });
    }

    private void setupIcons(View view) {
        ImageView notificationsIcon = view.findViewById(R.id.icon_notifications);
        notificationsIcon.setOnClickListener(v -> openFragment(new NotificationsFragment()));
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void filterBooks(String query) {
        List<Book> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Book book : bookList) {
            if (book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(book);
            } else if (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(book);
            }
        }

        adapter.updateBooks(filteredList);
    }

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
                    loadCollaborativeBooks(userId);
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                }
            });
        } else {
            addPreloadedBooks(); // Add predefined books if the user is not logged in
            adapter.updateBooks(bookList);
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
                        }
                    }
                    addPreloadedBooks(); // Add predefined books after fetching collaborative ones
                    adapter.updateBooks(bookList); // Update adapter with the full book list
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching collaborative books: ", e));
    }

    private void addPreloadedBooks() {
        bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
        bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
        bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
        bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));
    }

    @Override
    public void onPdfClick(Book book) {
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    @Override
    public void onPdfClick(String pdfUrl) {
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
