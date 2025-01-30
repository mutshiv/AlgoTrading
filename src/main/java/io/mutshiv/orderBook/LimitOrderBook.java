package io.mutshiv.orderBook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LimitOrderBook {

    private final PriorityBlockingQueue<Order> buyOrders;
    private final PriorityBlockingQueue<Order> sellOrders;

    private final Lock lock = new ReentrantLock();
    private final ConcurrentHashMap<String, Order> liveOrders;
    private final List<IOrderBookObserver> observers;

    public LimitOrderBook() {
        this.buyOrders = new PriorityBlockingQueue<>(50, Comparator.<Order>comparingDouble(Order::getPrice)
                .thenComparingLong(Order::getOrderTimeStamp));

        this.sellOrders = new PriorityBlockingQueue<>(50,
                Comparator.<Order>comparingDouble(Order::getPrice)
                        .thenComparingLong(Order::getOrderTimeStamp));

        this.liveOrders = new ConcurrentHashMap<>();
        this.observers = new ArrayList<>();
    }

    public PriorityBlockingQueue<Order> getBuyOrders() {
        return buyOrders;
    }

    public PriorityBlockingQueue<Order> getSellOrders() {
        return sellOrders;
    }

    public void registerObserver(IOrderBookObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(IOrderBookObserver observer) {
        observers.remove(observer);
    }

    /**
     * This is an auxilliary function that show all orders in one list.
     * These orders will not be ordered as the Map is not ordered.
     * 
     * @return ConcurrentHashMap<String, Order> : key is the order UUID
     */
    public ConcurrentHashMap<String, Order> getLiveOrders() {
        return liveOrders;
    }

    /**
     * A Orders view based on side and price.
     * Returns back a sorted list of orders by their price priority level (Price
     * then time if prices are equal)
     *
     * @param side  : BUY || SELL
     * @param price : Double value
     * @return List<Order> || an Empty Map if there are no order yet
     */
    public List<Order> viewOrders(String side, double price) {
        PriorityBlockingQueue<Order> ordersQueue = side.equalsIgnoreCase("BUY") ? this.getBuyOrders() : this.getSellOrders();
        lock.lock();

        try {
            return ordersQueue.stream()
                    .filter(order -> order.getPrice() == price
                            && order.getSide().equalsIgnoreCase(side))
                    .sorted(Comparator.comparingDouble(Order::getPrice)
                            .thenComparingLong(Order::getOrderTimeStamp))
                    .collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds an order to the queue based on the order side.
     * Also updates that liveOrder map.
     *
     * @param order : {@link Order}
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
            notifyObservers(order, "ADD");
        } finally {
            lock.unlock();
        }
    }

    /**
     * This modifies an existing order, it loses its priority after.
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
                 * The next two lines could have been achieved by creating a new order object
                 * entirely.
                 * This would have allowed me to fully encapsulate the OrderTimeStamp, because
                 * opening it up with a setter
                 * allows for possible modification to increase the order priority.
                 *
                 * although Java is a Garbage Collected (GC) language, I figured creating a new
                 * Order object every
                 * time a modification (per thread) has to happen
                 * would pollute the heap memory. Even though we can request GC, it however runs
                 * when it runs and not on demand. This
                 * would mean in high volume usage the program at some point may slow down due
                 * to
                 * JVM (Heap Space) memory issues if scaling is not done properly
                 * on cloud environment, on Virtual machines the CPU usage will affect the
                 * performance of the application. GC itself also takes up compute time on the
                 * CPU
                 */
                order.modifyOrder(newOrderQuantity);
                order.setOrderTimeStamp(System.currentTimeMillis());

                this.addOrder(order);

                notifyObservers(order, "MODIFY");

                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param orderId : UUID Id of the order
     * @return Boolean : true if order with such ID exist, else false
     *
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

            notifyObservers(order, "DELETE");
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notifies all registered observers for an order event.
     * Order updates are also done here as all this gets invoked on every order
     * event.
     *
     * @param order     : The order that triggered the event.
     * @param eventType : The type of event ("ADD" or "MODIFY" or "DELETE").
     *
     */
    private void notifyObservers(Order order, String eventType) {
        lock.lock();

        try {
            for (IOrderBookObserver observer : observers) {
                if (order.getQuantity() == 0)
                    liveOrders.remove(order.getId());

                observer.onOrderEvent(order, eventType);

                if (order.getSide().equalsIgnoreCase("BUY") && order.getQuantity() == 0) {
                    buyOrders.remove(order);
                } else if (order.getSide().equalsIgnoreCase("SELL") && order.getQuantity() == 0) {
                    sellOrders.remove(order);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
