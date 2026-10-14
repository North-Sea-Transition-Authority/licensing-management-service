# Split pipeline

## Context

A user needs to split a shape into several other shapes by drawing a cutter line on a map. A shape is stored as a 
`feature`, which is made up of `polygons` which is in turn made up of `lines`. Java does no geometry maths. All 
calculations go over gRPC to the ArcGIS node server. Some shapes may be 3D, but no 3D geometry is stored, they are 2D 
outlines extruded over a depth range. A split happens in a `CommandJourney` so it is recorded and can be undone or 
redone.

## Pipeline stages

The whole split runs in a single transaction, so a failure at any stage rolls back everything, leaving the journey as
it was before the split.

1. **Frontend capture** — `useSplitFeature` calls the `operator-api` and posts the cutter line coordinates, the 
   `commandJourneyId` and an optional `depth` (`SplitFromMapRequest`).

2. **Snap points** — the cutter line is drawn by clicking on snap points on the map, so every vertex lands on
   an exact grid position in the features' coordinate system.
    - `SnapPointsLayer` takes the visible map extent (WGS84, as the map uses `useGeographic()`), converts it to the
      features' coordinate system, and `generateSnapPoints` builds a regular grid in that system. Spacing depends on the
      zoom level and coordinate system (arc-seconds for ED50/ETRS89, metres for BNG), and no points are generated below
      a minimum zoom.
    - Each point is projected on the fly to WGS84 (`coordinates`) so OpenLayers can draw it in the right place, while
      the untouched grid value is kept on the point as `originalSrsCoordinates`.
    - Points can also be typed in on the coordinate entry page (`SplitByCoordinateEntryPage`). The typed values become
      `originalSrsCoordinates` directly and are projected to WGS84 for display in the same way.
    - When the line is submitted, `splitFeature` in `operator.api.ts` sends only `originalSrsCoordinates`, as 2-point
      segments. The projected WGS84 values are used for display only and are never sent back, so the cut is calculated
      on the exact grid coordinates with no reprojection rounding.

3. **Select input features** — `OperatorCommandReceiver.getFeaturesToSplit` takes the journey's active features, filtered
   to those with a polygon covering the depth (or all of them if the depth is null). Only active features are considered 
   so that a split always applies to the journey's current state.

4. **Build cutter line** — The `OperatorCommandReceiver` then converts the coordinates to an EsriJSON polyline in the 
   features' coordinate system (WKID) via the `convertPointsToPolyline` RPC. The line must share the features' 
   coordinate system, otherwise the cut would be calculated against the wrong positions.

5. **Split each polygon** — `SplitOperatorService.splitPolygon` makes a call to the `splitPolygon` RPC and cuts every 
   polygon covering the depth with the same 2D line. Polygons that aren't cut, or don't cover the depth, are carried 
   over unchanged. A single 2D cut divides the polygon at every depth it spans.

6. **Node-side cut** (`split-operator.ts`):
   - `removeOverlappingSegments` ensures that the cutter line does not backtrack, and continues in one direction.
   - `cutOperator`performs the cut, then `multiPartToSinglePart` is called so each disjoint piece becomes its own 
     polygon.
   - Checks a cut happened: if a cut returns more pieces than were input, then return the new pieces, otherwise 
     return `[]`. Without this check, a target that was already multipart would come back as its existing parts and 
     look like a successful split.

7. **Result processing** (`OperatorResultProcessingService.processOutputPolygon`), per output polygon:
   - Explode into lines and match them to the parent lines to copy attributes (new cut edges get none). Edges that 
     existed before the split keep their history; edges created by the cutter line are new and have no history to keep.
   - Build the new `Feature` and `Polygon`, keeping the source polygon's attributes and depths, with a numbered name
     suffix.
   - `numberLines` assigns ring numbers and connection order (top-most then west-most polygon first, outer ring before
     holes). The numbering is not used to rebuild the polygon, as the node server's `linesToPolygonsOperator` ignores
     it. It is used to generate the feature's textual description, so it must be consistent.
   - `validateLinesAreValid` checks the lines reconstruct the polygon returned by the node server, by comparing the union
     of the split result with the original polygon. Lines that don't rebuild the same polygon would mean a corrupt 
     shape, so the split fails rather than persisting it.
   - The new features, polygons, and lines are persisted to the database.

8. **Journey processing** — The `OperatorCommandReceiver` clears the undo stack, records a `SPLIT` `OperatorCommand` 
   against only the features that were cut, deactivates those inputs, creates journey states for the outputs, and 
   returns the output feature ids (`JsonSplitResponse`). A new command discards any undone commands, so redo history 
   can't be applied to a state it wasn't recorded against. Only features that were actually cut are recorded and 
   deactivated, so features the line missed stay as they are.

If no polygon is cut, the response is empty: nothing is persisted, the undo stack is not cleared, and the frontend
shows "No split took place. Make sure your line crosses the feature boundary."
