package com.example.annotatex_mobile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CategoryActivity extends AppCompatActivity {
    private RecyclerView booksRecyclerView;
    private EditText searchView;
    private CategoryBooksAdapter adapter;
    private List<Book> booksList;
    private FirebaseFirestore firestore;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize books list
        booksList = new ArrayList<>();

        // Get category name from intent
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        TextView categoryTitle = findViewById(R.id.categoryTitle);
        categoryTitle.setText(categoryName);

        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize RecyclerView and adapter
        booksRecyclerView = findViewById(R.id.booksRecyclerView);
        booksRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new CategoryBooksAdapter(this);
        booksRecyclerView.setAdapter(adapter);

        // Load books for this category
        loadCategoryBooks();

        // Set up search functionality
        setupSearch();
    }

    private void loadCategoryBooks() {
        Log.d("CategoryActivity", "Loading books for category: " + categoryName);
        
        // Normalize the category name for comparison
        String normalizedCategory = normalizeCategoryName(categoryName);
        Log.d("CategoryActivity", "Normalized category name: " + normalizedCategory);

        firestore.collection("books")
                .whereEqualTo("category", normalizedCategory)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("CategoryActivity", "Retrieved " + queryDocumentSnapshots.size() + " documents");
                    booksList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String title = document.getString("title");
                        String author = document.getString("author");
                        String description = document.getString("description");
                        String coverUrl = document.getString("coverUrl");
                        String pdfUrl = document.getString("pdfUrl");
                        String userId = document.getString("userId");
                        String category = document.getString("category");

                        Log.d("CategoryActivity", "Processing book: " + title);
                        Log.d("CategoryActivity", "Cover URL: " + coverUrl);
                        Log.d("CategoryActivity", "Category: " + category);

                        // Handle Firebase Storage URLs
                        if (coverUrl != null && coverUrl.startsWith("gs://")) {
                            // Convert Firebase Storage URL to HTTP URL
                            FirebaseStorage.getInstance().getReferenceFromUrl(coverUrl)
                                .getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    Book book = new Book(id, uri.toString(), pdfUrl, title, author, description, userId);
                                    booksList.add(book);
                                    adapter.updateBooks(booksList);
                                    Log.d("CategoryActivity", "Added book with converted URL: " + uri.toString());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("CategoryActivity", "Failed to get download URL for " + title, e);
                                    // Add book with original URL as fallback
                                    Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                                    booksList.add(book);
                                    adapter.updateBooks(booksList);
                                });
                        } else {
                            Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                            booksList.add(book);
                        }
                    }
                    adapter.updateBooks(booksList);
                    Log.d("CategoryActivity", "Final books list size: " + booksList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("CategoryActivity", "Error loading books", e);
                    Toast.makeText(this, "Failed to load books: " + e.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSearch() {
        searchView = findViewById(R.id.searchView);
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

    private void filterBooks(String query) {
        if (query.isEmpty()) {
            adapter.updateBooks(booksList);
            return;
        }

        List<Book> filteredList = new ArrayList<>();
        for (Book book : booksList) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                book.getAuthor().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(book);
            }
        }
        adapter.updateBooks(filteredList);
    }

    private void addExampleBook() {
        String bookId = firestore.collection("books").document().getId();
        String normalizedCategory = normalizeCategoryName(categoryName); // Normalize the category

        Map<String, Object> book = new HashMap<>();
        book.put("title", "Social Process");
        book.put("author", "Charles Horton Cooley");
        book.put("description", "PROFESSOR OF SOCIOLOGY IN THE UNIVERSITY OF MICHIGAN");
        book.put("coverUrl", "https://firebasestorage.googleapis.com/v0/b/annotation-890ff.appspot.com/o/%2Fannotation-890ff.appspot.com%2Fcovers%2FCover_Social_Process.jpg?alt=media");
        book.put("pdfUrl", "https://firebasestorage.googleapis.com/v0/b/annotation-890ff.appspot.com/o/%2Fannotation-890ff.appspot.com%2FUploads%2FSocial_Process.pdf?alt=media");
        book.put("category", normalizedCategory); // Use normalized category
        book.put("timestamp", com.google.firebase.Timestamp.now());

        firestore.collection("books").document(bookId)
            .set(book)
            .addOnSuccessListener(aVoid -> {
                Log.d("CategoryActivity", "Book successfully added with category: " + normalizedCategory);
                loadCategoryBooks();
            })
            .addOnFailureListener(e -> {
                Log.e("CategoryActivity", "Error adding book", e);
                Toast.makeText(this, "Failed to add book: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            });
    }

    // Add this helper method to normalize category names
    private String normalizeCategoryName(String category) {
        switch (category.toLowerCase().trim()) {
            case "novels":
            case "novel":
                return "Novel";
            case "horror":
            case "horrors":
                return "Horror";
            case "science fiction":
            case "sci-fi":
            case "scifi":
                return "Science Fiction";
            case "biography":
            case "biographies":
                return "Biography";
            case "history":
            case "historical":
                return "History";
            case "fantasy":
                return "Fantasy";
            case "mystery":
            case "mysteries":
                return "Mystery";
            case "romance":
            case "romantic":
                return "Romance";
            default:
                return category; // Return original if no match
        }
    }
} 