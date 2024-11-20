package com.example.annotatex_mobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;

public class Book implements Serializable {
    private static final String TAG = "Book";

    private String id;
    private int imageResId = -1;
    private Bitmap coverImageBitmap;
    private String coverImageUrl;
    private String coverImageLocalPath;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;
    private String userId;
    private boolean isPreloaded;
    private boolean hidden;

    // Constructor for user-uploaded books
    public Book(String id, String coverImageUrl, String pdfUrl, String title, String author, String description, String userId) {
        this.id = id;
        this.coverImageUrl = coverImageUrl != null && !coverImageUrl.trim().isEmpty() ? coverImageUrl : null;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
        this.userId = userId;
        this.isPreloaded = false;
        this.hidden = false;
    }

    // Constructor for preloaded books
    public Book(int imageResId, String pdfUrl, String title, String author, String description) {
        this.imageResId = imageResId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
        this.isPreloaded = true;
        this.hidden = false;
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

    public String getCoverImageLocalPath() {
        return coverImageLocalPath; // Getter for the local path
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
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPreloaded(boolean preloaded) {
        isPreloaded = preloaded;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        Log.d(TAG, "Updating cover image URL to: " + coverImageUrl);
        this.coverImageUrl = coverImageUrl != null && !coverImageUrl.trim().isEmpty() ? coverImageUrl : null;
    }

    public void setCoverImageLocalPath(String coverImageLocalPath) {
        this.coverImageLocalPath = coverImageLocalPath; // Setter for the local path
    }

    // Utility methods
    public boolean hasBitmapCover() {
        return coverImageBitmap != null;
    }

    public boolean hasUrlCover() {
        return coverImageUrl != null && !coverImageUrl.trim().isEmpty();
    }

    public boolean hasResIdCover() {
        return imageResId != -1;
    }

    // Save the book cover to a local file
    public void saveCoverImageToLocal(File directory) {
        if (coverImageBitmap == null || id == null) {
            Log.w(TAG, "Cannot save cover image: Bitmap or ID is null");
            return;
        }
        File coverFile = new File(directory, id + ".png");
        try (FileOutputStream out = new FileOutputStream(coverFile)) {
            coverImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            setCoverImageLocalPath(coverFile.getAbsolutePath()); // Save the local path
            Log.d(TAG, "Cover image saved locally for book: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error saving cover image locally for book: " + title, e);
        }
    }

    // Load the book cover from a local file
    public boolean loadCoverImageFromLocal(File directory) {
        if (id == null) {
            Log.w(TAG, "Cannot load cover image: Book ID is null");
            return false;
        }
        File coverFile = new File(directory, id + ".png");
        if (coverFile.exists()) {
            coverImageBitmap = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
            setCoverImageLocalPath(coverFile.getAbsolutePath()); // Update the local path
            Log.d(TAG, "Cover image loaded locally for book: " + title);
            return true;
        } else {
            Log.d(TAG, "No local cover image found for book: " + title);
            return false;
        }
    }
}
