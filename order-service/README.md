# order-service

Order Service will provide functionality for purchasing books.
It will use the reactive programming paradigm to improve scalability, resilience, and cost-effectiveness.

##### The Order Service application exposes an API to submit and retrieve book orders, uses a PostgreSQL database to store data, and communicates with Book Service to fetch book details.

![](https://github.com/sanjayrawat1/bookshop/blob/main/order-service/diagrams/order-service.drawio.svg)

#### Asynchronous and non-blocking architectures with Reactor and Spring

##### From thread-per-request to event loop
Non-reactive applications allocate a thread per request. Until a response is returned, the thread will not be used for anything. When the request handling
involves intensive operations like I/O, the thread will block until those operations are completed. For example, if a database read is required, the thread
will wait until data is returned from the database. During the waiting time, the resources allocated to the handling thread are not used efficiently.

![](https://github.com/sanjayrawat1/bookshop/blob/main/order-service/diagrams/thread-per-request-model.drawio.svg "In the thread-per-request model, each request
is handled by a thread dedicated exclusively to its handling.")

Reactive applications are more scalable and efficient by design. Handling requests in a reactive application doesn't involve allocating a given thread
exclusively—requests are fulfilled asynchronously based on events. For example, if a database read is required, the thread handling that part of the flow will
not wait until data is returned from the database. Instead, a callback is registered, and whenever the information is ready, a notification is sent, and one of
the available threads will execute the callback. During that time, the thread that requested the data can be used to process other requests rather than waiting idle.

![](https://github.com/sanjayrawat1/bookshop/blob/main/order-service/diagrams/event-loop-model.drawio.svg "In the event loop model, requests are handled by
threads that don’t block while waiting for an intensive operation, allowing them to process other requests in the meantime.")