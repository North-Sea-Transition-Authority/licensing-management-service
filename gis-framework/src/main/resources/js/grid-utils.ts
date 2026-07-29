import {
  fromWgs84,
  isOffshore,
  SupportedWkid,
  toWgs84,
} from "./coordinate-system-utils";
import OsGridRef from "geodesy/osgridref.js";
import Dms from "geodesy/dms.js";

interface SrsGridConfig {
  /** Multiplier to convert from coord system units to spacing units (degrees to arc-seconds in ED50, metres to metres in BNG). */
  readonly coordUnitFactor: number;
  /** Minimum map zoom before snap points are generated. */
  readonly minSnapZoom: number;
  /** Zoom tiers as [minZoom, spacing] pairs, ordered highest zoom first. */
  readonly zoomTiers: number[][];
  /**
   * Snap coordinates to this resolution (in SRS units) when building IDs.
   * Must divide every spacing tier evenly so IDs are stable across tier changes.
   */
  readonly idResolution: number;
  /** Optional hard bounds in SRS units; coordinates outside this range are not generated. */
  readonly validBounds?: { minX: number; maxX: number; minY: number; maxY: number };
}

const GRID_CONFIGS: Record<SupportedWkid, SrsGridConfig> = {
  [SupportedWkid.ED50_WKID]: {
    coordUnitFactor: 3600, // 1 degree = 3600 arc-second
    minSnapZoom: 11,
    zoomTiers: [[15, 5], [14, 10], [13, 15], [12, 30], [11, 60]],
    idResolution: 1, // 1 arc-second, finest possible resolution
  },
  [SupportedWkid.ETRS89_WKID]: {
    coordUnitFactor: 3600, // 1 degree = 3600 arc-second
    minSnapZoom: 11,
    zoomTiers: [[15, 5], [14, 10], [13, 15], [12, 30], [11, 60]],
    idResolution: 1, // 1 arc-second, finest possible resolution
  },
  [SupportedWkid.BNG_WKID]: {
    coordUnitFactor: 1, // 1 m = 1m
    minSnapZoom: 12,
    zoomTiers: [[15, 100], [14, 250], [13, 500], [12, 1000]],
    idResolution: 50,  // 50m, finest possible resolution
    validBounds: { minX: 0, maxX: 700000, minY: 0, maxY: 1300000 },
  },
};

export interface LinePoint {
  coordinates: [number, number],
  originalSrsCoordinates: [number, number],
}

export interface SnapPoint extends LinePoint {
  id: string,
  displayName: string,
}

/**
 * Return the snap-point grid spacing for a given zoom level.
 * ED50 result is in arc-seconds; BNG result is in metres.
 */
export function getSpacingForZoom(zoom: number, srsWkid: SupportedWkid): number {
  const tiers = GRID_CONFIGS[srsWkid].zoomTiers;
  for (const [minZoom, spacing] of tiers) {
    if (zoom >= minZoom) return spacing;
  }
  return tiers[tiers.length - 1][1];
}

/**
 * Return the minimum map zoom level at which snap points should be generated for the given CRS.
 */
export function getMinSnapZoom(srsWkid: SupportedWkid): number {
  return GRID_CONFIGS[srsWkid].minSnapZoom;
}

/**
 * Return the hard coordinate bounds (in SRS units) for the given CRS, or undefined if unbounded.
 * Used to validate coordinate entry against the valid range of a projected grid (e.g. BNG).
 */
export function getValidBounds(srsWkid: SupportedWkid): SrsGridConfig["validBounds"] {
  return GRID_CONFIGS[srsWkid].validBounds;
}

/**
 * Generate snap points for a given WGS84 map extent and spatial reference.
 * @param {number} wgs84MinLon - West bound of the map extent.
 * @param {number} wgs84MinLat - South bound of the map extent.
 * @param {number} wgs84MaxLon - East bound of the map extent.
 * @param {number} wgs84MaxLat - North bound of the map extent.
 * @param {SupportedWkid} srsWkid - Spatial reference WKID.
 * @param {number} snapPointSpacing - Spacing between snap points.
 * @returns {SnapPoint[]} Array of snap points with WGS84 coordinates.
 */
export function generateSnapPoints(
    wgs84MinLon: number,
    wgs84MinLat: number,
    wgs84MaxLon: number,
    wgs84MaxLat: number,
    srsWkid: SupportedWkid,
    snapPointSpacing: number,
): SnapPoint[] {
  if (snapPointSpacing <= 0) {
    throw new Error(`Grid spacing must be greater than zero: ${snapPointSpacing}`);
  }
  const srsMin: [number, number] = fromWgs84(srsWkid, wgs84MinLon, wgs84MinLat);
  const srsMax: [number, number] = fromWgs84(srsWkid, wgs84MaxLon, wgs84MaxLat);


  const { validBounds } = GRID_CONFIGS[srsWkid];
  if (validBounds) {
    //Don't try to generate points outside valid bounds for coordinate system
    if (srsMin[0] > validBounds.maxX || srsMax[0] < validBounds.minX ||
        srsMin[1] > validBounds.maxY || srsMax[1] < validBounds.minY) {
      return [];
    }
    srsMin[0] = Math.max(srsMin[0], validBounds.minX);
    srsMin[1] = Math.max(srsMin[1], validBounds.minY);
    srsMax[0] = Math.min(srsMax[0], validBounds.maxX);
    srsMax[1] = Math.min(srsMax[1], validBounds.maxY);
  }

  const minIndexX = coordToGridIndex(srsMin[0], srsWkid, snapPointSpacing);
  const maxIndexX = coordToGridIndex(srsMax[0], srsWkid, snapPointSpacing);
  const minIndexY = coordToGridIndex(srsMin[1], srsWkid, snapPointSpacing);
  const maxIndexY = coordToGridIndex(srsMax[1], srsWkid, snapPointSpacing);

  const points: SnapPoint[] = [];
  for (let indexX = minIndexX; indexX <= maxIndexX; indexX++) {
    for (let indexY = minIndexY; indexY <= maxIndexY; indexY++) {
      const srsLon = gridIndexToCoord(indexX, srsWkid, snapPointSpacing);
      const srsLat = gridIndexToCoord(indexY, srsWkid, snapPointSpacing);

      const wgs84Coordinates = toWgs84(srsWkid, srsLon, srsLat);

      points.push({
        id: createGridPointId(srsLon, srsLat, srsWkid),
        coordinates: wgs84Coordinates,
        originalSrsCoordinates: [srsLon, srsLat],
        displayName: getCoordinateDisplayName(srsLon, srsLat, srsWkid)
      });
    }
  }
  return points;
}

function coordToGridIndex(coord: number, srsWkid: SupportedWkid, gridSpacing: number): number {
  return Math.round((coord * GRID_CONFIGS[srsWkid].coordUnitFactor) / gridSpacing);
}

function gridIndexToCoord(index: number, srsWkid: SupportedWkid, gridSpacing: number): number {
  return (index * gridSpacing) / GRID_CONFIGS[srsWkid].coordUnitFactor;
}

/**
 * Create a stable grid point ID from SRS coordinates.
 * Coordinates are expressed in idResolution units so IDs remain stable across spacing-tier changes.
 */
function createGridPointId(srsX: number, srsY: number, srsWkid: SupportedWkid): string {
  const { coordUnitFactor, idResolution } = GRID_CONFIGS[srsWkid];
  const idX = Math.round((srsX * coordUnitFactor) / idResolution);
  const idY = Math.round((srsY * coordUnitFactor) / idResolution);
  return `${idX},${idY}`;
}

function getCoordinateDisplayName(lon: number, lat: number, srsWkid: SupportedWkid): string {
  if (isOffshore(srsWkid)) {
    return convertLatLonToDms(lat, lon);
  } else {
    // Onshore (BNG) snap points use lon=easting, lat=northing (metres)
    return convertBngToGridReference(lon, lat);
  }
}

/**
 * Converts BNG metre coordinates (easting, northing) directly to an OS grid reference string.
 * lon == easting, lat == northing
 */
function convertBngToGridReference(easting: number, northing: number): string {
  const gridRef = new OsGridRef(easting, northing);
  return gridRef.toString(8);
}

/**
 * Converts latitude and longitude to degrees, minutes, seconds format.
 */
function convertLatLonToDms(lat: number, lon: number): string {
  const latDms = removeLeadingZeros(Dms.toLat(lat, 'dms', 0));
  const lonDms = removeLeadingZeros(Dms.toLon(lon, 'dms', 0));
  return `${latDms}\n${lonDms}`;
}

function removeLeadingZeros(str: string): string {
  return str.replace(/^0+(?=\d)/, '');
}