package com.example.annotatex_mobile;

import java.io.Serializable;

public class FriendRequest implements Serializable {
    private String senderId;
    private String senderName;
    private String receiverId;
    private long timestamp;

    public FriendRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(FriendRequest.class)
    }

    public FriendRequest(String senderId, String senderName, String receiverId, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
    }

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

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
