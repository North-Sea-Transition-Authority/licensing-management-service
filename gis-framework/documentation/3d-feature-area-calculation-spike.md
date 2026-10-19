# Spike: Area calculation for features at different height levels

## Problem
Currently, `OperatorResultProcessingService` sums the area of each polygon in the feature, but it
repeatedly includes any area shared by polygons at different depths.

## Approach
A feature's area is its **plan area**: the 2D footprint covered at any depth, with every overlap counted once.

Projecting every polygon onto the surface and taking the union gives the same footprint as slicing at each depth and 
unioning the slices.

1. **Get all the polygons** of the feature, ignoring start and end depths.

2. **Build each polygon** from its lines. If the coordinate system is not British National Grid, densify the loxodrome 
   edges first so the geodetic area operator measures them correctly.

3. **Union** every polygon into one 2D shape.
    - Polygons stacked at different depths with the same footprint collapse into one.
    - Polygons that partly overlap in plan only count the overlap once.
    - Disjoint polygons stay as separate parts of one multipart polygon.
   
4. **Calculate the area** of the union based on the feature's coordinate system: planar for British National Grid, 
   geodetic for everything else.

Cost: one union and one area calculation per feature, in a single gRPC call, whatever the number of depth levels.

## Implementation
Either change the existing `CalculateAreaRequest` to take all the polygons in the feature, or create a new RPC.
The densify loxodrome part of `densifyLoxodromesAndCalculateArea` can be extracted to a shared helper.

`buildFeature` (merge/split path) passes all output polygons' lines to the existing `calculateArea` as one flat
list. These could be overlapping polygons at different depths, so it must pass them grouped by polygon. A flat list is 
built into one shape, losing disjoint parts and miscounting overlaps.
