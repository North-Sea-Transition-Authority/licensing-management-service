# Shape data standardisation - Spike

Technical Story: https://fivium.atlassian.net/browse/EPGF-95

## Context and Problem Statement

We need a consistent way to update how shape data is displayed and interacted with per service depending on the layer and
coordinate system. At a minimum we want to specify custom colour schemes, coordinate entry precision, minimum snap point
resolution, and possibly more.

---

## How should we resolve the correct config?

### Option 1: Consumers resolve the necessary config before the page is loaded

* Good, because this would give consumers more freedom,
* Bad, because this could be a potential burden to remember to resolve the correct config.

### Option 2: Consumers provide all config variations, and the framework resolves which one is needed when a request is sent for a specific feature

* Good, because this would be more standardised and less work for consumers.

---

## Implementation of the config:

### Option 1: Framework defines a configuration class

On the framework we would provide these:

```java
record GisMetadataConfiguration(
    Map<Layer, LayerBasedGisMetadata> metadataByCoordinateSystemAndLayer
) {
}

record LayerBasedGisMetadata(
    String fillColour,
    Map<CoordinateSystem, SrsBasedGisMetadata> srsBasedGisMetadataByCoordinateSystem
) {
}

record SrsBasedGisMetadata(
    int minSnapPointResolution, // e.g. min distance between snap points.
    int coordinateEntryPrecision // e.g. number of decimal places
) {
}
```

The service could then create a .yaml file to define everything:

```yaml
metadataByCoordinateSystemAndLayer:
  SUB_AREAS:
    fillColour: ffffff
    SRS:
      ED50:
        minSnapPointResolution: 100
        coordinateEntryPrecision: 6
  BLOCKS:
    ...
```

The correct metadata is then either passed to Vue components from the service as props, or from the framework as JSON as part of
the request.

* Good, because YAML is easy for anyone to understand.
* Bad, because the mapping could become quite complex if we add lots of config.

### Option 2: Framework defines an interface

The interface would be in a similar structure as the configuration class, but it would instead allow for the config to be defined
in the code.

* Good, because there would be stronger typing, whereas a YAML could easily contain a typo, which might not get picked up.
* Bad, because the config just needs to be static and unchanging, whereas an interface opens the possibility for a programmatic
  config.

---

## Outcome

We have decided to go for option 2 in the first case and option 1 in the second case for now; however, once we know all the shape
data we will want to customize, we might be able to simplify the approach in the future using default values.

The endpoints the framework will use is influence by [rest-endpoint-authentication.md](rest-endpoint-authentication.md), where we
have decided to not have endpoint directly in the framework. Consumers will have to create the endpoints and provide them to the
frontend. The endpoints they make, however, will wrap service methods provided by the framework, allowing the framework to resolve
the correct config.