========== Microservice Core ==========
- [x] Create endpoints by using a list of callbacks that are passed to service on create
- [x] Each endpoint should take in a json object as a request
- [x] Each endpoint should return a json object as a result, this will be null if there is an error or failure
- [x] Simple way of creating a threaded update loop
- [x] Simple method of making a request to another service or self
- [x] Auto assign a port to each microservice if a port is not assigned to the microservice
- [x] Auto find other active services
- [x] Each service should have an endpoint for path "/" to function as a ping, can be overriden
- [x] Each service should have another overrideable endpoint to get service info on path "/info"
- [X] UUID, that can optionally be specified, this should also work to identify a service

========== Waterfall Service ==========
- [ ] Plugin for waterfall
- [ ] Command to run endpoints
- [ ] Automatically connect nodes to waterfall, default to first node for initial node
- [ ] Endpoint: get player info (by name or uuid)
- [ ] Endpoint: move player
- [ ] Endpoint: set initial node (this is the node that players initially connect too, and this is the fallback server)

============ Node  Service ============
- [ ] Endpoint: Create node
  - Create node of a given type
- [ ] Endpoint: Stop node
  - Move all players to initial node before stopping a node
- [ ] Endpoint: Add player to Node
  - Called when a node gets a player
  - Used to keep track who is on a node and how many players are on each node
- [ ] Endpoint: Remove player from Node
  - See above
- [ ] Endpoint: Move player to template
  - Find a node with the given template, and send the player too it
- [ ] Endpoint: Node started
  - Node calls this endpoint when they start
- [ ] Endpoint: Node stopped
  - Node calls this endpoint when they stop
- [ ] Update loop: Make sure all nodes are running by pinging them.  Close them if they do not respond for more than 5 pings.
- [ ] Template Settings
  - "auto avoid max player count": boolean, if a node with this template is approaching max player count, a new node is created.
  - "overflow": enum, describes the overflow behavior of the template.
    - "allow": if no nodes with this template have an available slot, another one will be picked.
    - "wait": players are put into a queue that waits until a slot is open in a node or another node with this template is created.
    - "kick": kick the player if there are no active slots.
    - "cancel": simply do not move the player to the template if no slot can be found.
  - "shutdown if no players": boolean, if true, a node will be closed if it has no players.
  - "max players for merge": int, if greater than 0, if a node stays at or below the given number of players, all players will be moved to another node with this template if it has space.
  - "min number nodes": int, the minimum number of nodes required for this template
  - "default": boolean, if true, a node with this template will be the 

=========== Node Plugin ===========
- [ ] When a player joins or leaves, update the node service
- [ ] When node starts or shuts down, update the node service
- [ ] Allow any plugin that depends on this plugin to add endpoints (only in onLoad since service will not be started yet), and request endpoints
- [ ] Endpoint: Get player info
  - Return player info like inventory, health, potion effects, etc
- [ ] Endpoint: Set player info
  - Optionally set any info given in the "get player info" endpoint
- [ ] Endpoint: Info override
  - Add information like directory, active players, and active plugins