package io.mutshiv.orderBook;

/**
 * IOrderBookObserver
 */
public interface IOrderBookObserver {

    public void onOrderEvent(Order order, String orderEvent);
}
