## Configuration Data Store

How does Spring Cloud Config resolve the correct configuration data for each application?
How should you organize the repository to host properties for multiple application?
The library rely on three parameters:
1. {application} - The name of the application as defined by the spring.application.name property.
2. {profile} - One of the active profiles defined by the spring.profiles.active property.
3. {label} - A discriminator defined by the specific configuration data repository. In the case of the Git, it can be a tag, a branch name, or a commit id.

Depending on your needs, you can organize the folder structure using different combinations, such as:
1. /{application}/application-{profile}.yml
2. /{application}/application.yml
3. /{application}-{profile}.yml
4. /{application}.yml
5. /application-{profile}.yml
6. /application.yml

For each application, you can either use property files named after the application itself and placed in the root folder
(e.g., /catalog-service.yml or /catalog-service-prod.yml) or use the default naming and put them in a subfolder named
after the application (e.g., /catalog-service/application.yml or /catalog-service/application-prod.yml).

You can also put application.yml or application-{profile}.yml files in the root folder to define default values for all applications.
They can be used as a fallback whenever there is no more specific property source. 