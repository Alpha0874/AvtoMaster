package com.avtoforward.automaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ForumStorage {
    private static ForumStorage instance;

    // Категории → список тем (названий)
    private Map<String, List<String>> categories;
    // ID темы → список сообщений
    private Map<String, List<com.avtoforward.automaster.models.ChatMessage>> topics;

    private ForumStorage() {
        categories = new HashMap<>();
        topics = new HashMap<>();

        // Инициализируем стандартные категории
        List<String> generalTopics = new ArrayList<>();
        generalTopics.add("Общие вопросы");
        generalTopics.add("Диагностика");
        generalTopics.add("Инструменты");
        categories.put("Общие", generalTopics);

        List<String> premiumTopics = new ArrayList<>();
        premiumTopics.add("Схемы ремонта");
        premiumTopics.add("Закрытый чат");
        categories.put("Премиум", premiumTopics);
    }

    public static ForumStorage getInstance() {
        if (instance == null) instance = new ForumStorage();
        return instance;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public List<String> getTopics(String category) {
        return categories.get(category);
    }

    public List<com.avtoforward.automaster.models.ChatMessage> getMessages(String topicId) {
        if (!topics.containsKey(topicId)) {
            topics.put(topicId, new ArrayList<>());
        }
        return topics.get(topicId);
    }

    public void addMessage(String topicId, com.avtoforward.automaster.models.ChatMessage msg) {
        getMessages(topicId).add(msg);
    }

    public static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}