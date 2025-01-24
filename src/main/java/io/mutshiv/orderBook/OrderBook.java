package io.mutshiv.orderBook;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OrderBook {
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final Map<String, Order> liveOrders;
    private final Lock lock = new ReentrantLock();

    public OrderBook() {
        this.buyOrders = new PriorityQueue<>((o1, o2) -> Double.compare(o2.getPrice(), o1.getPrice()));
        this.sellOrders = new PriorityQueue<>(Comparator.comparingDouble(Order::getPrice));
        this.liveOrders = new ConcurrentHashMap<>();
    }

    /**
     * A live Orders view based on side and price.
     * Returns back a sorted list of orders by their price priority level
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
            PriorityQueue<Order> targetQueue = "BUY".equalsIgnoreCase(order.getSide()) ? buyOrders : sellOrders;
            matchOrder(order, "BUY".equalsIgnoreCase(order.getSide()) ? sellOrders : buyOrders);
            if (order.getQuantity() > 0) {
                targetQueue.add(order);
                this.liveOrders.put(order.getId(), order);
            }
        } finally {
            lock.unlock();
        }
    }

    private void matchOrder(Order transacationOrder, PriorityQueue<Order> sideOrderQueue) {
        while (!sideOrderQueue.isEmpty() && transacationOrder.getQuantity() > 0) {
            Order bestMatch = sideOrderQueue.peek();
            boolean canTrade = "BUY".equalsIgnoreCase(transacationOrder.getSide())
                    ? transacationOrder.getPrice() >= bestMatch.getPrice()
                    : transacationOrder.getPrice() <= bestMatch.getPrice();

            if (!canTrade)
                break;

            int tradeQuantity = Math.min(transacationOrder.getQuantity(), bestMatch.getQuantity());

            System.out.printf("Trade Executed: %d units @ %.2f%n", tradeQuantity, bestMatch.getPrice());

            transacationOrder.reduceQuantity(tradeQuantity);
            bestMatch.reduceQuantity(tradeQuantity);

            if (bestMatch.getQuantity() == 0) {
                sideOrderQueue.poll();
                this.liveOrders.remove(bestMatch.getId());
            }
        }
    }
}
