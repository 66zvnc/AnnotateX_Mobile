package com.example.annotatex_mobile;

import java.io.Serializable;

public class FriendRequest implements Serializable {
    private String senderId;
    private String senderName;
    private String receiverId;
    private long timestamp;

    // Default constructor required for Firestore deserialization
    public FriendRequest() {
        // Initialize with a default timestamp if not set
        this.timestamp = System.currentTimeMillis();
    }

    public FriendRequest(String senderId, String senderName, String receiverId) {
        this.senderId = senderId != null ? senderId : "";
        this.senderName = senderName != null ? senderName : "Unknown User";
        this.receiverId = receiverId != null ? receiverId : "";
        this.timestamp = System.currentTimeMillis(); // Automatically set the current timestamp
    }

    public FriendRequest(String senderId, String senderName, String receiverId, long timestamp) {
        this.senderId = senderId != null ? senderId : "";
        this.senderName = senderName != null ? senderName : "Unknown User";
        this.receiverId = receiverId != null ? receiverId : "";
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    // Getters
    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setSenderId(String senderId) {
        this.senderId = senderId != null ? senderId : "";
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName != null ? senderName : "Unknown User";
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId != null ? receiverId : "";
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    // Override toString method for debugging purposes
    @Override
    public String toString() {
        return "FriendRequest{" +
                "senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    // Method to check if the FriendRequest object is valid
    public boolean isValid() {
        return senderId != null && !senderId.isEmpty() &&
                receiverId != null && !receiverId.isEmpty();
    }

    // Equals method to compare FriendRequest objects based on senderId and receiverId
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FriendRequest request = (FriendRequest) obj;
        return senderId.equals(request.senderId) && receiverId.equals(request.receiverId);
    }

    // Overriding hashCode() for better performance in collections
    @Override
    public int hashCode() {
        return senderId.hashCode() + receiverId.hashCode();
    }
}
