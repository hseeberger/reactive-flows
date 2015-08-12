# Reactive Flows #

Reactive Flows is a demo project showing a Reactive web app built with:

- Akka Actors
- Akka HTTP
- Akka SSE (Server-Sent Events)
- Akka Distributed Data
- Akka Cluster Sharding
- Akka Persistence
- AngularJS
- Cassandra
- Scala
- etcd

## Usage

- Important: Reactive Flows makes use of [advanced JavaScript](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/find) which isn't yet available for older browsers, so make sure to use Firefox > 25.0, Safari > 7.1 or Chrome > 45.0
- To run the first node, execute `rf1` in an sbt session; you can shutdown the app with `reStop`
- To run further nodes, execute `sbt rf2` or `sbt rf3` from the command line; stop the app with `CTRL+C`
- As the names and labels of the flows aren't persisted, you have to create them after the app has started; see below examples
- Important: Reactive Flows uses the [Cassandra Plugin](https://github.com/krasserm/akka-persistence-cassandra) for the Akka Persistence journal; make sure Cassandra is started and available under the configured contact point, e.g. via `docker run -d --name cassandra -p 9042:9042 cassandra:3.0`
- Important: Reactive Flows uses [ConstructR](https://github.com/hseeberger/constructr) for initializing the cluster; make sure etcd is started and available, e.g. via `docker run -d -p 2379:2379 --name etcd quay.io/coreos/etcd:v2.2.2 -advertise-client-urls http://192.168.99.100:2379 -listen-client-urls http://0.0.0.0:2379`

## REST API Examples ##

```
curl -i 127.0.0.1:8001/flows
curl -i -H 'Content-Type: application/json' -d '{ "label": "Akka" }' 127.0.0.1:8001/flows
curl -i -H 'Content-Type: application/json' -d '{ "label": "AngularJS" }' 127.0.0.1:8001/flows
curl -i -H 'Content-Type: application/json' -d '{ "text": "Akka rocks!" }' 127.0.0.1:8001/flows/akka/messages
curl -i 127.0.0.1:8001/flows/akka/messages

curl -N 127.0.0.1:8001/messages
curl -i -H 'Content-Type: application/json' -d '{ "text": "Akka and AngularJS are a great combination!" }' 127.0.0.1:8001/flows/akka/messages
curl -i -H 'Content-Type: application/json' -d '{ "text": "AngularJS rocks!" }' 127.0.0.1:8001/flows/angularjs/messages

curl -i -X DELETE 127.0.0.1:8001/flows/akka

curl -X DELETE 127.0.0.1:8001
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
