package com.example.annotatex_mobile;

import android.graphics.Bitmap;
import java.io.Serializable;

public class Book implements Serializable {
    private String id;
    private int imageResId = -1;
    private Bitmap coverImageBitmap;
    private String coverImageUrl;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;
    private String userId;
    private boolean isPreloaded;
    private boolean hidden; // New field to hide books

    // Constructor for user-uploaded books
    public Book(String id, String coverImageUrl, String pdfUrl, String title, String author, String description, String userId) {
        this.id = id;
        this.coverImageUrl = coverImageUrl;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
        this.userId = userId;
        this.isPreloaded = false; // Default to false for user-uploaded books
        this.hidden = false; // Default to not hidden
    }

    // Constructor for preloaded books
    public Book(int imageResId, String pdfUrl, String title, String author, String description) {
        this.imageResId = imageResId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
        this.isPreloaded = true; // Set to true for predefined books
        this.hidden = false; // Default to not hidden
    }

    // Default constructor
    public Book() {
        this.isPreloaded = false;
        this.hidden = false;
    }

    // Getters
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

    public String getUserId() {
        return userId;
    }

    public boolean isPreloaded() {
        return isPreloaded;
    }

    public boolean isHidden() {
        return hidden;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPreloaded(boolean preloaded) {
        isPreloaded = preloaded;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    // Utility methods
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
