import Polyline from '@arcgis/core/geometry/Polyline.js';
import { makeLineWithNavigationAndId } from './esrijson-test-util';
import { LineNavigationType } from '../../generated/uk/co/fivium/grpc/gis/LineNavigationType';

export const ED50_MIXED_POLYLINES = [
  makeLineWithNavigationAndId(
    Polyline.fromJSON(
      JSON.parse(`{"spatialReference":{"wkid":4230},"paths":[[[2.8912568728287478,53.8833333333333],[2.8,53.8833333333333]]]}`),
    ),
    LineNavigationType.LOXODROME,
    1,
  ),
  makeLineWithNavigationAndId(
    Polyline.fromJSON(
      JSON.parse(
        `{"spatialReference":{"wkid":4230},"paths":[[[2.8963374090220513,53.866666638946164],[2.8963372411295505,53.86666718996421],[2.8958026431325092,53.86842157449991],[2.8955353273567535,53.86929876568048],[2.8950006622466766,53.87105314586683],[2.8944659523863803,53.87280752315318],[2.894198580672851,53.873684710708744],[2.8936638036749294,53.87543908364444],[2.8931289819103907,53.87719345367929],[2.8928615542385896,53.87807063760879],[2.892326665311824,53.87982500329172],[2.891791731602035,53.881579366072955],[2.8912568728287478,53.8833333333333]]]}`,
      ),
    ),
    LineNavigationType.GEODESIC,
    2,
  ),
  makeLineWithNavigationAndId(
    Polyline.fromJSON(
      JSON.parse(`{"spatialReference":{"wkid":4230},"paths":[[[2.8,53.8722222222222],[2.8963374090220513,53.866666638946164]]]}`),
    ),
    LineNavigationType.LOXODROME,
    3,
  ),
  makeLineWithNavigationAndId(
    Polyline.fromJSON(JSON.parse(`{"spatialReference":{"wkid":4230},"paths":[[[2.8,53.8833333333333],[2.8,53.8722222222222]]]}`)),
    LineNavigationType.LOXODROME,
    4,
  ),
];
export const ED50_MIXED_POLYGON_AREA = 9485774.995286297;
