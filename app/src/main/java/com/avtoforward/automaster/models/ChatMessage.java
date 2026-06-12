package com.avtoforward.automaster.models;

import java.util.List;

public class ChatMessage {
    private String id;
    private String authorId;
    private String authorName;
    private String messageText;
    private List<String> imageUrls;
    private String created;

    public ChatMessage(String id, String authorId, String authorName, String messageText, List<String> imageUrls, String created) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.messageText = messageText;
        this.imageUrls = imageUrls;
        this.created = created;
    }

    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getMessageText() { return messageText; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getCreated() { return created; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
}