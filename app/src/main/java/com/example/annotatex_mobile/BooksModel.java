package com.example.annotatex_mobile;

import java.io.Serializable;

public class BooksModel implements Serializable {
    private int imageResId;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;

    public BooksModel(int imageResId, String pdfUrl, String title, String author, String description) {
        this.imageResId = imageResId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    // Getters and setters for each field
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
