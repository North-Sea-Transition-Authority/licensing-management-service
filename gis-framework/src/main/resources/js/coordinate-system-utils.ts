import proj4 from "proj4";

//ED50 (wkid:4230) as per https://epsg.io/4230
proj4.defs('EPSG:4230', '+proj=longlat +ellps=intl +towgs84=-87,-98,-121,0,0,0,0 +no_defs');
//British national grid (wkid: 27700) as per https://epsg.io/27700
proj4.defs('EPSG:27700', '+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 +y_0=-100000 +ellps=airy +towgs84=446.448,-125.157,542.06,0.15,0.247,0.842,-20.489 +units=m +no_defs');

export const SupportedWkid = {
  ED50_WKID: 4230,
  BNG_WKID: 27700
} as const;

export type SupportedWkid = typeof SupportedWkid[keyof typeof SupportedWkid];

/**
 * Converts WGS84 (EPSG:4326) coordinates to ED50 (EPSG:4230).
 * @param longitude WGS84 longitude
 * @param latitude WGS84 latitude
 * @returns {[number, number]} ED50 [longitude, latitude]
 */
export function wgs84ToEd50(longitude: number, latitude: number): [number, number] {
  return proj4('EPSG:4326', 'EPSG:4230', [longitude, latitude]);
}

/**
 * Converts WGS84 (EPSG:4326) coordinates to British National Grid (EPSG:27700).
 * @param longitude WGS84 longitude
 * @param latitude WGS84 latitude
 * @returns {[number, number]} BNG [easting, northing]
 */
export function wgs84ToBng(longitude: number, latitude: number): [number, number] {
  return proj4('EPSG:4326', 'EPSG:27700', [longitude, latitude]);
}

/**
 * Converts ED50 (EPSG:4230) coordinates to WGS84 (EPSG:4326).
 * @param longitude ED50 longitude
 * @param latitude ED50 latitude
 * @returns {[number, number]} WGS84 [longitude, latitude]
 */
export function ed50ToWgs84(longitude: number, latitude: number): [number, number] {
  return proj4('EPSG:4230', 'EPSG:4326', [longitude, latitude]);
}

/**
 * Converts British National Grid (EPSG:27700) coordinates to WGS84 (EPSG:4326).
 * @param easting BNG easting
 * @param northing BNG northing
 * @returns {[number, number]} WGS84 [longitude, latitude]
 */
export function bngToWgs84(easting: number, northing: number): [number, number] {
  return proj4('EPSG:27700', 'EPSG:4326', [easting, northing]);
}
