package com.example.annotatex_mobile;

import java.io.Serializable;

public class Friend implements Serializable {
    private String id;
    private String name;
    private String profileImageUrl;
    private String status;
    private boolean removed;

    public Friend(String id, String name, String profileImageUrl, String status, boolean removed) {
        this.id = (id != null) ? id : "";
        this.name = (name != null) ? name : "";
        this.profileImageUrl = (profileImageUrl != null) ? profileImageUrl : "";
        this.status = (status != null) ? status : "Offline";
        this.removed = removed;
    }

    public Friend() {
        this.id = "";
        this.name = "";
        this.profileImageUrl = "";
        this.status = "Offline";
        this.removed = false;
    }

    // Getters
    public String getId() {
        return (id != null) ? id : "";
    }

    public String getName() {
        return (name != null) ? name : "Unknown User";
    }

    public String getProfileImageUrl() {
        return (profileImageUrl != null) ? profileImageUrl : "";
    }

    public String getStatus() {
        return (status != null) ? status : "Offline";
    }

    public boolean isRemoved() {
        return removed;
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

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean hasProfileImage() {
        return profileImageUrl != null && !profileImageUrl.isEmpty();
    }

    public boolean isValid() {
        return !id.isEmpty() && !name.isEmpty();
    }

    public void update(Friend updatedFriend) {
        if (updatedFriend == null) return;

        if (updatedFriend.getName() != null && !updatedFriend.getName().isEmpty()) {
            this.name = updatedFriend.getName();
        }

        if (updatedFriend.getProfileImageUrl() != null && !updatedFriend.getProfileImageUrl().isEmpty()) {
            this.profileImageUrl = updatedFriend.getProfileImageUrl();
        }

        if (updatedFriend.getStatus() != null && !updatedFriend.getStatus().isEmpty()) {
            this.status = updatedFriend.getStatus();
        }

        this.removed = updatedFriend.isRemoved();
    }

    @Override
    public String toString() {
        return "Friend{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", status='" + status + '\'' +
                ", removed=" + removed +
                '}';
    }

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
