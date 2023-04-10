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

When only one function is registered as a bean, Spring Cloud Function will automatically expose it through the root endpoint. In the case of multiple functions,
you can choose the function through the **spring.cloud.function.definition** configuration property. For example, we could expose the allQuotes function through
the root endpoint.

Since the allQuotes function is a Supplier returning a Flux of Quote, you can leverage the streaming capabilities of Project Reactor and ask the application to
return the quotes as they become available. That is done automatically when the **Accept:text/event-stream** header is used.
e.g, curl -H 'Accept:text/event-stream' localhost:9102. When using the httpie utility, you'll also need to use the --stream argument to enable data streaming:

```shell
$ http :9102 Accept:text/event-stream --stream
```

When functions are exposed as HTTP endpoints, you can use the comma (,) character to compose functions on the fly. For example, you could combine the genreQuote
function with logQuote as follows:

```shell
$ echo 'FANTASY' | http :9102/genreQuote,logQuote
```

Since logQuote is a consumer, the HTTP response has a 202 status with no body. If you check the application logs, you'll see that the random quote by genre has
been printed out instead.

Spring Cloud Function integrates with several communication channels. The framework also supports RSocket, which is a binary reactive protocol, and CloudEvents,
a specification standardizing the format and distribution of events in cloud architectures (https://cloudevents.io).

CloudEvents can be consumed over HTTP, messaging channels like AMPQ (RabbitMQ), and RSocket. They ensure a standard way of describing events, thus making them
portable across a wide variety of technologies, including applications, messaging systems, build tools, and platforms.

Since Quote Function is already configured to expose functions as HTTP endpoints, you can make it consume CloudEvents without changing any code. Send an HTTP
request with the additional headers defined by the CloudEvents specification:

```shell
$ echo 'FANTASY' | http :9102/genreQuote ce-specversion:1.0 ce-type:quote ce-id:394
```

##### Explanation of above command parameters
| command breakdown  | description                           |
|--------------------|---------------------------------------|
| ce-specversion:1.0 | The CloudEvents specification version |
| ce-type:quote      | The type of event (domain-specific)   |
| ce-id:394          | The ID of the event                   |

#### Deploying serverless applications on the cloud
Applications using Spring Cloud Function can be deployed in a few different ways. You can package them as JAR artifacts or container images and deploy them on
servers or container runtimes like Docker or Kubernetes, respectively.
When Spring Native is included, you also have the option to compile them to native images and run them on servers or container runtimes. Instant startup time
and reduced memory consumption, you can also seamlessly deploy such applications on serverless platforms.

Spring Cloud Function also supports deploying applications on vendor-specific FaaS platforms like AWS Lambda, Azure Functions, and Google Cloud Functions. Once
you choose a platform, you can add the related adapter provided by the framework to accomplish the integration.

When you use one of those adapters, you must choose which function to integrate with the platform. If there's only one function registered as a bean, that's
the one used. If there are more (like in Quote Function), you need to use the **spring.cloud.function.definition** property to declare which function the FaaS
platform will manage.
