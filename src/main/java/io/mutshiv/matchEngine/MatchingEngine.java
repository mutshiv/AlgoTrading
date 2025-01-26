package io.mutshiv.matchEngine;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.mutshiv.orderBook.IOrderBookObserver;
import io.mutshiv.orderBook.LimitOrderBook;
import io.mutshiv.orderBook.Order;

/**
 * MatchingEngine
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
            System.out.printf("Trading on order: %s%n", order.getId());
            this.tradeOnOrder(order);
        } else if ("DELETE".equalsIgnoreCase(orderEventType)) {
            System.out.printf("Order Deleted: %s%n", order.getId());
        }
	}

    public void tradeOnOrder(Order newOrder) {
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
     * matches the incoming order with best match prices, it also implements partial-fills
     *
     * @param transactionOrder : incoming tradind order
     * @param sideOrderQueue : for selection of BUY or SELL queue
     */
    private void matchOrder(Order transactionOrder, PriorityQueue<Order> sideOrderQueue) {
        while (!sideOrderQueue.isEmpty() && transactionOrder.getQuantity() > 0) {
            Order bestMatch = sideOrderQueue.peek();
            boolean canTrade = "BUY".equalsIgnoreCase(transactionOrder.getSide())
                    ? transactionOrder.getPrice() >= bestMatch.getPrice()
                    : transactionOrder.getPrice() <= bestMatch.getPrice();

            if (canTrade)
                break;

            int tradeQuantity = Math.min(transactionOrder.getQuantity(), bestMatch.getQuantity());

            System.out.printf("Trade Executed: %d units @ %.2f%n", tradeQuantity, bestMatch.getPrice());

            transactionOrder.reduceQuantity(tradeQuantity);
            bestMatch.reduceQuantity(tradeQuantity);

            if (bestMatch.getQuantity() == 0) {
                sideOrderQueue.poll();
            }
        }
    }
}
