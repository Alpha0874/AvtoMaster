package com.avtoforward.automaster;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderStorage {
    private static OrderStorage instance;
    private List<com.avtoforward.automaster.Order> orders;

    private OrderStorage() {
        orders = new ArrayList<>();
    }

    public static OrderStorage getInstance() {
        if (instance == null) {
            instance = new OrderStorage();
        }
        return instance;
    }

    public void addOrder(com.avtoforward.automaster.Order order) {
        orders.add(order);
    }

    public List<com.avtoforward.automaster.Order> getOrders() {
        return orders;
    }

    public com.avtoforward.automaster.Order getOrderById(String id) {
        for (com.avtoforward.automaster.Order o : orders) {
            if (o.getId().equals(id)) return o;
        }
        return null;
    }

    public static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}