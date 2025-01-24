package io.mutshiv.orderBook;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class LimitOrderBookTest {

    @Test
    void viewOrders() {
        LimitOrderBook lob = new LimitOrderBook();

        assertNull(lob.viewOrders("SELL", 56.3));
        assertNull(lob.viewOrders("BUY", 56.3));
    }

    @Test
    void viewOrders2() {
        LimitOrderBook lob = new LimitOrderBook();

        lob.addOrder(new Order(65.4, 15, "SELL"));
        lob.addOrder(new Order(5.34, 75, "SELL"));
        lob.addOrder(new Order(65.4, 35, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));

        // assertNotEquals(new ConcurrentHashMap<>(), lob.viewOrders("SELL", 5.43));

        assertEquals(1, lob.viewOrders("SELL", 5.43).size());
    }

    @Test
    void addOrder() {
        LimitOrderBook lob = new LimitOrderBook();

        lob.addOrder(new Order(65.4, 15, "SELL"));
        lob.addOrder(new Order(5.34, 75, "SELL"));
        lob.addOrder(new Order(65.4, 35, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));

        assertEquals(3, lob.getBuyOrders().size());
        assertEquals(2, lob.getSellOrders().size());
    }

    @Test
    void modifyOrder() {
        LimitOrderBook orderBook = new LimitOrderBook();

        Order order1 = new Order(101.0, 50, "BUY");
        Order order2 = new Order(101.0, 30, "BUY");
        Order order3 = new Order(100.0, 20, "BUY");
        Order order4 = new Order(100.0, 20, "BUY");

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(order4);

        System.out.println("Buy Orders:");
        for(Order order : orderBook.getBuyOrders()) {
            System.out.printf("Order ID = %s, order price = %.2f, order quantity = %d\n",
                    order.getId(), order.getPrice(),
                    order.getQuantity());
        }

        assertEquals(order1, orderBook.getBuyOrders().peek(), "Order1 should have the highest priority initially");

        boolean modified = orderBook.modifyOrder(order1.getId(), 40);
        assertTrue(modified, "Order1 modification should succeed");

        Order topOrder = orderBook.getBuyOrders().peek();
        assertNotEquals(order1, topOrder, "Order1 should lose its original priority after modification");

        assertEquals(order2, topOrder, "Order2 should now have the highest priority (FIFO for price 101.0)");

        System.out.println("Updated Buy Orders:");
        while (!orderBook.getBuyOrders().isEmpty()) {
            System.out.printf("Order ID = %s, order price = %.2f, order quantity = %d\n",
                    orderBook.getBuyOrders().poll().getId(), orderBook.getBuyOrders().poll().getPrice(),
                    orderBook.getBuyOrders().poll().getQuantity());
        }
    }

    @Test
    void deleteOrder() {
        LimitOrderBook lob = new LimitOrderBook();

        lob.addOrder(new Order(65.4, 15, "SELL"));
        lob.addOrder(new Order(5.34, 75, "SELL"));
        Order orderToDelete = new Order(65.4, 35, "BUY");
        lob.addOrder(orderToDelete);
        lob.addOrder(new Order(6.4, 15, "BUY"));
        lob.addOrder(new Order(6.4, 15, "BUY"));

        lob.deleteOrder(orderToDelete.getId());

        assertEquals(2, lob.getBuyOrders().size());
        assertEquals(2, lob.getSellOrders().size());
    }
}
