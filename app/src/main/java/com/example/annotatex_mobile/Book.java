package com.example.annotatex_mobile;

import android.graphics.Bitmap;
import java.io.Serializable;

public class Book implements Serializable {
    private String id; // Firestore document ID
    private int imageResId = -1; // Default to -1 to indicate no resource ID
    private Bitmap coverImageBitmap;
    private String coverImageUrl;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;

    // Constructor for Firestore ID and URL-based image
    public Book(String id, String coverImageUrl, String pdfUrl, String title, String author, String description) {
        this.id = id;
        this.coverImageUrl = coverImageUrl;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Constructor for predefined books with resource ID image
    public Book(int imageResId, String pdfUrl, String title, String author, String description) {
        this.imageResId = imageResId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Getter for Firestore document ID
    public String getId() {
        return id;
    }

    public int getImageResId() {
        return imageResId;
    }

    public Bitmap getCoverImageBitmap() {
        return coverImageBitmap;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasBitmapCover() {
        return coverImageBitmap != null;
    }

    public boolean hasUrlCover() {
        return coverImageUrl != null && !coverImageUrl.isEmpty();
    }

    public boolean hasResIdCover() {
        return imageResId != -1;
    }
}
