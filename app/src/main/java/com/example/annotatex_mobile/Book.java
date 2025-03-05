package com.example.annotatex_mobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Book implements Parcelable {
    private static final String TAG = "Book";

    private String id;
    private String coverUrl;
    private String pdfUrl;
    private String title;
    private String author;
    private String description;
    private String userId;
    private int imageResourceId; // For preloaded books
    private List<String> collaborators; // Add collaborators field
    private boolean isPreloaded; // Add isPreloaded field
    private boolean isHidden; // Add isHidden field

    // Constructor for Firestore books
    public Book(String id, String coverUrl, String pdfUrl, String title, String author, String description, String userId) {
        this.id = id;
        this.coverUrl = coverUrl;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
        this.userId = userId;
        this.collaborators = new ArrayList<>();
        this.isPreloaded = false;
        this.isHidden = false;
    }

    // Constructor for preloaded books
    public Book(int imageResourceId, String pdfUrl, String title, String author, String description) {
        this.imageResourceId = imageResourceId;
        this.pdfUrl = pdfUrl;
        this.title = title;
        this.author = author;
        this.description = description;
        this.collaborators = new ArrayList<>();
        this.isPreloaded = true;
        this.isHidden = false;
    }

    // Add copy constructor
    public Book(Book other) {
        this.id = other.id;
        this.coverUrl = other.coverUrl;
        this.pdfUrl = other.pdfUrl;
        this.title = other.title;
        this.author = other.author;
        this.description = other.description;
        this.userId = other.userId;
        this.imageResourceId = other.imageResourceId;
        this.collaborators = new ArrayList<>(other.collaborators);
        this.isPreloaded = other.isPreloaded;
        this.isHidden = other.isHidden;
    }

    // Getters
    public String getId() { return id; }
    public String getCoverUrl() { return coverUrl; }
    public String getPdfUrl() { return pdfUrl; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getUserId() { return userId; }
    public int getImageResourceId() { return imageResourceId; }
    public List<String> getCollaborators() { return collaborators; }
    public boolean isPreloaded() { return isPreloaded; }
    public boolean isHidden() { return isHidden; }
    public String getCoverImageUrl() {
        if (isPreloaded) {
            return null;
        }
        return coverUrl;
    }
    public boolean hasCoverImage() {
        return coverUrl != null && !coverUrl.isEmpty() || imageResourceId != 0;
    }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setDescription(String description) { this.description = description; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setImageResourceId(int imageResourceId) { this.imageResourceId = imageResourceId; }
    public void setCollaborators(List<String> collaborators) { this.collaborators = collaborators; }
    public void setPreloaded(boolean preloaded) { isPreloaded = preloaded; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    // Helper method for cover image handling
    public boolean hasResIdCover() {
        return imageResourceId != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return id != null && id.equals(book.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(coverUrl);
        dest.writeString(pdfUrl);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(description);
        dest.writeString(userId);
        dest.writeInt(imageResourceId);
        dest.writeStringList(collaborators);
        dest.writeByte((byte) (isPreloaded ? 1 : 0));
        dest.writeByte((byte) (isHidden ? 1 : 0));
    }

    protected Book(Parcel in) {
        id = in.readString();
        coverUrl = in.readString();
        pdfUrl = in.readString();
        title = in.readString();
        author = in.readString();
        description = in.readString();
        userId = in.readString();
        imageResourceId = in.readInt();
        collaborators = new ArrayList<>();
        in.readStringList(collaborators);
        isPreloaded = in.readByte() != 0;
        isHidden = in.readByte() != 0;
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };
}
