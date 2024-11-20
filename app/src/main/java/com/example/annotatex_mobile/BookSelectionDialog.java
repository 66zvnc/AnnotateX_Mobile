package com.example.annotatex_mobile;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookSelectionDialog extends Dialog {

    private final Context context;
    private final List<Book> books = new ArrayList<>();
    private BookSelectionAdapter adapter;

    public BookSelectionDialog(Context context, List<Book> initialBooks) {
        super(context);
        this.context = context;
        if (initialBooks != null) {
            this.books.addAll(initialBooks); // Use the provided books list
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_book_selection);

        // Set dynamic width for the dialog
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9), // 90% of the screen width
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Set up RecyclerView
        setupRecyclerView();

        // Fetch books from Firestore and add preloaded books
        fetchBooksFromFirestore();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewBooks);

        // Set up GridLayoutManager for 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        // Initialize adapter with the current book list
        adapter = new BookSelectionAdapter(context, books, book -> {
            // Handle book selection
            if (context instanceof CollaborativeChatActivity) {
                ((CollaborativeChatActivity) context).addCollaborativeBook(book);
            }
            dismiss();
        });

        recyclerView.setAdapter(adapter);
    }

    private void fetchBooksFromFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            addPreloadedBooks(); // Add preloaded books if no user is logged in
            notifyAdapter();
            return;
        }

        // Query the global "books" collection and filter by userId
        firestore.collection("books")
                .whereEqualTo("userId", currentUserId) // Filter books by the current user's ID
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    books.clear(); // Clear the list to avoid duplicates
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Book book = documentSnapshot.toObject(Book.class);

                            // Ensure user-uploaded books have proper IDs
                            if (book.getId() == null || book.getId().isEmpty()) {
                                book.setId(documentSnapshot.getId());
                            }

                            books.add(book);
                        }
                    }
                    addPreloadedBooks(); // Add preloaded books after fetching user books
                    notifyAdapter();
                })
                .addOnFailureListener(e -> {
                    // Handle the failure and add preloaded books
                    addPreloadedBooks();
                    notifyAdapter();
                });
    }

    private void addPreloadedBooks() {
        if (!containsPreloadedBooks()) { // Avoid duplicate addition of preloaded books
            books.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
            books.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
            books.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
            books.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
            books.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));
            books.add(new Book(R.drawable.book_6, "url_to_pdf_6", "Stripped", "Brenda M. Gonzalez", "Living and loving after trauma."));
            books.add(new Book(R.drawable.book_7, "url_to_pdf_7", "12 Rules for Life", "Jordan B. Peterson", "An antidote to chaos."));
            books.add(new Book(R.drawable.book_8, "url_to_pdf_8", "Readistan", "Shah Rukh Nadeem", "Summaries of the best books."));
            books.add(new Book(R.drawable.book_9, "url_to_pdf_9", "Reclaim Your Heart", "Yasmin Mogahed", "Free yourself from life's shackles."));
            books.add(new Book(R.drawable.book_10, "url_to_pdf_10", "Lost Islamic History", "Firas Alkhateeb", "Muslim civilization through the ages."));
        }
    }

    private boolean containsPreloadedBooks() {
        for (Book book : books) {
            if (book.isPreloaded()) {
                return true;
            }
        }
        return false;
    }

    private void notifyAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
