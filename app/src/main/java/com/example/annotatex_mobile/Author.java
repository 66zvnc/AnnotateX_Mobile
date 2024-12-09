package com.example.annotatex_mobile;

public class Author {
    private String name;
    private int imageResId;

    public Author(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
} 