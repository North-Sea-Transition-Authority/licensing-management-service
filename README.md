# Licence Management Service

## Background
The Licence Management Service (LMS) is used to track licence schedule and work programme information for a variety of licence types including:
- Production
- Carbon Storage
- Gas Storage
- Methane Drainage
- Exploration

The service also sends reminders to licensees to provide updates on future licence scheduled events who then provide those updates through the service and processed by the NSTA.


## Pre-requisites
- Java 21
- Node 24.16.0 + NPM
- [Docker for Windows](https://hub.docker.com/editions/community/docker-ce-desktop-windows)
  (See [Docker setup](https://confluence.fivium.co.uk/display/JAVA/Java+development+environment+setup#Javadevelopmentenvironmentsetup-Docker)
  for further information about adding your account to the `docker-users` group)

## Setup

### 1. Run the backend services
- Ensure that you have [Docker for Windows](https://hub.docker.com/editions/community/docker-ce-desktop-windows)
  installed and running (or an alternative way of running docker).  
- Run the backing services defined in the `local-dev-compose.yml`. This can be done by clicking the run icon
  next to `services` when in the file.
  - If IntelliJ doesn't detect the file as a docker compose file automatically you may need to 
    [Associate docker-compose as file type](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009394620-Associate-docker-compose-as-file-type) manually.

### 2. Add the required profile

### Development
- In your IntelliJ run configuration for the Spring app, include `development` in your active profiles

| Environment Variable                     | Description                                                                                                        |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| **Tracing**                              |                                                                                                                    |
| `LMS_ENABLE_ZIPKIN`                      | Sampling probability for Zipkin tracing. Set to `1.0` to trace every request. Defaults to `0.0` (disabled).       |
| **Energy Portal Message Queue**          |                                                                                                                    |
| `LMS_EPMQ_SNS_SQS_AWS_ACCESS_KEY_ID`     | Refer to [EPMQ readme](https://github.com/Fivium/energy-portal-message-queue#2-add-required-environment-variables) |
| `LMS_EPMQ_SNS_SQS_AWS_SECRET_ACCESS_KEY` | Refer to [EPMQ readme](https://github.com/Fivium/energy-portal-message-queue#2-add-required-environment-variables) |
| `LMS_EPMQ_ENVIRONMENT_SUFFIX`            | Refer to [EPMQ readme](https://github.com/Fivium/energy-portal-message-queue#2-add-required-environment-variables) |

### Production
- In your IntelliJ run configuration for the Spring app, include `production` in your active profiles
- The following environment variables are required when using this profile:

| Environment Variable                     | Description                                                                                                                                                                                                                                                                                                |
|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Database**                             |                                                                                                                                                                                                                                                                                                            |
| `LMS_DATABASE_URL`                       | The URL to the database the service connect to                                                                                                                                                                                                                                                             |
| `LMS_DATABASE_PASSWORD`                  | Database schema password for the `XYZ` user                                                                                                                                                                                                                                                                |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Energy Portal**                        |                                                                                                                                                                                                                                                                                                            |
| `LMS_ENERGY_PORTAL_API_TOKEN`            | EPA token                                                                                                                                                                                                                                                                                                  |
| `LMS_ENERGY_PORTAL_LOGOUT_KEY`           | Key to allow logout from the energy portal                                                                                                                                                                                                                                                                 |
| `FOX_REDIRECT_URL`                       | The URL to the LMS redirector in the energy portal - used to generate links into PEARS view licence screen                                                                                                                                                                                                 |
| `EPAS_REDIRECT_URL`                      | The URL to the EPAS redirector in the energy portal - used to keep users authenticated when redirecting to a FOX module                                                                                                                                                                                    |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **S3**                                   |                                                                                                                                                                                                                                                                                                            |
| `LMS_S3_ACCESS_TOKEN`                    |                                                                                                                                                                                                                                                                                                            |
| `LMS_S3_SECRET_TOKEN`                    |                                                                                                                                                                                                                                                                                                            |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Feedback Management Service**          |                                                                                                                                                                                                                                                                                                            |
| `LMS_FMS_URL_BASE`                       | The URL for the FMS instance on your environment                                                                                                                                                                                                                                                           |
| `LMS_FMS_CONNECTION_TIMEOUT_SECONDS`     | Connection timeout in seconds. Defaults to `20`                                                                                                                                                                                                                                                            |
| `LMS_FMS_SUBMIT_ENDPOINT`                | The FMS endpoint where feedback will be sent here. Defaults to `/api/v1/save-feedback`                                                                                                                                                                                                                     |
| `LMS_FMS_PRESHARED_KEY`                  | This is the pre-shared key used when making requests                                                                                                                                                                                                                                                       |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Energy Portal Message Queue**          |                                                                                                                                                                                                                                                                                                            |
| `LMS_EPMQ_SNS_SQS_AWS_ACCESS_KEY_ID`     | Refer to [EPMQ readme](https://github.com/Fivium/energy-portal-message-queue#2-add-required-environment-variables)                                                                                                                                                                                         |
| `LMS_EPMQ_SNS_SQS_AWS_SECRET_ACCESS_KEY` | Refer to [EPMQ readme](https://github.com/Fivium/energy-portal-message-queue#2-add-required-environment-variables)                                                                                                                                                                                         |
| `LMS_EPMQ_ENVIRONMENT_SUFFIX`            | Refer to [EPMQ readme](https://github.com/Fivium/energy-portal-message-queue#2-add-required-environment-variables)                                                                                                                                                                                         |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Metrics**                              |                                                                                                                                                                                                                                                                                                            |
| `LMS_METRICS_ENVIRONMENT_NAME`           | Metrics environment name                                                                                                                                                                                                                                                                                   |
| `LMS_METRICS_INSTANCE_TAG`               | Metrics instance name                                                                                                                                                                                                                                                                                      |
| `LMS_ENABLE_STATSD`                      | Flag to enable/disable metrics gathering                                                                                                                                                                                                                                                                   |
| `LMS_STATSD_HOST`                        | Metrics host address                                                                                                                                                                                                                                                                                       |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Google Analytics**                     |                                                                                                                                                                                                                                                                                                            |
| `LMS_ANALYTICS_SERVICE_IDENTIFIER`       | Google analytics measurement ID for the service                                                                                                                                                                                                                                                            |
| `LMS_ANALYTICS_ENERGY_PORTAL_IDENTIFIER` | Google analytics measurement ID for the energy portal                                                                                                                                                                                                                                                      |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Energy Portal Accounts Service**       |                                                                                                                                                                                                                                                                                                            |
| `EPAS_SAML_ENTITY_ID`                    | The entity ID is either:<ul><li>`energy-portal-accounts-service-dev` for dev/local</li><li>`energy-portal-accounts-service-st` for st</li><li>`energy-portal-accounts-service-uat` for uat</li><li>`energy-portal-accounts-service-prod` for prod</li></ul>                                                |
| `EPAS_SAML_LOGIN_URL`                    | The login url e.g. https://desnz.itportal.dev.fivium.co.uk/accounts/saml/login                                                                                                                                                                                                                             |
| `EPAS_LOGOUT_REQUEST_URL`                | The logout url e.g https://desnz.itportal.dev.fivium.co.uk/accounts/service-provider-sign-out                                                                                                                                                                                                              |
| `EPAS_SAML_BASE_URL`                     | The base url (e.g. https://desnz.itportal.dev.fivium.co.uk)                                                                                                                                                                                                                                                |
| `EPAS_SPRING_BOOT_STARTER_PRESHARED_KEY` | The preshared key that EPAS will use when calling out to this service                                                                                                                                                                                                                                      |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **Notify**                               |                                                                                                                                                                                                                                                                                                            |
| `LMS_NOTIFY_MODE`                        | For all environments aside from prod, we should use the `test` mode, for prod we use the `production` mode                                                                                                                                                                                                 |
| `LMS_NOTIFY_API_KEY`                     | The key for notify. See Notify section of readme for more details on test vs live mode. See TPM for API keys  - <br/> [deployed and local](https://tpm.fivium.co.uk/index.php/prj/view/202), [ST](https://tpm.fivium.co.uk/index.php/prj/view/203), [UAT](https://tpm.fivium.co.uk/index.php/prj/view/209) |
| `LMS_NOTIFY_TEST_EMAIL`                  | The email address to send the email to. This will override the email in the code for testing purposes.                                                                                                                                                                                                     |
|                                          |                                                                                                                                                                                                                                                                                                            |
| **GIS Framework**                        |                                                                                                                                                                                                                                                                                                            |
| `ORACLE_DB_URL`                          | The url to connect to the oracle database. This is needed for the GIS data migration. e.g.  `jdbc:oracle:thin:@db-ogadev1.sb2.dev:1521:ogadev1`                                                                                                                                                            |
| `ORACLE_DB_USERNAME`                     | The username for the user to access the oracle GIS schema.                                                                                                                                                                                                                                                 |
| `ORACLE_DB_PASSWORD`                     | The password for the user to access the oracle GIS schema.                                                                                                                                                                                                                                                 |


### Logging

LMS can log in either JSON or text mode.

In order to turn on JSON logging, set the profile `json-logging`. This will automatically include any MDC attributes.

JSON logging is the preferred solution for SB2.

### 3. Initialise the Fivium Design System
```bash
git submodule update --init --recursive
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
```

### 3a. Upgrade FDS (the developer doing the upgrade)
Update `.gitmodules` to reflect the new version of FDS, then
```bash
git submodule update --remote
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
```
To test that the update has worked locally you will need to rebuild the frontend in this project, i.e.
```bash
npx gulp buildAll
```
> After upgrading FDS as above you should be committing the change to `.gitmodules` and a new commit hash for the `fivium-design-system-core` submodule only

### 3b. If you're a developer working on a project where another developer has upgraded FDS, you need to:
```bash
git submodule update
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
npx gulp buildAll
```

### 4. Build frontend components
```bash
npm install && npx gulp buildAll
```

### 5. Start the GIS framework

See [GIS framework README](gis-framework/README.md) for initialization instructions.

### 6. Run the app
Create a run configuration for the Spring app and start the application.

The application will be running on `localhost:8080/lms/<endpoint>`

## Development setup

### Tracing with Zipkin

The `local-dev-compose.yml` includes a Zipkin container. Tracing is **disabled by default** — the app starts normally without Zipkin running.

To enable tracing:

1. Ensure the Zipkin container is running (start it alongside the other backing services via `local-dev-compose.yml`).
2. Set `LMS_ENABLE_ZIPKIN=1.0` in your IntelliJ run configuration environment variables. This sets the sampling probability to 100%, meaning every request is traced.
3. Start the app and make a request to any endpoint.
4. Open the Zipkin UI at `http://localhost:9411`.

**Using the Zipkin UI:**

- Click **Run Query** on the home screen to list recent traces, or filter by service name (`licensing-management-service`).
- Click a trace to open the waterfall view, which shows the full call chain for that request: controller → service → repository → SQL queries, each as a timed span.
- Spans are named `ClassName.methodName`. Hovering a span shows its duration and any tags (e.g. the SQL query text for database calls).
- Slow spans are highlighted in red, making hotspots easy to identify at a glance.

To stop tracing, remove `LMS_ENABLE_ZIPKIN` from your run configuration (or set it to `0.0`) and restart the app.

### Checkstyle
1. In Intellij install the Checkstyle-IDEA plugin (from third-party repositories)
2. Go to File > Settings > Tools > Checkstyle 
3. Click the plus icon under "Configuration File"
4. Select "Use a local Checkstyle file"
5. Select `devtools/checkstyle.xml`
6. Check the "Active" box next to the new profile

Note that Checkstyle rules are checked during the build process and any broken rules will fail the build.
