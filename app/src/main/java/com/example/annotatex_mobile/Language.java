package com.example.annotatex_mobile;

public class Language {
    private String name;
    private int flagResource;

    public Language(String name, int flagResource) {
        this.name = name;
        this.flagResource = flagResource;
    }

    public String getName() {
        return name;
    }

    public int getFlagResource() {
        return flagResource;
    }
} 