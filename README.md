# Reactive Flows #

Reactive Flows is a demo project showing a Reactive web app built with:

- Scala (there are plans to have a Java version, too)
- Akka
- Akka HTTP
- Akka SSE (Server-Sent Events)
- Akka Cluster Sharding
- Akka Data Replication
- AngularJS

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

curl 127.0.0.1:9001/shutdown
```

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
