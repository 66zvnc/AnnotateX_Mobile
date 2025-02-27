package com.example.annotatex_mobile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.annotatex_mobile.LibraryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookSelectionActivity extends AppCompatActivity implements LibraryAdapter.OnPdfClickListener {
    private RecyclerView recyclerView;
    private LibraryAdapter adapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_selection);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.bookSelectionRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new LibraryAdapter(this, new ArrayList<>(), this, true); // Pass 'this' as the listener
        recyclerView.setAdapter(adapter);
        
        // Set up toolbar back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadAvailableBooks();
    }

    @Override
    public void onPdfClick(Book book) {
        if (book == null) {
            Log.e("BookSelection", "Cannot select null book");
            Toast.makeText(this, "Error: Invalid book selection", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create intent and explicitly cast book as Parcelable
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_book", (Parcelable) book);
            
            // Log the book data being sent
            Log.d("BookSelection", "Sending book: " + book.getTitle());
            
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            Log.e("BookSelection", "Error sending book result", e);
            Toast.makeText(this, "Error: Could not select book", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPdfClick(String pdfUrl) {
        // Not needed for book selection, but must be implemented
    }

    private void loadAvailableBooks() {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("books")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> books = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String coverUrl = document.getString("coverUrl");
                        String pdfUrl = document.getString("pdfUrl");
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");

                        Log.d("BookSelection", "Found book: " + title);
                        
                        // Create book with all fields explicitly
                        Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                        books.add(book);
                    }
                    
                    // Add preloaded books after fetching user's books
                    addPreloadedBooks(books);
                    
                    // Update adapter with all books
                    adapter.updateBooks(books);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load books", Toast.LENGTH_SHORT).show();
                    Log.e("BookSelection", "Error loading books", e);
                    
                    // If loading fails, at least show preloaded books
                    List<Book> preloadedBooks = new ArrayList<>();
                    addPreloadedBooks(preloadedBooks);
                    adapter.updateBooks(preloadedBooks);
                });
    }

    private void addPreloadedBooks(List<Book> books) {
        books.add(new Book(R.drawable.book_1, "url_to_pdf_1", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Financial wisdom from the rich."));
        books.add(new Book(R.drawable.book_2, "url_to_pdf_2", "Atomic Habits", "James Clear", "Build good habits, break bad ones."));
        books.add(new Book(R.drawable.book_3, "url_to_pdf_3", "Best Self", "Mike Bayer", "Be you, only better."));
        books.add(new Book(R.drawable.book_4, "url_to_pdf_4", "How to Be Fine", "Kristen Meinzer", "Lessons from self-help books."));
        books.add(new Book(R.drawable.book_5, "url_to_pdf_5", "Out of the Box", "Suzanne Dudley", "A journey of emotional resilience."));
    }
} 