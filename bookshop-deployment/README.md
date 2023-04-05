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
$ kubectl create secret generic bookshop-postgres-catalog-credentials
 --from-literal=spring.datasource.url=jdbc:postgresql://<postgres_host>:<postgres_port>/bookshop_catalog
  --from-literal=spring.datasource.username=<postgres_username>
   --from-literal=spring.datasource.password=<postgres_password>
```

Similarly, create a Secret for Order Service. Pay attention to the slightly different syntax required by Spring Data R2DBC for the URL:

```shell
$ kubectl create secret generic bookshop-postgres-order-credentials
 --from-literal="spring.flyway.url=jdbc:postgresql://<postgres_host>:<postgres_port>/bookshop_order"
  --from-literal="spring.r2dbc.url=r2dbc:postgresql://<postgres_host>:<postgres_port>/bookshop_order?ssl=true&sslMode=require"
   --from-literal=spring.r2dbc.username=<postgres_username> --from-literal=spring.r2dbc.password=<postgres_password>
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
$ kubectl create secret generic bookshop-redis-credentials
 --from-literal=spring.redis.host=<redis_host>
  --from-literal=spring.redis.port=<redis_port>
   --from-literal=spring.redis.username=<redis_username>
    --from-literal=spring.redis.password=<redis_password>
     --from-literal=spring.redis.ssl=true
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
**http://<external-ip>/admin**, and log in with the credentials returned by the deployment script.

Now that you have a public DNS name for Keycloak, you can define a couple of Secrets to configure the Keycloak integration in Edge Service (OAuth2 Client),
Catalog Service, and Order Service (OAuth2 Resource Servers). Navigate to the kubernetes/platform/production/keycloak folder, and run the following command to
create the Secrets that the applications will use to integrate with Keycloak. Remember to replace <external-ip> with the external IP address assigned to your
Keycloak server:

```shell
$ ./create-secrets.sh http://<external-ip>/realms/Bookshop
```

#### Running Polar UI
Polar UI is a single-page application built with Angular and served by NGINX.

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
