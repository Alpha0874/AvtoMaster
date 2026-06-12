package com.avtoforward.automaster;

public class Order {
    private String id;
    private String service;           // Тип мастера (автоэлектрик и т.д.)
    private String address;
    private String description;       // Описание проблемы
    private String comment;
    private String vehicleType;       // Легковой, грузовой и т.д.
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleYear;
    private int price;                // Предварительная цена (estimated_price)
    private String status;
    private String userId;
    private boolean isPremium;
    private String assignedTo;
    private long createdAt;

    // Новые поля
    private String clientName;
    private String clientPhone;
    private String city;
    private int distanceMkadKm;
    private String masterType;        // дублирует service, но оставим для ясности
    private String paymentMethod;
    private boolean isPriceByAgreement;
    private int finalPrice;           // для будущего

    // Конструктор для создания нового заказа (без assignedTo и createdAt)
    public Order(String id, String service, String address, String description, String comment,
                 String vehicleType, String vehicleBrand, String vehicleModel, String vehicleYear,
                 int price, String userId, boolean isPremium) {
        this(id, service, address, description, comment, vehicleType, vehicleBrand, vehicleModel,
                vehicleYear, price, "new", userId, isPremium, null, System.currentTimeMillis(),
                "", "", "", 0, "", "", false, 0);
    }

    // Полный конструктор (используется при парсинге из БД)
    public Order(String id, String service, String address, String description, String comment,
                 String vehicleType, String vehicleBrand, String vehicleModel, String vehicleYear,
                 int price, String status, String userId, boolean isPremium, String assignedTo, long createdAt,
                 String clientName, String clientPhone, String city, int distanceMkadKm,
                 String masterType, String paymentMethod, boolean isPriceByAgreement, int finalPrice) {
        this.id = id;
        this.service = service;
        this.address = address;
        this.description = description;
        this.comment = comment;
        this.vehicleType = vehicleType;
        this.vehicleBrand = vehicleBrand;
        this.vehicleModel = vehicleModel;
        this.vehicleYear = vehicleYear;
        this.price = price;
        this.status = status;
        this.userId = userId;
        this.isPremium = isPremium;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.city = city;
        this.distanceMkadKm = distanceMkadKm;
        this.masterType = masterType;
        this.paymentMethod = paymentMethod;
        this.isPriceByAgreement = isPriceByAgreement;
        this.finalPrice = finalPrice;
    }

    // Геттеры
    public String getId() { return id; }
    public String getService() { return service; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public String getComment() { return comment; }
    public String getVehicleType() { return vehicleType; }
    public String getVehicleBrand() { return vehicleBrand; }
    public String getVehicleModel() { return vehicleModel; }
    public String getVehicleYear() { return vehicleYear; }
    public int getPrice() { return price; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public boolean isPremiumOrder() { return isPremium; }
    public String getAssignedTo() { return assignedTo; }
    public long getCreatedAt() { return createdAt; }
    public String getClientName() { return clientName; }
    public String getClientPhone() { return clientPhone; }
    public String getCity() { return city; }
    public int getDistanceMkadKm() { return distanceMkadKm; }
    public String getMasterType() { return masterType; }
    public String getPaymentMethod() { return paymentMethod; }
    public boolean isPriceByAgreement() { return isPriceByAgreement; }
    public int getFinalPrice() { return finalPrice; }

    // Сеттеры (если нужны)
    public void setStatus(String status) { this.status = status; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
}