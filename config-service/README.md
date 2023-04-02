# config-service

#### Centralized configuration management with Spring Cloud Config Server

![A centralized configuration server manages external properties for many applications across all environments.](https://github.com/sanjayrawat1/bookshop/blob/main/config-service/diagrams/centralized-configuration-server.drawio.svg "Centralized configuration management with Spring Cloud Config Server")

A centralized configuration server manages external properties for many applications across all environments. Using Git to store the configuration data.

Spring cloud config server works seamlessly with Spring Boot applications, providing properties in their native formats through a REST API.

Make an HTTP GET request to /catalog-service/default:

`$ http :8888/catalog-service/default`

The result is the configuration that's returned when no Spring profile is active.

To fetch the configuration for the scenario where **prod** profile is active as follows:

`$ http :8888/catalog-service/prod`

The result is the configuration defined for the Catalog Service application in catalog-service.yml and catalog-service-prod.yml,
where the latter takes precedence over the former because the **prod** profile is specified.

Spring Cloud Config Server exposes properties through a series of endpoints using different combinations of the {application},
{profile}, and {label} parameters:
1. /{application}/{profile}[/{label}]
2. /{application}-{profile}.yml
3. /{label}/{application}-{profile}.yml
4. /{application}-{profile}.properties
5. /{label}/{application}-{profile}.properties

You won’t need to call these endpoints from your application when using Spring Cloud Config Client (it does that for you), but it’s useful to know
how the server exposes configuration data. A configuration server built with Spring Cloud Config Server exposes a standard REST API that any application can
access over a network. You can use the same server for applications built with other languages and frameworks and use the REST API directly.

#### Containerizing Spring Boot with Cloud Native Buildpacks
To transform application source code into image and to publish the image directly to a container registry we will use spring boot plugin.
Build and publish the image by running below commands:

`$ ./gradlew bootBuildImage --imageName ghcr.io/<your_github_username>/config-service --publishImage -PregistryUrl=ghcr.io -PregistryUsername=<your_github_username> -PregistryToken=<your_github_token>`

#### Refreshing configuration at runtime with Spring Cloud Bus
Imagine you have deployed your Spring Boot applications in a cloud environment like Kubernetes. During the startup phase, each application loaded its
configuration from an external config server, but at some point you decide to make changes in the config repo. How can you make the applications aware of the
configuration changes and have them reload it?

You could trigger a configuration refresh operation by sending a POST request to the /actuator/refresh endpoint provided by Spring Boot Actuator. A request to
that endpoint results in a RefreshScopeRefreshedEvent event inside the application context. All beans marked with @ConfigurationProperties or @RefreshScope
listen to that event and get reloaded when it happens.

You tried the refresh mechanism on Catalog Service, and it worked fine, since it was just one application, and not even replicated. How about in production?
Considering the distribution and scale of cloud native applications, sending an HTTP request to all the instances of each application might be a problem.
Automation is a crucial part of any cloud native strategy, so we need a way to trigger a RefreshScopeRefreshedEvent event in all of them in one shot. There are
a few viable solutions. Using Spring Cloud Bus is one of them.

Spring Cloud Bus (https://spring.io/projects/spring-cloud-bus) establishes a convenient communication channel for broadcasting events among all the application
instances linked to it. It provides an implementation for AMQP brokers (like RabbitMQ) and Kafka, relying on the Spring Cloud Stream.

Any configuration change consists of pushing a commit to the config repo. It would be convenient to set up some automation to make Config Service refresh the
configuration when a new commit is pushed to the repository, completely removing the need for manual intervention. Spring Cloud Config provides a Monitor
library that makes that possible. It exposes a /monitor endpoint that can trigger a configuration change event in Config Service, which then would send it over
the Bus to all the listening applications. It also accepts arguments describing which files have been changed and supports receiving push notifications from the
most common code repository providers like GitHub, GitLab, and Bitbucket. You can set up a webhook in those services to automatically send a POST request to
Config Service after each new push to the config repo.

Spring Cloud Bus solves the problem of broadcasting a configuration change event to all connected applications. With Spring Cloud Config Monitor, we can further
automate the refresh and make it happen after a configuration change is pushed to the repository backing the config server.

![](https://github.com/sanjayrawat1/bookshop/blob/main/config-service/diagrams/refreshing-configuration-at-runtime.drawio.svg)
