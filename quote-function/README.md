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

##### Deploying applications with the Knative manifests
Kubernetes is an extensible system. Besides using built-in objects like Deployments and Pods, we can define our own objects via Custom Resource Definitions
(CRDs). That is the strategy used by many tools built on top of Kubernetes, including Knative.

One of the benefits of using Knative is a better developer experience and the possibility to declare the desired state for our applications in a more
straightforward and less verbose way. Rather than dealing with Deployments, Services, and Ingresses, we can work with a single type of resource: the Knative Service.

Like any other Kubernetes resource, you can apply a Knative Service manifest to a cluster with kubectl apply -f <manifest-file> or through an automated flow
like Argo CD. Using the Kubernetes CLI, run the following command to deploy the quote function from the Knative Service manifest:

```shell
$ kubectl apply -f knative/kservice.yml
```

To get the information about all the created Knative Services and their URL, run the following command:

```shell
$ kubectl get ksvc
```

To verify that the application is correctly deployed by sending HTTP request to the root endpoint, run following commands:

```shell
$ minikube tunnel --profile knative
```

```shell
$  http http://quote-function.default.127.0.0.1.sslip.io
```

Knative provides an abstraction on top of Kubernetes. However, it still runs Deployments, ReplicaSets, Pods, Services, and Ingresses under the hood. You can
configure Quote Function through ConfigMaps and Secrets.

If you wait for 30 seconds and then check for the running Pods in your local Kubernetes cluster, you'll see there are none, because Knative scaled the
application to zero due to inactivity:

```shell
$ kubectl get pod
```
`No resources found in default namespace.`

Now try sending a new request to the application on http http://quote-function.default.127.0.0.1.sslip.io. Knative will immediately spin up a new Pod for Quote
Function to answer the request. Run below command and this time you'll the output with pod information:

```shell
$ kubectl get pod
```

When you're done testing the application, remove it with:

```shell
$ kubectl delete -f knative/kservice.yml.
```

Finally, stop and delete the local cluster with the following command:

```shell
$ minikube stop --profile knative
$ minikube delete --profile knative
```

The Knative Service resource represents an application service in its entirety. Thanks to this abstraction, we no longer need to deal directly with Deployments,
Services, and Ingresses. Knative takes care of all that. It creates and manages them under the hood while freeing us from dealing with those lower-level
resources provided by Kubernetes. By default, Knative can even expose an application outside the cluster without the need to configure an Ingress resource,
providing you directly with a URL to call the application.

Thanks to its features focused on developer experience and productivity, Knative can be used to run and manage any kind of workload on Kubernetes, limiting its
scale-to-zero functionality only to the applications that provide support for it (for example, using Spring Native). We could easily run the entire Bookshop
system on Knative. We could use the **autoscaling.knative.dev/minScale** annotation to mark the applications we don't want to be scaled to zero:

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: catalog-service
  annotations:
    # Ensures this Service is never scaled to zero
    autoscaling.knative.dev/minScale: "1"
```

Knative offers such a great developer experience that it’s becoming the de facto abstraction when deploying workloads on Kubernetes, not only for serverless
but also for more standard containerized applications.

Another great feature offered by Knative is an intuitive and developer-friendly option for adopting deployment strategies like blue/green deployments, canary
deployments, or A/B deployments, all via the same Knative Service resource. Implementing those strategies in plain Kubernetes would require a lot of manual
work. Instead, Knative supports them out of the box.

#### Summary
* By replacing a standard OpenJDK distribution with GraalVM as the runtime environment for your Java applications, you can increase their performance and
efficiency, thanks to a new optimized technology for performing JIT compilation (the GraalVM compiler).
* What makes GraalVM so innovative and popular in the serverless context is the Native Image mode.
* Rather than compiling your Java code into bytecode and relying on a JVM to interpret it and convert it to machine code at runtime, GraalVM offers a new
technology (the Native Image builder) to compile Java applications directly into machine code, obtaining a native executable or native image.
* Java applications compiled as native images have faster startup times, optimized memory consumption, and instant peak performance, unlike the JVM options.
* The main goal of Spring Native is to make it possible to compile any Spring application into a native executable using GraalVM without any code changes.
* Spring Native provides an AOT infrastructure (invoked from a dedicated Gradle/ Maven plugin) for contributing all the required configurations for GraalVM to AOT-compile Spring classes.
* There are two ways to compile your Spring Boot applications into native executables. The first option produces an OS-specific executable and runs the
application directly on a machine. The second option relies on Buildpacks to containerize the native executable and run it on a container runtime like Docker.
* Serverless is a further abstraction layer on top of virtual machines and containers, which moves even more responsibility from product teams to the platform.
* Following the serverless computing model, developers focus on implementing the business logic for their applications.
* Serverless applications are triggered by an incoming request or a specific event. We call such applications request-driven or event-driven.
* Applications using Spring Cloud Function can be deployed in a few different ways.
* When Spring Native is included, you can also compile applications to native images and run them on servers or container runtimes. Thanks to instant startup
time and reduced memory consumption, you can seamlessly deploy such applications on Knative.
* Knative is a Kubernetes-based platform to deploy and manage modern serverless workloads (https://knative.dev). You can use it to deploy standard containerized
workloads and event-driven applications.
* The Knative project offers a superior user experience to developers and higher abstractions that make it simpler to deploy applications on Kubernetes.
* Knative offers such a great developer experience that it's becoming the de facto abstraction when deploying workloads on Kubernetes, not only for serverless
but also for more standard containerized applications.
