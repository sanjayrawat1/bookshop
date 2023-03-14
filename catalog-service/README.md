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

![After changing the configuration in the Git repository backing the Config Service, a signal is sent to Catalog Service to refresh the parts of the application using the configuration.](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/hot-reload-config-data.drawio.svg "Refreshing configuration at runtime")

### Running a PostgreSQL Database
Run PostgreSQL as a Docker container

`$ docker run -d --name bookshop-postgres -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=catalog -p 5432:5432 postgres:14.2`

### Container commands

| Docker command                  | Description      |
|---------------------------------|------------------|
| docker stop bookshop-postgres   | Stop container.  |
| docker start bookshop-postgres  | Start container. |
| docker remove bookshop-postgres | Remove container |

### Database commands
Start an interactive PSQL console:
`$ docker exec -it bookshop-postgres psql -U user -d catalog`

| PSQL command     | Description                                  |
|------------------|----------------------------------------------|
| \list            | List all databases.                          |
| \connect catalog | Connect to specific database (e.g. catalog). |
| \dt              | List all tables.                             |
| \d book          | Show the `book` table schema.                |
| \quit            | Quit interactive PSQL console                |

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

`$ docker run -d --name bookshop-postgres --net catalog-network -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=catalog -p 5432:5432 postgres:14.2`

Now first build the JAR artifacts:

`$ ./gradlew clean bootJar`

Build the container image:

`$ docker build -t catalog-service .`

Run the Docker container image using port forwarding to 9001 and using the Docker build-in DNS server to connect to the catalog-network:

`$ docker run -d --name catalog-service --net catalog-network -p 9001:9001 -e SPRING_DATASOURCE_URL=jdbc:postgresql://bookshop-postgres:5432/catalog -e SPRING_PROFILES_ACTIVE=test-data catalog-service`

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

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/config-to-debug-containerized-java-app-from-intellij-idea.png "Configuration to debug a containerized Java application from IntelliJ IDEA")

### Deployment Pipeline: Package and publish

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/deployment-pipeline-package-and-publish-commit-stage.drawio.svg)

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

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/application-container-management-in-docker.drawio.svg)

Kubernetes' clients interact with the Control Plane, which manages containerized applications in a cluster consisting of one or more nodes.
Applications are deployed as Pods to the nodes of a cluster.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/application-container-management-in-kubernetes.drawio.svg)

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

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/client-side-service-discovery-and-load-balancing.drawio.svg)

#### Server side service discovery and load balancing

Server-side service discovery solutions move a lot of responsibility to the deployment platform, so that developers can focus on the business logic and rely on
the platform to provide all the necessary functionality for service discovery and load balancing.
Such solutions automatically register and deregister application instances and rely on a load-balancer component to route any incoming requests to one of the
available instances according to a specific strategy. In this case, the application doesn't need to interact with the service registry, which is updated and
managed by the platform.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/server-side-service-discovery-and-load-balancing.drawio.svg)

The k8s implementation of this service discovery pattern is base on **Service** objects. A service is an abstract way to expose an application running on a set
of Pods as a Network service.

The IP address assigned to a Service is fixed for its lifetime. Therefore, the DNS resolution of a Service name doesn't change as often as it would with
application instances.

After resolving the Service name to its IP address, Kubernetes relies on a proxy (called kube-proxy), which intercepts the connection to the Service object and
forwards the request to one of the Pods targeted by the Service. The proxy knows all the replicas available and adopts a load-balancing strategy depending on
the type of Service and the proxy configuration. There is no DNS resolution involved in this step.

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/server-side-service-discovery-and-load-balancing-with-k8s.drawio.svg)

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

![](https://github.com/sanjayrawat1/bookshop/blob/main/catalog-service/expose-spring-boot-app-with-k8s-service.drawio.svg)

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
