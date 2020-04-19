## Apache MPM event 

The original artical from @link:[Apache MPM event](https://httpd.apache.org/docs/2.4/mod/event.html) { open=new }



### Summary
The event Multi-Processing Module (MPM) is designed to allow more requests to be served simultaneously by passing off some processing work to the listeners threads, freeing up the worker threads to serve new requests.

To use the event MPM, add --with-mpm=event to the configure script's arguments when building the httpd.


### Relationship with the Worker MPM
event is based on the worker MPM, which implements a hybrid multi-process multi-threaded server. A single control process (the parent) is responsible for launching child processes. Each child process creates a fixed number of server threads as specified in the ThreadsPerChild directive, as well as a listener thread which listens for connections and passes them to a worker thread for processing when they arrive.

Run-time configuration directives are identical to those provided by worker, with the only addition of the AsyncRequestWorkerFactor.

### How it Works
This MPM tries to fix the 'keep alive problem' in HTTP. After a client completes the first request, it can keep the connection open, sending further requests using the same socket and saving significant overhead in creating TCP connections. However, Apache HTTP Server traditionally keeps an entire child process/thread waiting for data from the client, which brings its own disadvantages. To solve this problem, this MPM uses a dedicated listener thread for each process to handle both the Listening sockets, all sockets that are in a Keep Alive state, sockets where the handler and protocol filters have done their work and the ones where the only remaining thing to do is send the data to the client.

This new architecture, leveraging non-blocking sockets and modern kernel features exposed by APR (like Linux's epoll), no longer requires the mpm-accept Mutex configured to avoid the thundering herd problem.

The total amount of connections that a single process/threads block can handle is regulated by the AsyncRequestWorkerFactor directive.

#### Async connections
Async connections would need a fixed dedicated worker thread with the previous MPMs but not with event. The status page of mod_status shows new columns under the Async connections section:

#### Writing
While sending the response to the client, it might happen that the TCP write buffer fills up because the connection is too slow. Usually in this case, a write() to the socket returns EWOULDBLOCK or EAGAIN to become writable again after an idle time. The worker holding the socket might be able to offload the waiting task to the listener thread, that in turn will re-assign it to the first idle worker thread available once an event will be raised for the socket (for example, "the socket is now writable"). Please check the Limitations section for more information.
#### Keep-alive
Keep Alive handling is the most basic improvement from the worker MPM. Once a worker thread finishes to flush the response to the client, it can offload the socket handling to the listener thread, that in turn will wait for any event from the OS, like "the socket is readable". If any new request comes from the client, then the listener will forward it to the first worker thread available. Conversely, if the KeepAliveTimeout occurs then the socket will be closed by the listener. In this way, the worker threads are not responsible for idle sockets, and they can be re-used to serve other requests.
#### Closing
Sometimes the MPM needs to perform a lingering close, namely sending back an early error to the client while it is still transmitting data to httpd. Sending the response and then closing the connection immediately is not the correct thing to do since the client (still trying to send the rest of the request) would get a connection reset and could not read the httpd's response. The lingering close is time-bounded, but it can take a relatively long time, so it's offloaded to a worker thread (including the shutdown hooks and real socket close). From 2.4.28 onward, this is also the case when connections finally timeout (the listener thread never handles connections besides waiting for and dispatching their events).
These improvements are valid for both HTTP/HTTPS connections.
