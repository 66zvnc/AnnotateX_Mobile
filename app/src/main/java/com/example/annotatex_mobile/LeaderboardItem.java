package com.example.annotatex_mobile;

public class LeaderboardItem {
    private String userName;
    private long booksRead;
    private String profileImageUrl;
    private boolean isCurrentUser;

    public LeaderboardItem(String userName, long booksRead, String profileImageUrl, boolean isCurrentUser) {
        this.userName = userName;
        this.booksRead = booksRead;
        this.profileImageUrl = profileImageUrl;
        this.isCurrentUser = isCurrentUser;
    }

    public String getUserName() { return userName; }
    public long getBooksRead() { return booksRead; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public boolean isCurrentUser() { return isCurrentUser; }
} 