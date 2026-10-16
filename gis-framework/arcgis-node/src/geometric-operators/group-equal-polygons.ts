import { polygonsAreTopologicallyEqual } from "./polygon-equality-operator";

export interface EsriJsonPolygonWithId {
  id: string,
  esriJsonPolygon: string,
}

/**
 * Groups polygons that are topologically equal, i.e. cover exactly the same area.
 * @param polygons The polygons to group, each with an id.
 * @return The ids of each group of equal polygons. A polygon equal to no other is a group of its own.
 */
export function groupEqualPolygons(polygons: EsriJsonPolygonWithId[]): string[][] {
  const groups: EsriJsonPolygonWithId[][] = [];
  for (const polygon of polygons) {
    const group = groups.find(existing =>
      polygonsAreTopologicallyEqual(existing[0].esriJsonPolygon, polygon.esriJsonPolygon));
    if (group) {
      group.push(polygon);
    } else {
      groups.push([polygon]);
    }
  }
  return groups.map(group => group.map(polygon => polygon.id));
}
