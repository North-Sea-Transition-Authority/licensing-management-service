# REST Endpoint Authentication — Spike

Context: the gis-framework starter needs to expose REST endpoints (e.g. split, undo, redo) called by the Vue frontend. These endpoints require some form of authentication and feature-level authorisation.
The starter must not impose its own auth model on consuming apps.
Some of the rest endpoint that might be needed are:

- split
- merge
- undo
- redo
- get feature esriJsons

Some of these endpoints will trigger a POST and modify the entities. Therefore, we need to ensure that the user has the appropriate permissions to see/update certain features.

---

## Option 1 — Starter-owned endpoints with a security hook

The starter auto-configures a `SecurityFilterChain` matching `/api/gis-framework/**` requiring `.anyRequest().authenticated()`. It also defines an interface the consuming app must implement:

```java
public interface GisFeatureAccessService {
  boolean canAccessFeature(UUID featureId);
}
```

The starter's REST controllers call this interface before executing any operation. The consuming app provides a bean with its own access logic.

**Pros**
- Consistent security baseline enforced across all consumers
- Minimal boilerplate for consumers — implement one interface

**Cons**
- The starter's `SecurityFilterChain` may conflict with the consuming app's existing security config (e.g. LMS uses SAML2 with its own filter chain ordering)
- There is no easy way to update or extend the logic on the rest controller to meet specific consumer needs.
- Couples the starter to Spring Security policy decisions

---

## Option 2 — No REST endpoints in the starter

The starter exposes only service methods. Each consuming app implements its own REST controllers, wires in the starter's services, and applies its own authentication and authorisation.
The starter will also have the POJO classes for the request and response objects (e.g. `SplitRequest`, `CommandResponse`) that the consuming app can use in its controllers.
The consuming app will need to pass the rest endpoint as a prop to the frontend.

```java
// LMS controller — full control over routing, auth, and extra logic
@RestController
@RequestMapping("/api/gis/split")
@HasAnyRole(roles = {Role.GIS_EDITOR})
public class SplitController {

  private final SplitOperatorService splitOperatorService;

  @PostMapping("/execute")
  public CommandResponse execute(@RequestBody SplitRequest request) {
    return splitOperatorService.executeSplit(request);
  }
}
```

**Pros**
- No security coupling in the starter
- Each consumer can apply its own auth model, interceptors, and extra logic
- Aligns with the Spring Boot starter philosophy: provide mechanisms, not policies

**Cons**
- More boilerplate per consumer.
- Risk of inconsistent implementations if multiple consumers exist. But since LMS is the only consumer right now, that boilerplate is one controller class.

---

## Option 3 — Abstract base controller (possible upgrade)

A middle ground. The starter provides an abstract controller class with the core implementation but no `@RestController`, `@RequestMapping`, or security annotations. The consuming app subclasses it and supplies those.
This can be done as an enhacement to Option 2 in the future but it is not needed right now to satisy our current needs.

```java
// in starter
public abstract class AbstractSplitController {
  protected CommandResponse executeSplit(SplitRequest request) { ... }
  protected CommandResponse undo(UUID journeyId) { ... }
  protected CommandResponse redo(UUID journeyId) { ... }
}

// in consuming app
@RestController
@RequestMapping("/api/gis/split")
@HasAnyRole(roles = {Role.GIS_EDITOR})
public class SplitController extends AbstractSplitController { }
```

**Pros**
- Starter owns the logic; consumer owns the contract
- Reduces boilerplate compared to Option 2 without imposing a security model
- Consistent implementation across consumers while remaining flexible

**Cons**
- Introduces inheritance, which limits further extension in the consuming app
- Abstract class API must be stable — breaking changes are harder to manage in a library

---

## Option 4 — Some rest endpoints in the starter, some in the consuming app

A middle ground. The starter provides a rest endpoint for stateles functions that do not modify the database (like the `getFeatureEsriJsons` endpoint) using the application's default security filter chain.
The consuming app provides its own implementation for the rest endpoints that modify the database (like the `executeSplit` endpoint). This way, the starter can enforce a security model for the stateless endpoints while allowing flexibility for the stateful endpoints.


**Pros**
- We can abstract more logic into the starter.
- Less boilerplate for consuming apps.
- Consuming apps can enforce their own security model for stateful endpoints.

**Cons**
- Starter relies on the consuming app security filter chain.
- Cannot have custom access logic for stateful endpoints. Anyone authenticated can access them.

---

## Selected approach

Option 2. We will start by having the rest controllers on LMS. Later in the project we will re-evaluate and check if there are common patterns we can extract into the starter.