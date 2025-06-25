package entity;

public enum OrderStatus {

    // initial order status
    SUBMITTED,

    // statuses that restaurant handles
    UNPAID_AND_CANCELLED,
    WAITING_VENDOR,
    CANCELLED,
    FINDING_COURIER,
    ON_THE_WAY,
    COMPLETED,


    // statuses for handling delivery
    ACCEPTED,
    RECEIVED,
    DELIVERED
}