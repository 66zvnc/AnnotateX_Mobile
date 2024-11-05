package com.example.annotatex_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements PdfGalleryAdapter.OnPdfClickListener {

    private static final String TAG = "LibraryFragment";
    private RecyclerView pdfGalleryRecyclerView;
    private PdfGalleryAdapter adapter;
    private List<Book> bookList;
    private List<Book> filteredList;
    private FirebaseFirestore firestore;
    private SearchView searchView;
    private MotionLayout motionLayout;
    private boolean isSearchViewVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_library, container, false);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize MotionLayout
        motionLayout = view.findViewById(R.id.motionLayout);

        // Initialize book list and adapter
        bookList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new PdfGalleryAdapter(getContext(), filteredList, this);
        pdfGalleryRecyclerView = view.findViewById(R.id.pdfGalleryRecyclerView);
        pdfGalleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        pdfGalleryRecyclerView.setAdapter(adapter);

        // Initialize SearchView and set up query listener
        searchView = view.findViewById(R.id.searchView);
        setupSearchView();

        // Load books from Firestore and add predefined books
        loadBooksFromFirestore();

        // Set up scroll listener for hiding/showing SearchView
        setupScrollListener();

        return view;
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

    private void filterBooks(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(bookList); // Show all books if query is empty
        } else {
            for (Book book : bookList) {
                String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
                String author = book.getAuthor() != null ? book.getAuthor().toLowerCase() : "";
                if (title.contains(query.toLowerCase()) || author.contains(query.toLowerCase())) {
                    filteredList.add(book);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadBooksFromFirestore() {
        CollectionReference booksCollection = firestore.collection("books");
        booksCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                bookList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String pdfUrl = document.getString("pdfUrl");
                    String title = document.getString("title");
                    String author = document.getString("author");
                    String coverUrl = document.getString("coverUrl");
                    String description = document.getString("description");

                    String id = document.getId();
                    Book book = new Book(id, coverUrl, pdfUrl, title, author, description);

                    bookList.add(book);
                }

                addPredefinedBooks();
                filterBooks(searchView.getQuery().toString()); // Initial filter
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    private void addPredefinedBooks() {
            bookList.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "What the rich teach their kids about money."));
            bookList.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "An easy & proven way to build good habits."));
            bookList.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
            bookList.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "What we learned from living by the rules of 50 self-help books."));
            bookList.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey in and out of emotional captivity."));
            bookList.add(new Book(R.drawable.book_6, "url_to_pdf_6", "Stripped", "Brenda M. Gonzalez", "Learning to live and love after trauma."));
            bookList.add(new Book(R.drawable.book_7, "url_to_pdf_7", "12 Rules for Life", "Jordan B. Peterson", "An antidote to chaos."));
            bookList.add(new Book(R.drawable.book_8, "url_to_pdf_8", "Readistan", "Shah Rukh Nadeem", "The best books summarized in 10 minutes."));
            bookList.add(new Book(R.drawable.book_9, "url_to_pdf_9", "Reclaim Your Heart", "Yasmin Mogahed", "Breaking free from life’s shackles."));
            bookList.add(new Book(R.drawable.book_10, "url_to_pdf_10", "Lost Islamic History", "Firas Alkhateeb", "Reclaiming Muslim civilization from the past."));
    }

    private void setupScrollListener() {
        pdfGalleryRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0 && isSearchViewVisible) {
                    // Scrolling down, hide the SearchView
                    motionLayout.transitionToEnd();
                    isSearchViewVisible = false;
                } else if (dy < 0 && !isSearchViewVisible) {
                    // Scrolling up, show the SearchView
                    motionLayout.transitionToStart();
                    isSearchViewVisible = true;
                }
            }
        });
    }

    @Override
    public void onPdfClick(Book book) {
        Intent intent = new Intent(getActivity(), DetailsActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    @Override
    public void onPdfClick(String pdfUrl) {
        // Optional, not required if using the Book object
    }

    public void addBookToLibrary(Book book) {
        firestore.collection("books").document(book.getId()).set(book)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Book successfully added to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding book to Firestore", e));

        // Update book list locally
        bookList.add(book);
        filterBooks(searchView.getQuery().toString()); // Update filter with new book
    }
}
