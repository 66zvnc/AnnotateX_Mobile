package com.example.annotatex_mobile;

public class Categories {
    private String name;
    private int imageResId;
    private boolean isHeader;

    public Categories(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
        this.isHeader = false;
    }

    public Categories(String name, int imageResId, boolean isHeader) {
        this.name = name;
        this.imageResId = imageResId;
        this.isHeader = isHeader;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public boolean isHeader() {
        return isHeader;
    }
} 