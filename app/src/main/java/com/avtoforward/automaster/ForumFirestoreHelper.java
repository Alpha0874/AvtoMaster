package com.avtoforward.automaster;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForumFirestoreHelper {
    private static ForumFirestoreHelper instance;
    private FirebaseFirestore db;

    public static ForumFirestoreHelper getInstance() {
        if (instance == null) instance = new ForumFirestoreHelper();
        return instance;
    }

    private ForumFirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // Возвращает коллекцию сообщений конкретной темы (roomId)
    public CollectionReference getMessagesCollection(String topicId) {
        return db.collection("forum")
                .document("topics")
                .collection(topicId)
                .document("messages")
                .collection("items");
    }

    // Добавляет сообщение в тему
    public void addMessage(String topicId, com.avtoforward.automaster.models.ChatMessage msg) {
        getMessagesCollection(topicId).add(msg);
    }
}