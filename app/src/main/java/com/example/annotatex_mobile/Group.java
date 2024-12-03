package com.example.annotatex_mobile;

import java.util.List;
import java.util.ArrayList;

public class Group {
    private String id;
    private String name;
    private List<String> members;
    private long createdAt;
    private String createdBy;

    public Group() {
        // Required empty constructor for Firestore
        members = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMembers() {
        return members != null ? members : new ArrayList<>();
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
