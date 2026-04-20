export function makePolygonEsriJson(rings: number[][][]): string {
  return JSON.stringify({
    rings,
    spatialReference: { wkid: 4326 },
  });
}

export function makePolylineEsriJson(paths: number[][][]): string {
  return JSON.stringify({
    paths,
    spatialReference: { wkid: 4326 },
  });
}
