# Energy Portal GIS Framework

This is a submodule responsible for the GIS capabilities of the repo. The code should remain independent of the LMS code
in case it needs to be separated in the future.

The GIS framework consists of spring services that you can inject into your main code and use to perform operations on GIS data.
The framework also provides frontend components that can be used to display and interact with GIS data.

//TODO add code examples

## Infrastructure

The GIS framework is built as a Spring Boot starter that can be added as dependency to any project.
It also contains a node server that runs separately from the java app and is responsible for performing operations on the GIS data using the [ArcGIS JS SDK](https://developers.arcgis.com/javascript/latest/). 
The java app communicates with the node server via gRPC to perform operations on the data.

## Building the app.

The backend contains two parts; the java app and the node server.
The java app is responsible for serving the frontend, but also critically getting data from the database and
communicating with the node server via gRPC to perform operations on the data.
gRPC uses protobuf to define messages and types which are generated and used on both the java and node app.

Additionally, the frontend also needs to build the vue components before they can be served.

To build everything you can run `cd gis-framework` and `npm run build-all` to generate the proto for both apps and build the
frontend vue components.

Alternatively, you can build the parts separately by:
- running `cd arcgis-js-sdk/ && npm install && npm run copy:core && npm run proto-gen && cd .. ` to install dependencies and build the gRPC proto files.
- running the gradle clean and build tasks to generate the java proto classes defined in `src/main/proto`

## Starting the node server

Go to `gis-alpha-test/arcgis-js-sdk` and right click `grpc-server.ts` then run

## Developing proto features

We use protobuf to define the messages and types used in the gRPC communication.
This allows us to define the messages and types once and use them in both the java and node apps. Then proto will create the java and typescript classes based on the definitions.

Follow the [best practices for proto code](https://protobuf.dev/best-practices/)
Once you add a new proto message or enum definition, run `npm run proto-gen` to generate the java classes. 
Or just run `npm run build-all` to generate the proto files (java and typescript) and build the frontend.


## Committing Changes

When you commit changes to the repo, Husky will automatically run the linting and formatting checks.
If any issues are found, the commit will be blocked until they are resolved, but any that can be automatically fixed will be.
If using intellij to commit, you may need to go to settings > commit > advanced commit checks > run custom configurations and select `prepare`. 
Disable Run advanced checks after a commit is done, so the commit is blocked if there are issues.

## Production
- In your IntelliJ run configuration for the Spring app, include `production` in your active profiles
- The following environment variables are required when using this profile:

| Environment Variable | Description                                                                                                                                     |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Migration**        |                                                                                                                                                 |
| `ORACLE_DB_URL`      | The url to connect to the oracle database. This is needed for the GIS data migration. e.g.  `jdbc:oracle:thin:@db-ogadev1.sb2.dev:1521:ogadev1` |
| `ORACLE_DB_USERNAME` | The username for the user to access the oracle GIS schema.                                                                                      |
| `ORACLE_DB_PASSWORD` | The password for the user to access the oracle GIS schema.                                                                                      |
