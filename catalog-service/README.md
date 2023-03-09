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
`docker run -d --name bookshop-postgres -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=catalog -p 5432:5432 postgres:14.2`

### Container commands

| Docker command                  | Description      |
|---------------------------------|------------------|
| docker stop bookshop-postgres   | Stop container.  |
| docker start bookshop-postgres  | Start container. |
| docker remove bookshop-postgres | Remove container |

### Database commands
Start an interactive PSQL console:
`docker exec -it bookshop-postgres psql -U user -d catalog`

| PSQL command     | Description                                  |
|------------------|----------------------------------------------|
| \list            | List all databases.                          |
| \connect catalog | Connect to specific database (e.g. catalog). |
| \dt              | List all tables.                             |
| \d book          | Show the `book` table schema.                |
| \quit            | Quit interactive PSQL console                |
