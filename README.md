# RMB Algo Trading LOB

`#RRGGBB` A Limit Order Book with its Trade Matching Engine.

## Running the application

There are two test files, the LimitOrderBookTest and MatchingEngineIntegrationTest with the latter testing the functionality of application as a whole.

```mvn test```

## Solution Approach

The Matching Engine is the driver of the application, as such it monitors what happens via the LimitOrderBook (LOB).
For this I chose to make use of the observer pattern, as the trading should happen on **adding** of an order.

Any time an **Order** is **added, modified, & removed** the Matching Engine attempts to do the matching.

There's application of the some of the SOLID and Clean-Code principles, by making the methods more shorter and readable. 

## Efficiency Mechanisms

The use of a combination of ConcurrentHashMap and List data structure ensures that the viewing of order (viewOrders both SELL & BUY orders) function is quick with 
the Big O Notation of a List is O(1) for selection and insert into the list. The ConcurrentHashMap is used for thread-safety, while the management of thread locking and waiting 
provides a performance overhead, it avoid at lot of boiler code to manually managing any other data structure...

The ReentrantLock ensures threads are managed by the JVM. The lock can put itself aside if long running and then reenter once it's ready to resume

## Data structures

- CurrentHashMap
- List
- PriorityQueue
