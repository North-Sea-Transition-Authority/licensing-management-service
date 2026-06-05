import {
  bngToWgs84,
  ed50ToWgs84,
  SupportedWkid,
  wgs84ToBng,
  wgs84ToEd50,
} from "./coordinate-system-utils";

const GRID_CONFIGS = {
  [SupportedWkid.ED50_WKID]: {
    // 1 degree = 3600 arc seconds
    spacingArcSeconds: 30, //must be divisor of arcSecondsPerDegree to match NSTA's quad/bocks
    arcSecondsPerDegree: 3600,
    originLon: 0,  // Grid aligned to whole degrees
    originLat: 0,
  },
  [SupportedWkid.BNG_WKID]: {
    spacingMeters: 500,
    originLon: 0,
    originLat: 0,
  },
} as const;

export interface SnapPoint {
  id: string,
  coordinates: [number, number],
  originalSrsCoordinates: [number, number],
}

/**
 * Generate snap points for a given WGS84 map extent and spatial reference.
 * @param {number} wgs84MinLon - West bound of the map extent.
 * @param {number} wgs84MinLat - South bound of the map extent.
 * @param {number} wgs84MaxLon - East bound of the map extent.
 * @param {number} wgs84MaxLat - North bound of the map extent.
 * @param {SupportedWkid} srsWkid - Spatial reference WKID.
 * @param {number} snapPointSpacing - Optional grid spacing override.
 * @returns {SnapPoint[]} Array of snap points with WGS84 coordinates.
 */
export function generateSnapPoints(
    wgs84MinLon: number,
    wgs84MinLat: number,
    wgs84MaxLon: number,
    wgs84MaxLat: number,
    srsWkid: SupportedWkid,
    snapPointSpacing?: number,
): SnapPoint[] {
  let srsMin: [number, number];
  let srsMax: [number, number];
  if (srsWkid === SupportedWkid.ED50_WKID) {
    srsMin = wgs84ToEd50(wgs84MinLon, wgs84MinLat);
    srsMax = wgs84ToEd50(wgs84MaxLon, wgs84MaxLat);
  } else {
    srsMin = wgs84ToBng(wgs84MinLon, wgs84MinLat);
    srsMax = wgs84ToBng(wgs84MaxLon, wgs84MaxLat);
  }

  const minIndexX = coordToGridIndex(srsMin[0], srsWkid, false, snapPointSpacing);
  const maxIndexX = coordToGridIndex(srsMax[0], srsWkid, false, snapPointSpacing);
  const minIndexY = coordToGridIndex(srsMin[1], srsWkid, true, snapPointSpacing);
  const maxIndexY = coordToGridIndex(srsMax[1], srsWkid, true, snapPointSpacing);

  const points: SnapPoint[] = [];
  for (let indexX = minIndexX; indexX <= maxIndexX; indexX++) {
    for (let indexY = minIndexY; indexY <= maxIndexY; indexY++) {
      const srsLon = gridIndexToCoord(indexX, srsWkid, false, snapPointSpacing);
      const srsLat = gridIndexToCoord(indexY, srsWkid, true, snapPointSpacing);

      const wgs84Coordinates = srsWkid === SupportedWkid.ED50_WKID
          ? ed50ToWgs84(srsLon, srsLat)
          : bngToWgs84(srsLon, srsLat);

      points.push({
        id: createGridPointId(indexX, indexY),
        coordinates: wgs84Coordinates,
        originalSrsCoordinates: [srsLon, srsLat],
      });
    }
  }
  return points;
}

function getGridSpacing(srsWkid: SupportedWkid, gridSpacing?: number): number {
  if (gridSpacing !== undefined) {
    if (gridSpacing <= 0) {
      throw new Error(`Grid spacing must be greater than zero: ${gridSpacing}`);
    }
    return gridSpacing;
  }

  if (srsWkid === SupportedWkid.ED50_WKID) {
    return GRID_CONFIGS[SupportedWkid.ED50_WKID].spacingArcSeconds;
  }

  return GRID_CONFIGS[SupportedWkid.BNG_WKID].spacingMeters;
}

function assertSupportedGridSrsWkid(srsWkid: SupportedWkid): asserts srsWkid is SupportedWkid {
  const supportedValues = Object.values(SupportedWkid);
  if (!supportedValues.includes(srsWkid)) {
    throw new Error(`Unsupported SRS WKID: ${srsWkid}`);
  }
}

/**
 * Convert coordinate to grid index
 * @param {number} coord - Coordinate in original SRS
 * @param {number} srsWkid - Spatial reference WKID
 * @param {boolean} isLat - true for latitude/northing, false for longitude/easting
 * @param {number} gridSpacing - Optional grid spacing. ED50 uses arc seconds; BNG uses metres. Will use default spacing
 * if not provided.
 * @returns {number} Integer grid index
 */
function coordToGridIndex(coord: number, srsWkid: SupportedWkid, isLat: boolean, gridSpacing?: number): number {
  assertSupportedGridSrsWkid(srsWkid);
  const spacing = getGridSpacing(srsWkid, gridSpacing);

  if (srsWkid === SupportedWkid.ED50_WKID) {
    const config = GRID_CONFIGS[SupportedWkid.ED50_WKID];
    // Convert degrees to arc seconds for exact integer arithmetic
    const arcSeconds = Math.round(coord * config.arcSecondsPerDegree);
    const origin = isLat ? config.originLat : config.originLon;
    const originArcSeconds = origin * config.arcSecondsPerDegree;
    return Math.round((arcSeconds - originArcSeconds) / spacing);
  }

  const config = GRID_CONFIGS[SupportedWkid.BNG_WKID];
  const origin = isLat ? config.originLat : config.originLon;
  return Math.round((coord - origin) / spacing);
}

/**
 * Convert grid index to coordinate
 * @param index {number} - Integer grid index
 * @param srsWkid {SupportedWkid} - Spatial reference WKID
 * @param isLat {boolean} - true for latitude/northing, false for longitude/easting
 * @param gridSpacing {number} - Optional grid spacing. ED50 uses arc seconds; BNG uses metres. Will use default spacing
 * if not provided.
 * @returns number Coordinate in original SRS
 */
function gridIndexToCoord(index: number, srsWkid: SupportedWkid, isLat: boolean, gridSpacing?: number): number {
  assertSupportedGridSrsWkid(srsWkid);
  const spacing = getGridSpacing(srsWkid, gridSpacing);

  if (srsWkid === SupportedWkid.ED50_WKID) {
    const config = GRID_CONFIGS[SupportedWkid.ED50_WKID];
    const origin = isLat ? config.originLat : config.originLon;
    const originArcSeconds = origin * config.arcSecondsPerDegree;
    const arcSeconds = originArcSeconds + (index * spacing);
    return arcSeconds / config.arcSecondsPerDegree;
  }

  const config = GRID_CONFIGS[SupportedWkid.BNG_WKID];
  const origin = isLat ? config.originLat : config.originLon;
  return origin + (index * spacing);
}

/**
 * Create grid point ID from indices
 * @param {number} indexX - Longitude/easting index
 * @param {number} indexY - Latitude/northing index
 * @returns {string} Canonical point ID
 */
function createGridPointId(indexX: number, indexY: number): string {
  return `${indexX},${indexY}`;
}
