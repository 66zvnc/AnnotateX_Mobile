package com.example.annotatex_mobile;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CategoryActivity extends AppCompatActivity {
    private RecyclerView booksRecyclerView;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Get category name from intent
        String categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        TextView categoryTitle = findViewById(R.id.categoryTitle);
        categoryTitle.setText(categoryName);

        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize RecyclerView
        booksRecyclerView = findViewById(R.id.booksRecyclerView);
        booksRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Set up search functionality
        setupSearch();
    }

    private void setupSearch() {
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // TODO: Implement search functionality
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Implement search functionality
                return true;
            }
        });
    }
} 