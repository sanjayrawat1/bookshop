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

![From a container, you can expose as many ports as you want. For Catalog Service, expose both the server port and the debug port.](https://github.com/sanjayrawat1/bookshop/blob/main/bookshop-deployment/debug-spring-boot-containers.drawio.svg "Debugging Spring Boot containers")

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
