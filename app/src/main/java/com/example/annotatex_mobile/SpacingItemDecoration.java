package com.example.annotatex_mobile;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;

    public SpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.left = spacing;
        outRect.right = spacing;
        outRect.bottom = spacing;

        // Apply top margin only for the first row to avoid double spacing between items
        if (parent.getChildAdapterPosition(view) < 2) {  // Adjust based on the span count in GridLayoutManager
            outRect.top = spacing;
        }
    }
}
