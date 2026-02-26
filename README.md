# Shopizer 3.X (current repo configuration: Java 25 + Spring Boot 4.0.3)

> [!NOTE]
> The team is working on an upcoming efficient microservices version. Stay tuned !

[![last_version](https://img.shields.io/badge/last_version-v3.2.5-blue.svg?style=flat)](https://github.com/shopizer-ecommerce/shopizer/tree/3.2.5)
[![Official site](https://img.shields.io/website-up-down-green-red/https/shields.io.svg?label=official%20site)](http://www.shopizer.com/)
[![Docker Pulls](https://img.shields.io/docker/pulls/shopizerecomm/shopizer.svg)](https://hub.docker.com/r/shopizerecomm/shopizer)
[![stackoverflow](https://img.shields.io/badge/shopizer-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/shopizer)
[![CircleCI](https://circleci.com/gh/shopizer-ecommerce/shopizer.svg?style=svg)](https://circleci.com/gh/shopizer-ecommerce/shopizer)


Java open source e-commerce software

Headless commerce and Rest api for ecommerce

- Catalog
- Shopping cart
- Checkout
- Merchant
- Order
- Customer
- User

Shopizer Headless commerce consists of the following components:

- Spring boot Java / Spring boot backend
- Angular administration web application
- React JS front end application

## Current Project Configuration (this repository)

- Java version: `25` (`pom.xml`, `sm-core-model/pom.xml`)
- Spring Boot parent: `4.0.3` (`pom.xml`)
- OpenAPI / Swagger UI dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1` (`sm-shop/pom.xml`)
- Maven wrapper distribution: `3.5.2` (`.mvn/wrapper/maven-wrapper.properties`)
- Backend modules:
  - `sm-core-model`
  - `sm-core-modules`
  - `sm-core`
  - `sm-shop-model`
  - `sm-shop`
- Backend server port: `8080` (`sm-shop/src/main/resources/application.properties`)
- Actuator endpoints: `management.endpoints.web.exposure.include=*`
- Health details: `management.endpoint.health.show-details=always`
- Multipart limits:
  - `spring.servlet.multipart.max-file-size=4MB`
  - `spring.servlet.multipart.max-request-size=10MB`
- Spring compatibility flags enabled:
  - `spring.main.allow-bean-definition-overriding=true`
  - `spring.main.allow-circular-references=true`
- API docs / UI endpoints (security config allows these):
  - `/swagger-ui/index.html` (canonical UI)
  - `/swagger-ui.html` (redirect alias)
  - `/v3/api-docs` (OpenAPI import URL)
  - `/v3/api-docs/**`
- Default database config: MySQL (`sm-shop/src/main/resources/database.properties`)
  - schema: `SALESMANAGER`
  - `hibernate.hbm2ddl.auto=update`
- Spring XML property profiles configured in `sm-core/src/main/resources/spring/shopizer-core-config.xml`:
  - `default`, `firebase`, `gcp`, `cloud`, `local`, `mysql`
- Additional DB property file present for H2: `sm-shop/src/main/resources/profiles/docker/database.properties` (not part of the XML profile list above by default)



See the demo: [**New demo on the way 2023]
-------------------
Headless demo Available soon

1.  Run from Docker images:

From the command line:

```
docker run -p 8080:8080 shopizerecomm/shopizer:latest
```
       
2. Run the administration tool

⋅⋅⋅ Requires the java backend to be running

```
docker run \
 -e "APP_BASE_URL=http://localhost:8080/api" \
 -p 82:80 shopizerecomm/shopizer-admin
```


3. Run react shop sample site

⋅⋅⋅ Requires the java backend to be running

```
docker run \
 -e "APP_MERCHANT=DEFAULT"
 -e "APP_BASE_URL=http://localhost:8080"
 -p 80:80 shopizerecomm/shopizer-shop-reactjs
```

API documentation:
-------------------

https://app.swaggerhub.com/apis-docs/shopizer/shopizer-rest-api/3.0.1#/

Get the source code:
-------------------
Clone the repository:
     
	 $ git clone git://github.com/shopizer-ecommerce/shopizer.git
	 
	 $ git clone git://github.com/shopizer-ecommerce/shopizer-admin.git
	 
	 $ git clone git://github.com/shopizer-ecommerce/shopizer-shop-reactjs.git

If this is your first time using Github, review http://help.github.com to learn the basics.

You can also download the zip file containing the code from https://github.com/shopizer-ecommerce for each of the the projects above

To build the application:
-------------------

1. Shopizer backend


From the command line:

	$ cd shopizer
	$ mvn -Dmaven.test.skip=true clean install
	$ cd sm-shop
	$ mvn spring-boot:run

Optional profile examples (depending on your environment and DB properties):

	$ mvn -Dspring-boot.run.profiles=local spring-boot:run
	$ mvn -Dspring-boot.run.profiles=mysql spring-boot:run

Notes:

- The repository Maven wrapper is currently pinned to Maven `3.5.2`; if you hit plugin incompatibility errors, use a system Maven `3.9+`.
- Default backend DB properties point to MySQL (`localhost:3306`, schema `SALESMANAGER`).

2. Shopizer admin

Form compiling and running Shopizer admin consult the repo README file

3. Shop sample site

Form compiling and running Shopizer admin consult the repo README file


### Access the application:
-------------------

Access the backend locally at:

- Swagger UI (SpringDoc): http://localhost:8080/swagger-ui/index.html
- Swagger UI alias (redirects): http://localhost:8080/swagger-ui.html
- OpenAPI JSON (SpringDoc import URL for Postman/Swagger tools): http://localhost:8080/v3/api-docs
- OpenAPI YAML (optional): http://localhost:8080/v3/api-docs.yaml
- Actuator health: http://localhost:8080/actuator/health


The instructions above will let you run the application with the repository's current default settings.
Please review database, email, authentication, and profile-specific property files before deploying to a non-local environment.


### Documentation:
-------------------

Documentation available <https://shopizer-ecommerce.github.io/documentation/>

Api documentation <https://app.swaggerhub.com/apis-docs/shopizer/shopizer-rest-api/3.0.1#/>

ChatOps <https://shopizer.slack.com>  - Join our Slack channel <https://communityinviter.com/apps/shopizer/shopizer>

More information is available on shopizer web site here <http://www.shopizer.com>

### Participation:
-------------------

If you have interest in giving feedback or for participating to Shopizer project in any way
Feel to use the contact form <http://www.shopizer.com/contact.html> and share your email address
so we can send an invite to our Slack channel

### How to Contribute:
-------------------
Fork the repository to your GitHub account

Clone from fork repository
-------------------

       $ git clone https://github.com/yourusername/shopizer.git

Build application according to steps provided above

Synchronize lastest version with the upstream
-------------------

      $ git remote add upstream https://github.com/yourusername/shopizer.git
	  $ git pull upstream 3.2.5

Create new branch in your repository
-------------------

	   $ git checkout -b branch-name


Push your changes to Shopizer
-------------------

Please open a PR (pull request) in order to have your changes merged to the upstream
