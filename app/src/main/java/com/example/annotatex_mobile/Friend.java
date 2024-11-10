package com.example.annotatex_mobile;

import java.io.Serializable;

public class Friend implements Serializable {
    private String id;
    private String name;
    private String profileImageUrl;
    private String status;

    // Constructor with parameters
    public Friend(String id, String name, String profileImageUrl, String status) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : "";
        this.status = status != null ? status : "Offline";
    }

    // Default constructor
    public Friend() {
        this.id = "";
        this.name = "";
        this.profileImageUrl = "";
        this.status = "Offline";
    }

    // Getters
    public String getId() {
        return id != null ? id : "";
    }

    public String getName() {
        return name != null ? name : "Unknown User";
    }

    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : "";
    }

    public String getStatus() {
        return status != null ? status : "Offline";
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Method to check if the friend has a profile image URL
    public boolean hasProfileImage() {
        return profileImageUrl != null && !profileImageUrl.isEmpty();
    }

    // Method to check if the friend object is valid (has all required fields)
    public boolean isValid() {
        return !id.isEmpty() && !name.isEmpty();
    }

    // Overriding toString() for debugging
    @Override
    public String toString() {
        return "Friend{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    // Equals method to compare Friend objects based on their IDs
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Friend friend = (Friend) obj;
        return id.equals(friend.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
