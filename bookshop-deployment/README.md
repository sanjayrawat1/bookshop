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
