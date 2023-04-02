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
- [x] Ping services
- [x] Switch nodes to use completable futures

========== Waterfall Service ==========
- [x] Automatically connect nodes to waterfall, default to first node for initial node
- [x] Endpoint: get player info (by name or uuid)
- [x] Endpoint: kick player
- [x] Endpoint: move player
- [x] Endpoint: set initial node (this is the node that players initially connect too, and this is the fallback server)

============ Node  Service ============
- [x] Endpoint: Create node
  - Create node of a given type
- [x] Endpoint: Stop node
  - Move all players to initial node before stopping a node
- [x] Endpoint: Get node from template
  - Find a node with the given template, and send the player too it
- [x] Endpoint: Get all nodes of template
- [x] Endpoint: Get all templates
- [x] Update loop: Make sure all nodes are running by pinging them.  Close them if they do not respond for more than 5 pings.
- [ ] Template Settings
  - [ ] "auto avoid max player count": boolean, if a node with this template is approaching max player count, a new node is created.
  - [ ] "shutdown if no players": boolean, if true, a node will be closed if it has no players.
  - [ ] "max players for merge": int, if greater than 0, if a node stays at or below the given number of players, all players will be moved to another node with this template if it has space.
  - [ ] "min number nodes": int, the minimum number of nodes required for this template
  - [ ] "default": boolean, if true, a node with this template can be a initial node

=========== Node Plugin ===========
- [x] When a player joins or leaves, update any service with a player join or quit endpoints
- [x] Allow any plugin that depends on this plugin to add endpoints (only in onLoad since service will not be started yet), and request endpoints
- [x] Endpoint: Get player info
  - Return player info like inventory, health, potion effects, etc
- [x] Endpoint: Info override
  - Add information like directory, active players, and active plugins
- [x] Endpoint: Get all players
- [x] Template argument with default
- [x] Helper functions
  - [x] Move player to node
  - [x] Move player to template
  - [x] Move player to player with name
  - [x] Move player to player with uuid