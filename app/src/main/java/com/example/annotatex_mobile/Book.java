// Book.java
package com.example.annotatex_mobile;

import java.io.Serializable;

public class Book implements Serializable {
    private int imageResId;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;

    public Book(int imageResId, String pdfUrl, String title, String author, String description) {
        this.imageResId = imageResId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
    }

    public int getImageResId() {
        return imageResId;
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
}
