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

##### Managing database schemas with flyway
Flyway doesn't support R2DBC yet: https://github.com/flyway/flyway/issues/2502, so we need to provide a JDBC driver to communicate with the database. The flyway migration tasks are only run at application
startup and in a single thread, so using a non-reactive communication for this one case doesn't impact the overall application's scalability and efficiency.

Finally, in the application.yml file, configure Flyway to use the same database managed with Spring Data R2DBC but using the JDBC driver.

##### Interaction between order service and catalog service
You will use WebClient to establish non-blocking request/response interactions. I will also explain how to make your application more resilient by adopting patterns like
timeouts, retries, and fail-overs using the Reactor operators timeout(), retryWhen(), and onError().

When an order is submitted, Order Service calls Catalog Service over HTTP to check the book's availability and fetch its details.

![](https://github.com/sanjayrawat1/bookshop/blob/main/order-service/diagrams/order-service-interaction-with-catalog-service.drawio.svg)

#### Resilient applications with Reactive Spring

Resilience is about keeping a system available and delivering its service, even when failures happen. It's critical to design fault-tolerant applications.
The goal is to keep the system available without the user noticing any failures. In the worst-case scenario, the system may have degraded functionality
(graceful degradation), but it should still be available.
The critical point in achieving resilience (or fault-tolerance) is keeping the faulty component isolated until the fault is fixed. By doing that you will
prevent _crack propagation_.

##### Timeouts
Whenever your application calls a remote service, you don't know if and when a response will be received. Timeouts (also called _time limiters_) are for
preserving the responsiveness of your application in case a response is not received within a reasonable time period.

Two main reasons for setting up timeouts:
1. If you don't limit the time your client waits, you risk your computational resources being blocked for too long (for imperative applications).
In the worst-case scenario, your application will be completely unresponsive because all the available threads are blocked, waiting for responses from a
remote service, and there are no threads available to handle new requests.
2. If you can't meet Service Level Agreement (SLAs) there is no reason to keep waiting for an answer. It's better to fail the request.

![](https://github.com/sanjayrawat1/bookshop/blob/main/order-service/diagrams/request-response-interaction-when-timeouts-and-failovers-defined.drawio.svg)

When a response is received from the remote service within the time limit, the request is successful. If the timeout expires and no response is received,
then a fallback behavior is executed, if any. Otherwise, an exception is thrown.

Timeouts improve application resilience and follow the principle of failing fast. But setting a good value for the timeout can be tricky. You should consider
your system architecture as a whole. You should carefully design a time-limiting strategy for all the integration points in your system to meet your software’s
SLAs and guarantee a good user experience.
If Catalog Service were available, but a response couldn't get to Order Service within the time limit, the request would likely still be processed by Catalog
Service. That is a critical point to consider when configuring timeouts.
