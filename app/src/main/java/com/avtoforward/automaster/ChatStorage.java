package com.avtoforward.automaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatStorage {
    private static ChatStorage instance;
    private Map<String, List<com.avtoforward.automaster.models.ChatMessage>> rooms;

    private ChatStorage() {
        rooms = new HashMap<>();
    }

    public static ChatStorage getInstance() {
        if (instance == null) instance = new ChatStorage();
        return instance;
    }

    public List<com.avtoforward.automaster.models.ChatMessage> getMessages(String roomId) {
        if (!rooms.containsKey(roomId)) {
            rooms.put(roomId, new ArrayList<>());
        }
        return rooms.get(roomId);
    }

    public void addMessage(String roomId, com.avtoforward.automaster.models.ChatMessage msg) {
        getMessages(roomId).add(msg);
    }

    public static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}