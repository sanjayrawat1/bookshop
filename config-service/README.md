# config-service

#### Centralized configuration management with Spring Cloud Config Server

![A centralized configuration server manages external properties for many applications across all environments.](https://github.com/sanjayrawat1/bookshop/blob/main/config-service/centralized-configuration-server.drawio.svg "Centralized configuration management with Spring Cloud Config Server")

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
