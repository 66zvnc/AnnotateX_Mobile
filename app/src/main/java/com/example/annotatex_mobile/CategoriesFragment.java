package com.example.annotatex_mobile;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CategoriesFragment extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private CategoriesAdapter categoriesAdapter;
    private List<Categories> categoriesList;
    private EditText searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // Initialize search view
        searchView = view.findViewById(R.id.searchInCategories);
        setupSearchView();

        // Initialize RecyclerView
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        
        // Set up layout with span count of 2 (for grid layout)
        GridLayoutManager gridManager = new GridLayoutManager(getContext(), 2);
        // Configure the GridLayoutManager to make headers span the full width
        gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // If it's a header, make it span the full width (2 columns)
                return categoriesList.get(position).isHeader() ? 2 : 1;
            }
        });
        categoriesRecyclerView.setLayoutManager(gridManager);

        // Add spacing
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        categoriesRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));

        // Initialize categories list with header items
        categoriesList = new ArrayList<>();
        
        // Add header for Popular section
        categoriesList.add(new Categories("Popular", -1, true)); // Use a flag for headers
        
        // Add popular categories (first 4)
        categoriesList.add(new Categories("Classic", R.drawable.img_category_classic));
        categoriesList.add(new Categories("Fantasy", R.drawable.img_category_fantasy));
        categoriesList.add(new Categories("For Kids", R.drawable.img_category_kids));
        categoriesList.add(new Categories("Novels", R.drawable.img_category_novels));
        
        // Add header for All Categories section
        categoriesList.add(new Categories("All Categories", -1, true));
        
        // Add remaining categories
        categoriesList.add(new Categories("Comedy", R.drawable.img_category_comedy));
        categoriesList.add(new Categories("Horror", R.drawable.img_category_horror));
        categoriesList.add(new Categories("Science Fiction", R.drawable.img_category_scifi));
        categoriesList.add(new Categories("Historical Fiction", R.drawable.img_category_historical_fiction));
        categoriesList.add(new Categories("Romance", R.drawable.img_category_romance));
        categoriesList.add(new Categories("Mystery", R.drawable.img_category_mystery));
        categoriesList.add(new Categories("Autobiography", R.drawable.img_category_autobio));

        // Set up adapter
        categoriesAdapter = new CategoriesAdapter(getContext(), categoriesList);
        categoriesRecyclerView.setAdapter(categoriesAdapter);

        return view;
    }

    private void setupSearchView() {
        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterCategories(searchView.getText().toString());
                return true;
            }
            return false;
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCategories(String query) {
        List<Categories> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Categories category : categoriesList) {
            if (category.isHeader() || category.getName().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(category);
            }
        }

        categoriesAdapter.updateCategories(filteredList);
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
