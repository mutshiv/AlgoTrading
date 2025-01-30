# RMB Algo Trading LOB

A Limit Order Book with its Trade Matching Engine.

## Dev Env

Java 21
Why 21? No particular reason but to experiment.

## Running the application

The main class is there to simulate an `**investment banking trading desk**` making use of the Executor service for concurrent processing.
```mvn exec:java -Dexec.mainClass="io.mutshiv.InstrumentTrading"```

There are two test files, the LimitOrderBookTest and MatchingEngineIntegrationTest with the latter testing the functionality of application as a whole.

```mvn test```

## Solution Approach

The Matching Engine is the driver of the application, as such it monitors what happens via the LimitOrderBook (LOB).
For this I chose to make use of the observer pattern, as the trading should happen on **adding** of an order.

Order are sorted on FIFO principles following price-time model. A basic Queue follows the FIFO algorithm, however for this use case a PriorityBlockingQueueQueue was
chosen; as it allows for a custom comparator during instantiation and support by default multi-threading. 

Any time an **Order** is **added, modified** the Matching Engine attempts to do the matching.

There's application of the some of the SOLID and Clean-Code principles, by making the methods more shorter and readable. 

## Efficiency Mechanisms

The use of a combination of ConcurrentHashMap and List data structure ensures that the viewing of order (viewOrders both SELL & BUY orders) function is quick with 
the Big O Notation of a List is O(1) for selection and insert into the list. The usage data structures sparingly also enhances performance of the application 
as the Big O Notation of different data structures can be taxing as data grows.

The ConcurrentHashMap is used for thread-safety, while the management of thread locking and waiting 
provides a performance overhead, it avoids at lot of boiler code to manually managing other data structure...

The ReentrantLock ensures threads are managed by the JVM. The lock can put itself aside if long running and then reenters once it's ready to resume


## Data structures

- CurrentHashMap
- List
- PriorityBlockingQueue
