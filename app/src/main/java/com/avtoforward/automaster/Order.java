package com.avtoforward.automaster;

public class Order {
    private String id;
    private String service;
    private String address;
    private String description;
    private String comment;
    private String vehicleType;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleYear;
    private int price;
    private String status;
    private String userId;
    private boolean isPremium;
    private String assignedTo;
    private long createdAt;
    private String clientName;
    private String clientPhone;
    private String city;
    private int distanceMkadKm;
    private String masterType;
    private String paymentMethod;
    private boolean isPriceByAgreement;
    private int finalPrice;

    public Order(String id, String service, String address, String description, String comment,
                 String vehicleType, String vehicleBrand, String vehicleModel, String vehicleYear,
                 int price, String status, String userId, boolean isPremium, String assignedTo,
                 long createdAt, String clientName, String clientPhone, String city,
                 int distanceMkadKm, String masterType, String paymentMethod,
                 boolean isPriceByAgreement, int finalPrice) {
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

    // Геттеры и сеттеры (без orderNumber)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleBrand() { return vehicleBrand; }
    public void setVehicleBrand(String vehicleBrand) { this.vehicleBrand = vehicleBrand; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(String vehicleYear) { this.vehicleYear = vehicleYear; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isPremiumOrder() { return isPremium; }
    public void setPremiumOrder(boolean premium) { isPremium = premium; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getDistanceMkadKm() { return distanceMkadKm; }
    public void setDistanceMkadKm(int distanceMkadKm) { this.distanceMkadKm = distanceMkadKm; }

    public String getMasterType() { return masterType; }
    public void setMasterType(String masterType) { this.masterType = masterType; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isPriceByAgreement() { return isPriceByAgreement; }
    public void setPriceByAgreement(boolean priceByAgreement) { isPriceByAgreement = priceByAgreement; }

    public int getFinalPrice() { return finalPrice; }
    public void setFinalPrice(int finalPrice) { this.finalPrice = finalPrice; }
}