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

### Kubernetes in production with DigitalOcean
Before moving on, you need to ensure that you have a DigitalOcean account. When you sign up, DigitalOcean offers a 60-day free trial with a $200 credit. Follow
the instructions on the official website to create an account and start a free trial (https://try.digitalocean.com/freetrialoffer).

There are two main options for interacting with the DigitalOcean platform. The first one is through the web portal (https://cloud.digitalocean.com), which is
very convenient for exploring the available services and their features. The second option is via doctl, the DigitalOcean CLI. We're going to use the 2nd option.

You can find instructions for installing doctl on the official website (https://docs.digitalocean.com/reference/doctl/how-to/install).
Install the doctl using homebrew on macOS:

`$ brew install doctl`

You can follow the subsequent instructions on the same doctl page to generate an API token and grant doctl access to your DigitalOcean account.

Once API token is generated, grant doctl access to DigitalOcean account, **doctl auth init --context <context-name>**. Pass in the token string when prompted.

`$ doctl auth init --context bookshop`

List the authentication context:

`$ doctl auth list`

Switch the authentication context:

`$ doctl auth switch --context bookshop`

Validate that doctl is working:

`$ doctl account get`

#### Running a Kubernetes cluster on DigitalOcean
The first resource we need to create on DigitalOcean is a Kubernetes cluster. You could rely on the IaaS capabilities offered by the platform and install a
Kubernetes cluster manually on top of virtual machines. Instead, we’ll move up the abstraction staircase and go for a solution managed by the platform. When we
use DigitalOcean Kubernetes (https://docs.digitalocean.com/products/kubernetes), the platform will take care of many infrastructural concerns, so that we
developers can focus more on application development.

Each cloud resource can be created in a data center hosted in a specific geographical region. For better performance, I recommend you choose one near to you.
I'll use "Bangalore 1" (blr1), but you can get the complete list of regions with the following command:

`$ doctl k8s options regions`

Let's go ahead and initialize a Kubernetes cluster using DigitalOcean Kubernetes (DOKS). It will be composed of three worker nodes, for which you can decide the
technical specifications. You can choose between different options in terms of CPU, memory, and architecture. I'll use nodes with 2 vCPU and 4 GB of memory:

`$ doctl k8s cluster create bookshop-cluster --node-pool "name=basicnp;size=s-2vcpu-4gb;count=3;label=type=basic;" --region blr1`

##### Explanation of above command
| command breakdown                                                     | description                                              |
|-----------------------------------------------------------------------|----------------------------------------------------------|
| doctl k8s cluster create bookshop-cluster                             | defines the name of the cluster to create                |
| --node-pool "name=basicnp;size=s-2vcpu-4gb;count=3;label=type=basic;" | provides the requested specification for the worker node |
| --region blr1                                                         | the data center region of your choice, such as "blr1"    |

If you'd like to know more about the different compute options and their prices, you can use the **doctl compute size list** command.

The cluster provisioning will take a few minutes. In the end, it will print out the unique ID assigned to the cluster. Take note, since you'll need it later.
You can fetch the cluster ID at any time by running the following command:

`$ doctl k8s cluster list`

At the end of the cluster provisioning, doctl will also configure the context for your Kubernetes CLI so that you can interact with the cluster running on
DigitalOcean from your computer, similar to what you've done so far with your local cluster. You can verify the current context for kubectl by running the
following command:

`$ kubectl config current-context`

If you want to change the context, you can run **kubectl config use-context <context-name>**.

Once the cluster is provisioned, you can get information about the worker nodes as follows:

`$ kubectl get nodes`

As you used Octant dashboard to visualize the workloads on your local Kubernetes cluster. You can now use it to get information about the cluster on
DigitalOcean as well. Start Octant with the following command:

`$ octant`

Octant will open in your browser and show data from your current Kubernetes context, which should be the cluster on DigitalOcean.

Kubernetes doesn't come packaged with an Ingress Controller; it's up to you to install one. Since we'll rely on an Ingress resource to allow traffic from the
public internet to the cluster, we need to install an Ingress Controller. Let's install the same one we used locally: ingress-nginx.

Navigate to the kubernetes/platform/production/ingress-nginx folder, and run the following command to deploy ingress-nginx to production Kubernetes cluster:

`$ ./deploy.sh`

You might need to make the script executable first with the command **chmod +x deploy.sh**.

#### Running a PostgreSQL database on DigitalOcean
In local environment, you've been running PostgreSQL database instances as containers, both in Docker and in your local Kubernetes cluster. In production, we'd
like to take advantage of the platform and use a managed PostgreSQL service provided by DigitalOcean (https://docs.digitalocean.com/products/databases/postgresql).

Moving from a PostgreSQL container running in your local environment to a managed service with high availability, scalability, and resilience is a matter of
changing the values of a few configuration properties for Spring Boot.

Create a new PostgreSQL server named **bookshop-postgres**. We'll use PostgreSQL 14, which is the same version we used for development and testing. Use the
geographical region you'd like to use. It should be the same as the region you used for the Kubernetes cluster.

```shell
$ doctl databases create bookshop-postgres --engine pg --region blr1 --version 14
```

Verify the installation status with the following command:

```shell
$ doctl databases list
```

To mitigate unnecessary attack vectors, you can configure a firewall so that the PostgreSQL server is only accessible from the Kubernetes cluster created
previously. Use kubernetes and postgres resource ID in the following command to configure the firewall and secure access to the database server:

```shell
$ doctl databases firewalls append <postgres_id> --rule k8s:<cluster_id>
```

Next, let's create two databases to be used by Catalog Service (bookshop_catalog) and Order Service (bookshop_order). Remember to replace <postgres_id> with
your PostgreSQL resource ID:

```shell
$ doctl databases db create <postgres_id> bookshop_catalog
```

```shell
$ doctl databases db create <postgres_id> bookshop_order
```

Finally, let's retrieve the details for connecting to PostgreSQL. Remember to replace <postgres_id> with your PostgreSQL resource ID:

```shell
$ doctl databases connection <postgres_id> --format Host,Port,User,Password
```

Let's create some Secrets in the Kubernetes cluster with the PostgreSQL credentials required by the two applications. In a real-world scenario, we should create
dedicated users for the two applications and grant limited privileges. For simplicity, we'll use the admin account for both.

First, create a Secret for Catalog Service using the information returned by the previous doctl command:

```shell
$ kubectl create secret generic bookshop-postgres-catalog-credentials \
  --from-literal=spring.datasource.url=jdbc:postgresql://<postgres_host>:<postgres_port>/bookshop_catalog \
  --from-literal=spring.datasource.username=<postgres_username> \
  --from-literal=spring.datasource.password=<postgres_password>
```

Similarly, create a Secret for Order Service. Pay attention to the slightly different syntax required by Spring Data R2DBC for the URL:

```shell
$ kubectl create secret generic bookshop-postgres-order-credentials \
  --from-literal="spring.flyway.url=jdbc:postgresql://<postgres_host>:<postgres_port>/bookshop_order" \
  --from-literal="spring.r2dbc.url=r2dbc:postgresql://<postgres_host>:<postgres_port>/bookshop_order?ssl=true&sslMode=require" \
  --from-literal=spring.r2dbc.username=<postgres_username> \
  --from-literal=spring.r2dbc.password=<postgres_password>
```

#### Running Redis on DigitalOcean
In production, we'd like to take advantage of the platform and use a managed Redis service provided by DigitalOcean (https://docs.digitalocean.com/products/databases/redis/).

First, create a new Redis server named bookshop-redis:

```shell
$ doctl databases create bookshop-redis --engine redis --region blr1 --version 7
```

Configure a firewall so that the Redis server is only accessible from the Kubernetes cluster:

```shell
$ doctl databases firewalls append <redis_id> --rule k8s:<cluster_id>
```

Retrieve the details for connecting to Redis:

```shell
$ doctl databases connection <redis_id> --format Host,Port,User,Password
```

```shell
$ kubectl create secret generic bookshop-redis-credentials \
  --from-literal=spring.data.redis.host=<redis_host> \
  --from-literal=spring.data.redis.port=<redis_port> \
  --from-literal=spring.data.redis.username=<redis_username> \
  --from-literal=spring.data.redis.password=<redis_password> \
  --from-literal=spring.data.redis.ssl=true
```

#### Running RabbitMQ using a Kubernetes Operator
We initialized and configured PostgreSQL and Redis servers that are offered and managed by the platform. We can't do the same for RabbitMQ because DigitalOcean
doesn't have a RabbitMQ offering, similar to other cloud providers like Azure or GCP.

A popular and convenient way of deploying and managing services like RabbitMQ in a Kubernetes cluster is to use the operator pattern. Operators are software
extensions to Kubernetes that make use of custom resources to manage applications and their components (https://kubernetes.io/docs/concepts/extend-kubernetes/operator).

To use it in production, you'll need to configure it for high availability and resilience. Depending on the workload, you might want to scale it dynamically.
When a new version of the software is available, you'll need a reliable way of upgrading the service and migrating existing constructs and data.
You could perform all those tasks manually. Or you could use an Operator to capture all those operational requirements and instruct Kubernetes to take care of
them automatically. In practice, an Operator is an application that runs on Kubernetes and interacts with its API to accomplish its functionality.

The RabbitMQ project provides an official Operator to run the event broker on a Kubernetes cluster.

Navigate to the kubernetes/platform/production/rabbitmq folder, and run the following command to deploy RabbitMQ to production Kubernetes cluster:

```shell
$ ./deploy.sh
```

The script will output details about all the operations performed to deploy RabbitMQ. Finally, it will create a bookshop-rabbitmq-credentials Secret with the
credentials that Order Service and Dispatcher Service will need to access RabbitMQ. You can verify that the Secret has been successfully created as follows:

```shell
$ kubectl get secrets bookshop-rabbitmq-credentials
```
The RabbitMQ broker is deployed in a dedicated **rabbitmq-system** namespace. Applications can interact with it at
**bookshop-rabbitmq.rabbitmq-system.svc.cluster.local** on port **5672**

#### Running Keycloak using a Helm chart
As with RabbitMQ, DigitalOcean doesn't provide a managed Keycloak service. The Keycloak project is working on an Operator, but it's still in beta at this time,
so we'll deploy it using a different approach: **Helm charts**.

Install Helm on your computer. You can find the instructions on the official website (https://helm.sh). If you are on macOS or Linux, you can install Helm with
Homebrew:

```shell
$ brew install helm
```

Navigate to the kubernetes/platform/production/keycloak folder, and run the following command to deploy Keycloak to production Kubernetes cluster:

```shell
$ ./deploy.sh
```

The script will output details about all the operations performed to deploy Keycloak and print the admin username and password you can use to access the
Keycloak Admin Console. Feel free to change the password after your first login. Note the credentials down, since you might need them later.

Finally, the script will create a **bookshop-keycloak-client-credentials** Secret with the Client secret that Edge Service will need to authenticate with
Keycloak. You can verify that the Secret has been successfully created as follows. The value is generated randomly by the script:

```shell
$ kubectl get secrets bookshop-keycloak-client-credentials
```

The Keycloak Helm chart spins up a PostgreSQL instance inside the cluster and uses it to persist the data used by Keycloak. We could have integrated it with the
PostgreSQL service managed by DigitalOcean, but the configuration on the Keycloak side would have been quite complicated. If you'd like to use an external
PostgreSQL database, you can refer to the Keycloak Helm chart documentation (https://bitnami.com/stack/keycloak/helm).

The Keycloak server is deployed in a dedicated **keycloak-system** namespace. Applications can interact with it at
**bookshop-keycloak.keycloak-system.svc.cluster.local** on port **8080** from within the cluster. It's also exposed outside the cluster via a public IP address.
You can find the external IP address with the following command:

```shell
$ kubectl get service bookshop-keycloak -n keycloak-system
```

The platform might take a few minutes to provision a load balancer. During the provisioning, the EXTERNAL-IP column will show a <pending> status. Wait and try
again until an IP address is shown. Note it down, since we're going to use it in multiple scenarios.

Since Keycloak is exposed via a public load balancer, you can use the external IP address to access the Admin Console. Open a browser window, navigate to
`http://<external-ip>/admin`, and log in with the credentials returned by the deployment script.

Now that you have a public DNS name for Keycloak, you can define a couple of Secrets to configure the Keycloak integration in Edge Service (OAuth2 Client),
Catalog Service, and Order Service (OAuth2 Resource Servers). Navigate to the kubernetes/platform/production/keycloak folder, and run the following command to
create the Secrets that the applications will use to integrate with Keycloak. Remember to replace <external-ip> with the external IP address assigned to your
Keycloak server:

```shell
$ ./create-secrets.sh http://<external-ip>/realms/Bookshop
```

#### Running Bookshop UI
Bookshop UI is a single-page application built with Angular and served by NGINX.

Navigate to the kubernetes/platform/production/bookshop-ui folder, and run the following command to deploy bookshop-ui to production Kubernetes cluster:

```shell
$ ./deploy.sh
```

#### Deleting all cloud resources
When you're done experimenting with the Bookshop project, follow the instructions in this section to delete all the cloud resources created on DigitalOcean.
That's fundamental to avoid incurring unexpected costs.

First, delete the Kubernetes cluster:

```shell
$ doctl k8s cluster delete bookshop-cluster
```

Next, delete the PostgreSQL and Redis databases. You'll need to know their IDs first, so run this command to extract that information:

```shell
$ doctl databases list
```

Then go ahead and delete both of them using the resource identifiers returned by the previous command:

```shell
$ doctl databases delete <postgres-id>
$ doctl databases delete <redis-id>
```
Finally, open a browser window, navigate to the DigitalOcean web interface (https://cloud.digitalocean.com), and go through the different categories of cloud
resources in your account to verify that there’s no outstanding services. If there are, delete them. There could be load balancers or persistent volumes created
as a side effect of creating a cluster or a database, and that may not have been deleted by the previous commands.

#### Configuring CPU and memory for Spring Boot containers
When dealing with containerized applications, it's best to assign resource limits explicitly. Containers are isolated contexts leveraging Linux features, like
namespaces and cgroups, to partition and limit resources among processes. However, suppose you don't specify any resource limits. In that case, each container
will have access to the whole CPU set and memory available on the host machine, with the risk of some of them taking up more resources than they should and
causing other containers to crash due to a lack of resources.

For JVM-based applications like Spring Boot, defining CPU and memory limits is even more critical because they will be used to properly size items like JVM
thread pools, heap memory, and non-heap memory. Configuring those values has always been a challenge for Java developers, and it's critical since they directly
affect application performance. Fortunately, if you use the Paketo implementation of Cloud Native Buildpacks included in Spring Boot, you don't need to worry
about that. When you packaged the Catalog Service application with Paketo, a Java Memory Calculator component was included automatically. When you run the
containerized application, that component will configure the JVM memory based on the resource limits assigned to the container. If you don't specify any limits,
the results will be unpredictable.

There's also an economic aspect to consider. If you run your applications in a public cloud, you're usually charged based on how many resources you consume.
Consequently, you'll probably want to be in control of how much CPU and memory each container can use to avoid nasty surprises when the bill arrives.

When it comes to orchestrators like Kubernetes, there's another critical issue related to resources that you should consider. Kubernetes schedules Pods to be
deployed in any of the cluster nodes. But what if a Pod is assigned to a node that has insufficient resources to run the container correctly? The solution is to
declare the minimum CPU and memory a container needs to operate (resource requests). Kubernetes will use that information to deploy a Pod to a specific node
only if it can guarantee the container will get at least the requested resources.

Resource requests and limits are defined per container. You can specify both requests and limits in a Deployment manifest.

Even though we're considering a production scenario, we'll use low values to optimize the resource usage in your cluster and avoid incurring additional costs.
In a real-world scenario, you might want to analyze more carefully which requests and limits would be appropriate for your use case.

##### OPTIMIZING CPU AND MEMORY FOR SPRING BOOT APPLICATIONS
The memory request and limit are the same, but that's not true for the CPU.
The amount of CPU available to a container directly affects the startup time of a JVM-based application like Spring Boot. In fact, the JVM leverages as much
CPU as available to run the initialization tasks concurrently and reduce the startup time. After the startup phase, the application will use much lower CPU
resources.

A common strategy is to define the CPU request (resources.requests.cpu) with the amount the application will use under normal conditions, so that it's always
guaranteed to have the resources required to operate correctly. Then, depending on the system, you may decide to specify a higher CPU limit or omit it entirely
(resources.limits.cpu) to optimize performance at startup so that the application can use as much CPU as available on the node at that moment.

CPU is a compressible resource, meaning that a container can consume as much of it as is available. When it hits the limit (either because of
resources.limits.cpu or because there's no more CPU available on the node), the operating system starts throttling the container process, which keeps running
but with possibly lower performance. Since it's compressible, not specifying a CPU limit can be a valid option sometimes to gain a performance boost. Still,
you'll probably want to consider the specific scenario and evaluate the consequences of such a decision.

Unlike CPU, memory is a non-compressible resource. If a container hits the limit (either because of resources.limits.memory or because there's no more memory
available on the node), a JVM-based application will throw the dreadful OutOfMemoryError, and the operating system will terminate the container process with an
OOMKilled (OutOfMemory killed) status. There is no throttling. Setting the correct memory value is, therefore, particularly important. There is no shortcut to
inferring the proper configuration; you must monitor the application running under normal conditions. That's true for both CPU and memory.

Growing and shrinking the container memory dynamically will affect the application’s performance, since the heap memory is dynamically allocated based on the
memory available to the container. Using the same value for the request and the limit ensures that a fixed amount of memory is always guaranteed, resulting in
better JVM performance. Furthermore, it allows the Java Memory Calculator provided by the Paketo Buildpacks to configure the JVM memory in the most efficient way.

##### CONFIGURING RESOURCES FOR THE JVM
The Paketo Buildpacks used by the Spring Boot plugin for Gradle/Maven provide a Java Memory Calculator component when building container images for Java
applications.

In a production scenario, the default configuration is a good starting point for most applications. However, it can be too resource-demanding for local
development or demos. One way to make the JVM consume fewer resources is to lower the default 250 JVM thread count for imperative applications. Reactive
applications are already configured with fewer threads, since they are much more resource-efficient than their imperative counterparts.

The Paketo team is working on extending the Java Memory Calculator to provide a low-profile mode, which will be helpful when working locally or on low-volume
applications. In the future, it will be possible to control the memory configuration mode via a flag rather than having to tweak the individual parameters. You
can find more information about this feature on the GitHub project for Paketo Buildpacks (http://mng.bz/5Q87).

The JVM has two main memory areas: heap and non-heap. The Calculator focuses on computing values for the different non-heap memory parts according to a specific
formula. The remaining memory resources are assigned to the heap.

If the default configuration is not good enough, you can customize it as you prefer. If your application required more direct memory than was configured by
default. In that case, you can use the standard -XX:MaxDirectMemorySize=50M JVM setting via the JAVA_TOOL_OPTIONS environment variable and increase the maximum
size for the direct memory from 10 MB to 50 MB. If you customize the size of a specific memory region, the Calculator will adjust the allocation of the
remaining areas accordingly.

#### Deploying Spring Boot in production
Our end goal is to automate the full process from code commit to production. Before looking into the production stage of the deployment pipeline, let's verify
that the customizations we've defined so far are correct by deploying Catalog Service in production manually.

Navigate to the production overlay folder for Catalog Service (kubernetes/applications/catalog-service/production), and run the following command to deploy the
application via Kustomize:

```shell
$ kubectl apply -k .
```

You can follow their progress and see when the two application instances are ready to accept requests and verify the application is running correctly and
returning the value specified in the ConfigMap for the prod spring profile instead of default one by running this commands:

```shell
$ kubectl get pods -l app=catalog-service --watch
$ kubectl logs deployment/catalog-service
$ kubectl port-forward service/catalog-service 9001:80
$ http :9001/
```

When you're done verifying, delete the deployment by running the following command from the production overlay folder for Catalog Service:

```shell
$ kubectl delete -k .
```

Kubernetes provides the infrastructure for implementing different types of deployment strategies. When we update our application manifests with a new release
version and apply them to the cluster, Kubernetes performs a **rolling update**. This strategy consists in incrementally updating Pod instances with new ones
and guarantees zero downtime for the user.

By default, Kubernetes adopts the rolling update strategy, but there are other techniques that you can employ based on the standard Kubernetes resources, or
you can rely on a tool like Knative. For example, you might want to use **blue/green deployments**, consisting of deploying the new version of the software in
a second production environment. By doing that, you can test one last time that everything runs correctly. When the environment is ready, you move the traffic
from the first (**blue**) to the second (**green**) production environment (http://mng.bz/WxOl).

Another deployment technique is the **canary release**. It's similar to the blue/green deployment, but the traffic from the blue to the green environment is
moved gradually over time. The goal is to roll out the change to a small subset of users first, perform some verifications, and then do the same for more and
more users until everyone is using the new version (http://mng.bz/8Mz5). Both blue/green deployments and canary releases provide a straightforward way to roll
back changes.

### Deployment pipeline: Production stage
After a release candidate has gone through the commit and acceptance stages, we are confident enough to deploy it to production. The production stage can be
triggered manually or automatically, depending on whether you'd like to achieve continuous deployment.

**Continuous delivery** is a software development discipline where you build software in such a way that the software can be released to production at any time
(http://mng.bz/7yXV). The key part is understanding that the software can be released to production, but it doesn't have to. That's a common source of confusion
between continuous delivery and continuous deployment. If you also want to take the newest release candidate and deploy it to production automatically, then you
would have continuous deployment.

The production stage consists of two main steps:
1. Update the deployment scripts (in our case, the Kubernetes manifests) with the new release version.
2. Deploy the application to the production environment.

At the end of the acceptance stage, we have a release candidate that's proven to be ready for production. After that, we need to update the Kubernetes manifests
in our production overlay with the new release version. When we're keeping both the application source code and deployment scripts in the same repository, the
production stage could be listening to a specific event published by GitHub whenever the acceptance stage completes successfully, much like how we configured
the flow between the commit and acceptance stages.

In our case, we are keeping the deployment scripts in a separate repository, which means that whenever the acceptance stage workflow completes its execution in
the application repository, we need to notify the production stage workflow in the deployment repository. GitHub Actions provides the option of implementing
this notification process via a custom event. Let's see how it works.

Open your Catalog Service project (catalog-service), and go to the acceptance-stage.yml file within the .github/workflows folder. After all the acceptance tests
have run successfully, we have to define a final step that will send a notification to the bookshop-deployment repository and ask it to update the Catalog
Service production manifests with the new release version. That will be the trigger for the production stage.

By default, GitHub Actions doesn't allow you to trigger workflows located in other repositories, even if they both belong to you or your organization.
Therefore, we need to provide the **repository-dispatch** action with an access token that grants it such permissions. The token can be a personal access token
(PAT).

Go to your GitHub account, navigate to Settings > Developer Settings > Personal Access Token, and choose Generate New Token. Input a meaningful name, and
assign it the **workflow** scope to give the token permissions to trigger workflows in other repositories. Finally, generate the token and copy its value.
GitHub will show you the token value only once.

Next, go to your Catalog Service repository on GitHub, navigate to the Settings tab, and then select Secrets > Actions. On that page, choose New Repository
Secret, name it DISPATCH_TOKEN (the same name we used in acceptance-stage.yml), and input the value of the PAT you generated earlier. Using the Secrets feature
provided by GitHub, we can provide the PAT securely to the acceptance stage workflow.

**WARNING** - When using actions from the GitHub marketplace, you should handle them like any other third-party application and manage the security risks
accordingly. In the acceptance stage, we provide an access token to a third party action with permissions to manipulate repositories and workflows. You
shouldn't do that light-heartedly. In this case, I trusted the author of the action and decided to trust the action with the token.

The production stage is triggered whenever the acceptance stage from an application repository dispatches an **app_delivery** event. The event itself contains
contextual information about the application name, image, and version for the newest release candidate. Since the application-specific information is
parameterized, we can use this workflow for all the applications of the Bookshop system, not only Catalog Service.

The first job of the production stage is updating the production Kubernetes manifests with the new release version. This job will consist of three steps:
1. Check out the bookshop-deployment source code.
2. Update the production Kustomization with the new version for the given application.
3. Commit the changes to the bookshop-deployment repository.

**The commit stage goes from code commit to a release candidate, which goes through the acceptance stage. If it passes all the tests, the production stage updates
the deployment manifests.**

![](https://github.com/sanjayrawat1/bookshop/blob/main/bookshop-deployment/diagrams/deployment-pipeline-code-commit-to-production-deployment.drawio.svg)

### Continuous deployment with GitOps
Traditionally, continuous deployment is implemented by adding a further step to the production stage of the deployment pipeline. This additional step would
authenticate with the target platform (such as a virtual machine or a Kubernetes cluster) and deploy the new version of the application. In recent years, a
different approach has become more and more popular: GitOps.

GitOps is a set of practices for operating and managing software systems, enabling continuous delivery and deployment while ensuring agility and reliability.
Compared to the traditional approach, GitOps favors decoupling between delivery and deployment. Instead of having the pipeline pushing deployments to the
platform, it's the platform itself pulling the desired state from a source repository and performing deployments. In the first case, the deployment step is
implemented within the production stage workflow. In the second case, the deployment is still theoretically considered part of the production stage, but the
implementation differs.

GitOps doesn't enforce specific technologies, but it's best implemented with Git and Kubernetes. The GitOps Working Group, part of the CNCF, defines GitOps in
terms of four principles (https://opengitops.dev):

1. **Declarative** — A system managed by GitOps must have its desired state expressed declaratively.
    * Working with Kubernetes, we can express the desired state via YAML files (manifests).
    * Kubernetes manifests declare what we want to achieve, not how. The platform is responsible for finding a way to achieve the desired state.
2. **Versioned and immutable** — Desired state is stored in a way that enforces immutability, versioning and retains a complete version history.
    * Git is the preferred choice for ensuring the desired state is versioned and the whole history retained. That makes it possible, among other things, to
      roll back to a previous state with ease.
    * The desired state stored in Git is immutable and represents the single source of truth.
3. **Pulled automatically** — Software agents automatically pull the desired state declarations from the source.
    * Examples of software agents (GitOps agents) are Flux (https://fluxcd.io), Argo CD (https://argoproj.github.io/cd), and kapp-controller (https://carvel.dev/kapp-controller).
    * Rather than granting CI/CD tools like GitHub Actions full access to the cluster or running commands manually, we grant the GitOps agent access to a source
      like Git so that it pulls changes automatically.
4. **Continuously reconciled** — Software agents continuously observe actual system state and attempt to apply the desired state.
    * Kubernetes is composed of controllers that keep observing the system and ensuring the actual state of the cluster matches the desired state.
    * On top of that, GitOps ensures that it’s the right desired state to be considered in the cluster. Whenever a change is detected in the Git source, the
      agent steps up and reconciles the desired state with the cluster.

![](https://github.com/sanjayrawat1/bookshop/blob/main/bookshop-deployment/diagrams/deployment-pipeline-with-gitops-principles.drawio.svg)

We've applied the first two already. We expressed the desired state for our applications declaratively using Kubernetes manifests and Kustomize. And we stored
the desired state in a Git repository (bookshop-deployment), making it versioned and immutable. We are still missing a software agent that automatically pulls
the desired state declarations from the Git source and continuously reconciles them inside the Kubernetes cluster, therefore achieving continuous deployment.

We'll start by installing Argo CD (https://argo-cd.readthedocs.io), a GitOps software agent. Then we'll configure it to complete the final step of the
deployment pipeline and let it monitor our bookshop-deployment repository. Whenever there's a change in the application manifests, Argo CD will apply the
changes to our production Kubernetes cluster.

Let's start by installing the Argo CD CLI. Refer to the project website for installation instructions (https://argo-cd.readthedocs.io). If you are on macOS or
Linux, you can use Homebrew as follows:

```shell
$ brew install argocd
```

Verify Kubernetes CLI is still configured to access the production cluster on DigitalOcean. You can check that with **kubectl config current-context**. If you
need to change the context, you can run **kubectl config use-context <context-name>**. A list of all the contexts available can be retrieved from **kubectl
config get-contexts**.

Navigate to the kubernetes/platform/production/argocd folder, and run the following command to deploy Argo CD to production Kubernetes cluster:

```shell
$ ./deploy.sh
```

During the installation, Argo CD will have autogenerated a password for the admin account (the username is admin). Run the following command to fetch the
password value (it will take a few seconds before the value is available):

```shell
$ kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```

To identify the external IP address assigned to Argo CD server, run following command:

```shell
$ kubectl -n argocd get service argocd-server
```

The platform might take a few minutes to provision a load balancer for Argo CD. During the provisioning, the EXTERNAL-IP column will show a <pending> status.
Wait and try again until an IP address is shown.

Since the Argo CD server is now exposed via a public load balancer, we can use the external IP address to access its services, we'll use CLI, but you can
achieve the same results by opening <argocd-external-ip> (the IP address assigned to your Argo CD server) in a browser window. The username is **admin**, and
the password is the one you fetched earlier.

```shell
$ argocd login <argocd-external-ip>
```

Now we'll configure Argo CD to monitor the production overlay for Catalog Service and synchronize it with the production cluster whenever it detects a change
in the repository. In other words, Argo CD will continuously deploy new versions of Catalog Service as made available by the deployment pipeline.

```shell
argocd app create catalog-service --repo https://github.com/sanjayrawat1/bookshop.git \
  --path bookshop-deployment/kubernetes/applications/catalog-service/production \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default \
  --sync-policy auto \
  --auto-prune
```

##### Explanation of above command
| command breakdown                                                             | description                                                                                                                          |
|-------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| argocd app create catalog-service                                             | Create a catalog-service application in Argo CD.                                                                                     |
| --repo https://github.com/sanjayrawat1/bookshop.git                           | The GIT repository to monitor for changes.                                                                                           |
| --path bookshop-deployment/kubernetes/applications/catalog-service/production | The folder to monitor for changes within the configured repository.                                                                  |
| --dest-server https://kubernetes.default.svc                                  | The kubernetes cluster where the application should be deployed. We are using the default cluster configured in the kubectl context. |
| --dest-namespace default                                                      | The namespace where the application should be deployed. We are using the "default" namespace.                                        |
| --sync-policy auto                                                            | Configures Argo CD to automatically reconcile the desired state in the Git repo with the actual state in the cluster.                |
| --auto-prune                                                                  | Configures Argo CD to delete old resources after a synchronization automatically.                                                    |

You can verify the status of the continuous deployment of Catalog Service with the following command:

```shell
argocd app get catalog-service
```

Argo CD has automatically applied the production overlay for Catalog Service (kubernetes/applications/catalog-service/production) to the cluster.

Once all the resources listed by the previous command have the Synced status, we can verify that the application is running correctly. The application is not
exposed outside the cluster yet, but you can use the port-forwarding functionality to forward traffic from your local environment on port 9001 to the Service
running in the cluster on port 80:

```shell
$ kubectl port-forward service/catalog-service 9001:80
```

Next, call the root point exposed by the application. We expect to get the value we configured for the bookshop.greeting property in the Catalog Service
production overlay.

```shell
$ http :9001/
```

Update the value of bookshop.greeting property in application-prod.yml file. Then commit and push the changes. By default, Argo CD checks the Git repository for
changes every three minutes. It will notice the change and apply the Kustomization again, resulting in a new ConfigMap being generated by Kustomize and a
rolling restart of the Pods to refresh the configuration.

Using Argo CD CLI, register each application as we did for catalog-service. Once the whole system is deployed, we can access the applications as intended: via
the Edge Service. The platform automatically configures a load balancer with an external IP address whenever we deploy an Ingress resource. Let's discover the
external IP address for the Ingress sitting in front of edge-service by following command:

```shell
$ kubectl get ingress
```

Using the Ingress external IP address, you can access the Bookshop from the public internet.
Ensure that you can't access the Actuator endpoints by visiting <ip-address>/management/health, for example. NGINX, the technology that powers the Ingress
Controller, will reply with a 403 response.

#### Summary
* The idea behind continuous delivery is that an application is always in releasable state.
* When the delivery pipeline completes its execution, you'll obtain an artifact (the container image) you can use to deploy the application in production.
* When it comes to continuous delivery, each release candidate should be uniquely identifiable.
* Using the Git commit hash, you can ensure uniqueness, traceability, and automation. Semantic versioning can be used as the display name communicated to users
  and customers.
* At the end of the commit stage, a release candidate is delivered to the artifact repository. Next, the acceptance stage deploys the application in a
  production-like environment and runs functional and non-functional tests. If they all succeed, the release candidate is ready for production.
* The Kustomize approach to configuration customization is based on the concepts of bases and overlays. Overlays are built on top of base manifests and
  customized via patches.
* You saw how to define patches for customizing environment variables, Secrets mounted as volumes, CPU and memory resources, ConfigMaps, and Ingress.
* The final part of a deployment pipeline is the production stage, where the deployment manifests are updated with the newest release version and ultimately
  deployed.
* Deployment can be push-based or pull-based.
* GitOps is a set of practices for operating and managing software systems.
* GitOps is based on four principles according to which a system deployment should be declarative, versioned and immutable, pulled automatically, and
  continuously reconciled.
* Argo CD is a software agent running in a cluster that automatically pulls the desired state from a source repository and applies it to the cluster whenever
  the two states diverge. That's how we implemented continuous deployment.


#### Deploying serverless applications with Knative
Knative is a Kubernetes-based platform to deploy and manage modern serverless workloads (https://knative.dev). It's a CNCF project that you can use to deploy
standard containerized workloads and event-driven applications. The project offers a superior user experience to developers and higher abstractions that make
it simpler to deploy applications on Kubernetes.

You can decide to run your own Knative platform on top of a Kubernetes cluster or choose a managed service offered by a cloud provider, such as VMware Tanzu
Application Platform, Google Cloud Run, or Red Hat OpenShift Serverless. Since they are all based on open source software and standards, you could migrate
from Google Cloud Run to VMware Tanzu Application Platform without changing your application code and with minimal changes to your deployment pipeline.

The Knative project consists of two main components: Serving and Eventing.
* **Knative Serving** is for running serverless workloads on Kubernetes. It takes care of autoscaling, networking, revisions, and deployment strategies while
letting engineers focus on the application business logic.
* **Knative Eventing** provides management for integrating applications with event sources and sinks based on the CloudEvents specification, abstracting
backends like RabbitMQ or Kafka.

Our focus will be on using Knative Serving to run serverless workloads while avoiding vendor lock-in.

**NOTE** Originally, Knative consisted of a third component called **Build** that subsequently became a standalone product, renamed Tekton (https://tekton.dev)
and donated to the Continuous Delivery Foundation (https://cd.foundation). Tekton is a Kubernetes-native framework for building deployment pipelines that
support continuous delivery. For example, you could use Tekton instead of GitHub Actions.

##### Setting up a local Knative platform
Since Knative runs on top of Kubernetes, we first need a cluster. Create one with minikube by running following command:

```shell
$ minikube start --profile knative
```

Next, install Knative. Navigate to the kubernetes/platform/development/knative folder, and run the following command to install Knative on local Kubernetes
cluster:

```shell
$ ./install-knative.sh
```

The Knative project provides a convenient CLI tool that you can use to interact with Knative resources in a Kubernetes cluster. On macOS and Linux, you can
install it with Homebrew as follows

```shell
$ brew install kn
```

##### Deploying applications with the Knative CLI
Knative provides a few different options for deploying applications. In production, we'll want to stick to a declarative configuration as we did for standard
Kubernetes deployments and rely on a GitOps flow to reconcile the desired state (in a Git repository) and actual state (in the Kubernetes cluster).

When experimenting or working locally, we can also take advantage of the Knative CLI to deploy applications in an imperative way.

Run the following command to deploy Quote Function. The container image is the one published by the commit stage workflow:

```shell
$ kn service create quote-function \
    --image ghcr.io/sanjayrawat1/quote-function \
    --port 9102
```

**The Knative command for creating a Service from a container image. Knative will take care of creating all the resources necessary to deploy the applications
on Kubernetes.**

##### Explanation of above command
| command breakdown                           | description                               |
|---------------------------------------------|-------------------------------------------|
| kn service create                           | Create a Knative service.                 |
| quote-function                              | The name of the Knative service.          |
| --image ghcr.io/sanjayrawat1/quote-function | The container image to run.               |
| --port 9102                                 | The port that the application listens to. |

The command will initialize a new **quote-function** service in the default namespace on Kubernetes. It will return the public URL through which the application
is exposed in the output message of above command.

To test it out! First we need to open a tunnel to the cluster with minikube, by running following command:

```shell
$ minikube tunnel --profile knative
```

Now, call the application at the root endpoint to fetch the complete list of quotes. The URL to call is the same one returned by the previous command:
(http://quote-function.default.127.0.0.1.sslip.io), which is in the format <service-name>.<namespace>.<domain>:

```shell
$ http http://quote-function.default.127.0.0.1.sslip.io
```

Knative takes care of scaling the application without any further configuration. For each request, it determines whether more instances are required. When an
instance stays idle for a specific time period (30 seconds, by default), Knative will shut it down. If no request is received for more than 30 seconds, Knative
will scale the application to zero, meaning there will be no instances of Quote Function running. When a new request is eventually received, Knative starts a
new instance and uses it to handle the request.

Using an open source platform like Knative has the advantage of letting you migrate your applications to another cloud provider without any code changes. You
can even use the same deployment pipeline as-is, or with minor modifications.

To delete the Quote Function instance you created previously, run the following command:

```shell
$ kn service delete quote-function
```
