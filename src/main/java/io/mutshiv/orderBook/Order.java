package io.mutshiv.orderBook;

import java.util.UUID;

/**
 * Order
 */
public class Order {

    private final String id;
    private final String side;
    private final double price;
    private int quantity;
    private long orderTimeStamp;

    public Order(double price, int quantity, String side) {
        this.id = UUID.randomUUID().toString();
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.orderTimeStamp = System.nanoTime();
    }

    public long getOrderTimeStamp() {
        return orderTimeStamp;
    }

    public void setOrderTimeStamp(long orderTimeStamp) {
        this.orderTimeStamp = orderTimeStamp;
    }

    public String getId() {
        return id;
    }

    public String getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Modifies the quantity of the order.
     *
     * @param newQuantity
     */
    public void modifyOrder(int newQuantity) {
        this.quantity = newQuantity;
    }

}
