package io.mutshiv.matchEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.mutshiv.orderBook.LimitOrderBook;
import io.mutshiv.orderBook.Order;

public class MatchingEngineIntegrationTest {

    /**
     * Helper method to display the current live orders in the LimitOrderBook.
     * 
     * @param liveOrders Map of all live orders.
     */
    private void viewLiveOrders(Map<String, Order> liveOrders) {
        System.out.println("\nLive Orders:");
        liveOrders.forEach((key, value) -> System.out.printf(
                "Order ID = %s, Price = %.2f, Quantity = %d, Side = %s, Time = %s\n",
                value.getId(), value.getPrice(), value.getQuantity(), value.getSide(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(value.getOrderTimeStamp()), ZoneId.systemDefault())));
    }

    @Test
    public void testMatchingEngine() {
        // Step 1: Initialize the LimitOrderBook and MatchingEngine
        LimitOrderBook lob = new LimitOrderBook();
        MatchingEngine matchingEngine = new MatchingEngine(lob);

        // Step 2: Add initial SELL orders to the LimitOrderBook
        System.out.println("Adding SELL orders...");
        Order sellOrder1 = new Order(100.0, 50, "SELL"); // Sell 50 units @ $100
        Order sellOrder2 = new Order(105.0, 30, "SELL"); // Sell 30 units @ $105
        lob.addOrder(sellOrder1);
        lob.addOrder(sellOrder2);

        // Step 3: Add a BUY order that partially matches the SELL order
        System.out.println("\nAdding BUY order (partial fill scenario)...");
        Order buyOrder1 = new Order(100.0, 30, "BUY"); // Buy 30 units @ $100
        lob.addOrder(buyOrder1);
        // matchingEngine.tradeOnOrder(buyOrder1);

        viewLiveOrders(lob.getLiveOrders());

        // Validate SELL order quantities after partial fill
        assertEquals(20, lob.getLiveOrders().get(sellOrder1.getId()).getQuantity(), "SELL order 1 should have 20 units remaining.");
        assertEquals(30, lob.getSellOrders().peek().getQuantity(), "The remaining SELL order should be at the top.");

        // Step 4: Add another BUY order that fully matches the remaining SELL order
        System.out.println("\nAdding BUY order (full fill scenario)...");
        Order buyOrder2 = new Order(100.0, 20, "BUY"); // Buy 20 units @ $100
        lob.addOrder(buyOrder2);
        // matchingEngine.tradeOnOrder(buyOrder2);

        viewLiveOrders(lob.getLiveOrders());

        // Validate that the first SELL order is fully matched and removed
        assertEquals(1, lob.getSellOrders().size(), "There should be only one SELL order remaining.");
        assertEquals(30, lob.getSellOrders().peek().getQuantity(), "The remaining SELL order should have 30 units.");

        // Step 5: Add a BUY order that does not match any SELL orders
        System.out.println("\nAdding BUY order (no match scenario)...");
        Order buyOrder3 = new Order(95.0, 10, "BUY"); // Buy 10 units @ $95
        lob.addOrder(buyOrder3);
        // matchingEngine.tradeOnOrder(buyOrder3);

        viewLiveOrders(lob.getLiveOrders());

        // Validate that the BUY order is added to the book
        assertEquals(1, lob.getBuyOrders().size(), "There should be one BUY order in the book.");
        assertEquals(10, lob.getBuyOrders().peek().getQuantity(), "The unmatched BUY order should have 10 units.");

        // Validate the final state of the order book
        assertEquals(2, lob.getLiveOrders().size(), "There should be two live orders (1 SELL, 1 BUY).");
    }
}
