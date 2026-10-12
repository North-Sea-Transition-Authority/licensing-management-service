export interface EsriJsonPolygonWithId {
  id: string,
  esriJsonPolygon: string,
}

export interface FeaturePolygons {
  featureId: string,
  polygons: EsriJsonPolygonWithId[],
}
