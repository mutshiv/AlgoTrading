# RMB Algo Trading LOB

A Limit Order Book with its Trade Matching Engine.

## Running the application

There are two test files, the LimitOrderBookTest and MatchingEngineIntegrationTest with the latter testing the functionality of application as a whole.

```mvn test```

## Solution Approach

The Matching Engine is the driver of the application, as such it monitors what happens via the LimitOrderBook (LOB).
For this I chose to make use of the observer pattern, as the trading should happen on **adding** of an order.

Any time an **Order** is --added, modified, & removed-- the Matching Engine attempts to do the matching.

## Efficiency Mechanisms


## Data structures

- CurrentHashMap
- List
- PriorityQueue
