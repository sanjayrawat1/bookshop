# dispatcher-service

Cloud native applications should be loosely coupled. The microservices expert Sam Newman identifies a few different types of coupling, including implementation,
deployment, and temporal coupling. Let's consider the Bookshop system we've been working on so far.
We can change the implementation of the applications without having to change the others. For example, we can re-implement Catalog Service using the reactive
paradigm without affecting Order Service. Using a service interface like a REST API, we hide the implementation details, improving loose coupling. All the
applications can be deployed independently. They're not coupled, reducing risks and increasing agility.

However, if you think about how the applications we built so far interact, you'll notice that they need other components of the system to be available. Order
Service needs Catalog Service to ensure that a user can order a book successfully. We know that failures happen all the time, so we adopted several strategies
to ensure resilience even in the face of adversity, or at least ensuring a graceful degradation of functionality. That's a consequence of temporal coupling:
Order Service and Catalog Service need to be available at the same time to fulfill the system requirements.

Event-driven architectures describe distributed systems that interact by producing and consuming events. The interaction is asynchronous, solving the problem
of temporal coupling.

### Event-driven architecture
An event is an occurrence. It's something relevant that happened in a system, like a state change, and there can be many sources of events. Here we will focus
on applications, but events can very well be happening in IoT devices, sensors, or networks. When an event occurs, interested parties can be notified. Event
notification is usually done through messages, which are data representations of events.

In an event-driven architecture, we identify event producers and event consumers. A producer is a component that detects the event and sends a notification.
A consumer is a component that is notified when a specific event occurs. Producers and consumers don't know each other and work independently. A producer sends
an event notification by publishing a message to a channel operated by an event broker that’s responsible for collecting and routing messages to consumers.
A consumer is notified by the broker when an event occurs and can act upon it.

Producers and consumers have minimal coupling when using a broker that takes the processing and distribution of events on itself. In particular, they are
temporally decoupled, because the interaction is asynchronous. Consumers can fetch and process messages at any time without affecting the producers whatsoever.

Event-driven architectures can be based on two main models:
* **Publisher/subscriber (pub/sub)**—This model is based on subscriptions. Producers publish events that are sent to all subscribers to be consumed. Events cannot
be replayed after being received, so new consumers joining will not be able to get the past events.
* **Event streaming**—In this model, events are written to a log. Producers publish events as they occur, and they are all stored in an ordered fashion.
Consumers don't subscribe to them, but they can read from any part of the event stream. In this model, events can be replayed. Clients can join at any time and
receive all the past events.

In a basic scenario, consumers receive and process events as they arrive. For specific use cases like pattern matching, they can also process a series of
events over a time window. In the event streaming model, consumers have the additional possibility of processing event streams. At the core of event-driven
architectures are platforms that can process and route events. For example, RabbitMQ is a common choice to use with the pub/sub model. Apache Kafka is a
powerful platform for event stream processing.

In the Bookshop system, we need to implement an event-driven solution to allow different applications to communicate with each other asynchronously while
reducing their coupling. These are the requirements:

* When an order is accepted:
    - Order Service should notify interested consumers of the event.
    - Dispatcher Service should execute some logic to dispatch the order.
* When an order is dispatched:
    - Dispatcher Service should notify consumers interested in such an event.
    - Order Service should update the order status in the database.

The requirements don't specify which applications Order Service should notify upon order creation. In our example, only the new Dispatcher Service application
will be interested in those events. Still, more applications might subscribe to the order creation events in the future. The beauty of this design is that you
can evolve a software system and add more applications without affecting the existing ones at all. For example, you could add a Mail Service that sends an
email to users whenever an order they made has been accepted, and Order Service wouldn't even be aware of it. This type of interaction should be asynchronous
and can be modeled with the pub/sub model.

Order Service and Dispatcher Service communicate asynchronously and indirectly by producing and consuming events that are collected and distributed by an event
broker (RabbitMQ).

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/async-communication-between-order-and-dispatcher-service.drawio.svg)

RabbitMQ will be the event-processing platform responsible for collecting, routing, and distributing messages to consumers.
In the Bookshop system, Order Service and Dispatcher Service communicate asynchronously based on events distributed by RabbitMQ.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/bookshop-system-event-driven-part-after-dispatcher-service-and-rabbitmq-introduction.drawio.svg)

When using an AMQP-based solution like RabbitMQ, the actors involved in the interaction can be categorized as follows:
* **Producer**—The entity sending messages (publisher)
* **Consumer**—The entity receiving messages (subscriber)
* **Message** broker—The middleware accepting messages from producers and routing them to consumers

In AMQP, a broker accepts messages from producers and routes them to consumers.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/interaction-between-amqp-actors.drawio.svg)

The AMQP messaging model is based on exchanges and queues. Producers send messages to an exchange. RabbitMQ computes which queues should receive a copy of the
message according to a given routing rule. Consumers read messages from a queue.
Producers publish messages to an exchange. Consumers subscribe to queues. Exchanges route messages to queues according to a routing algorithm.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/amqp-messaging-model.drawio.svg)

The protocol establishes that a message comprises attributes and a payload. AMQP defines some attributes, but you can add your own to pass the information
that's needed to route the message correctly. The payload must be of a binary type and has no constraints besides that.
An AMQP Message is composed of attributes and a payload.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/amqp-message.drawio.svg)

#### Functions with Spring Cloud Function
Is there any business feature that you cannot define in terms of suppliers, functions, and consumers? Most software requirements can be expressed with functions.
Why use functions in the first place? They are a simple, uniform, and portable programming model that is a perfect fit for event-driven architectures,
inherently based on these concepts. Spring Cloud Function promotes the implementation of business logic via functions based on the standard interfaces
introduced by Java 8: Supplier, Function, and Consumer.

* **Supplier**—A supplier is a function with only output, no input. It's also known as a producer, publisher, or source.
* **Function**—A function has both input and output. It's also known as a processor.
* **Consumer**—A consumer is a function with input but no output. It's also known as a subscriber or sink.

Whenever an order is accepted, Dispatcher Service should be responsible for packing and labeling the order, and for notifying interested parties once the order
has been dispatched.

The two actions to be performed as part of dispatching an order could be represented as functions:

* The **pack** function takes the identifier of an accepted order as input, packs the order, and returns the order identifier as output, ready to be labeled.
* The **label** function takes the identifier of a packed order as input, labels the order, and returns the order identifier as output, completing the dispatch.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/composition-of-pack-and-label-function.drawio.svg)

##### Composing and integrating functions
We need to compose the two functions, pack() and label(). These two steps to be executed in sequence: pack() first and label() after.

Java provides features to compose Function objects in sequence using the andThen() or compose() operators. The problem is that you can use them only when the
output type of the first function is the same as the second function’s input. Spring Cloud Function provides a solution to that problem and lets you compose
functions seamlessly through transparent type conversion, even between imperative and reactive functions.

Composing functions with Spring Cloud is as simple as defining a property in your application.yml

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/spring-cloud-function-composition.drawio.svg)

You can combine functions with different input and output types and mix imperative and reactive types as well. Spring Cloud Function will transparently handle
any type conversion.

In a serverless application like those meant to be deployed on a FaaS platform (such as AWS Lambda, Azure Functions, Google Cloud Functions, or Knative),
you would usually have one function defined per application. The cloud function definition can be mapped one-to-one to a function declared in your application,
or you can use the pipe (|) operator to compose functions together in a data flow. If you need to define multiple functions, you can use the semicolon (;)
character as the separator instead of the pipe (|).

Once you define the functions, the framework can expose them in different ways depending on your needs. For example, Spring Cloud Function can automatically
expose the functions defined in `spring.cloud.function.definition` as REST endpoints. Then you can directly package the application, deploy it on a FaaS
platform like Knative. That's what we'll do when we build serverless applications. Or you can use one of the adapters provided by the framework to package the
application and deploy it on AWS Lambda, Azure Functions, or Google Cloud Functions. Or you can combine it with Spring Cloud Stream and bind the function to
message channels in an event broker like RabbitMQ or Kafka.

#### Processing messages with Spring Cloud Stream
The principles that drive the Spring Cloud Function framework can also be found in Spring Cloud Stream. The idea is that you, as a developer, are responsible
for the business logic, while the framework handles infrastructural concerns like how to integrate a message broker.

Spring Cloud Stream is a framework for building scalable, event-driven, and streaming applications. It’s built on top of Spring Integration, which offers the
communication layer with message brokers. Spring Boot, which provides autoconfiguration for the middleware integration; and Spring Cloud Function, which
produces, processes, and consumes events. Spring Cloud Stream relies on the native features of each message broker, but it also provides an abstraction to
ensure a seamless experience independently of the underlying middleware. For example, features like consumer groups and partitions (native in Apache Kafka) are
not present in RabbitMQ, but you can still use them.

Spring Cloud Stream's feature is that you can drop a dependency in a project and get functions automatically bound to an external message broker. You don’t have
to change any code in the application, just the configuration in application.yml.

Spring Cloud Stream is based on a few essential concepts:
* **Destination Binder** - The component providing the integration with external messaging system like RabbitMQ or Kafka.
* **Destination Binding** - The bridge between the external messaging system entities, like queues and topics, and the application provided producers and
consumers.
* **Message** - The data structure used by the application producers and consumers to communicate with the destination binders, and therefore with the external
messaging system.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/spring-cloud-stream-application-model.drawio.svg)

In Spring Cloud Stream, a destination binder provides integration with external messaging systems and establishes message channels with them.

Spring Cloud Stream will auto-generate and configure the bindings to exchanges and queues in RabbitMQ.

Spring Cloud Stream provides application with a destination binder that integrates with an external messaging system. The binder is also responsible for
establishing communication channels between the application producers and consumers and the messaging system entities (exchanges and queues for RabbitMQ).
These communication channels are called destination bindings, and they are bridges between applications and brokers.

A destination binding can be either an input channel or an output channel. By default, Spring Cloud Stream maps each binding (both input and output) to an
exchange in RabbitMQ (a topic exchange, to be more precise). Furthermore, for each input binding, it binds a queue to the related exchange. That’s the queue
from which consumers receive and process events.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/spring-cloud-stream-application-terminology.drawio.svg "In Spring Cloud Stream,
bindings establish message channels between applications and message brokers.")

##### Understanding Destination Binding
Destination bindings are an abstraction representing a bridge between application and broker. When using functional programming model, Spring Cloud Stream
generates an input binding for each function accepting input data, and an output binding for each function returning output data.
Each binding assigned a logical name following below convention:
* Input binding: **<functionName>** + **-in-** + **<index>**
* Output binding: **<functionName>** + **-out-** + **<index>**

Unless you use partitions (for example, with Kafka), the <index> part of the name will always be 0. The <functionName> is computed from the value of the
`spring.cloud.function.definition` property. In case of a single function, there is a one-to-one mapping. For example, if in Dispatcher Service we only had one
function called **dispatch**, the related binding would be named **dispatch-in-0** and **dispatch-out-0**. We actually used a composed function (pack|label),
so the binding names are generated by combining the names of all the functions involved in the composition:
* Input binding: **packlabel-in-0**
* Output binding: **packlabel-out-0**

These names are only relevant for configuring the bindings themselves in the application. They're like unique identifiers that let you reference a specific
binding and apply custom configuration. Notice that these names exist only in Spring Cloud Stream—they're logical names. RabbitMQ doesn't know about them.

##### Consumer groups ensure that each message is received and processed by only one consumer within the same group.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/consumer-group-with-spring-cloud-stream-and-rabbitmq.drawio.svg)

#### Exploring Exchanges and Queues in RabbitMQ
First, start the RabbitMQ container, navigate to the folder in bookshop-deployment where you keep docker-compose.yml file and run the following command:

`$ docker-compose up -d bookshop-rabbitmq`

Then run the dispatcher-service application:

`$ ./gradlew bootRun`

Open browser and navigate to http://localhost:15672. The credentials are the same that we defined in docker compose (user/password). Then go to the **Exchange**
section. You will see a list of default exchanges provided by RabbitMQ and the two exchanges generated by our application: **order-accepted** and
**order-dispatched**. They are mapped to the **packlabel-in-0** and **packlabel-out-0** bindings respectively by Spring Cloud Stream.
The exchanges are durable (denoted by the D icon in the management console), meaning that they will survive a broker restart.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/rabbitmq-exchanges.png "Spring Cloud Stream maps the two destination binding to two
exchanges in RabbitMQ.")

In Dispatcher Service we configured a **packlabel-in-0** binding and a consumer group. That’s the only input channel for the application, so it should result
in a single queue. You can see a durable **order-accepted.dispatcher-service** queue in the Queues section.

![](https://github.com/sanjayrawat1/bookshop/blob/main/dispatcher-service/diagrams/rabbitmq-queues.png "Spring Cloud Stream maps each input binding to a queue, named
according to the configured consumer group.")

No queue has been created for the **packlabel-out-0** binding because no consumer subscribed to it. Later you’ll see that a queue will be created after
configuring Order Service to listen to it.

#### Integration test with a test binder
The framework provides a binder specifically for implementing integration test focusing on the business logic rather than the middleware. The test binder
provided by Spring Cloud Stream is mean to verify the correct configuration and integration with a technology-agnostic destination binder.
