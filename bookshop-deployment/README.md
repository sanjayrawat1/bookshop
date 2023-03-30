# bookshop-deployment

### Managing Spring Boot containers with Docker Compose

Finally, you can remove the network you used to make catalog service communicate with postgres. You won't need it anymore, as you will use docker-compose to
manage the container life cycle. If you don't add any network configuration, docker compose will automatically create one for you and make all the containers
in the file join it. That means they can interact with each other through their container names, relying on docker's built-in DNS server.

`$ docker network rm catalog-network`

### Debugging Spring Boot Containers
First you need to instruct the JVM inside the container to listen for debug connections on a specific port. The container image produced by Paketo Buildpacks 
supports dedicated environment variables for running the application in debug mode (BPL_DEBUG_ENABLED and BPL_DEBUG_PORT). Then you need to expose the 
debug port outside the container so that your IDE can reach it.

**From a container, you can expose as many ports as you want. For Catalog Service, expose both the server port and the debug port.**

![](https://github.com/sanjayrawat1/bookshop/blob/main/bookshop-deployment/diagrams/debug-spring-boot-containers.drawio.svg "Debugging Spring Boot containers")

After adding remote config in docker-compose file go to the location of docker-compose.yml file and run the following command:

`$ docker-compose up -d`

### Managing data services in a local cluster
Data services are the stateful components of a system and require special care in a cloud environment due to the challenges of handling their storage. 
Managing persistence and storage in Kubernetes is a complex topic, and it’s not usually the responsibility of developers.
When you deploy the Bookshop system in production, you’ll rely on the managed data services offered by the cloud provider.
For local k8s cluster, I have prepared the configuration for deploying PostgreSQL, under [services](kubernetes%2Fplatform%2Fdevelopment%2Fservices) folder.

Navigate to the [development](kubernetes%2Fplatform%2Fdevelopment) folder and run following command to deploy PostgreSQL in local cluster:

`$ kubectl apply -f services`

The above command creates the resources defined in the manifests within the services' folder. The result will be a Pod running a PostgreSQL container in local
k8s cluster. Run below command to check it out:

`$ kubectl get pod`

To check the database logs run below command:

`$ kubectl logs deployment/bookshop-postgres`

To undeploy the database, you can run the below command from the same folder:

`$ kubectl delete -f services`

##### Deployment Scripts
Scripts that performs all the previous operations with a single command. You can run it to create a local Kubernetes cluster with minikube, enable the Ingress 
NGINX add-on, and deploy the backing services used by Bookshop. You’ll find the create-cluster.sh and destroy-cluster.sh files in the
kubernetes/platform/development folder. 
On macOS and Linux, you might need to make the scripts executable via the **chmod +x create-cluster.sh** command.

Until now, the only application supposed to be accessed by users directly was Edge Service. All the other Spring Boot applications interact with each other from
within the environment where they are deployed.

Service-to-service interactions within the same Docker network or Kubernetes cluster can be configured using the container name or the Service name respectively.
For example, Edge Service forwards requests to bookshop UI via the http://bookshop-ui:9004 URL on Docker (<container-name>:<container-port>) and via the
http://bookshop-ui URL on Kubernetes (Service name).

Keycloak is different because it's involved in service-to-service interactions (for now, those are just interactions with Edge Service) and also interactions
with end users via the web browser. In production, Keycloak will be accessible via a public URL that both applications and users will use, so there will be no
problem. How about in local environments?

Since we don't deal with public URLs when working locally, we need to configure things differently. On Docker, we can solve the problem by using the
http://host.docker.internal special URL configured automatically when installing the software. It resolves to your localhost IP address and can be used both
within and can be used both within a Docker network and outside.

On Kubernetes, we don't have a generic URL to let Pods within a cluster access your local host. That means Edge Service will interact with Keycloak via its
Service name (http://bookshop-keycloak). When Spring Security redirects a user to Keycloak to log in, the browser will return an error because the
http://bookshop-keycloak URL cannot be resolved outside the cluster. To make that possible, we can update the local DNS configuration to resolve the
bookshop-keycloak hostname to the cluster IP address. Then a dedicated Ingress will make it possible to access Keycloak when requests are directed to the
bookshop-keycloak hostname.

If you're on Linux or macOS, you can map the bookshop-keycloak hostname to the minikube local IP address in the /etc/hosts file. On Linux, the IP address is the
one returned by the **minikube ip --profile bookshop** command. On macOS, it's going to be **127.0.0.1**. Open a Terminal window, and run the following command
(make sure you replace the <ip-address> placeholder with the cluster IP address, depending on your operating system):

`$ echo "<ip-address> bookshop-keycloak" | sudo tee -a /etc/hosts`

On Windows you must map the **bookshop-keycloak** hostname to **127.0.0.1** in the hosts file. Open a PowerShell window as an administrator, and run the
following command:

`$ Add-Content C:\Windows\System32\drivers\etc\hosts "127.0.0.1 bookshop-keycloak"`

#### Managing logs with Loki, Fluent Bit, and Grafana
When you move to distributed systems like microservices and complex environments like the cloud, managing logs becomes challenging and requires a different
solution than in more traditional applications. If something goes wrong, where can we find data about the failure? Traditional applications would rely on log
files stored on the host machine. Cloud native applications are deployed in dynamic environments, are replicated, and have different life spans. We need to
collect the logs from all applications running in the environment and send them to a central component where they can be aggregated, stored, and searched.

There are plenty of options for managing logs in the cloud. Cloud providers have their own offerings, like Azure Monitor Logs and Google Cloud Logging. There
are also many enterprise solutions available on the market, such as Honeycomb, Humio, New Relic, Datadog, and Elastic.

For Bookshop, we'll use a solution based on the Grafana observability stack (https://grafana.com). It's composed of open source technologies, and you can run it
yourself in any environment. It's also available as a managed service (Grafana Cloud) offered by Grafana Labs.

The components of the Grafana stack we'll use for managing logs are:
1. **Loki** for log storage and search.
2. **Fluent Bit** for log collection and aggregation.
3. **Grafana** for log data visualization and querying.

Fluent Bit enables you to collect logs and metrics from multiple sources, enrich them with filters, and distribute them to any defined destination
(https://fluentbit.io). Fluent Bit is a subproject of Fluentd, an open source data collector for unified logging layer (www.fluentd.org).

Fluent Bit will collect logs from all running containers and forward them to Loki, which will store them and make them searchable. Loki is a log aggregation
system designed to store and query logs from all your applications and infrastructure (https://grafana.com/oss/loki).

Finally, Grafana will use Loki as a data source and provide log visualization features. Grafana allows you to query, visualize, alert on and understand your
telemetry, no matter where it is stored (https://grafana.com/oss/grafana).

![](https://github.com/sanjayrawat1/bookshop/blob/main/bookshop-deployment/diagrams/logging-architecture-using-grafana-stack.drawio.svg)

**Logging architecture for cloud native applications based on the Grafana stack**

Fluent Bit can be configured to collect logs from different sources. For Bookshop, we'll rely on the Fluentd driver available in Docker to collect logs
automatically from running containers. The Docker platform itself listens to the log events from each container and routes them to the specified service.
In Docker, a logging driver can be configured directly on a container.

To test the logs in grafana stack, run the catalog service by following below commands:

`$ docker-compose up -d grafana catalog-service`

The dependent services of grafana and catalog service will automatically be started as well.

Now, send a few requests to catalog service to trigger the generation of some log messages:

`$ http :9001/books`

Then open a browser, head to Grafana http://localhost:3000, and use credentials configured in the docker compose to log in (user/password). Then select Explore
page from left menu, choose Loki as the data source, choose last 1 hour from the time drop-down menu and run the following query to search for all the logs
produced by the **catalog-service** container:

`{container_name="/catalog-service"}`
