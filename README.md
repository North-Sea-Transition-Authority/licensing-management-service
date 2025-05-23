# Template SpringBoot Project

## What's provided

- Java 21 project running Spring 3.2
- Fivium Design System submodule setup
- SAML login via the Energy Portal
- Global logout via service or Energy Portal
- Session persistence
- Error handling and FDS error pages
- Accessibility statement, cookie banner, contact information pages, linked in footer
- Energy Portal API integration
- Basic setup for Digital Form library, File Upload library and Payments library
- Envers
- Shedlock

## Using the template
Create a new GitHub repository and select `Fivium/digital-springboot-template` from the `Repository template` dropdown.
This will create a new repo with the contents of this template.

In your new repository, do the following:
- Rename the `uk.co.nstauthority.licensingmanagementservice` package, replacing `template` with your service name. You may also need to replace `co.nstauthority` with `gov.desnz` for a DESNZ/BEIS app.
- `Ctrl` + `Shift` + `F` in IntelliJ and find all usages of the string `uk.co.nstauthority.licensingmanagementservice`, then update as above.
- `Ctrl` + `Shift` + `F` in IntelliJ and find all usages of the string `xyz`. Replace all of these with your service name, or whatever makes the most sense in context based on the comments. You should have no references to `xyz` in your codebase after proper configuration. Pay special attention to:
  - The project name in `settings.gradle`
  - Placeholder values in the various `.properties` files
  - `.drone.yml`, and the `devtools-xyz` folder
  - The Freemarker templates folder `src/main/resources/templates/xyz` and its references in controllers
  - Dates of audits in the accessibility statement, and business support details in the contact page
- Update the `banner.txt`
- Update this readme to remove these template related tasks.
- Delete the `.drone_for_template.yml`

Then follow from step 3 onwards in [this document](https://docs.google.com/document/d/1kNwiGmVaugnoO8DmeziFpOv1oGa_fEC6fhJjCHD-qIg/edit) for setup of Drone secrets and other services.

Additionally, follow the [FOX side setup instructions](https://fivium.atlassian.net/wiki/spaces/BESPOKE/pages/1738805/Logging+out+of+Energy+Portal+and+Service) to configure the global logout from the Energy Portal. The Java setup has already been done in this template.  

To deploy your service into the dev stack see the [SB2 new service setup guide](https://fivium.atlassian.net/wiki/spaces/BESPOKE/pages/729645094/SB2+new+service+setup+guide).

## Default users
Running the provided `setup-dev-users-teams.sql` provides you with access for two users, `industry.editor@template.co.uk` and `administrator@template.co.uk`


# XYZ

## Pre-requisites
- Java 21
- Node 18 + NPM
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

### Logging

XYZ can log in either JSON or text mode.

In order to turn on JSON logging, set the profile `json-logging`. This will automatically include any MDC attributes.

JSON logging is the preferred solution for SB2.

### Development
- In your IntelliJ run configuration for the Spring app, include `development` in your active profiles

### Production
- In your IntelliJ run configuration for the Spring app, include `production` in your active profiles
- The following environment variables are required when using this profile:

| Environment Variable                 | Description                                                                            |
|--------------------------------------|----------------------------------------------------------------------------------------|
| **Database**                         |                                                                                        |
| `XYZ_DATABASE_URL`                   | The URL to the database the service connect to                                         |
| `XYZ_DATABASE_PASSWORD`              | Database schema password for the `XYZ` user                                            |
|                                      |                                                                                        |
| **Feedback Management Service**      |                                                                                        |
| `XYZ_FMS_URL_BASE`                   | The URL for the FMS instance on your environment                                       |
| `XYZ_FMS_CONNECTION_TIMEOUT_SECONDS` | Connection timeout in seconds. Defaults to `20`                                        |
| `XYZ_FMS_SUBMIT_ENDPOINT`            | The FMS endpoint where feedback will be sent here. Defaults to `/api/v1/save-feedback` |
| `XYZ_FMS_PRESHARED_KEY`              | This is the pre-shared key used when making requests                                   |


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

### 5. Run the app
Create a run configuration for the Spring app and start the application.

The application will be running on `localhost:8080/lms/<endpoint>`

## Development setup

### Checkstyle
1. In Intellij install the Checkstyle-IDEA plugin (from third-party repositories)
2. Go to File > Settings > Tools > Checkstyle 
3. Click the plus icon under "Configuration File"
4. Select "Use a local Checkstyle file"
5. Select `devtools/checkstyle.xml`
6. Check the "Active" box next to the new profile

Note that Checkstyle rules are checked during the build process and any broken rules will fail the build.
