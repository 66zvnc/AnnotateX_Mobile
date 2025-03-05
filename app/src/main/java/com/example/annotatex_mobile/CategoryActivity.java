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
    private static final String TAG = "CategoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize books list
        booksList = new ArrayList<>();

        // Get category name from intent with validation
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        if (categoryName == null || categoryName.trim().isEmpty()) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up category title in toolbar
        TextView categoryTitle = findViewById(R.id.categoryTitle);
        String normalizedCategory = normalizeCategoryName(categoryName);
        categoryTitle.setText(normalizedCategory);

        // Verify and fix categories in database (run this once)
        // verifyAndFixBookCategories();

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

        // Run this once to fix all book categories
        fixAllBookCategories();
    }

    private void loadCategoryBooks() {
        Log.d(TAG, "Loading books for category: " + categoryName);
        
        // Normalize the category name for comparison
        String normalizedCategory = normalizeCategoryName(categoryName);
        Log.d(TAG, "Normalized category name: " + normalizedCategory);

        // Query with case-insensitive comparison
        firestore.collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Retrieved total documents: " + queryDocumentSnapshots.size());
                    booksList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String documentCategory = document.getString("category");
                            String normalizedDocCategory = normalizeCategoryName(documentCategory);
                            
                            // Log for debugging
                            Log.d(TAG, "Document category: " + documentCategory);
                            Log.d(TAG, "Normalized doc category: " + normalizedDocCategory);
                            Log.d(TAG, "Comparing with: " + normalizedCategory);
                            
                            // Compare normalized categories
                            if (normalizedDocCategory.equalsIgnoreCase(normalizedCategory)) {
                                String id = document.getId();
                                String title = getStringOrDefault(document, "title", "Untitled");
                                String author = getStringOrDefault(document, "author", "Unknown Author");
                                String description = getStringOrDefault(document, "description", "");
                                String coverUrl = getStringOrDefault(document, "coverUrl", "");
                                String pdfUrl = getStringOrDefault(document, "pdfUrl", "");
                                String userId = getStringOrDefault(document, "userId", "");

                                Log.d(TAG, String.format("Adding book: %s, Category: %s", title, documentCategory));

                                Book book = new Book(id, coverUrl, pdfUrl, title, author, description, userId);
                                booksList.add(book);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing document: " + document.getId(), e);
                        }
                    }
                    
                    if (booksList.isEmpty()) {
                        Log.w(TAG, "No books found for category: " + normalizedCategory);
                        Toast.makeText(this, "No books available in this category", 
                                     Toast.LENGTH_SHORT).show();
                    }
                    
                    adapter.updateBooks(booksList);
                    Log.d(TAG, "Updated adapter with " + booksList.size() + " books");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading books for category: " + normalizedCategory, e);
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

    // Update the normalizeCategoryName method
    private String normalizeCategoryName(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "uncategorized";
        }

        // Trim and convert to lowercase for consistent comparison
        String normalizedInput = category.trim().toLowerCase();
        
        // Log the input category for debugging
        Log.d(TAG, "Normalizing category: " + normalizedInput);

        switch (normalizedInput) {
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
            
            case "historical fiction":
            case "historical":
            case "history fiction":
            case "historicalfiction":
                return "Historical Fiction";
            
            case "romance":
            case "romantic":
            case "love":
                return "Romance";
            
            case "mystery":
            case "mysteries":
            case "thriller":
                return "Mystery";
            
            case "autobiography":
            case "auto-biography":
            case "auto biography":
            case "biography":
            case "biographies":
                return "Autobiography";
            
            case "fantasy":
            case "fantasies":
                return "Fantasy";
            
            default:
                // Log unmatched categories
                Log.w(TAG, "Unmatched category: " + normalizedInput);
                // Return the capitalized version of the category
                return capitalizeFirstLetter(category.trim());
        }
    }

    // Helper method to capitalize first letter
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    // Helper method to safely get string values from documents
    private String getStringOrDefault(QueryDocumentSnapshot document, String field, String defaultValue) {
        String value = document.getString(field);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    private void verifyAndFixBookCategories() {
        firestore.collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String currentCategory = document.getString("category");
                        String normalizedCategory = normalizeCategoryName(currentCategory);
                        
                        if (!normalizedCategory.equals(currentCategory)) {
                            Log.d(TAG, "Fixing category for book: " + document.getString("title") +
                                  " from: " + currentCategory + " to: " + normalizedCategory);
                            
                            document.getReference().update("category", normalizedCategory)
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "Successfully updated category"))
                                    .addOnFailureListener(e -> 
                                        Log.e(TAG, "Failed to update category", e));
                        }
                    }
                });
    }

    // Add this method to fix existing books in the database
    public void fixAllBookCategories() {
        firestore.collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String currentCategory = document.getString("category");
                        String normalizedCategory = normalizeCategoryName(currentCategory);
                        
                        Log.d(TAG, "Checking book: " + document.getString("title"));
                        Log.d(TAG, "Current category: " + currentCategory);
                        Log.d(TAG, "Normalized category: " + normalizedCategory);
                        
                        if (!normalizedCategory.equals(currentCategory)) {
                            document.getReference().update("category", normalizedCategory)
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "Successfully updated category for: " + 
                                             document.getString("title")))
                                    .addOnFailureListener(e -> 
                                        Log.e(TAG, "Failed to update category", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching books", e));
    }
} 