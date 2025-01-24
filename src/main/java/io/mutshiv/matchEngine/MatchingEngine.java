package io.mutshiv.matchEngine;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.mutshiv.orderBook.LimitOrderBook;
import io.mutshiv.orderBook.Order;

/**
 * MatchingEngine
 */
public class MatchingEngine {

    private final LimitOrderBook lob;
    private final Lock lock = new ReentrantLock();

    public MatchingEngine(LimitOrderBook lob) {
        this.lob = lob;
    }

    public void tradeOnOrder(Order newOrder) {
        lock.lock();

        try {
            if ("BUY".equalsIgnoreCase(newOrder.getSide())) {
                this.matchOrder(newOrder, this.lob.getBuyOrders());
            } else {
                this.matchOrder(newOrder, this.lob.getSellOrders());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param transacationOrder
     * @param sideOrderQueue : for selection BUY or SELL queue
     */
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
            }
        }
    }
}
