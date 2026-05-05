# "New" Flag Functionality using Composite Key Approach

* Status: proposed
* Date: 2026-05-01
* Author: Nischal Malla

Technical Story: The "New" flag functionality is designed to show users which work area items they have not yet viewed, or which have become relevant again due to a status change. It relies on tracking the viewed items and viewers of the items.

## Context and Problem Statement

How can we ensure the [NEW] flag correctly appears for work area items that users have not yet viewed, and how can we guarantee the [NEW] flag reappears when an item's status or assignment changes, even if the user has viewed it before?

## Decision Drivers

* Need to handle multiple user scenarios where an unassigned item is "new" for everyone until they view it individually.
* Need to prevent unmanageable database table growth.
* URLs can change, breaking the references, so tracking mechanisms must be robust.
* Must handle state changes automatically.

## Considered Options

* Option 1: URL-Based Tracking
* Option 2: Store "New" Items Instead of "Viewed" Items
* Option 3: Composite Key Approach with Hibernate Event Listeners

## Pros and Cons of the Options

### Option 1: URL-Based Tracking

Store the viewed item URL as a primary key in the work area flag table.

* Good, because it could potentially work across different entity types.
* Bad, because URLs can change, which breaks the reference.
* Bad, because extracting IDs from URLs introduces unnecessary complexity.

### Option 2: Store "New" Items Instead of "Viewed" Items

Maintain a list of items that haven't been viewed yet.

* Good, because it is conceptually simple.
* Bad, because it complicates multiple user scenarios, as everyone would see the same "new" list.
* Bad, because table growth is extremely high since it would store every unviewed item for every user.

### Option 3: Composite Key Approach with Hibernate Event Listeners

Store viewed items in a `work_area_item_views` (or `work_area_item_log`) table with a composite key consisting of Item Type, Item ID, and User ID. An interceptor triggers via `@LogViewedWorkAreaItem` when a user navigates to an item's detail page, and a `HibernatePostUpdateEventListener` clears the logs automatically on status changes.

* Good, because each user gets their own log.
* Good, because state changes automatically clear the flag via Hibernate events.
* Good, because logging only viewed items prevents an unmanageable table size.

## Decision Outcome

Chosen option: "Option 3: Composite Key Approach store the viewed applications on the database for a user

### Positive Consequences

* Each user gets their own log, so an unassigned item is "new" for everyone until they view it individually.
* Works efficiently with multiple user scenarios because the log is strictly per user.
* State changes (like stage or status updates from DRAFT to SUBMITTED) automatically trigger a service to clear the flag log via `HibernatePostUpdateEventListener`.
* Only viewed items are logged, preventing high and unmanageable table growth.

### Negative Consequences

* Lacks actor/role-based awareness: Because the `HibernatePostUpdateEventListener` automatically clears the view log on a status change, it clears it for everyone, including the person who triggered the change. For example, when a submitter changes an application from DRAFT to SUBMITTED, the item will immediately reappear as [NEW] in their own work area, even though they were the ones who just submitted it.
* To fix this in the future, we would need to add logic to the event listener to identify the "actor" making the change and preserve their specific viewed log.
