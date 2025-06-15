package entity;

public enum OrderStatus {
    SUBMITTED,
    UNPAID_AND_CANCELLED,
    WAITING_VENDOR,
    CANCELLED,
    FINDING_COURIER,
    ON_THE_WAY,
    COMPLETED
}