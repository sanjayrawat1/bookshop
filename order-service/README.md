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

##### Retries
When a service downstream doesn't respond within a specific time limit or replies with a server error related to its momentary inability to process the request,
you can configure your client to try again. When a service doesn't respond correctly, it's likely because it's going through some issues, and it's unlikely
that it will manage to recover immediately. Starting a sequence of retry attempts, one after the other, risks making the system even more unstable.
You don't want to launch a DoS attack on your own applications!
A better approach is using an _exponential backoff_ strategy to perform each retry attempt with a growing delay. By waiting for more and more time between one
attempt and the next, you're more likely to give the backing service time to recover and become responsive again.

![](https://github.com/sanjayrawat1/bookshop/blob/main/order-service/diagrams/request-response-interaction-when-retries-defined.drawio.svg)

When Catalog Service doesn't respond successfully, Order Service will try at most three more times with a growing delay.

Project Reactor provides a retryWhen() operator to retry an operation when it fails. The position where you apply it to the reactive stream matters.
1. Placing the retryWhen() operator after timeout() means that the timeout is applied to each retry attempt.
2. Placing the retryWhen() operator before timeout() means that the timeout is applied to the overall operation (that is, the whole sequence of the initial
request and retries has to happen within the given time limit).

In BookClient, we want the timeout to apply to each retry attempt, so we'll use the first option. The time limiter is applied first. If the timeout expires,
the retryWhen() operator kicks in and tries the request again.

Retries increase the chance of getting a response back from a remote service when it's momentarily overloaded or unresponsive. Use them wisely.
Idempotent requests like read operations can be retried without harm. Even some write requests can be idempotent. You could perform it a few times,
but the outcome will not change. You shouldn't retry non-idempotent requests, or you'll risk generating inconsistent states.

Retries are a helpful pattern whenever the service downstream is momentarily unavailable or slow due to overloading, but it's likely to heal soon. In this case,
you should limit the number of retries and use exponential backoff to prevent adding extra load on an already overloaded service. On the other hand,
you shouldn't retry the request if the service fails with a recurrent error, such as if it's entirely down or returns an acceptable error like 404.

##### Fallback and error handling
A system is resilient if it keeps providing its services in the face of faults without the user noticing. Sometimes that's not possible, so the least you can
do is ensure a graceful degradation of the service level. Specifying a fallback behavior can help you limit the fault to a small area while preventing the rest
of the system from misbehaving or entering a faulty state.
A fallback function can be triggered when some errors or exceptions occur, but they’re not all the same. Some errors are acceptable and semantically meaningful
in the context of your business logic. When Order Service calls Catalog Service to fetch information about a specific book, a 404 response might be returned.
That's an acceptable response that should be addressed to inform the user that the order cannot be submitted because the book is not available in the catalog.
However, in that case you don't want to retry the request as well. Project Reactor provides an onErrorResume() operator to define a fallback when a specific
error occurs. You can add it to reactive stream after timeout() and before retry() operator so that if an acceptable error response (for example 404) is
received the retry operator is not triggered. Then you can use the same operator again at the end of stream to catch any other exception and fallback.

In a real-world scenario, you would probably want to return some contextual information depending on the type of error, instead of always returning an empty
object. For example, you could add a reason field to the Order object to describe why it's been rejected. Was it because the book is unavailable in the catalog
or because of network problems? In the second case, you could inform the user that the order cannot be processed because it's momentarily unable to check the
book's availability. A better option would be to save the order in a pending state, queue the order submission request, and try it again later.

#### Producing and consuming message with Spring Cloud Stream
##### Implementing event consumers and the problem of idempotency
Dispatcher service produces message when orders are dispatched, the order service should be notified when that happens so that it can update the order status
in the database.
We will use a Consumer object, responsible for listening to the incoming message and updating the database entities. Consumer objects are functions with input
but no output. Functions and Consumers are naturally activated.

We need to configure Spring Cloud Stream in the application.yml file so that the `dispatchOrder-in-0` binding (inferred from the dispatchOrder function name) is
mapped to the `order-dispatched` exchange in RabbitMQ. Also, define `dispatchOrder` as the function that Spring Cloud Function should manage and integrate with
RabbitMQ. The consumers in Order Service will be part of the order-service consumer group, and Spring Cloud Stream will define a message channel between them
and an `order-dispatched.order-service` queue in RabbitMQ.

##### Implement event producers and the problem of atomicity
Suppliers are message sources. They produce messages when an event happens. A supplier should notify the interested parties whenever an order has been accepted.
Unlike functions and consumers, suppliers need to be activated. They act only upon invocation.

Spring Cloud Stream provides a few ways to define suppliers and cover different scenarios. In order-service, the event source is not a message broker, but a
REST endpoint. When a user sends a POST request to Order Service for purchasing a book, we want to publish an event signaling whether the order has been
accepted.

We can bridge the REST layer with the stream part of the application using a **StreamBridge** object that allows us to send data to a specific destination
imperatively.

Since the data source is a REST endpoint, there is no Supplier bean we can register with Spring Cloud Function, and therefore there is no trigger for the
framework to create the necessary bindings with RabbitMQ. There is no acceptOrder function! At startup time, Spring Cloud Stream will notice that StreamBridge
wants to publish messages via an acceptOrder-out-0 binding, and it will create one automatically. Similar to the bindings created from functions, we can
configure the destination name in RabbitMQ.

To ensure consistency in your system, persisting an order in the database and sending a message about it must be done atomically. Either both operations
succeed, or they both must fail. A simple yet effective way to ensure atomicity is by wrapping the two operations in a local transaction.

Spring Boot comes preconfigured with transaction management functionality and can handle transactional operations involving relational databases. However, the
channel established with RabbitMQ for the message producer is not transactional by default. To make the event-publishing operation join the existing
transaction, we need to enable RabbitMQ’s transactional support for the message producer in the `application.yml` file.
