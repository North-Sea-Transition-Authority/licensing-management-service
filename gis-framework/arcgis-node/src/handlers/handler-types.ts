import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";

export type MergeAndGeneralizeLinesHandler = ArcGisServiceHandlers["mergeAndGeneralizeLines"];
export type MergePolygonsHandler = ArcGisServiceHandlers["mergePolygons"];
export type SplitPolygonHandler = ArcGisServiceHandlers["splitPolygon"];
export type BuildPolygonHandler = ArcGisServiceHandlers["buildPolygon"];
export type ExplodePolygonHandler = ArcGisServiceHandlers["explodePolygon"];
export type GeneralizePolygonHandler = ArcGisServiceHandlers["generalizePolygon"];
export type CalculateAreaHandler = ArcGisServiceHandlers["calculateArea"];
export type CoordinatesToPolylineHandler = ArcGisServiceHandlers["coordinatesToPolyline"];
export type FindParentLinesHandler = ArcGisServiceHandlers["findParentLines"];
export type FindNorthwestMostLineHandler = ArcGisServiceHandlers["findNorthwestMostLine"];
export type GetLineStartAndEndPointsHandler = ArcGisServiceHandlers["getLineStartAndEndPoints"];
export type ValidatePolygonReconstructionFromPolylinesHandler = ArcGisServiceHandlers["validatePolygonReconstructionFromPolylines"];
export type PolygonContainsHandler = ArcGisServiceHandlers["polygonContains"];
