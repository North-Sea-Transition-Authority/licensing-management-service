import Polyline from '@arcgis/core/geometry/Polyline.js';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType';

export type LineWithNavigationTypeAndId = {
  line: Polyline;
  navigationType: LineNavigationType;
  id: number;
};
