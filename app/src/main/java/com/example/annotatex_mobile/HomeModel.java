package com.example.annotatex_mobile;

import java.io.Serializable;
import java.util.ArrayList;

public class HomeModel implements Serializable {
    public static final int LAYOUT_HOME = 0;
    public static final int LAYOUT_BOD = 1;

    private int LAYOUT_TYPE;
    private ArrayList<BooksModel> booksList;
    private BooksModel bod; // Book of the Day object
    private String catTitle;

    public HomeModel(int LAYOUT_TYPE, ArrayList<BooksModel> booksList, String catTitle) {
        this.LAYOUT_TYPE = LAYOUT_TYPE;
        this.booksList = booksList;
        this.catTitle = catTitle;
    }

    public HomeModel(int LAYOUT_TYPE, BooksModel bod) {
        this.LAYOUT_TYPE = LAYOUT_TYPE;
        this.bod = bod;
    }

    public int getLAYOUT_TYPE() {
        return LAYOUT_TYPE;
    }

    public ArrayList<BooksModel> getBooksList() {
        return booksList;
    }

    public BooksModel getBod() {
        return bod;
    }

    public String getCatTitle() {
        return catTitle;
    }
}
