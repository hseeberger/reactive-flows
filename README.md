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
- Important: Reactive Flows uses [ConstructR](https://github.com/hseeberger/constructr) for initializing the cluster; make sure etcd is started and available
- Important: Reactive Flows uses the Cassandra plugin for Akka Persistence; make sure Cassandra is started and available under the configured contact point

### Run in sbt

First start Cassandra and etcd:

```
docker-compose rm -f -s -v
docker-compose up -d etcd cassandra
```

Then start a first instance in sbt:

```
reStart --- -Dcassandra-journal.contact-points.0=127.0.0.1:9042 -Dconstructr.coordination.host=127.0.0.1 -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2550
```

To start another instance in sbt, provide additional configuration settings:

```
reStart --- -Dcassandra-journal.contact-points.0=127.0.0.1:9042 -Dconstructr.coordination.host=127.0.0.1 -Dakka.remote.netty.tcp.hostname=127.0.0.1 -Dakka.remote.netty.tcp.port=2551 -Dreactive-flows.api.port=8001
```

## REST API Examples ##

```
http localhost:8000/flows
http localhost:8000/flows label=Akka
http localhost:8000/flows label=Scala
http localhost:8000/flows/akka/posts text='Akka rocks!'
http localhost:8000/flows/akka/posts text='Akka really rocks!'
http localhost:8000/flows/scala/posts text='Scala rocks, too!'
http localhost:8000/flows/scala/posts text='Scala really rocks, too!'
http localhost:8000/flows/akka/posts count==99
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
