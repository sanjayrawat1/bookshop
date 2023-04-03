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

#### Managing Kubernetes configuration for multiple environments with Kustomize
For each environment, we can specify _patches_ to apply changes or additional configurations on top of those basic manifests. All the customization steps will
be applied without changing anything in the application source code but using the same release artifacts produced earlier. That's quite a powerful concept and
one of the main features of cloud native applications.

The Kustomize approach to configuration customization is based on the concepts of bases and overlays. The k8s folder we created in the Catalog Service project
can be considered a base : a directory with a kustomization.yml file that combines Kubernetes manifests and customizations. An overlay is another directory with
a kustomization.yml file. What makes it special is that it defines customizations in relation to one or more bases and combines them. Starting from the same
base, you can specify an overlay for each deployment environment (such as development, test, staging, and production).

Each Kustomization includes a kustomization.yml file. The one acting as the base composes together several Kubernetes resources like Deployments, Services, and
ConfigMaps. Also, it’s not aware of the overlays, so it’s completely independent of them. The overlays use one or more bases as a foundation and provide
additional configuration via patches.

**Kustomize bases can be used as the foundation for further customizations (overlays) depending on the deployment environment.**

![](https://github.com/sanjayrawat1/bookshop/blob/main/bookshop-deployment/diagrams/customizing-configuration-for-multiple-environment-with-kustomize.drawio.svg)

Bases and overlays can be defined either in the same repository or different ones. We'll use the k8s folder in each application project as a base and define
overlays in the bookshop-deployment repository. You can decide whether to keep your deployment configuration in the same repository as your application or not.
I decided to go for a separate repository for a few reasons:
* It makes it possible to control the deployment of all the system components from a single place.
* It allows focused version-control, auditing, and compliance checks before deploying anything to production.
* It fits the GitOps approach, where delivery and deployment tasks are decoupled.

Another decision to make is whether to keep the base Kubernetes manifests together with the application source code or move them to the deployment repository.
I decided to go with the first approach for the Bookshop example, similar to what we did with the default configuration properties. One of the benefits is that
it makes it simple to run each application on a local Kubernetes cluster during development, either directly or using Tilt.

After defining overlay kustomization, like, a patch for customizing environment variables, ConfigMaps, image name and version, replicas for container, it's time
to test it.

Run staging overlay for catalog-service, navigate to applications/catalog-service/staging folder and run following command:

`$ kubectl apply -k .`

You can monitor the operation’s result via the Kubernetes CLI (`$ kubectl get pod -l app=catalog-service`)

The application is not exposed outside the k8s cluster, use port-forwarding functionality to forward traffic from local env on port 9001 to service running in
the cluster on port 80

`$ kubectl port-forward service/catalog-service 9001:80`

and then test the catalog-service greeting endpoint, the result will be the customized message defined in the application-staging.yml file

Verify the number of replicas we applied through the customization:

`$ kubectl get pod -l app=catalog-service`

You can check which node each Pod has been allocated on with:

`$ kubectl get pod -o wide`

You can also try to update the application-staging.yml file, apply the Kustomization to the cluster again (kubectl apply -k .), and see how the Catalog Service
Pods are restarted one after the other (rolling restarts) to load the new ConfigMap with zero downtime. To visualize the sequence of events, you can either use
Octant or launch this command on a separate Terminal window before applying the Kustomization: 

`$ kubectl get pods -l app=catalog-service --watch`
