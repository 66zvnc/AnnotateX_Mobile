package com.example.annotatex_mobile;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
    private EditText searchView;
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
        setupRecyclerView();
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
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterBooks(searchView.getText().toString());
                return true;
            }
            return false;
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
                    loadCollaborativeBooks(userId); // Load collaborative books after personal books
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
                        // Explicitly get all fields instead of using toObject
                        String id = document.getId();
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String pdfUrl = document.getString("pdfUrl");
                        String coverUrl = document.getString("coverUrl");
                        
                        Log.d(TAG, "Loading collaborative book: " + title);
                        Log.d(TAG, "Cover URL from Firestore: " + coverUrl);
                        
                        if (coverUrl != null && !coverUrl.isEmpty()) {
                            // Create book with all fields explicitly
                            Book collaborativeBook = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                            
                            // Set any additional collaborative-specific properties
                            List<String> collaborators = (List<String>) document.get("collaborators");
                            if (collaborators != null) {
                                collaborativeBook.setCollaborators(collaborators);
                            }
                            
                            if (!bookList.contains(collaborativeBook)) {
                                Log.d(TAG, "Adding collaborative book: " + title + " with cover: " + coverUrl);
                                bookList.add(collaborativeBook);
                            }
                        } else {
                            Log.w(TAG, "No cover URL found for collaborative book: " + title);
                        }
                    }
                    addPreloadedBooks(); // Add predefined books after fetching collaborative ones
                    adapter.updateBooks(bookList); // Update adapter with the full book list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching collaborative books: ", e);
                    // Still add preloaded books and update adapter even if collaborative books fail to load
                    addPreloadedBooks();
                    adapter.updateBooks(bookList);
                });
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

    private void setupRecyclerView() {
        int spanCount = calculateSpanCount();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        pdfGalleryRecyclerView.setLayoutManager(layoutManager);
    }

    private int calculateSpanCount() {
        // Get the screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        
        // Different minimum widths for phones and tablets
        int desiredMinWidth;
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            desiredMinWidth = 280; // Much bigger minimum width for tablets (increased from 200)
        } else {
            desiredMinWidth = 160; // Original minimum width for phones
        }
        
        // Convert dp to px
        float density = getResources().getDisplayMetrics().density;
        int minWidthPx = (int) (desiredMinWidth * density);
        
        // Calculate span count
        return Math.max(2, screenWidth / minWidthPx);
    }
}
