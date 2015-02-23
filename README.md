# Reactive Flows #

Reactive Flows is a demo project showing a Reactive web app built with:

- Scala
- Akka
- Akka Cluster Sharding
- Akka Data Replication
- Akka HTTP
- Akka Persistence
- Akka SSE (Server-Sent Events)
- AngularJS

## Usage

- Important: Reactive Flows makes use of [advanced JavaScript](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/find) which isn't yet available in Chrome, so make sure to use Firefox or Safari
- To run the first node, execute `rf1` in an sbt session; you can shutdown the app with `reStop`
- To run further nodes, execute `sbt rf2` or `sbt rf3` from the command line; stop the app with `CTRL+C`
- As the names and labels of the flows aren't persisted, you have to create them after the app has started; see below examples
- Important: Reactive Flows uses a shared journal for Akka Persistence; this single point of failure gets started on the first node, if you shutdown this one all others will fail

## REST API Examples ##

```
curl -i 127.0.0.1:9001/flows
curl -i -H 'Content-Type: application/json' -d '{ "label": "Akka" }' 127.0.0.1:9001/flows
curl -i -H 'Content-Type: application/json' -d '{ "label": "AngularJS" }' 127.0.0.1:9001/flows
curl -i -H 'Content-Type: application/json' -d '{ "text": "Akka rocks!" }' 127.0.0.1:9001/flows/akka/messages
curl -i 127.0.0.1:9001/flows/akka/messages

curl -N 127.0.0.1:9001/messages
curl -i -H 'Content-Type: application/json' -d '{ "text": "Akka and AngularJS are a great combination!" }' 127.0.0.1:9001/flows/akka/messages
curl -i -H 'Content-Type: application/json' -d '{ "text": "AngularJS rocks!" }' 127.0.0.1:9001/flows/angularjs/messages

curl -i -X DELETE 127.0.0.1:9001/flows/akka

curl -X DELETE 127.0.0.1:9001
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
