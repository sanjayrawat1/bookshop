# quote-function

### Serverless applications with Spring Cloud Function
Serverless is a further abstraction layer on top of virtual machines and containers, moving even more responsibilities from product teams to the platform.
Following the serverless computing model, developers focus on implementing the business logic for their applications. Using an orchestrator like Kubernetes
still requires infrastructure provisioning, capacity planning, and scaling. In contrast, a serverless platform takes care of setting up the underlying
infrastructure needed by the applications to run, including virtual machines, containers, and dynamic scaling.

Serverless applications typically only run when there is an event to handle, such as an HTTP request (request-driven) or a message (event-driven). The event
can be external or produced by another function. For example, whenever a message is added to a queue, a function might be triggered, process the message, and
then exit the execution. When there is nothing to process, the platform shuts down all the resources involved with the function, so you can really pay for
your actual usage. In the other cloud native topologies like CaaS or PaaS, there is always a server involved running 24/7.

Compared to traditional systems, you get the advantage of dynamic scalability, reducing the number of resources provisioned at any given time. Still, there is
always something up and running that has a cost. In the serverless model, however, resources are provisioned only when necessary. If there is nothing to
process, everything is shut down. That's what we call **scaling to zero**, and it's one of the main features offered by serverless platforms.

A consequence of scaling applications to zero is that when eventually there’s a request to handle, a new application instance is started, and it must be ready
to process the request very quickly. Standard JVM applications are not suitable for serverless applications, since it’s hard to achieve a startup time lower
than a few seconds. That's why GraalVM native images became popular. Their instant startup time and reduced memory consumption make them perfect for the
serverless model. The instant startup time is required for scaling. The reduced memory consumption helps reduce costs, which is one of the goals of serverless
and cloud native in general.

#### Building serverless applications with Spring Cloud Function
Spring Cloud Function is very flexible. It integrates transparently with external messaging systems like RabbitMQ and Kafka, a handy feature for building
serverless applications that are triggered by messages.

The application should expose similar functionality to Quote Service:

* Returning all the quotes can be expressed as a Supplier, since it takes no input.
* Returning a random quote can also be expressed as a Supplier, since it takes no input.
* Returning a random quote for a given genre can be expressed as a Function, since it has both input and output.
* Logging a quote to standard output can be expressed as a Consumer, since it has input but no output.

Spring Cloud Function will automatically expose all the registered functions as HTTP endpoints when the Spring web dependencies are on the classpath. Each
endpoint uses the same name as the function. In general, suppliers can be invoked through GET requests and functions and consumers as POST requests.

Run the Quote Function application (./gradlew bootRun) and test the two suppliers by sending GET requests:

```shell
$ http :9102/allQuotes
$ http :9102/randomQuote
```

To get a random quote by genre, you need to provide a genre string in the body of a POST request:

```shell
$ echo 'FANTASY' | http :9102/genreQuote
```
