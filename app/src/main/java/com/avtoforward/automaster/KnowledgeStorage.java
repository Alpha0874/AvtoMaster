package com.avtoforward.automaster;

import java.util.LinkedHashMap;
import java.util.Map;

public class KnowledgeStorage {
    private static KnowledgeStorage instance;

    // Карта: раздел знаний -> список подкатегорий (название, topicId)
    private Map<String, Map<String, String>> knowledgeTree;

    private KnowledgeStorage() {
        knowledgeTree = new LinkedHashMap<>();

        // Заполняем раздел "Знания автоэлектрика"
        Map<String, String> autoElectric = new LinkedHashMap<>();
        autoElectric.put("Легковые", "autoelectric_light");
        autoElectric.put("Грузовые", "autoelectric_truck");
        autoElectric.put("Спецтехника", "autoelectric_special");
        autoElectric.put("Общий чат", "autoelectric_general");
        knowledgeTree.put("Знания автоэлектрика", autoElectric);

        // Раздел "Знания автомеханика"
        Map<String, String> autoMechanic = new LinkedHashMap<>();
        autoMechanic.put("Легковые", "automechanic_light");
        autoMechanic.put("Грузовые", "automechanic_truck");
        autoMechanic.put("Спецтехника", "automechanic_special");
        autoMechanic.put("Общий чат", "automechanic_general");
        knowledgeTree.put("Знания автомеханика", autoMechanic);

        // Раздел "Знания гидравлика"
        Map<String, String> hydraulic = new LinkedHashMap<>();
        hydraulic.put("Легковые", "hydraulic_light");
        hydraulic.put("Грузовые", "hydraulic_truck");
        hydraulic.put("Спецтехника", "hydraulic_special");
        hydraulic.put("Общий чат", "hydraulic_general");
        knowledgeTree.put("Знания гидравлика", hydraulic);
    }

    public static KnowledgeStorage getInstance() {
        if (instance == null) instance = new KnowledgeStorage();
        return instance;
    }

    public Map<String, Map<String, String>> getKnowledgeTree() {
        return knowledgeTree;
    }

    // Получить подкатегории по названию раздела
    public Map<String, String> getSubCategories(String knowledgeName) {
        return knowledgeTree.get(knowledgeName);
    }
}