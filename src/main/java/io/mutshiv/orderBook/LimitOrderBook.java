package io.mutshiv.orderBook;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LimitOrderBook {
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;

    private final Lock lock = new ReentrantLock();
    private final Map<String, Order> liveOrders;

    public LimitOrderBook() {
        this.buyOrders = new PriorityQueue<>(Comparator.<Order>comparingDouble(Order::getPrice)
                .thenComparing(Order::getOrderTimeStamp));
        this.sellOrders = new PriorityQueue<>(
                Comparator.<Order>comparingDouble(Order::getPrice)
                        .thenComparing(Order::getOrderTimeStamp));
        this.liveOrders = new ConcurrentHashMap<>();
    }

    public PriorityQueue<Order> getBuyOrders() {
        return buyOrders;
    }

    public PriorityQueue<Order> getSellOrders() {
        return sellOrders;
    }

    /**
     * A live Orders view based on side and price.
     * Returns back a sorted list of orders by their price priority level (Price then time if prices are equal)
     *
     * @param side
     * @param price
     * @return Map<String, Order>
     */
    public Map<String, Order> viewOrders(String side, double price) {
        return this.liveOrders.entrySet().stream()
                .filter(order -> order.getValue().getPrice() == price
                        && order.getValue().getSide().equalsIgnoreCase(side))
                .sorted((entry1, entry2) -> Double.compare(entry2.getValue().getPrice(), entry1.getValue().getPrice()))
                .sorted((entry1, entry2) -> Long.compare(entry2.getValue().getOrderTimeStamp(), entry1.getValue().getOrderTimeStamp()))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Adds an order to the queue based on the order side.
     * Also updates that liveOrder map.
     *
     * @param order
     */
    public void addOrder(Order order) {
        lock.lock();
        try {
            if ("BUY".equalsIgnoreCase(order.getSide())) {
                buyOrders.add(order);
            } else {
                sellOrders.add(order);
            }

            this.liveOrders.put(order.getId(), order);
        } finally {
            lock.unlock();
        }
    }

    /**
     * This modifies an existing order, it loses it's priority after.
     *
     * @param orderId
     * @param newOrderQuantity
     * @return
     */
    public boolean modifyOrder(String orderId, int newOrderQuantity) {
        lock.lock();

        try {
            Order order = this.liveOrders.remove(orderId);

            if (order != null) {
                if ("BUY".equalsIgnoreCase(order.getSide())) {
                    this.buyOrders.remove(order);
                } else {
                    this.sellOrders.remove(order);
                }

                /*
                 * The next two line could have be achieved by creating a new order entirely.
                 * This would have allowed me to fully encapsulate the OrderTimeStamp, because opening it up with a setter
                 * allows for possible modification to increase the order priority.
                 *
                 * although Java is a GC language, I figured creating a new Order object every time a modification has to happen
                 * would pollute the heap memory. Even though we can invoke GC, it however runs when it runs and not on demand. This
                 * would mean in high volume usage the program at some point may slow down do to JVM (Heap Space) memory issues if scaling is not done properly
                 * on cloud environment, on Virtual machines the CPU usage will affect the performance of the application.
                 */
                order.modifyOrder(newOrderQuantity);
                order.setOrderTimeStamp(System.nanoTime());

                this.addOrder(order);

                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }


    /**
     * @param orderId
     * @return
     */
    public boolean deleteOrder(String orderId) {
        lock.lock();

        try {
            Order order = this.liveOrders.remove(orderId);

            if (order != null) {
                if ("BUY".equalsIgnoreCase(order.getSide())) {
                    return this.buyOrders.remove(order);
                } else {
                    return this.sellOrders.remove(order);
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
