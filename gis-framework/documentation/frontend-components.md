# GIS Frontend Components

The GIS framework starter provides frontend components to consuming Spring Boot apps by packaging two things in the starter jar:

- FreeMarker macros under `classpath:/templates/gis`
- Vite-built JavaScript and CSS under `classpath:/public/gis/dist`

The consuming app does not compile the GIS Vue source. It imports the FreeMarker macro from the starter and loads the static assets from the starter jar at runtime.

## Source Layout

Vue and frontend TypeScript entrypoint source lives in:

```text
src/main/resources/js
```

FreeMarker macros exposed to consuming apps live in:

```text
src/main/resources/templates/gis
```

Generated frontend assets are emitted by Vite into:

```text
src/main/resources/public/gis/dist
```

That directory is generated build output. Source changes should be made to the Vue, TypeScript, SCSS, or FreeMarker source files, then rebuilt.

## Vite Build

The Vite config builds the GIS frontend as an ES module library. The entry assets are:

```text
/gis/dist/gis-bundle.js
/gis/dist/gis-framework.css
```

Vite may also emit hashed `.mjs` chunks next to those files. Those chunks are imported by `gis-bundle.js`, so they must be packaged with the entry files.

## Gradle Build

The Gradle build uses the Gradle Node plugin:

```gradle
id 'com.github.node-gradle.node' version 'x.x.x'
```

The plugin downloads a pinned Node/npm toolchain for the build:

```gradle
node {
  version = '22.12.0'
  npmVersion = '10.9.0'
  download = true
  nodeProjectDir = projectDir
}
```

This means CI and developer machines do not need a system `node` or `npm` executable for the Gradle build to package frontend assets.

The frontend build has two Gradle tasks:

```text
installFrontendDependencies
buildFrontend
```

`installFrontendDependencies` runs:

```text
npm ci
```

using the Gradle-provisioned npm.

`buildFrontend` runs:

```text
npm run build
```

which runs Vite and writes assets into `src/main/resources/public/gis/dist`.

The normal Java `processResources` task depends on `buildFrontend`, so a Gradle build copies the generated frontend files into:

```text
build/resources/main/public/gis/dist
```

Those files are then included in the starter jar. This is what lets a consuming app serve the GIS TypeScript and CSS from the GIS starter dependency.

## Creating a GIS Freemarker Macros

A test macro would be:

```ftl
<#macro testMap height="500px" colour="">
  <div
      id="test-map"
      data-gis-map-height="${height}"
      <#if colour?has_content>data-gis-map-colour="${colour}"</#if>
   >
  </div>
</#macro>

```

### Passing props to the Vue component

Notice how the macro takes a `height` and `colour` parameter. These can be passed to the Vue component as props when
being assigned to the `data-gis-map-height` and `data-gis-map-colour` attributes on the div.
These attributes should be captured on `gis-all` and passed to the Vue component as follows:

```ts
for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='test-map']")) {
    createApp(TestMap, {
        height: element.dataset.gisMapHeight || "500px",
    })
        .use(OpenLayersMap)
        .mount(element);
}

```
We can define default values for props with missing values like the colour prop that defaults to blue.

### Asset Loading on the client

Note the `assets` macro renders the CSS and JavaScript bundle URLs. Which is why it is included in the `testMap` macro,
but can be disabled if the consuming app already includes those assets to avoid duplicate loading.

The actual URLs for the assets are generated with Spring's FreeMarker URL macro, so the consuming app's context path is included automatically. For LMS, those URLs become:

```text
/lms/gis/dist/gis-framework.css
/lms/gis/dist/gis-bundle.js
```

The macro renders a div with the `data-gis-component` attribute set to `test-map`.

```html
<div data-gis-component="test-map" data-gis-map-height="500px"></div>
```

The `gis-all.ts` bundle finds elements with:

```text
[data-gis-component='test-map']
```

and mounts the Vue component onto each matching element.

## Consuming GIS Macros Example

In LMS or another consuming FreeMarker app:

```ftl
<#import "../gis/components/testMap/testMap.ftl" as gis>

<@gis.testMap />
```

The macro includes assets by default. If a page renders multiple GIS components, include the assets once to avoid duplicate loading:

```ftl
<#import "/gis/components/testMap/testMap.ftl" as gis>

<@gis.assets />
<@gis.testMap includeAssets=false />
<@gis.testMap includeAssets=false height="700px" />
```

## Static Resource Requirements In The Consuming App

If the consuming app uses Spring Boot's default static resource handling, resources under `classpath:/public` are served automatically.

If the consuming app defines its own `WebMvcConfigurer#addResourceHandlers`, make sure it also serves the GIS resource path:

```java
registry.addResourceHandler("/gis/**")
    .addResourceLocations("classpath:/public/gis/");
```

If the consuming app has security rules for static assets, make sure `/gis/**` is handled consistently with other static assets. For example:

```java
.requestMatchers("/assets/**", "/gis/**", "/error", "/actuator/health")
  .permitAll()
```

If the app uses MVC interceptors and excludes static assets, exclude `/gis/**` there too.

## Local Development

To build the frontend directly:

```text
npm run build
```

To build it through Gradle, using the Gradle-provisioned Node/npm toolchain:

```text
./gradlew buildFrontend
```

To package the starter jar with frontend assets:

```text
./gradlew build
```

The important packaging guarantee is:

```text
processResources -> buildFrontend -> Vite assets exist -> starter jar contains /public/gis/dist
```
