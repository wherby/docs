# NIO and Akka-Http

@@@ index

* [Appache MPM Event](code/appacheMPMEvent.md)

* [Scalable IO in Java](code/scalableIOInJava.md)

@@@

## What's Java NIO?

See the : [Java NIO浅析](https://tech.meituan.com/2016/11/04/nio.html)

## What's relationship about Java NIO and Akka-Http:
Akka IO is based on Java NIO library:

```scala  
package akka.io

import java.util.{ Iterator ⇒ JIterator }
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.channels.{ SelectableChannel, SelectionKey, CancelledKeyException }
import java.nio.channels.SelectionKey._

```

But is Akka-Http is same as Jetty or Netty?

No, you could find the answer from [Understanding Reactive IO and Back-Pressure with (your own) Akka Http Server](https://medium.com/@unmeshvjoshi/understanding-reactive-io-and-back-pressure-with-your-own-akka-http-server-d4b64921059a) 


## Why Akka-http is introduced?

Well, the main answer is in the same [Understanding Reactive IO and Back-Pressure with (your own) Akka Http Server](https://medium.com/@unmeshvjoshi/understanding-reactive-io-and-back-pressure-with-your-own-akka-http-server-d4b64921059a). 


## Go through the article

We will quickly go throught this article:

1. The classical Java BIO can't fit the requirement
```
Most web servers used to use thread per connection model to handle http requests. In this model, the
servers also used to use blocking IO libraries to read requests from connections and write responses.This works well when connections are short lived. Traditionally, most http requests used to be short
lived. A client opened a connection, sent request, read the response and closed the connection. This
allowed servers to have a fixed thread pool to serve the requests and use blocking IO to read and
write to connections. Using blocking IO, also allowed having synchronous APIs, which is a simple
programming model to work with.

This model created a big problem with changing patterns of web requests though.

To support Keep-Alive connections, http pipelining, servers could no longer rely on blocking IO and
 threads only. Apache, as of version 2.4 uses evented IO to handle connections

(https://httpd.apache.org/docs/2.4/mod/event.html)
```
You may want to see the [Appache MPM Event](https://httpd.apache.org/docs/2.4/mod/event.html)

2. Then there will intruce some Java NIO design pattern throw :[Scalable IO in Java](http://gee.cs.oswego.edu/dl/cpjslides/nio.pdf)  { open=new }. The short version from: @ref:[Scalable IO in Java](code/scalableIOInJava.md)

3. Because the actor is lightweight, so we could use create an Actor per connection. Actor could also resolve the state management difficult comparing use Java framework.  (This solution is just use Actor to hold working thread.)

4. For the back pressure issue, then introduce with Streaming method, which is the Akka-Http.








