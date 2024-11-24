package com.example.annotatex_mobile;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

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
    private List<Book> filteredList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private SearchView searchView;
    private MotionLayout motionLayout;
    private boolean isSearchViewVisible = true;

    // Button references for "ALL" and "COLLABS"
    private TextView sortAllButton;
    private TextView sortCollabsButton;
    private View underline;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        motionLayout = view.findViewById(R.id.motionLayout);

        bookList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new LibraryAdapter(getContext(), filteredList, this, false); // Pass false for library mode

        // Initialize UI elements
        sortAllButton = view.findViewById(R.id.sort_all);
        sortCollabsButton = view.findViewById(R.id.sort_collabs);
        underline = view.findViewById(R.id.underlineContainer);

        // Use ViewTreeObserver to wait until the layout is fully drawn
        sortAllButton.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // Ensure the listener is only triggered once
                sortAllButton.getViewTreeObserver().removeOnPreDrawListener(this);

                // Set the default underline to the "ALL" button when the fragment loads
                setUnderlinePosition(sortAllButton);
                return true;
            }
        });

        // Set up RecyclerView with dynamic columns based on screen size
        pdfGalleryRecyclerView = view.findViewById(R.id.pdfGalleryRecyclerView);
        int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2; // 3 for tablets, 2 for phones
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), columns);
        pdfGalleryRecyclerView.setLayoutManager(layoutManager);

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.recycler_view_item_spacing);
        pdfGalleryRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));
        pdfGalleryRecyclerView.setAdapter(adapter);

        // Set up SearchView
        searchView = view.findViewById(R.id.searchView);
        setupSearchView();

        setupIcons(view);

        addPredefinedBooks(); // Add preloaded books first
        showLoadingIndicator();
        loadBooksFromFirestore(); // Load Firestore books

        setupScrollListener();

        // Set the button click listeners for "ALL" and "COLLABS"
        sortAllButton.setOnClickListener(v -> setUnderlinePosition(sortAllButton));
        sortCollabsButton.setOnClickListener(v -> setUnderlinePosition(sortCollabsButton));

        return view;
    }

    private void setupScrollListener() {
        pdfGalleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Check if the user is scrolling up or down
                if (dy > 0 && isSearchViewVisible) {
                    // User is scrolling down, hide the search bar
                    motionLayout.transitionToEnd();
                    isSearchViewVisible = false;
                } else if (dy < 0 && !isSearchViewVisible) {
                    // User is scrolling up, show the search bar
                    motionLayout.transitionToStart();
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
        ImageView activitiesIcon = view.findViewById(R.id.icon_notifications);
        activitiesIcon.setOnClickListener(v -> openFragment(new NotificationsFragment()));
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void filterBooks(String query) {
        filteredList.clear();
        String lowerCaseQuery = query.toLowerCase();

        for (Book book : bookList) {
            if (book.isHidden() && (query.isEmpty() || !matchesQuery(book, lowerCaseQuery))) {
                continue;
            }
            if (query.isEmpty() || matchesQuery(book, lowerCaseQuery)) {
                filteredList.add(book);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private boolean matchesQuery(Book book, String query) {
        String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
        String author = book.getAuthor() != null ? book.getAuthor().toLowerCase() : "";
        return title.contains(query) || author.contains(query);
    }

    private void showLoadingIndicator() {
        filteredList.clear();
        filteredList.add(new Book("loading", "", "", "Loading books...", "", "", ""));
        adapter.notifyDataSetChanged();
    }

    private void loadBooksFromFirestore() {
        CollectionReference booksCollection = firestore.collection("books");
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId != null) {
            booksCollection.whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Book> firestoreBooks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String coverUrl = document.getString("coverUrl");
                        String description = document.getString("description");
                        String id = document.getId();
                        boolean hidden = document.getBoolean("hidden") != null && document.getBoolean("hidden");
                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        book.setHidden(hidden);
                        firestoreBooks.add(book);
                    }

                    // Add Firestore books first, then preloaded books
                    bookList.clear();
                    bookList.addAll(firestoreBooks);
                    addPredefinedBooks();
                    filterBooks(searchView.getQuery().toString());
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                }
            });
        }
    }


    private void addPredefinedBooks() {
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

    private void setUnderlinePosition(View selectedButton) {
        if (selectedButton instanceof TextView) {
            TextView button = (TextView) selectedButton;
            button.setTextColor(getResources().getColor(R.color.dark_pink));
        }

        ViewGroup.LayoutParams layoutParams = underline.getLayoutParams();
        layoutParams.width = selectedButton.getWidth();
        underline.setLayoutParams(layoutParams);

        float xPosition = selectedButton.getX();
        underline.setTranslationX(xPosition);
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

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration {
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
