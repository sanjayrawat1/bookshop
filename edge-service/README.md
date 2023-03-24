# edge-service

Edge servers are applications at the edge of a system that implement aspects like API gateways and cross-cutting concerns.

### The architecture of the Bookshop system after adding Edge Service and Redis.

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/bookshop-system-after-edge-service.drawio.svg)

We will use the Spring Cloud Gateway to build the Edge Service application and implement an API gateway and also use it as a central place to handle
cross-cutting concerns, such as Security, Monitoring, and Resilience.

An API gateway provides an entry point to the system. In distributed systems like microservices, that's a convenient way to decouple the clients from any
changes to the internal services' APIs. You're free to change how your system is decomposed into services and their APIs, relying on the fact that the gateway
can translate from a more stable, client-friendly, public API to the internal one.

However, it's important to remember that an edge server adds complexity to the system. It's another component to build, deploy, and manage in production.
It also adds a new network hop to the system, so the response time will increase. That's usually an insignificant cost, but you should keep it in mind.
Since the edge server is the entry point to the system, it's at risk of becoming a single point of failure. As a basic mitigation strategy, you should deploy
at least two replicas of an edge server.

Spring Cloud Gateway provides three main building blocks:
* **Route**: This is identified by a unique ID, a collection of predicates for deciding whether to follow the route, a URI for forwarding the request if the
predicates allow, and a collection of filters that are applied either before or after forwarding request downstream.
* **Predicate**: This matches anything from the HTTP request, including path, host, headers, query parameters, cookies and body.
* **Filter**: This modifies an HTTP request or response before or after forwarding the request to the downstream service.

Requests are matched against predicates, filtered, and finally forwarded to the downstream service, which replies with a response that goes through
another set of filters before being returned to the client.

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/routing-in-spring-cloud-gateway.drawio.svg)


By default, the Netty HTTP client used by Spring Cloud Gateway is configured with an elastic connection pool to increase the number of concurrent connections
dynamically as the workload increases. Depending on the number of requests your system receives simultaneously, you might want to switch to a fixed connection
pool, so you have more control over the number of connections.

#### Processing requests and responses through filters
Routes and predicate alone make the application act as a proxy, but it's filter that make Spring Cloud Gateway really powerful.

Pre-filters can run before forwarding incoming requests to a downstream application. They can be used for:
* Manipulating the request headers.
* Applying rate limiting and circuit breaking.
* Defining retries and timeouts for the proxied requests.
* Triggering an authentication flow with OAuth2 and OpenID Connect.

Post-filters can apply to outgoing responses after they are received from the downstream application and before sending them back to the client.
They can be used for:
* Setting security headers.
* Manipulating the response body to remove sensitive information.

Spring Cloud Gateway comes bundled with many filters that you can use to perform different actions, including adding headers to a request, configuring a
circuit breaker, saving the web session, retrying the request on failure, or activating a rate limiter.

##### Using the retry filter
We want to define retry attempts for all GET requests whenever the error is in the 5xx range (SERVER_ERROR). We don't want to retry requests when the error
is in the 4xx range. We can also list the exceptions for which a retry should be attempted, such as IOException and TimeoutException.
You shouldn't keep retrying requests one after the other. You should use backoff strategy instead. By default, the delay is computed using the formula
firstBackoff * (factor ^ n). If you set the basedOnPreviousValue parameter to true, the formula will be prevBackoff * factor.

#### Fault tolerance with Spring Cloud Circuit Breaker and Resilience4J
The retry pattern is useful when a downstream service is momentarily unavailable. But what if it stays down for more than a few instants? At that point we
could stop forwarding requests to it until we're sure that it's back. Continuing to send requests won't be beneficial for the caller or the callee. In that
scenario, the circuit breaker pattern comes in handy.

Resilience is a critical property of cloud native applications. One of the principles for achieving resilience is blocking a failure from cascading and
affecting other components. Consider a distributed system where application X depends on application Y. If application Y fails, will application X fail, too?
A circuit breaker can block a failure in one component from propagating to the others depending on it, protecting the rest of the system. That is accomplished
by temporarily stopping communication with the faulty component until it recovers.

In the world of distributed systems, you can establish circuit breakers at the integration points between components. Think about Edge Service and Catalog
Service. In a typical scenario, the circuit is closed, meaning that the two services can interact over the network. For each server error response returned by
Catalog Service, the circuit breaker in Edge Service would register the failure. When the number of failures exceeds a certain threshold, the circuit breaker
trips, and the circuit transitions to open.

While the circuit is open, communications between Edge Service and Catalog Service are not allowed. Any request that should be forwarded to Catalog Service
will fail right away. In this state, either an error is returned to the client, or fallback logic is executed. After an appropriate amount of time to permit
the system to recover, the circuit breaker transitions to a half-open state, allowing the next call to Catalog Service to go through. That is an exploratory
phase to check if there are still issues in contacting the downstream service. If the call succeeds, the circuit breaker is reset and transitions to closed.
Otherwise, it goes back to being open.

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/circuit-breaker-state.drawio.svg)

A circuit breaker ensures fault tolerance when a downstream service exceeds the maximum number of failures allowed by blocking any communication between
upstream and downstream services. The logic is based on three states: closed, open, and half-open.

When a circuit breaker switches to the open state, we'll want at least to degrade the service level gracefully and make the user experience as pleasant as
possible by defining the fallback REST APIs.

For simplicity, the fallback for GET requests returns an empty string, whereas the fallback for POST requests returns an HTTP 503 error. In a real scenario,
you might want to adopt different fallback strategies depending on the context, including throwing a custom exception to be handled from the client or
returning the last value saved in the cache for the original request.

#### Combining Resilience Patterns - circuit breakers, retries and time limiters
When you combine multiple resilience patterns, the sequence in which they are applied is fundamental. Spring cloud gateway takes care of applying the
TimeLimiters first (or the timeout on the HTTP client), the CircuitBreaker filter, and finally retry.

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/multiple-resilience-pattern-sequence-when-combined.drawio.svg)

You can verify the result of applying these patterns to Edge Service by using a tool like Apache Benchmark (https://httpd.apache.org/docs/2.4/programs/ab.html).
If you’re using macOS or Linux, you might have this tool already installed. Otherwise, you can follow the instructions on the official website and install it.

To verify the result, run below command and see the output:

`$ ab -n 21 -c 1 -m POST http://localhost:9000/orders`

`$ ab -n 21 -c 1 -m POST http://localhost:9000/books`

For books API call, all requests have been forwarded to the fallback endpoint, so the client didn't experience any errors.

#### Request rate limiting
Rate limiting is a pattern used to control the rate of traffic sent to or received from an application, helping to make your system more resilient and robust.
In the context of HTTP interactions, you can apply this pattern to control outgoing or incoming network traffic using client-side and server-side rate limiters,
respectively.
* Client-side rate limiters are for constraining the number of requests sent to a downstream service in a given period. It's a useful pattern to adopt when
third-party organizations like cloud providers manage and offer the downstream service. You’ll want to avoid incurring extra costs for having sent more
requests than are allowed by your subscription.
* Server-side rate limiters are for constraining the number of requests received by an upstream service (or client) in a given period. This pattern is handy
when implemented in an API gateway to protect the whole system from overloading or from DoS attacks.

Resilience4J supports the client-side rate limiter and bulkhead patterns for both reactive and non-reactive applications. Spring Cloud Gateway supports the
server-side rate limiter pattern by using Spring Cloud Gateway and Spring Data Redis Reactive.

##### Integrating Spring with Redis
We will use Redis to back the **RequestRateLimiter** gateway filter that provides server-side rate limiting support. Depending on the requirements, you can
configure the RequestRateLimiter filter for specific routes or as a default filter.

The implementation of **RequestRateLimiter** on redis is based on the _token bucket algorithm_. Each user is assigned a bucket inside which tokens are dripped
over time at a specific rate (the replenish rate). Each bucket has a maximum capacity (the burst capacity). When a user makes a request, a token is removed
from its bucket. When there are no more tokens left, the request is not permitted, and the user will have to wait until more tokens are dripped into its bucket.
To know more about the token bucket algorithms, read this blog - https://stripe.com/blog/rate-limiters

Spring Cloud Gateway relies on Redis to keep track of the number of requests happening each second. By default, each user is assigned a bucket. However, we
will use single bucket for all requests until we introduce an authentication mechanism.

What happens if Redis becomes unavailable? Spring Cloud Gateway has been built with resilience in mind, so it will keep its service level, but the rate limiters
would be disabled until Redis is up and running again.

When the rate limiter pattern is combined with other patterns like time limiters, circuit breakers, and retries, the rate limiter is applied first. If a user's
request exceeds the rate limit, it is rejected right away.

Spring Cloud Gateway is configured to append headers with details about rate limiting to each HTTP response.
You might not want to expose this information to clients in cases where the information could help bad actors craft attacks against your system. Or you might
need different header names. Either way, you can use the `spring.cloud.gateway.redis-rate-limiter` property group to configure that behavior.

#### Distributed Session Management with Redis
Spring provides session management features with the Spring Session project. By default, session data is stored in memory, but that's not feasible in a cloud
native application. You want to keep it in an external service so that the data survives the application shutdown. Another fundamental reason for using a
distributed session store is that you usually have multiple instances of a given application. You'll want them to access the same session data to provide a
seamless experience to the user. Redis is a popular option for session management, and it's supported by Spring Session Data Redis.

We want the session to be saved in Redis before forwarding a request downstream. How can we do that? Spring Cloud Gateway provides **SaveSession** filter.

#### Managing external access with Kubernetes Ingress
Edge Service represents the entry point to the Bookshop system. However, when it's deployed in a kubernetes cluster, it's only accessible from within the
cluster itself. We used port-forwarding feature to expose a k8s service defined in a minikube cluster to your local computer. That's a useful strategy during
development, but it's not suitable for production. Using the Ingress API you can manage the external access to the application running in a k8s cluster.

##### Ingress API and Ingress Controller
We can use Service object of type **ClusterIP** to expose applications inside a k8s cluster, that's what we have done so far to make it possible for Pods to
interact with each other within the cluster.

A Service object can also be of type **LoadBalancer**, which relies on an external load balancer provisioned by the cloud provider to expose an application to the
internet.
The **LoadBalancer** Service approach involves assigning a different IP address to each service we decide to expose to the internet. Since services are directly
exposed, we don't have the chance to apply any further network configuration, such as TLS termination. Spring ecosystem provides everything we need to address
those concerns. However, since we want to run our system on Kubernetes, we can manage those infrastructural concerns at the platform level. That's where the
Ingress API comes in handy.

An Ingress is an object that manages external access to the services in a cluster, typically HTTP. Ingress may provide load balancing, SSL termination and
name-based virtual hosting. An Ingress object acts as an entry point into a Kubernetes cluster and is capable of routing traffic from a single external IP
address to multiple services running inside the cluster. We can use an Ingress object to perform load balancing, accept external traffic directed to a specific
URL, and manage the TLS termination to expose the application services via HTTPS.

Ingress objects don't accomplish anything by themselves. We use an Ingress object to declare the desired state in terms of routing and TLS termination. The
actual component that enforces those rules and routes traffic from outside the cluster to the applications inside is the **ingress controller**.

Ingress controllers are applications that are usually built using reverse proxies like NGINX, HAProxy, or Envoy. Some examples are Ambassador Emissary, Contour,
and Ingress NGINX.

In our local environment, we'll need some additional configuration to make the routing work. For the Bookshop example, we'll use Ingress NGINX
(https://github.com/kubernetes/ingress-nginx) in both environments.

Since we use minikube to manage a local Kubernetes cluster, we can rely on a built-in add-on to enable the Ingress functionality based on Ingress NGINX.
First, let’s start the bookshop local cluster:

`$ minikube start --cpus 2 --memory 4g --driver docker --profile bookshop`

Next we can enable the ingress add-on, which will make sure that Ingress NGINX is deployed to our local cluster:

`$ minikube addons enable ingress --profile bookshop`

In the end, you can get information about the different components deployed with Ingress NGINX as follows:

`$ kubectl get all -n ingress-nginx`

**-n ingress-nginx** means that we want to fetch all objects created in the ingress-nginx namespace.
A namespace is an abstraction used by Kubernetes to support isolation of groups of resources within a single cluster. Namespaces are used to organize objects
in a cluster and provide a way to divide cluster resources.

We use namespaces to keep our clusters organized and define network policies to keep certain resources isolated for security reasons. So far, we've been working
with the default namespace, and we'll keep doing that for all our Bookshop applications. However, when it comes to platform services such as Ingress NGINX,
we'll rely on dedicated namespaces to keep those resources isolated.

Navigate to the kubernetes/platform/development folder located in your bookshop-deployment repository, and run following command to deploy PostgreSQL and Redis in
your local cluster:

`$ kubectl apply -f services`

Package the Edge Service as a container image from edge-service root folder by running following command:

`$ ./gradlew bootBuildImage`

Load the artifact to the local k8s cluster:

`$ minikube image load edge-service --profile bookshop`

#### Working with Ingress objects
Edge Service takes care of application routing, but it should not be concerned with the underlying infrastructure and network configuration. Using an Ingress
resource, we can decouple the two responsibilities. Developers would maintain Edge Service, while the platform team would manage the ingress controller and the
network configuration (perhaps relying on a service mesh like Linkerd or Istio).

##### The deployment architecture of the Bookshop system after introducing an Ingress to manage external access to the cluster

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/deployment-architecture-of-bookshop-after-introducing-ingress.drawio.svg)

It's common to define Ingress routes and configurations based on the DNS name used to send the HTTP request. Since we are working locally, and assuming we
don’t have a DNS name, we can call the external IP address provisioned for the Ingress to be accessible from outside the cluster. On Linux, you can use the IP
address assigned to the minikube cluster. You can retrieve that value by running the following command:

`$ minikube ip --profile bookshop`

On macOS and Windows, the ingress add-on doesn't yet support using the minikube cluster's IP address when running on Docker. Instead, we need to use the
**minikube tunnel --profile bookshop** command to expose the cluster to the local environment, and then use the 127.0.0.1 IP address to call the cluster. This is
similar to the kubectl port-forward command, but it applies to the whole cluster instead of a specific service.

Define the Ingress object in **ingress.yml** file in **k8s** folder. After this deploy edge-service and ingress to local k8s cluster, run following command:

`$ kubectl apply -f k8s`

Verify that the Ingress object has been created correctly with the following command:

`$ kubectl get ingress`

Test that edge-service is correctly available through th Ingress. If you're on Linux, you don't need any further preparation steps. If you're on macOS or
Windows run the following command to expose your minikube cluster to your localhost:

`$ minikube tunnel --profile bookshop`

Finally, test the application:

`http 127.0.0.1/books`

When you are done trying out the deployment, you can stop and delete the local Kubernetes cluster with the following commands:

`$ minikube stop --profile bookshop`

`$ minikube delete --profile bookshop`

### Security: Authentication and SPA
Security is one of the most critical aspects of web applications and probably the one with the most catastrophic effects when done wrong. Consider security
from the beginning of each new project or feature and never letting it go until the application is retired.

Access control systems allow users access to resources only when their identity has been proven, and they have the required permissions. To accomplish that,
we need to follow three pivotal steps: identification, authentication, and authorization.

* **Identification** happens when a user (human or machine) claims an identity. In the physical world, that's when I introduce myself by stating my name. In the
digital world, I would do that by providing my username or email address.
* **Authentication** is about verifying the user's claimed identity through factors like a passport, a driver's license, a password, a certificate, or a token.
When multiple factors are used to verify the user's identity, we talk about multi-factor authentication.
* **Authorization** always happens after authentication, and it checks what the user is allowed to do in a given context.

##### Understanding the Spring Security fundamentals
Spring Security is the de facto standard for securing Spring applications, supporting imperative and reactive stacks. It provides authentication and
authorization features as well as protection against the most common attacks. The framework provides its main functionality by relying on **filters**. Let's
consider a possible requirement for adding authentication to a Spring Boot application. Users should be able to authenticate with their username and password
through a login form. When we configure Spring Security to enable such a feature, the framework adds a filter that intercepts any incoming HTTP request. If the
user is already authenticated, it sends the request through to be processed by a given web handler, such as a **@RestController** class. If the user is not
authenticated, it forwards the user to a login page and prompts for their username and password.

The filter that handles authentication runs before the one that checks for authorization because we can't verify a user's authority before knowing who it is.

The central place for defining and configuring security policies in Spring Security is a **SecurityWebFilterChain** bean. That object tells the framework which
filters should be enabled. You can build a SecurityWebFilterChain bean through the DSL provided by **ServerHttpSecurity**.

Spring Security provides several authentication strategies, including HTTP Basic, login form, SAML, and OpenID Connect.

#### Managing user accounts with Keycloak
Keycloak (www.keycloak.org) is an open source identity and access management solution developed and maintained by the Red Hat community. It offers a broad set
of features, including single sign-on (SSO), social login, user federation, multi-factor authentication, and centralized user management. Keycloak relies on
standards like OAuth2, OpenID Connect, and SAML 2.0.

You can run Keycloak locally as a standalone Java application or a container. For production, there are a few solutions for running Keycloak on Kubernetes.
Keycloak also needs a relational database for persistence. It comes with an embedded H2 database, but you'll want to replace it with an external one in
production.

You can start Keycloak container by navigating to bookshop-deployment/docker folder, and running below command:

`$ docker-compose up -d bookshop-keycloak`

Before we can start managing user accounts, we need to define security realm.

##### Defining a security realm
In Keycloak, any security aspect of an application or a system is defined in the context of a realm, a logical domain in which we apply specific security
policies. By default, Keycloak comes preconfigured with a Master realm, but you'll probably want to create a dedicated one for each product you build. Let's
create a new Bookshop realm to host any security-related aspects of the Bookshop system.

Make sure the Keycloak container is running. Then enter a bash console inside the Keycloak container:

`$ docker exec -it bookshop-keycloak bash`

We will configure the Keycloak through its **Admin CLI**, but same can be achieved by using the GUI available at http://localhost:8080.
Navigate to the folder where the Keycloak Admin CLI scripts are located:

`$ cd /opt/keycloak/bin`

The Admin CLI is protected by the username and password we defined in Docker Compose for the Keycloak container. We'll need to start an authenticated session
before running any other commands:

`$ ./kcadm.sh config credentials --server http://localhost:8080 --realm master --user user --password password`

After successful authentication, create a new security realm where all the policies associated with Bookshop will be stored.

`$ ./kcadm.sh create realms -s realm=Bookshop -s enabled=true`

##### Managing users and roles
Bookshop has two types of users: customers and employees.

* Customers can browse books and purchase them.
* Employees can also add new books to the catalog, modify the existing ones, and delete them.

To manage the different permissions associated with each type of user, let's create two roles: customer and employee. Later you'll protect application
endpoints based on those roles. It's an authorization strategy called role-based access control (RBAC).

`$ ./kcadm.sh create roles -r Bookshop -s name=employee`
`$ ./kcadm.sh create roles -r Bookshop -s name=customer`

Then create user.

`$ ./kcadm.sh create users -r Bookshop -s username=sanjay -s firstName=Sanjay -s lastName=Rawat -s enabled=true`

`$ ./kcadm.sh add-roles -r Bookshop --uusername sanjay --rolename employee --rolename customer`

Then set the password

`“$ ./kcadm.sh set-password -r Bookshop --username sanjay --new-password password`

#### Authentication with OpenID Connect, JWT, and Keycloak
Since Keycloak now manages user accounts, we could go ahead and update Edge Service to check the user credentials with Keycloak itself, rather than using its
internal storage. But what happens if we introduce different clients to the Bookshop system, such as mobile applications and IoT devices? How should the
users authenticate then? What if the bookshop employees are already registered in the company’s Active Directory (AD) and want to log in via SAML? Can we
provide a single sign-on (SSO) experience across different applications? Will the users be able to log in via their GitHub or Twitter accounts (social login)?

We could think of supporting all those authentication strategies in Edge Service as we get new requirements. However, that is not a scalable approach. A better
solution is delegating a dedicated identity provider to authenticate users following any supported strategy. Edge Service would then use that service to verify
the identity of a user without being concerned about performing the actual authentication step. The dedicated service could let users authenticate in various
ways, such as using the credentials registered in the system, through social login, or via SAML to rely on the identity defined in the company's AD

##### Authenticating users with OpenID Connect
OpenID Connect (OIDC) is a protocol that enables an application (called the Client) to verify the identity of a user based on the authentication performed by a
trusted party (called an Authorization Server) and retrieve the user profile information. The authorization server informs the Client application about the
result of the authentication step via an ID Token.
OIDC is an identity layer on top of OAuth2, an authorization framework that solves the problem of delegating access using tokens for authorization but doesn't
deal with authentication. As you know, authorization can only happen after authentication.

When it comes to handling user authentication, we can identify three main actors in the OAuth2 framework that are used by the OIDC protocol:

* **Authorization Server** — The entity responsible for authenticating users and issuing tokens. In Bookshop, this will be Keycloak.
* **User** — Also called the Resource Owner, this is the human logging in with the Authorization Server to get authenticated access to the Client application.
In Bookshop, it's either a customer or an employee.
* **Client** — The application requiring the user to be authenticated. This can be a mobile application, a browser-based application, a server-side application,
or even a smart TV application. In Bookshop, it's Edge Service.

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/oidc-oatuh2-roles-in-bookshop-architecture.drawio.svg)

When an unauthenticated user calls a secure endpoint exposed by Edge Service, the following happens:

1. Edge Service (the Client) redirects the browser to Keycloak (the Authorization Server) for authentication.
2. Keycloak authenticates the user (for example, by asking for a username and password via a login form) and then redirects the browser back to Edge Service,
together with an Authorization Code.
3. Edge Service calls Keycloak to exchange the Authorization Code with an ID Token, containing information about the authenticated user.
4. Edge Service initializes an authenticated user session with the browser based on a session cookie. Internally, Edge Service maintains a mapping between the
session identifier and ID Token (the user identity).

##### The authentication flow supported by the OIDC protocol

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/authentication-flow-with-oidc.drawio.svg)

##### Exchanging user information with JWT
In distributed systems, including microservices and cloud native applications, the most-used strategy for exchanging information about an authenticated user
and their authorization is through tokens.

JSON Web Token (JWT) is an industry-standard for representing claims to be transferred between two parties. It's a widely used format for propagating
information about an authenticated user and their permissions securely among different parties in a distributed system. A JWT is not used by itself, but it's
included in a larger structure, the JSON Web Signature (JWS), which ensures the integrity of the claims by digitally signing the JWT object.

A digitally signed JWT (JWS) is a string composed of three parts encoded in Base64 and separated by a dot (.) character:

`<header>.<payload>.<signature>`

A digitally signed JWT has three parts:

* **Header** — A JSON object (called JOSE Header) containing information about the cryptographic operations performed on the payload.
* **Payload** — A JSON object (called Claims Set) containing the claims conveyed by the token.
* **Signature** — The signature of the JWT, ensuring that the claims have not been tampered with. A prerequisite of using a JWS structure is that we trust the
entity issuing the token (the issuer), and we have a way to check its validity.

##### Registering an application in Keycloak
An OAuth2 Client is an application that can request user authentication and ultimately receive tokens from an Authorization Server. In the Bookshop
architecture, this role is played by Edge Service. When using OIDC/OAuth2, you need to register each OAuth2 Client with the Authorization Server before using
it for authenticating users.

Clients can be public or confidential. We register an application as a public Client if it can't keep a secret. For example, mobile applications would be
registered as public Clients. On the other hand, confidential Clients are those that can keep a secret, and they are usually backend applications like
Edge Service. The registration process is similar either way. The main difference is that confidential Clients are required to authenticate themselves with the
Authorization Server, such as by relying on a shared secret. It's an additional protection layer we can't use for public Clients, since they have no way to
store the shared secret securely.

Rule of thumb. If the frontend is a mobile or desktop application like iOS or Android, that will be the OAuth2 Client, and it will be categorized as a public
Client. You can use libraries like AppAuth (https://appauth.io) to add support for OIDC/ OAuth2 and store the tokens as securely as possible on the device. If
the frontend is a web application, then a backend service should be the Client. In this case, it would be categorized as a confidential Client.

Register Edge Service as an OAuth2 Client in the Bookshop realm:

`$ ./kcadm.sh create clients -r Bookshop -s clientId=edge-service -s enabled=true -s publicClient=false -s secret=bookshop-keycloak-secret -s 'redirectUris=["http://localhost:9000", "http://localhost:9000/login/oauth2/code/*"]'`

Edge Service is a confidential client, not public. Since it's a confidential client, it needs a secret to authenticate with Keycloak. The application URLs to
which Keycloak is authorized to redirect a request after a user login or logout.

You can use a JSON file to load the entire configuration when starting up the Keycloak container. You can find out the keycloak realm config file in
bookshop-deployment repo under docker folder.

#### Authenticating users with Spring Security and OpenID Connect
##### Configuring the integration between Spring Security and Keycloak
After adding the relevant dependencies on Spring Security, we need to configure the integration with Keycloak.

Each Client registration in Spring Security must have an identifier (registrationId). In this example, it's **keycloak**. The registration identifier is used
to build the URL where Spring Security receives the Authorization Code from Keycloak. The default URL template is /login/oauth2/code/{registrationId}. For
Edge Service, the full URL is http://localhost:9000/login/oauth2/code/keycloak, which we already configured in Keycloak as a valid redirect URL.

**Scopes** are an OAuth2 concept for limiting an application's access to user resources. You can think of them as roles, but for applications instead of users.
When we use the OpenID Connect extension on top of OAuth2 to verify the user's identity, we need to include the **openid** scope to inform the Authorization
Server and receive an ID Token containing data about the user authentication.

The ServerHttpSecurity object provides two ways of configuring an OAuth2 Client in Spring Security. With **oauth2Login()**, you can configure an application to
act as an OAuth2 Client and also authenticate users through OpenID Connect. With **oauth2Client()**, the application will not authenticate users. We want to use
OIDC authentication, so we'll use **oauth2Login()**.

After adding SecurityConfiguration, first, start the Redis and Keycloak container:

`$ docker-compose up -d bookshop-redis bookshop-keycloak`

Then run the edge service application:

`$ ./gradlew bootRun`

Open a browser window, and head to http://localhost:9000. You should be redirected to a login page served by Keycloak, where you can authenticate as one of the
users we created previously.

The Keycloak login page for the Polar Bookshop realm, shown after Edge Service triggered the OIDC authentication flow.

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/keycloak-login-page-for-bookshop-realm.png)

If the authentication is successful, Spring Security will start an authenticated session with the browser and save information about the user.

##### Inspecting the authenticated user context
As part of the authentication process, Spring Security defines a context to hold information about the user and map a user session to an ID Token.

Independent of the authentication strategy adopted (whether username/password, OpenID Connect/OAuth2, or SAML2), Spring Security keeps the information about an
authenticated user (also called the principal) in an **Authentication** object. In the case of OIDC, the principal object is of type **OidcUser**, and it's
where Spring Security stores the ID Token. In turn, **Authentication** is saved in a **SecurityContext** object.

The main classes used to store information about the currently authenticated user:

![](https://github.com/sanjayrawat1/bookshop/blob/main/edge-service/diagrams/security-context-structure-for-oidc-authentication.drawio.svg)

Besides using **ReactiveSecurityContextHolder** directly, we can use the annotations **@CurrentSecurityContext** and **@AuthenticationPrincipal** to inject the
**SecurityContext** and the principal (in this case, **OidcUser**) respectively.

Consider what happened when you tried to access the **/user** endpoint and got redirected to Keycloak. After successfully validating the user's credentials,
Keycloak called Edge Service back and sent the ID Token for the newly authenticated user. Then Edge Service stored the token and redirected the browser to the
required endpoint, together with a session cookie. From that point on, any communication between the browser and Edge Service will use that session cookie to
identify the authenticated context for that user. No token is exposed to the browser.

The ID Token is stored in OidcUser, part of Authentication and ultimately included in SecurityContext. We used the Spring Session project to make Edge Service
store session data in an external data service (Redis), so it could remain stateless and be able to scale out. SecurityContext objects are included in the
session data and are therefore stored in Redis automatically, making it possible for Edge Service to scale out without any problem.

Another option for retrieving the currently authenticated user (the principal) is from the context associated with a specific HTTP request (called the exchange).
We'll use that option to update the rate limiter configuration. We implemented rate-limiting with Spring Cloud Gateway and Redis. Currently, the rate-limiting
is computed based on the total number of requests received every second. We should update it to apply the rate limits to each user independently.
