# Reactive Flows #

Reactive Flows is a demo project showing a Reactive web app built with:

- Akka Actors
- Akka HTTP
- Akka SSE (server-sent events)
- Akka Distributed Data
- Akka Cluster Sharding
- Akka Persistence
- AngularJS
- Cassandra
- Scala
- etcd

## Usage

- Important: Reactive Flows makes use of [server-sent events](https://www.w3.org/TR/eventsource) and [advanced JavaScript](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/find) which aren't available for all browsers, so make sure to use Firefox > 25.0, Safari > 7.1 or Chrome > 45.0
- To run a single node, simply execute `reStart` in an sbt session; you can shutdown the app with `reStop`
- To run multiple nodes, build a Docker image with `docker:publishLocal` and execute  the script `bin/run-reactive-flows.sh` without an argument or a number from [0, 10)
- As the names and labels of the flows aren't persisted, you have to create them after the app has started; see below examples
- Important: Reactive Flows uses [ConstructR](https://github.com/hseeberger/constructr) for initializing the cluster; make sure etcd is started and available, e.g. via `bin/run-etcd.sh`
- Important: Reactive Flows uses the Cassandra plugin for Akka Persistence; make sure Cassandra is started and available under the configured contact point, e.g. via `bin/run-cassandra.sh`

## REST API Examples ##

```
curl -s 127.0.0.1:8000/flows | jq
curl -s -H 'Content-Type: application/json' -d '{ "label": "Akka" }' 127.0.0.1:8000/flows | jq
curl -s -H 'Content-Type: application/json' -d '{ "label": "AngularJS" }' 127.0.0.1:8000/flows | jq
curl -s -H 'Content-Type: application/json' -d '{ "text": "Akka rocks!" }' 127.0.0.1:8000/flows/akka/messages | jq
curl -s '127.0.0.1:8000/flows/akka/messages?count=99' | jq

curl -s -N 127.0.0.1:8000/message-events
curl -s -H 'Content-Type: application/json' -d '{ "text": "Akka and AngularJS are a great combination!" }' 127.0.0.1:8000/flows/akka/messages | jq
curl -s -H 'Content-Type: application/json' -d '{ "text": "AngularJS rocks!" }' 127.0.0.1:8000/flows/angularjs/messages | jq

curl -s -X DELETE 127.0.0.1:8000/flows/akka
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
