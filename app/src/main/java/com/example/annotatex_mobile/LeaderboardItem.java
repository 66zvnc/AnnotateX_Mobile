package com.example.annotatex_mobile;

public class LeaderboardItem {
    private String userName;
    private int booksRead;
    private String profileImageUrl;

    public LeaderboardItem(String userName, int booksRead, String profileImageUrl) {
        this.userName = userName;
        this.booksRead = booksRead;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUserName() { return userName; }
    public int getBooksRead() { return booksRead; }
    public String getProfileImageUrl() { return profileImageUrl; }
} 