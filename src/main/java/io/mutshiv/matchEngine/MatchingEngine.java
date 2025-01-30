package io.mutshiv.matchEngine;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.mutshiv.orderBook.IOrderBookObserver;
import io.mutshiv.orderBook.LimitOrderBook;
import io.mutshiv.orderBook.Order;

/**
 * MatchingEngine
 *
 * All the fullfilling of orders done here.
 *
 */
public class MatchingEngine implements IOrderBookObserver {

    private final LimitOrderBook lob;
    private final Lock lock = new ReentrantLock();

    public MatchingEngine(LimitOrderBook lob) {
        this.lob = lob;
        this.lob.registerObserver(this);
    }

    @Override
    public void onOrderEvent(Order order, String orderEventType) {
        System.out.printf("Order Event: %s -> %s%n", orderEventType, order.getId());

        if ("ADD".equalsIgnoreCase(orderEventType) || "MODIFY".equalsIgnoreCase(orderEventType)) {
            this.tradeOnOrder(order);
        } else if ("DELETE".equalsIgnoreCase(orderEventType)) {
            System.out.printf("Order Deleted: %s%n", order.getId());
            this.lob.deleteOrder(order.getId());
            this.lob.getLiveOrders().remove(order.getId());
        }
    }

    /**
     * 
     * @param newOrder : incoming Order
     */
    private void tradeOnOrder(Order newOrder) {
        lock.lock();

        try {
            if ("BUY".equalsIgnoreCase(newOrder.getSide())) {
                this.matchOrder(newOrder, this.lob.getSellOrders());
            } else {
                this.matchOrder(newOrder, this.lob.getBuyOrders());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * matches the incoming order with best match prices, it also implements
     * partial-fills. If the order is filled completely, it is removed from the
     * particular queue and liveOrder ConcurrentHashMap.
     *
     * @param transactionOrder : incoming trade order
     * @param sideOrderQueue   : for selection of BUY or SELL queue
     */
    private void matchOrder(Order transactionOrder, PriorityBlockingQueue<Order> sideOrderQueue) {
        while (!sideOrderQueue.isEmpty() && transactionOrder.getQuantity() > 0) {
            Order bestMatch = sideOrderQueue.peek();
            boolean canTrade = "BUY".equalsIgnoreCase(transactionOrder.getSide())
                    ? transactionOrder.getPrice() >= bestMatch.getPrice()
                    : transactionOrder.getPrice() <= bestMatch.getPrice();

            if (!canTrade)
                break;

            int tradeQuantity = Math.min(transactionOrder.getQuantity(), bestMatch.getQuantity());

            System.out.printf(
                    "[Trade Executed: %d units @ %.2f\tThe Orders are:\t Transaction Order (ID: %s, [Price: %.2f, Side: %s]); \t Best Match (ID: %s, [Price: %.2f, Side: %s])]\n",
                    tradeQuantity, bestMatch.getPrice(), transactionOrder.getId(), transactionOrder.getPrice(),
                    transactionOrder.getSide(), bestMatch.getId(), bestMatch.getPrice(), bestMatch.getSide());

            transactionOrder.reduceQuantity(tradeQuantity);
            bestMatch.reduceQuantity(tradeQuantity);

            if (bestMatch.getQuantity() == 0) {
                sideOrderQueue.poll();
                this.lob.getLiveOrders().remove(bestMatch.getId());

                if (transactionOrder.getQuantity() == 0)
                    this.lob.getLiveOrders().remove(transactionOrder.getId());
            }
        }
    }

    /**
     * Clean-up method onDestroy.
     */
    public void removeObserver() {
        this.lob.unregisterObserver(this);
    }
}
