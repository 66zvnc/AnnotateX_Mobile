package com.example.annotatex_mobile;

import java.io.Serializable;

public class FriendRequest implements Serializable {
    private String senderId;
    private String senderName;
    private String receiverId;
    private long timestamp;

    // Default constructor required for Firestore deserialization
    public FriendRequest() {
    }

    // Constructor to create a new FriendRequest object
    public FriendRequest(String senderId, String senderName, String receiverId, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName != null ? senderName : "Unknown User"; // Handle null senderName
        this.receiverId = receiverId;
        this.timestamp = timestamp;
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
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName != null ? senderName : "Unknown User";
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
}
