package com.example.annotatex_mobile;

import android.graphics.Bitmap;
import java.io.Serializable;

public class Book implements Serializable {
    private int imageResId = -1; // Default to -1 to indicate no resource ID
    private Bitmap coverImageBitmap;
    private String coverImageUrl;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;

    // Constructor for resource ID image
    public Book(int imageResId, String pdfUrl, String title, String author, String description) {
        this.imageResId = imageResId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Constructor for Bitmap image
    public Book(Bitmap coverImageBitmap, String pdfUrl, String title, String author, String description) {
        this.coverImageBitmap = coverImageBitmap;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Constructor for URL-based image
    public Book(String coverImageUrl, String pdfUrl, String title, String author, String description) {
        this.coverImageUrl = coverImageUrl;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
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

    // Check if the book has a Bitmap cover image
    public boolean hasBitmapCover() {
        return coverImageBitmap != null;
    }

    // Check if the book has a URL cover image
    public boolean hasUrlCover() {
        return coverImageUrl != null && !coverImageUrl.isEmpty();
    }

    // Check if the book has a resource ID cover image
    public boolean hasResIdCover() {
        return imageResId != -1;
    }
}
