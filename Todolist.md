========== Microservice Core ==========
- [ ] Create endpoints by using a list of callbacks that are passed to service on create
- [ ] Each endpoint should take in a json object as a request
- [ ] Each endpoint should return a json object as a result, this will be null if there is an error or failure
- [ ] Simple way of creating a threaded update loop
- [ ] Simple method of making a request to another service or self
- [ ] Auto assign a port to each microservice if a port is not assigned to the microservice
- [ ] Auto find other active services
- [ ] Each service should have an endpoint for path "/" to function as a ping, can be overriden
- [ ] Each service should have another overrideable endpoint to get service info on path "/info"

========== Waterfall Service ==========
- [ ] Plugin for waterfall
- [ ] Endpoint: get player info
- [ ] Endpoint: move player
- [ ] Endpoint: set initial node (this is the node that players initially connect too, and this is the fallback server)

============ Node  Service ============
- [ ] Endpoint: Create node
- [ ] Endpoint: Stop node
  - [ ] Move all players to initial node before stopping a node
- [ ] Endpoint: Add player to Node
  - [ ] Called when a node gets a player
  - [ ] Used to keep track who is on a node and how many players are on each node