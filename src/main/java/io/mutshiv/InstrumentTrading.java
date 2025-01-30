package io.mutshiv;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.mutshiv.matchEngine.MatchingEngine;
import io.mutshiv.orderBook.LimitOrderBook;
import io.mutshiv.orderBook.Order;

/**
 * InstrumentTrading
 *
 * Simulating multiple traders on a single LOB.
 *
 */
public class InstrumentTrading {

   public static void main(String[] args) throws InterruptedException {

        LimitOrderBook lob = new LimitOrderBook();
        MatchingEngine tradeME = new MatchingEngine(lob);

        ExecutorService executor = Executors.newFixedThreadPool(15);
        Random random = new Random();

        Runnable buyer1 = createTrader(lob, "BUY", random);
        Runnable buyer2 = createTrader(lob, "BUY", random);
        Runnable buyer3 = createTrader(lob, "BUY", random);

        Runnable seller1 = createTrader(lob, "SELL", random);
        Runnable seller2 = createTrader(lob, "SELL", random);

        executor.submit(buyer1);
        executor.submit(seller1);
        executor.submit(buyer3);
        executor.submit(buyer2);
        executor.submit(seller2);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("\nFinal Buy Orders: " + lob.getBuyOrders());
        System.out.println("Final Sell Orders: " + lob.getSellOrders());

        tradeME.removeObserver();
    }

    private static Runnable createTrader(LimitOrderBook lob, String side, Random random) {
        return () -> {
            try {
                for (int i = 0; i < 3; i++) {
                    Order order = new Order(99 + random.nextDouble() * 5, random.nextInt(10) + 1, side);
                    lob.addOrder(order);
                    System.out.printf("[%s] Placed Order: %s (%.2f, %d)\n", side, order.getId(), order.getPrice(), order.getQuantity());

                    Thread.sleep(random.nextInt(200) + 100);
                    lob.modifyOrder(order.getId(), random.nextInt(5) + 1);
                    System.out.printf("[%s] Modified Order: %s\n", side, order.getId());
                }
            } catch (InterruptedException ignored) {}
        };
    }

}
