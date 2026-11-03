# Improving the correction timeline performance

* Status: Approved
* Date: 03/11/2026

## Context and Problem Statement

As part of the timeline validation when doing a correction we need to validate that licence blocks don't overlap with each other
after a correction, as this would result in the same area being licensed multiple times. This validation needs to be performed on
every partial surrender, block redefinition and block create after an operation of that type is added/updated in a correction.
Performing an overlaps check for blocks on a position against all other active blocks is quite computationally expensive,
resulting in the page taking over 1500ms to load locally after staging a single spatial operation.

## Considered Options

* 1: Accelerating the geometry
* 2: Caching
* 3: Filter before the overlaps check
* 4: Use crossesOperator instead of overlapsOperator

## Decision Outcome

Chosen option: "Filter before the overlaps check, and use crossesOperator instead of overlapsOperator",
because the initial main bottleneck was building polygons, which was taking 1242ms of total time. Using the crossesOperator, we
only need to build the resultant polygons from the operation, not all active polygons. Using a bounding box filter in Java we can
further reduce the amount of data sent via gRPC.

### Accelerating the geometry

* Good, because the ArcGIS JS relational operator providers all provide a way
  to [accelerate geometry](https://developers.arcgis.com/javascript/latest/spatial-analysis/intro-geometry-operators/#acceleration).
  Reading the docs this seemed like it could be useful as it is intended to be used when using the same shape in hundreds of the
  same operation, by constructing it in an optimised data structure.
* Bad, because after further investigating what was causing the most performance degradation, it looked to be mainly the building
  of the polygons, with the actual overlaps check taking only tens of milliseconds.

### Caching

* Good, because we could cache the built polygons once, or even the validation result, and then future page loads would be
  lightning fast.
* Bad, because it doesn't solve the issue with the first time load.

### Filter before the overlaps check

The filter involves making a bounding box of the resultant polygons, by getting the min X,Y and the max X,Y. We can then filter out
all lines that don't have any coordinates that fall inside that box.

* Good, because filtering in Java using a bounding box is cheap, and reduces the amount of work needed to be done on the node
  server.
* Bad, because the filtering only works for shapes in the same coordinate system.

### Use crossesOperator instead of overlapsOperator

* Good, because we can just use the lines from the other active polygons, and only build the resultant polygons from the
  operations. This massively reduces the amount of work needed on the node server.
* Bad, because it won't detect if a polygon is fully inside another polygon, however there shouldn't be any blocks that are in that
  scenario.

