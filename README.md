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

### 1. Add the required profile(s)

### Development
- In your IntelliJ run configuration for the Spring app, include `development` in your active profiles
- Add `enable-lms1` and `enable-lms2` to run the app with all phased go-live features switched on
  (see [Feature flags (phased go-live)](#feature-flags-phased-go-live)) — without them you get the initial-release
  feature set only

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

### 2. Initialise the Fivium Design System
```bash
git submodule update --init --recursive
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
```

### 2a. Upgrade FDS (the developer doing the upgrade)
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

### 2b. If you're a developer working on a project where another developer has upgraded FDS, you need to:
```bash
git submodule update
cd fivium-design-system-core && npm install && npx gulp buildAll && cd ..
npx gulp buildAll
```

### 3. Build frontend components
```bash
npm install && npx gulp buildAll
```

### 4. Start the GIS framework

See [GIS framework README](gis-framework/README.md) for initialization instructions.

### 5. Run the app
Create a run configuration for the Spring app and start the application.

The application will be running on [http://localhost:8080/lms](http://localhost:8080/lms)

## Feature flags (phased go-live)

LMS goes live in three phases, gated by profile-backed feature flags in the `phasedrelease` package.

| Phase | Active profiles | Unlocks |
|-------|-----------------|---------|
| **Initial** | *(none)* | Work area (without the Start application button), teams, licence contacts |
| **LMS1** | `enable-lms1` | + schedules management, licence search & management, schedule & continuation applications, document library |
| **LMS2** | `enable-lms1` + `enable-lms2` | + all other functionality (licence corrections, licence position/timeline) |

**Absence of a profile means locked**, so no phase profile is set in `application.properties`. For local development,
add `enable-lms1` and `enable-lms2` to your run configuration to work with the full app. Tests already run with both
on, via the profile groups in the test-only `src/test/resources/application.properties`. Live environments switch a
phase on by adding its profile to `SPRING_PROFILES_ACTIVE` — config only, no redeploy.

`FeatureFlagService` is the single source of truth for "is this on?", checked against two enums: `ReleasePhase`
(the profile-backed phase) and `ReleaseFeature` (the catalogue of toggleable actions and options, each mapped to a
phase). It gates three layers:

1. **URL access** — `PhasedReleaseInterceptor` is a default-deny allow-list; `PhasedReleasePolicy` maps each
   controller by package to a phase, and anything unclassified or off-phase 404s. **A new controller is unreachable
   until its package is classified there** — `PhasedReleasePolicyTest` fails the build as a reminder.
2. **In-page actions** — `isEnabled(...)` in the controller (ANDed with any existing role check), or the
   `enabledFeatures` model attribute in a template.
3. **Options and categories** — anything implementing `PhaseGated` (application types, work-area providers) is
   filtered with `filterEnabled(...)`; submit paths re-check, since hiding an option does not make it inaccessible.

Flags are scaffolding — see [ADR 0008](documentation/adr/0008-phased-go-live-feature-flag.md) for the full rationale,
the allow-list, and the recipe for retiring a flag once its phase is permanently live.

## Development setup

### Tracing with Zipkin

The `compose.yml` includes a Zipkin container. Tracing is **disabled by default** — the app starts normally without Zipkin running.

To enable tracing:

1. Ensure the Zipkin container is running (start it alongside the other backing services via `compose.yml`).
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
