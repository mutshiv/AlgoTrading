package io.mutshiv.orderBook;

/**
 * IOrderBookObserver
 *
 * The observer interface.
 */
public interface IOrderBookObserver {

    /**
     * Observer event.
     *
     * @param order : incoming order
     * @param orderEvent : ADD || MODIFY || DELETE
     */
    public void onOrderEvent(Order order, String orderEvent);
}
