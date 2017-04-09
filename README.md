# Fitzroy

Distributed system simulator and distributed algorithm scratchpad.

* [Installation](#installation)
* [Fitzroy Model](#fitzroy-model)
* [Getting Started](#getting-started)

Fitzroy exploring the treacherous waters of distributed systems theory:
![Fitzroy](/images/fitzroy.gif?raw=true "Fitzroy")

So far it includes:  
* Processes, Channels, and Topologies  
* A TopologyRunner that simulates time and concurrency
* Nodes that can be augmented with components for encapsulating state and behavior  
* A NodeComponent template for quickly implementing new components
  * NodeComponents allow algorithm decoupling. For example, the Two Phase Commit
    component sends a Broadcast command that will be executed by whatever
    broadcast component is present.
* Lamport clocks and vector clocks  
* The Tarry algorithm for finding a spanning tree of a network  
* The Chandy-Lamport snapshot algorithm  
* Simple broadcast (flooding) and echo algorithms
* Two Phase Commit
* A test module to check whether broadcast algorithms work
* A test module to check whether commit/consensus algorithms work

## Installation

## Fitzroy Model

## Getting Started

