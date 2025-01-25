package io.mutshiv.orderBook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class LimitOrderBookTest {

    /**
     * Util helper function...
     * Provides a view of all live orders in the orderBook in a formatted way.
     *
     * @param liveOrders : all available orders
     */
    private void viewLiveOrders(Map<String, Order> liveOrders) {
        liveOrders.forEach((key, value) -> System.out.printf(
                "Order ID = %s, price = %.2f, Order Quantity = %d, posted at = %s, Side: %s\n",
                value.getId(), value.getPrice(), value.getQuantity(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(value.getOrderTimeStamp()), ZoneId.systemDefault()),
                value.getSide()));
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    public void viewOrders() {
        LimitOrderBook lob = new LimitOrderBook();

        assertEquals(new ConcurrentHashMap<>(), lob.viewOrders("SELL", 56.3),
                "It should return an Empty Map denoting no orders in the orderBook");
        assertEquals(new ConcurrentHashMap<>(), lob.viewOrders("BUY", 56.3),
                "It should return an Empty Map denoting no orders in the orderBook");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void viewOrders2() {
        LimitOrderBook lob = new LimitOrderBook();

        lob.addOrder(new Order(65.4, 15, "SELL"));
        lob.addOrder(new Order(5.34, 75, "SELL"));
        lob.addOrder(new Order(65.4, 35, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));
        lob.addOrder(new Order(5.34, 75, "SELL"));
        lob.addOrder(new Order(6.4, 15, "BUY"));

        List<Order> filteredOrders = lob.viewOrders("SELL", 5.4);
        assertEquals(0, filteredOrders.size(), "there should not be an order available for that price");

        filteredOrders = lob.viewOrders("SELL", 5.34);
        assertEquals(2, filteredOrders.size(), "there should be two SELL orders available at that price");

        System.out.println("\nFiltered Orders::");
        filteredOrders.forEach(o -> {
            System.out.printf("OrderID = %s; Order Side = %s; Order Quantity = %d", o.getId(), o.getSide(),
                    o.getQuantity());
        });
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void addOrder() {
        LimitOrderBook lob = new LimitOrderBook();

        System.out.println("Adding orders");

        lob.addOrder(new Order(65.4, 15, "SELL"));
        lob.addOrder(new Order(5.34, 75, "SELL"));
        lob.addOrder(new Order(65.4, 35, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));

        this.viewLiveOrders(lob.getLiveOrders());
        System.out.println("\n");

        Order order = lob.getSellOrders().peek();

        assert order != null;
        assertEquals(65.4, order.getPrice(),
                "The first SELL order at the head of the Queue should be the one with a quantity of 15");

        System.out.printf("First order by priority: Order ID = %s, order price = %.2f, order quantity = %d\n",
                order.getId(), order.getPrice(),
                order.getQuantity());

        assertEquals(3, lob.getBuyOrders().size());
        assertEquals(2, lob.getSellOrders().size());
        assertEquals(5, lob.getLiveOrders().size(), "there should be a number matching the added orders.");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void deleteOrder() {
        LimitOrderBook lob = new LimitOrderBook();

        lob.addOrder(new Order(65.4, 15, "SELL"));
        lob.addOrder(new Order(5.34, 75, "SELL"));

        Order orderToDelete = new Order(65.4, 35, "BUY");
        lob.addOrder(orderToDelete);

        lob.addOrder(new Order(6.4, 15, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));

        assertTrue(lob.deleteOrder(orderToDelete.getId()));

        assertEquals(0, lob.viewOrders(orderToDelete.getSide(), orderToDelete.getPrice()), "This Order must not exist as it was just deleted.");

        assertEquals(2, lob.getBuyOrders().size());
        assertEquals(2, lob.getSellOrders().size());

        this.viewLiveOrders(lob.getLiveOrders());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void modifyOrder() throws InterruptedException {
        LimitOrderBook orderBook = new LimitOrderBook();

        Order order1 = new Order(101.0, 50, "BUY");
        Order order2 = new Order(101.0, 30, "BUY");
        Order order3 = new Order(100.0, 20, "BUY");
        Order order4 = new Order(100.0, 20, "BUY");
        Order order5 = new Order(101.0, 30, "BUY");

        orderBook.addOrder(order1);
        Thread.sleep(900);
        orderBook.addOrder(order2);
        Thread.sleep(500);
        orderBook.addOrder(order3);
        Thread.sleep(1300);
        orderBook.addOrder(order4);
        orderBook.addOrder(order5);

        this.viewLiveOrders(orderBook.getLiveOrders());

        assertEquals(order1, orderBook.getBuyOrders().peek(), "Order1 should have the highest priority initially");

        boolean modified = orderBook.modifyOrder(order1.getId(), 40);
        assertTrue(modified, "Order1 modification should succeed");

        System.out.println("\nOrder priority after an order was modified");
        this.viewLiveOrders(orderBook.getLiveOrders());

        Order topOrder = orderBook.getBuyOrders().peek();
        System.out.printf("\nThe new topOrder has ID: %s\n", topOrder.getId());
        assertNotEquals(order1, topOrder, "Order1 should lose its original priority after modification");

        assertEquals(order2, topOrder, "Order2 should now have the highest priority (FIFO for price 101.0)");
    }
}
