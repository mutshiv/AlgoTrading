package io.mutshiv.matchEngine;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.PriorityQueue;

import org.junit.jupiter.api.Test;

import io.mutshiv.orderBook.LimitOrderBook;
import io.mutshiv.orderBook.Order;

import static org.junit.jupiter.api.Assertions.*;

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

    private void viewOrderBySide(PriorityQueue<Order> pq) {
        System.out.println("\nBuy Orders:");
        pq.forEach(bO -> {
            System.out.printf(
                    "\nOrder ID = %s, Price = %.2f, Quantity = %d, Side = %s, Time = %s\n",
                    bO.getId(), bO.getPrice(), bO.getQuantity(), bO.getSide(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(bO.getOrderTimeStamp()), ZoneId.systemDefault()));
        });
    }

    @Test
    public void testMatchingEngine() {
        LimitOrderBook lob = new LimitOrderBook();
        MatchingEngine matchingEngine = new MatchingEngine(lob);

        System.out.println("Adding SELL orders...");
        Order sellOrder1 = new Order(100.0, 50, "SELL");
        Order sellOrder2 = new Order(105.0, 30, "SELL");
        Order sellOrder3 = new Order(100.0, 30, "SELL");
        lob.addOrder(sellOrder1);
        lob.addOrder(sellOrder2);
        lob.addOrder(sellOrder3);

        System.out.println("\nSorted orders by price...");
        viewLiveOrders(lob.getLiveOrders());

        System.out.println("\nAdding BUY order (partial fill scenario)...");
        Order buyOrder1 = new Order(100.0, 50, "BUY");
        lob.addOrder(buyOrder1);

        viewLiveOrders(lob.getLiveOrders());

        System.out.printf("The top order is %s\n", lob.getSellOrders().peek().getId());
        assertNull(lob.getLiveOrders().get(sellOrder1.getId()), "SELL Order 1 should not exist now.");
        assertEquals(sellOrder3, lob.getSellOrders().peek(), "The remaining SELL order should be at the top.");

        System.out.println("\nAdding BUY order (full fill scenario)...");
        Order buyOrder2 = new Order(100.0, 20, "BUY");
        lob.addOrder(buyOrder2);

        viewLiveOrders(lob.getLiveOrders());

        assertEquals(2, lob.getSellOrders().size(), "There should be only one SELL order remaining.");
        assertEquals(10, lob.getSellOrders().peek().getQuantity(), "The remaining SELL order should have 10 units.");

        System.out.println("\nAdding BUY order (no match scenario)...");
        Order buyOrder3 = new Order(95.0, 10, "BUY");
        lob.addOrder(buyOrder3);

        viewLiveOrders(lob.getLiveOrders());
        this.viewOrderBySide(lob.getBuyOrders());

        assertEquals(1, lob.getBuyOrders().size(), "There should be one BUY order in the book.");
        assertEquals(10, lob.getBuyOrders().peek().getQuantity(), "The unmatched BUY order should have 10 units.");
        assertEquals(2, lob.getLiveOrders().size(),
                "There should be two live orders (1 SELL, 1 BUY).");
    }
}
