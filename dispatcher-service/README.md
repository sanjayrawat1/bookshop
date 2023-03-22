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

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/async-communication-between-order-and-dispatcher-service.drawio.svg)

RabbitMQ will be the event-processing platform responsible for collecting, routing, and distributing messages to consumers.
In the Bookshop system, Order Service and Dispatcher Service communicate asynchronously based on events distributed by RabbitMQ.

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/bookshop-system-event-driven-part-after-dispatcher-service-and-rabbitmq-introduction.drawio.svg)

When using an AMQP-based solution like RabbitMQ, the actors involved in the interaction can be categorized as follows:
* **Producer**—The entity sending messages (publisher)
* **Consumer**—The entity receiving messages (subscriber)
* **Message** broker—The middleware accepting messages from producers and routing them to consumers

In AMQP, a broker accepts messages from producers and routes them to consumers.

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/interaction-between-amqp-actors.drawio.svg)

The AMQP messaging model is based on exchanges and queues. Producers send messages to an exchange. RabbitMQ computes which queues should receive a copy of the
message according to a given routing rule. Consumers read messages from a queue.
Producers publish messages to an exchange. Consumers subscribe to queues. Exchanges route messages to queues according to a routing algorithm.

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/amqp-messaging-model.drawio.svg)

The protocol establishes that a message comprises attributes and a payload. AMQP defines some attributes, but you can add your own to pass the information
that's needed to route the message correctly. The payload must be of a binary type and has no constraints besides that.
An AMQP Message is composed of attributes and a payload.

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/amqp-message.drawio.svg)

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

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/composition-of-pack-and-label-function.drawio.svg)

##### Composing and integrating functions
We need to compose the two functions, pack() and label(). These two steps to be executed in sequence: pack() first and label() after.

Java provides features to compose Function objects in sequence using the andThen() or compose() operators. The problem is that you can use them only when the
output type of the first function is the same as the second function’s input. Spring Cloud Function provides a solution to that problem and lets you compose
functions seamlessly through transparent type conversion, even between imperative and reactive functions.

Composing functions with Spring Cloud is as simple as defining a property in your application.yml

![](https://github.com/sanjayrawat1/bookshop/dispatcher-service/diagrams/spring-cloud-function-composition.drawio.svg)

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
