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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

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

        // Use GridLayoutManager with 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);

        // Add spacing between grid items
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        categoriesRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));

        // Initialize categories list
        categoriesList = new ArrayList<>();
        
        // Add categories with images
        categoriesList.add(new Categories("Classic", R.drawable.img_category_classic));
        categoriesList.add(new Categories("Fantasy", R.drawable.img_category_fantasy));
        categoriesList.add(new Categories("For Kids", R.drawable.img_category_kids));
        categoriesList.add(new Categories("Novels", R.drawable.img_category_novels));

        // Initialize and set adapter
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
            if (category.getName().toLowerCase().contains(lowerCaseQuery)) {
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
