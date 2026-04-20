import * as containsOperator from '@arcgis/core/geometry/operators/containsOperator.js';
import { esriJsonToPolygon } from '../util/esrijson-util';

/**
 * Verifies that a parent polygon fully contains a child polygon.
 * @param parentEsriJson
 * @param childEsriJson
 * @return true if the parent polygon fully contains the child polygon, false otherwise.
 */
export function parentContainsChild(parentEsriJson: string, childEsriJson: string): boolean {
  const parent = esriJsonToPolygon(parentEsriJson);
  const child = esriJsonToPolygon(childEsriJson);

  return containsOperator.execute(parent, child);
}
