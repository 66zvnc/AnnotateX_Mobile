package com.example.annotatex_mobile;

public class Categories {
    private String name;
    private int imageResId;

    public Categories(String name, int imageResId) {
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