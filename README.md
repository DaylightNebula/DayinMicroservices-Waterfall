# DaylinMicroservices-Waterfall
[![](https://jitpack.io/v/DaylightNebula/DaylinMicroservices-Waterfall.svg)](https://jitpack.io/#DaylightNebula/DaylinMicroservices-Waterfall)

These are a set of three modules that creates and manages Waterfall and Paper based Minecraft server using DaylinMicroservices for fault tolerance, simplicity and inter-node communication.

The three modules
 - DaylinMicroservices-Node: The plugin for Paper plugins that connects the Minecraft servers to the Waterfall network.
 - DaylinMicroservices-NodeManager: This is a standalone microservice that can create, destroy, and manage active nodes.  More information below.
 - DaylinMicroservices-Waterfall: This is a plugin for the Waterfall proxy that connects all nodes together.

## Module: DaylinMicroservices-Node
This should be added to the plugins folder of any network using the DaylinMicroservices-Waterfall Waterfall plugin.  This acts as a simple microservice that connects to the Waterfall module and provides the Waterfall module with player info and counts, as well as, the ability to move players around.

## Module: DaylinMicroservices-NodeManager