# catalog-service

### Getting Started

#### Setup Docker in your local environment
[Install Docker and Setup](https://docs.docker.com/desktop/install/mac-install/)

#### Useful Docker CLI commands to manage images and containers.
| Docker CLI command  | What it does                                        |
|---------------------|-----------------------------------------------------|
| docker images       | Shows all images.                                   |
| docker ps           | Shows the running containers.                       |
| docker ps -a        | Shows all containers created, started, and stopped. |
| docker run <image>  | Run a container from the given image.               |
| docker start <name> | Starts an existing container.                       |
| docker stop <name>  | Stops a running container.                          |
| docker logs <name>  | Shows the logs from a given container.              |
| docker rm <name>    | Removes a stopped container.                        |
| docker rmi <name>   | Removes an image.                                   |

#### Run application tests
`./gradlew test`

#### Run application
`./gradlew bootRun`

#### Running spring application as a container.
Package your application as a container image, using Cloud Native Buildpacks under the hood. Use below command:
`./gradlew bootBuildImage`

#### You can run the following command to get the details of the newly created image:
`docker images catalog-service:0.0.1-SNAPSHOT`

#### Run the container image
`docker run --rm --name catalog-service -p 8080:8080 catalog-service:0.0.1-SNAPSHOT`

##### Explanation of above command
| command breakdown              | description                                             |
|--------------------------------|---------------------------------------------------------|
| docker run                     | runs a container from an image                          |
| --rm                           | remove the container after its execution completes      |
| --name catalog-service         | name of the container                                   |
| -p 8080:8080                   | exposes service outside the container through port 8080 |
| catalog-service:0.0.1-SNAPSHOT | name and version of the image to run                    |


#### Managing containers with Kubernetes
Install minikube and setup kubernetes using below commands:

`brew install minikube`

`minikube start --driver=docker`

`minikube config set driver docker`

`brew install kubectl`

`kubectl get nodes`

`minikube stop`

`minikube start`

#### Useful Kubernetes CLI commands to manage Pods, Deployments, and Services.
| Kubernetes CLI command                                        | What it does                                                    |
|---------------------------------------------------------------|-----------------------------------------------------------------|
| kubectl get deployment                                        | Show all Deployments.                                           |
| kubectl get pod                                               | Show all Pods.                                                  |
| kubectl get svc                                               | Show all Services.                                              |
| kubectl logs <pod_id>                                         | Show the logs for the given Pod.                                |
| kubectl delete deployment <name>                              | Delete the given Deployment.                                    |
| kubectl delete pod <name>                                     | Delete the given Pod.                                           |
| kubectl delete svc <service>                                  | Delete the given Service.                                       |
| kubectl port-forward svc <service> <host-port>:<cluster-port> | Forwards traffic from your local machine to within the cluster. |

#### Running a Spring application on Kubernetes
First you need to tell Kubernetes to deploy Catalog Service from a container image.
By default, minikube uses the Docker Hub registry to pull images, and it doesn't have access to your local ones.
Therefore, it will not find the image you built for the Catalog Service application.
But don’t worry: you can manually import it into your local cluster using below command:

`minikube image load catalog-service:0.0.1-SNAPSHOT`

#### The Kubernetes command to create a Deployment from a container image. Kubernetes will take care of creating Pods for the application
`kubectl create deployment catalog-service --image=catalog-service:0.0.1-SNAPSHOT`

##### Explanation of above command
| command breakdown                      | description                      |
|----------------------------------------|----------------------------------|
| kubectl create                         | create a kubernetes resource     |
| deployment                             | type of resource to create       |
| catalog-service                        | name of the deployment           |
| --image=catalog-service:0.0.1-SNAPSHOT | name and version of image to run |

#### Verify the creation of Deployment object
`kubectl get deployment`
Behind the scenes, k8s created a Pod for the application defined in the Deployment resource, verify the creation of the Pod object
`kubectl get pod`

#### The application running in k8s are not accessible, expose catalog-service to the cluster through a Service resource
#### The Kubernetes command to expose a Deployment as a Service. The Catalog Service application will be exposed to the cluster network through port 8080
`kubectl expose deployment catalog-service --name=catalog-service --port=8080`

##### Explanation of above command
| command breakdown      | description                                   |
|------------------------|-----------------------------------------------|
| kubectl expose         | expose a kubernetes resource                  |
| deployment             | type of resource to expose                    |
| catalog-service        | name of the deployment to expose              |
| --name=catalog-service | name of the service                           |
| --port=8080            | port number from which the service is exposed |

The Service object exposes the application to other components inside the cluster. Verify that it's been created correctly
`kubectl get service catalog-service`

#### Now forward the traffic from a local port on your computer (ex - 8000) to the port exposed by the Service inside the cluster (8080).
`kubectl port-forward service/catalog-service 8000:8080`

##### Explanation of above command
| command breakdown       | description                 |
|-------------------------|-----------------------------|
| kubectl port-forward    | command for port forwarding |
| service/catalog-service | which resource to expose    |
| 8000                    | the port on your localhost  |
| 8080                    | the port of the Service     |

Now open the browser and navigate to [http://localhost:8000/](http://localhost:8000/)



### Deployment pipeline: Build and test

Continuous delivery is a holistic approach for quickly, reliably and safely delivering high quality software.
The primary pattern for adopting such an approach is deployment pipeline, which does from code commit to releasable software.
It should be automated as much as possible, and it should represent the only path to production.

Below are few key stages in a deployment pipeline:
1. Commit stage
2. Acceptance stage
3. Production stage

##### Commit Stage:
After a developer commits new code to the mainline, this stage goes through build, unit tests, integration tests, static code analysis and packaging.
At the end of this stage, an executable application artifact is published to an artifact repository. This stage supports the continuous integration practice.
It's supposed to be fast, possible under five minutes, to provide developers with fast feedback about their changes and allow them to move on to the next task.

##### Acceptance Stage:
The publication of a new release candidate to the artifact repository triggers this stage, which consists of deploying the application to production
like environments and running additional tests to increase the confidence about its release.
Examples of tests included in this stage are functional acceptance and tests and non-functional acceptance tests, such as performance tests, security tests,
and compliance tests. If necessary, this stage can also include manual tasks like exploratory and usability tests.
At the end of this stage, the release candidate is ready to be deployed to production at any time. If we are still not confident about it,
this stage is missing some test.

##### Production Stage:
After a release candidate has gone through the commit and acceptance stage, we are confident enough to deploy it to production.
This stage is triggered manually or automatically depending on the organization practices.
The new release candidate is deployed to a production environment using the same deployment scripts employed (and tested) in the acceptance stage.

### Using a configuration server with Spring Cloud Config Client.
A Spring Boot application can be configured through a config server using the Spring Cloud Config Client library.

#### Refreshing configuration at runtime

What happens when new changes are pushed to the Git repository that's backing the Config Service? For a standard Spring Boot application,
you would have to restart it when you change a property (either in properties file or an environment variable).
However, Spring Cloud Config gives the possibility to refresh configuration in client applications at runtime. Whenever a new changes pushed to the
configuration repository, you can signal all the application integrated with the config server, and they will reload parts affected by the configuration change.

After committing and pushing the new configuration changes to the remote Git repository, you can send a POST request to a client application through a specific
endpoint that will trigger a **RefreshScopeRefreshedEvent** inside the application context. You can rely on the Spring Boot Actuator project to expose
the refresh endpoint by adding Actuator dependency.

The Spring Boot Actuator library configures an /management/refresh endpoint that triggers a refresh event. By default, the endpoint is not exposed,
so you have to enable it explicitly in the application.yml
The refresh event, **RefreshScopeRefreshedEvent**, will have no effect if there is no component listening. You can use the **@RefreshScope** annotation on
any bean you’d like to be reloaded whenever a refresh is triggered. Since you defined your custom properties through a **@ConfigurationProperties** bean,
it is already listening to **RefreshScopeRefreshedEvent** by default, so you don’t need to make any changes to your code. When a refresh is triggered,
the **BookshopProperties** bean will be reloaded with the latest configuration available.

![After changing the configuration in the Git repository backing the Config Service, a signal is sent to Catalog Service to refresh the parts of the application using the configuration.](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/hot-reload-config-data.drawio.svg "Refreshing configuration at runtime")

### Running a PostgreSQL Database
Run PostgreSQL as a Docker container

`$ docker run -d --name bookshop-postgres -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=bookshop_catalog -p 5432:5432 postgres:14.2`

### Container commands

| Docker command                  | Description      |
|---------------------------------|------------------|
| docker stop bookshop-postgres   | Stop container.  |
| docker start bookshop-postgres  | Start container. |
| docker remove bookshop-postgres | Remove container |

### Database commands
Start an interactive PSQL console:
`$ docker exec -it bookshop-postgres psql -U user -d bookshop_catalog`

| PSQL command              | Description                                           |
|---------------------------|-------------------------------------------------------|
| \list                     | List all databases.                                   |
| \connect bookshop_catalog | Connect to specific database (e.g. bookshop_catalog). |
| \dt                       | List all tables.                                      |
| \d book                   | Show the `book` table schema.                         |
| \quit                     | Quit interactive PSQL console                         |

### Working with container images on Docker

The Docker Engine has a client/server architecture. The Docker CLI is the client you use to interact with the Docker Server.
The latter is responsible for managing all Docker resources (i.e., images, containers, and networks) through the docker daemon.
The server can also interact with container registry to upload and download images.

##### Container Images
Container images are lightweight executable packages that include everything needed to run the application that's inside.
Container images are the product of executing an ordered sequence of instructions, each resulting in a layer. Each image is made up of several layers,
and each layer represents a modification produced by the corresponding instruction. The final artifact, an image, can be run as a container.
Images can be created from scratch or starting from a base image.

Container images are composed of an ordered sequence of read-only layers. The first one represents the base image, the others represent modification
applied on top of it. Once read-only layers are applied, you can't modify them anymore. If you need to change something, you can do so by applying a new layer
on top of it. Changes applied to the upper layers will not affect the lower ones. This approach is call _copy-on-write_
Running containers have an extra layer on top of the image layers. That is the only writable layer, but remember that it’s volatile.

#### Creating images with Dockerfiles
Create a Dockerfile with instructions, and navigate where Dockerfile is located and run following command

`$ docker build -t my-image:1.0.0 .`

You can check the details of newly created image using the `docker images` command.

A container image can be run with the `docker run` command, which starts a container and executes the process described in the Dockerfile as the entry point:

`$ docker run --rm my-java-image:1.0.0`

#### Publishing images to GitHub container registry
A container registry is to images what a Maven repository is to Java libraries. By default, a Docker installation is configured to use the container registry
provided by the Docker company (Docker Hub), which hosts images for many popular open source projects, like PostgreSQL, RabbitMQ, and Redis.
We’ll keep using it to pull images for third parties. For this project I chose to rely on the GitHub Container Registry.

Create a Personal Access Token (PAT) granting write access to the GitHub Container Registry, authenticate with GitHub container registry using below command
when asked insert username and password (PAT).

`$ docker login ghcr.io`

Container images follow common naming conventions, which are adopted by OCI-compliant container registries:

`<container_registry>/<namespace>/<name>[:<tag>]`
1. container_registry: The hostname for the container registry. When using GitHub Container Registry, the hostname is ghcr.io and must be explicit.
2. namespace: The namespace will be your Docker/GitHub username written all in lowercase.
3. name and tag: The image name represents the repository (or package) that contains all the versions of your image.
It’s optionally followed by a tag for selecting a specific version. If no tag is defined, the latest tag will be used by default.

Since you already built an image with name my-image:1.0.0, now you have to assign it a fully qualified name before publishing it to a container registry.
You can do so with the docker tag command:

`$ docker tag my-image:1.0.0 ghcr.io/<your-github-username>/my-image:1.0.0`

Then you can finally push it to GitHub container registry:

`$ docker push ghcr.io/<your-github-username>/my-image:1.0.0`

Go to your GitHub account, navigate to your profile page, and enter the Packages section. You should see a new my-image entry.

### Packaging Spring Boot application as container images
Packaging a Spring Boot application as a container image means that the application will run in an isolated context, including computational resources
and network. Two main questions may arise from this isolation:
1. How can you reach the application through the network?
2. How can you make it interact with other containers?

By default, containers join an isolated network inside the Docker host. If you want to access any container from your local network, you must explicitly
configure the port mapping. For example, when you ran the Catalog Service application, you specified the mapping as an argument to the docker run command:
-p 8080:8080 (where the first is the external port and the second is the container port).

Docker has a built-in DNS server that can enable containers in the same network to find each other using the container name rather than a hostname or
an IP address. For example, Catalog Service will be able to call the PostgreSQL server through the URL jdbc:postgresql://bookshop-postgres:5432,
where bookshop-postgres is the container name.

The Catalog Service container can directly interact with the PostgreSQL container because they are both on the same Docker network.

Let’s create a network inside which Catalog Service and PostgreSQL can talk to each other using the container name instead of an IP address or a hostname.

`$ docker network create catalog-network`

Verify that the network has been successfully created:

`$ docker network ls`

Now start PostgreSQL container, specifying that it should be part of catalog-network. Using the **--net** argument ensures the container will join the
specified network and rely on the Docker built-in DNS server:

`$ docker run -d --name bookshop-postgres --net catalog-network -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=bookshop_catalog -p 5432:5432 postgres:14.2`

Now first build the JAR artifacts:

`$ ./gradlew clean bootJar`

Build the container image:

`$ docker build -t catalog-service .`

Run the Docker container image using port forwarding to 9001 and using the Docker build-in DNS server to connect to the catalog-network:

`$ docker run -d --name catalog-service --net catalog-network -p 9001:9001 -e SPRING_DATASOURCE_URL=jdbc:postgresql://bookshop-postgres:5432/bookshop_catalog -e SPRING_PROFILES_ACTIVE=test-data catalog-service`

#### Secure Dockerfile
You should be aware that containers run using the root user by default, potentially letting them get root access to the Docker host.
You can mitigate the risk by creating a non-privileged user and using it to run the entry-point process defined in the Dockerfile,
following the principle of the least privilege. You can improve it by adding new steps to create a new non-root user that will run the application.
creates a "spring" user

`RUN useradd spring`

configures "spring" as the current user

`USER spring`

### Dockerfiles or Buildpacks
Dockerfiles are very powerful, and they give you complete fine-grained control over the result. However, they require extra care and maintenance and can
lead to several challenges in your value stream.
Cloud Native Buildpacks provide a different approach, focusing on consistency, security, performance and governance. As a developer, you get a tool that
automatically builds a production-ready OCI image from your application source code without having to write a Dockerfile.

#### Containerizing Spring Boot with Cloud Native Buildpacks
Cloud Native Buildpacks (https://buildpacks.io) is a project hosted by the CNCF to transform your application source code into images that can run on any cloud.
It’s a mature project, and since Spring Boot 2.3, it has been integrated natively in the Spring Boot Plugin for both Gradle and Maven,
so you’re not required to install the dedicated Buildpacks CLI (pack).

These are some of its feature:
1. It auto-detects the type of application and packages it without requiring a Dockerfile.
2. It supports multiple languages and platforms.
3. It’s highly performant through caching and layering.
4. It guarantees reproducible builds.
5. It relies on best practices in terms of security.
6. It produces production-grade images.
7. It supports building native images using GraalVM.

Build image using buildpacks by running below commands:

`$ ./gradle bootBuildImage`

You can also configure the Spring Boot plugin to publish the image directly to a container registry. To do so, you first need to add configuration for
authenticating with the specific container registry in the build.gradle file.

Finally, you can build and publish the image by running the bootBuildImage task. With the --imageName argument, you can define a fully qualified image name
as container registries require. With the --publishImage argument, you can instruct the Spring Boot plugin to push the image to the container registry directly.
Also, remember to pass values for the container registry via the Gradle properties:

`$ ./gradlew bootBuildImage --imageName ghcr.io/<your_github_username>/catalog-service --publishImage -PregistryUrl=ghcr.io -PregistryUsername=<your_github_username> -PregistryToken=<your_github_token>`

Once the command completes successfully, go to your GitHub account, navigate to your profile page, and enter the Packages section. You should see a new
catalog-service entry (by default, packages hosting container images are private). If you click on the catalog-service entry,
you'll find the **ghcr.io/<your_github_username>/catalog-service:latest** image you just published.

### Managing Spring Boot containers with Docker Compose

Finally, you can remove the network you used to make catalog service communicate with postgres. You won't need it anymore, as you will use docker-compose to
manage the container life cycle. If you don't add any network configuration, docker compose will automatically create one for you and make all the containers
in the file join it. That means they can interact with each other through their container names, relying on docker's built-in DNS server.

`$ docker network rm catalog-network`

It’s good practice to gather all deployment-related scripts in a separate codebase and, possibly, in a separate repository.
We will use **bookshop-deployment** repository on GitHub. It'll contain all the Docker and Kubernetes scripts needed to run the application composing the
bookshop system.


### Configure IDE to run in debug mode

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/config-to-debug-containerized-java-app-from-intellij-idea.png "Configuration to debug a containerized Java application from IntelliJ IDEA")

### Deployment Pipeline: Package and publish

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/deployment-pipeline-package-and-publish-commit-stage.drawio.svg)

Once a release candidate is published, several parties can download it and use it, including the next stages in the deployment pipeline.
How can we ensure that all interested parties use a legitimate container image from the Bookshop project, and not one that has been compromised?
We can achieve that by signing the image. After the publishing step, we could add a new step for signing the release candidate. For example, we could use
Sigstore (www.sigstore.dev), a non-profit service that provides open source tools for signing, verifying, and protecting software integrity.

## Moving from Docker to Kubernetes
With docker compose, you can manage the deployment of several containers at once, including the configuration of networks and storage. Tha is extremely
powerful, but it's limited to one machine.

Using Docker CLI and Docker Compose, the interaction happens with a single Docker Daemon that manages docker resources on a single machine, called the docker
host. Furthermore, it's not possible to scale a container. All of this is limiting when you need cloud native properties like scalability and resilience
for your system.

Docker clients interact with a Docker daemon that can only manage resources on the machine where it is installed, called the Docker host.
Applications are deployed as containers to the Docker host.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/application-container-management-in-docker.drawio.svg)

Kubernetes' clients interact with the Control Plane, which manages containerized applications in a cluster consisting of one or more nodes.
Applications are deployed as Pods to the nodes of a cluster.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/application-container-management-in-kubernetes.drawio.svg)

With Docker, we deploy containers to an individual machine. With Kubernetes, we deploy containers to a cluster of machines, enabling scalability and resilience.

##### Main component of k8s infrastructure:
* **Cluster**: A set of nodes running containerized applications. It hosts the Control Plane and comprises one or more worker nodes.
* **Control Plane**: The cluster component exposing the API and interfaces to define, deploy and manage the life cycle of Pods. It provides features like
orchestration, cluster management, scheduling, scaling, self-healing and health monitoring.
* **Worker Nodes**: Physical or virtual machines providing capacity such as CPU, memory, network and storage so that containers can run and connect to a network.
* **Pods**: The smallest deployable unit wrapping an application container.

#### Working with local k8s cluster
We will use `minikube` CLI to create clusters on local environment. We will run minikube on top of Docker, remember to start the Docker Engine first.
We will not use the default cluster, instead, we will create a new custom one for working with Bookshop. With minikube you can create and control multiple
clusters identified via **profiles**.

Let's create a new k8s cluster named _bookshop_ on top of docker by running below command:

`$ minikube start --cpus 2 --memory 4g --driver docker --profile bookshop`

Get list of all the nodes in the cluster:

`$ kubectl get nodes`

The cluster we have just created is composed of single node, which hosts the Control Plane and acts as a worker node for deploying containerized workloads.

List all the available contexts with which you can interact (local or remote):

`$ kubectl config get-contexts`

Verify which is the current context:

`$ kubectl config current-context`

Change current context by running below command:

`$ kubectl config use-context bookshop`

Commands to start, stop and delete k8s cluster:

`$ kubectl stop --profile bookshop`

`$ kubectl start --profile bookshop`

`$ kubectl delete --profile bookshop`

#### Creating a Deployment for a Spring Boot application
In Kubernetes, the recommended approach is to describe an object’s desired state in a manifest file, typically specified in YAML format.

In a production scenario, the image would be fetched from a container registry. During development, it’s more convenient to work with local images.
Let’s build one for Catalog Service, and build a new container image as follows:

`$ ./gradlew bootBuildImage`

By default, minikube doesn't have access to your local container images, so it will not find the image you have just built for Catalog Service.
You can manually import it into your local cluster:

`$ minikube image load catalog-service --profile bookshop`

##### Creating a deployment object from manifest
Apply k8s manifest to a cluster using kubectl client, navigate to catalog-service root folder and run below command:

`$ kubectl apply -f k8s/deployment.yml`

The above command is processed by the k8s control plane, which will create and maintain all the related objects in the cluster. You can verify which objects
have been created with the below command:

`$ kubectl get all -l app=catalog-service`

Since you used labels consistently in your Deployment manifest, you can use the label to fetch all the k8s objects related to the catalog service deployment.
The declaration in deployment.yml resulted in the creation of a Deployment, a ReplicaSet and a Pod.

To check the catalog service logs from its deployment, run below command:

`$ kubectl logs deployment/catalog-service`

To get the more information about the pod, you can use below command:

`$ kubectl get pods`

`$ kubectl describe pod <pod_name>`

### Service discovery and load balancing
Catalog service application running as a Pod in you local k8s cluster, but there are still unanswered question:
1. How can it interact with the PostgreSQL pod running in the cluster?
2. How does it know where to find it?
3. How can you expose a spring boot application to be used by other Pods in the cluster?
4. How can you expose it outside the cluster?

Two important aspects of cloud native systems that answer the above questions:
1. Service Discovery
2. Load Balancing

Two main patterns are available to implement them while working with spring applications:
1. Client Side
2. Server Side

The server-side approach of service discovery and load balancing is natively offered by Kubernetes through Service objects, meaning you don't have to change
anything in your code to support it unlike the client-side option.

When you have multiple instances of a service running, each service instance will have its own IP address. A service instance will not live longer in the cloud.
Using IP addresses for interprocess communication in the cloud is not an option. DNS record can be one solution, but there's a high chance of using a
hostname/IP address resolution that is no longer valid.

Service discovery in cloud environment requires a different solution. First we need to keep track of all the service instances running and store that
information in a **service registry**. Whenever a new instance is created, an entry should be added to the registry. When it's shut down, it should be removed
accordingly.
The registry recognizes that multiple instances of the same applications can be up and running. When an application needs to call a backing service, it performs
a lookup in the registry to determine which IP address to connect. If multiple instances are available, a **load-balancing** strategy is applied to distribute
the workload across them.

#### Client side service discovery and load balancing

A drawback is that client service discovery assigns more responsibility to developers. If your system includes applications built using different languages and
frameworks, you’ll need to handle the client part of each of them in different ways.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/client-side-service-discovery-and-load-balancing.drawio.svg)

#### Server side service discovery and load balancing

Server-side service discovery solutions move a lot of responsibility to the deployment platform, so that developers can focus on the business logic and rely on
the platform to provide all the necessary functionality for service discovery and load balancing.
Such solutions automatically register and deregister application instances and rely on a load-balancer component to route any incoming requests to one of the
available instances according to a specific strategy. In this case, the application doesn't need to interact with the service registry, which is updated and
managed by the platform.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/server-side-service-discovery-and-load-balancing.drawio.svg)

The k8s implementation of this service discovery pattern is base on **Service** objects. A service is an abstract way to expose an application running on a set
of Pods as a Network service.

The IP address assigned to a Service is fixed for its lifetime. Therefore, the DNS resolution of a Service name doesn't change as often as it would with
application instances.

After resolving the Service name to its IP address, Kubernetes relies on a proxy (called kube-proxy), which intercepts the connection to the Service object and
forwards the request to one of the Pods targeted by the Service. The proxy knows all the replicas available and adopts a load-balancing strategy depending on
the type of Service and the proxy configuration. There is no DNS resolution involved in this step.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/server-side-service-discovery-and-load-balancing-with-k8s.drawio.svg)

In Kubernetes, the interprocess communication between Alpha App and Beta App happens through a Service object. Any request arriving at the Service is
intercepted by a proxy that forwards it to one of the replicas targeted by the Service based on a specific load-balancing strategy.

#### Exposing Spring boot applications with Kubernetes Services

##### Creating the Service object from the manifest

Apply the Service manifest

`$ kubectl apply -f k8s/service.yml`

Verify the result:

`$ kubectl get svc -l app=catalog-service`

Expose the application outside the cluster. For now, we will rely on the port-forwarding feature offered by k8s to expose an object to a local machine.

`$ kubectl port-forward service/catalog-service 9001:80`

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/expose-spring-boot-app-with-k8s-service.drawio.svg)

#### Disposability: Fast Startup and Graceful Shutdown
Fast startup is relevant in a cloud environment because applications are disposable and are frequently created, destroyed, and scaled. The quicker the startup,
the sooner a new application instance is ready to accept connections.

Having applications start quickly is not enough to address our scalability needs. Whenever an application instance is shut down, it must happen gracefully
without clients experiencing downtime or errors. Gracefully shutting down means the application stops accepting new requests, completes all those still in
progress, and closes any open resources, like database connections.

By default, Spring Boot stops the server immediately after receiving a termination signal (SIGTERM). You can switch to a graceful mode by configuring the
`server.shutdown` property.

After enabling application support for graceful shutdown, you also need to update Deployment manifest accordingly. Kubernetes sends SIGTERM signal to the Pod
when it has to be terminated, and also informs its own components to stop forwarding request to the terminating Pod.
You need to configure a delay in k8s to send the SIGTERM signal to the Pod so that k8s has enough time to spread the news across the cluster. By doing so, all
k8s component will already know not to send new requests to the Pod when it starts graceful shutdown.

When a Pod contains multiple containers, the SIGTERM signal is sent to all of them in parallel. Kubernetes will wait up to 30 seconds. If any of the
containers in the Pod are not terminated yet, it will shut them down forcefully.

#### Scaling app
In k8s, replication is handled at Pod level by a ReplicaSet object. Deployment objects are already configured to use ReplicaSet, all you need to specify the
number of replicas to be deployed. The replication is controlled using labels defined in the manifest (app=catalog-service).
Update number of replica count in deployment.yml and apply new changes to k8s:

`$ kubectl apply -f k8s/deployment.yml`

Verify the result:

`$ kubectl get pods -l app=catalog-service`

Before moving change number of replicas back to one and clean up your cluster by removing all the resources you have created so far. Navigate to the
catalog-service folder where you have defined the k8s manifests and delete all the objects created for catalog service.

`$ kubectl delete -f k8s`

Go to the bookshop-deployment repo, and navigate to the kubernetes/platform/development folder, and delete the PostgreSQL installation:

`$ kubectl delete -f services`


#### Local k8s development with Tilt
Setup local k8s development workflow to automate steps like building images and applying manifests to a k8s cluster. It's part of implementing
the **inner development loop** of working with a k8s platform.

Tilt (https://tilt.dev) is an open source tool that offers features for building, deploying and managing containerized workloads in you local environment.

Design a workflow that will automate the following steps:
1. Package a spring boot application as a container image using cloud native buildpacks.
2. Upload the image to a k8s cluster.
3. Apply all the k8s objects declared in the YAML manifest.
4. Enable the port-forwarding functionality.
5. Gives you easy access to the logs from the application running on the cluster.

Before running tilt, make sure postgres instance is up and running in local k8s cluster. Navigate to bookshop-deployment/kubernetes/platform/development, run:

`$ kubectl apply -f services`

Tilt can be configured via a _**Tiltfile**_. Create a file named "Tiltfile" in the root folder. The file will contain three main configuration:
1. How to build a container image.
2. How to deploy the application.
3. How to access the application.

The Tiltfile configures Tilt to use the same approach we used previously for building, loading, deploying, and publishing applications on the local k8s cluster.
The main difference? It’s all automated now! After configuring Tiltfile, run below command to start Tilt and follow-on screen instructions:

`$ tilt up`

Tilt will keep the application in sync with the source code. Whenever you make any change to the application, Tilt will trigger an update operation to build
and deploy a new container image. All of that happens automatically and continuously.

Verify the application is running:

`$ http :9001/books`

Stop the tilt process and undeploy the application by running following command:

`$ tilt down`

Rebuilding the whole container image every time you change something in your code is not very efficient. You can configure Tilt to synchronize only the changed
files and upload them into the current image. To achieve that, you can rely on the features offered by Spring Boot DevTools (https://mng.bz/nY8v) and Paketo
Buildpacks (https://mng.bz/vo5x).

#### Visualizing your Kubernetes workloads with Octant
When you start deploying multiple applications to a Kubernetes cluster, it can become challenging to manage all the related Kubernetes objects or investigate
failures when they happen. There are different solutions for visualizing and managing Kubernetes workloads.
We will cover Octant (https://octant.dev), an open source developer-centric web interface for Kubernetes that lets you inspect a Kubernetes cluster
and its applications.

Deploy application to local k8s cluster by running `$ tilt up` command. Then run `$ octant`. This command will open the Octant Dashboard in browser.

Octant offers a web interface for inspecting a Kubernetes cluster and its workloads.
Octant lets you access Pod information easily, check their logs, and enable a port forward.

When you are done stop the Octant by stopping its process with Ctrl-C. Then stop the Tilt process and run `$ tilt down` to undeploy the application.
Delete postgres installation with `$ kubectl delete -f services`. Finally, stop the cluster as follows:

`$ minikube stop --profile bookshop`

#### Deployment Pipeline: Validate Kubernetes Manifest
Since manifest specifies the desired state of an object, we should ensure that our specification complies with the API exposed by Kubernetes.
It’s a good idea to automate this validation in the commit stage of a deployment pipeline to get fast feedback in case of errors rather than waiting until
the acceptance stage, where we need to use those manifests to deploy the application in a Kubernetes cluster.
There are several ways of validating k8s manifests against the k8s API. We will use Kubeval (https://www.kubeval.com), an open source tool.

To validate the k8s manifest within the k8s directory (-d k8s) use below command in local environment from the root folder of catalog service:

`$ kubeval --strict -d k8s`

The --strict flag disallows adding additional properties not defined in the object schema.

#### Summary
* Docker works fine when running single-instance containers on a single machine. When your system needs properties like scalability and resilience,
you can use Kubernetes.
* Kubernetes provides all the features for scaling containers across a cluster of machines, ensuring resilience both when a container fails and when a
machine goes down.
* Pods are the smallest deployable units in Kubernetes.
* Rather than creating Pods directly, you can use a Deployment object to declare the desired state for your applications, and Kubernetes will ensure it matches
the actual state. That includes having the desired number of replicas up and running at any time.
* The cloud is a dynamic environment, and the topology keeps changing. Service discovery and load balancing let you dynamically establish interactions between
services, managed either on the client side (for example, using Spring Cloud Netflix Eureka) or on the server side (for example, using Kubernetes).
* Kubernetes provides a native service-discovery and load-balancing feature that you can use through the Service objects.
* Each Service name can be used as a DNS name. Kubernetes will resolve the name to the Service IP address and, ultimately, forward the request to one of
the instances available.
* You can deploy Spring Boot applications to a Kubernetes cluster by defining two YAML manifests: one for the Deployment object and one for the Service object.
* The kubectl client lets you create objects from a file with the command `$ kubectl apply -f <your-file.yml>`.
* Cloud native applications should be disposable (fast startup and graceful shutdown) and stateless (rely on data services for storing the state).
* Graceful shutdown is supported both by Spring Boot and Kubernetes and is an essential aspect of scalable applications.
* Kubernetes uses ReplicaSet controllers to replicate your application Pods and keep them running.
* Tilt is a tool that automates your local development workflow with Kubernetes: you work on the application while Tilt takes care of building the image,
deploying it to your local Kubernetes cluster, and keeping it up-to-date whenever you change something in the code.
* You can start Tilt for your project with `$ tilt up`.
* The Octant dashboard lets you visualize your Kubernetes workloads.
* Octant is a convenient tool that you can use not only for inspecting and troubleshooting a local Kubernetes cluster but also for a remote one.
* Kubeval is a convenient tool you can use to validate Kubernetes manifests. It’s particularly useful when it’s included in your deployment pipeline.

#### Securing Spring Boot as an OAuth2 Resource Server
##### Configuring the integration between Spring Security and Keycloak
Spring Security supports protecting endpoints using two data formats for the Access Token:
1. **JWT** - When the Access Token is a JWT, we can also include relevant information as claims about the authenticated user and propagate this context to
Catalog Service and Order Service.
2. **Opaque tokens** - opaque tokens would require the application downstream to contact Keycloak every time to fetch the information associated with the token.

We'll work with Access Tokens defined as JWTs, similar to what we did for ID Tokens. With Access Tokens, Keycloak grants Edge Service access to downstream
applications on behalf of the user.

When working with JWTs, the application will contact Keycloak mainly to fetch the public keys necessary to verify the token’s signature. Using the issuer-uri
property, we'll let the application auto-discover the Keycloak endpoint where it can find the public keys.
OAuth2 Authorization Servers provide their public keys using the JSON Web Key (JWK) format. The collection of public keys is called a JWK Set. The endpoint
where Keycloak exposes its public keys is called the JWK Set URI. Spring Security will automatically rotate the public keys whenever Keycloak makes new ones
available.

For each incoming request containing an Access Token in the Authorization header, Spring Security will automatically validate the token's signature using the
public keys provided by Keycloak and decode its claims via a JwtDecoder object.

##### Role-based access control with Spring Security and JWT
Spring Security associates each authenticated user with a list of GrantedAuthority objects that model the authorities the user has been granted. Granted
authorities can be used to represent fine-grained permissions, roles, or even scopes and come from different sources depending on the authentication strategy.
The authorities are available through the Authentication object representing the authenticated user and stored in the SecurityContext.

Since Catalog Service is configured as an OAuth2 Resource Server and uses JWT authentication, Spring Security extracts the list of scopes from the scopes claim
of the Access Token and uses them as granted authorities for the given user automatically. Each GrantedAuthority object built in this way will be named with the
SCOPE_ prefix and the scope value.

We'll define a custom converter for the Access Token to build a list of GrantedAuthority objects using the values in the roles claim and the ROLE_ prefix. Then
we'll use those authorities to define authorization rules for the endpoints of Catalog Service.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/conversion-of-user-roles-from-jwt-to-granted-authority.drawio.svg)

**Since granted authorities can be used to represent different items (roles, scopes, permissions), Spring Security uses prefixes to group them.**

#### Auditing data with Spring Security and Spring Data JDBC
We need to address to main concerns:
1. How can we tell which users created what data? Who changed it last?
2. How can we ensure that each user can only access their own data?

We need to include the usernames of the person who created the entity and the person who modified it last. First we need to tell Spring Data where to get the
information about the currently authenticated user.

Any change to a database schema should also be backward compatible to support common deployment strategies for cloud native applications, like rolling upgrades,
blue/green deployments, or canary releases. In this case, we need to add new columns to the book table. As long as we don't make them mandatory, the change will
be backward compatible. After we change the schema, any running instance of the previous release of Catalog Service will continue to work without errors, simply
ignoring the new columns.

The tradeoff of enforcing backward-compatible changes is that we now have to treat as optional two fields that we need to have always filled in, and that may
fail validation if they're not. That is a common problem that can be solved over two subsequent releases of the application:
1. In the first release, you add the new columns as optional and implement a data migration to fill in the new columns for all the existing data. For Catalog
Service, you could use a conventional value to represent that we don't know who created or updated the entity, such as unknown or anonymous.
2. In the second release, you can create a new migration to update the schema safely and make the new columns required.

#### Health probes with Spring Boot Actuator and Kubernetes
Once an application is deployed, how can we tell if it's healthy? Is it capable of handling new requests? Did it enter a faulty state? Cloud native applications
should provide information about their health so that monitoring tools and deployment platforms can detect when there's something wrong and act accordingly.

The deployment platform can periodically invoke health endpoints exposed by applications. A monitoring tool could trigger an alert or a notification when an
application instance is unhealthy. In the case of Kubernetes, the platform will check the health endpoints and automatically replace the faulty instance or
temporarily stop sending traffic to it until it’s ready to handle new requests again.

There are a few viable solutions for protecting the Spring Boot Actuator endpoints. For example, you could enable HTTP Basic authentication just for the
Actuator endpoints, while all the others will keep using OpenID Connect and OAuth2. For simplicity, in the Bookshop system, we'll keep the Actuator endpoints
unauthenticated from inside the Kubernetes cluster and block any access to them from the outside.

In a real production scenario, I would recommend protecting access to the Actuator endpoints even from within the cluster.

Besides showing detailed information about the application's health, Spring Boot Actuator automatically detects when the application runs on a Kubernetes
environment and enables the health probes to return liveness (/actuator/health/liveness) and readiness (/actuator/health/readiness) states.

* **Liveness state** — When an application is not live, this means it has entered a faulty internal state from which it won't recover. By default, Kubernetes
will try restarting it to fix the problem.
* **Readiness state** — When an application is not ready, this means it can't process new requests, either because it's still initializing all its components
(during the startup phase) or because it's overloaded. Kubernetes will stop forwarding requests to that instance until it's ready to accept new requests again.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/diagrams/application-health-probes.drawio.svg)

**Kubernetes uses liveness and readiness probes to accomplish its self-healing features in case of failures.**

Endpoint for liveness probe:

`$ http :9001/management/health/liveness`

The liveness state of a Spring Boot application indicates whether it's in a correct or broken internal state. If the Spring application context has started
successfully, the internal state is valid. It doesn't depend on any external components. Otherwise, it will cause cascading failures, since Kubernetes will try
to restart the broken instances.

Endpoint for readiness probe:

`$ http :9001/management/health/readiness`

The readiness state of a Spring Boot application indicates whether it's ready to accept traffic and process new requests. During the startup phase or graceful
shutdown, the application is not ready and will refuse any requests. It might also become temporarily not ready if, at some point, it's overloaded. When it's
not ready, Kubernetes will not send any traffic to the application instance.

#### Configuring Liveness and Readiness Probes in Kubernetes
Kubernetes relies on the health probes (liveness and readiness) to accomplish its tasks as a container orchestrator. For example, when the desired state of an
application is to have three replicas, Kubernetes ensures there are always three application instances running. If any of them doesn't return a 200 response
from the liveness probe, Kubernetes will restart it. When starting or upgrading an application instance, we'd like the process to happen without downtime for
the user. Therefore, Kubernetes will not enable an instance in the load balancer until it's ready to accept new requests (when Kubernetes gets a 200 response
from the readiness probe).

Since liveness and readiness information is application-specific, Kubernetes needs the application itself to declare how to retrieve that information. Relying
on Actuator, Spring Boot applications provide liveness and readiness probes as HTTP endpoints.

Both probes can be configured so that Kubernetes will start using them after an initial delay (initialDelaySeconds), and you can also define the frequency with
which to invoke them (periodSeconds). The initial delay should consider that the application will take a few seconds to start, and it will depend on the
available computational resources. The polling period should not be too long, to reduce the time between the application instance entering a faulty state and
the platform taking action to self-heal.

If you run these examples on resource-constrained environments, you might need to adjust the initial delay and the polling frequency to allow the application
more time to start and get ready to accept requests.
