import { describe, expect, test } from 'vitest';
import { makePolygon, makePolyline, makePolylineEsriJson } from '../test-utils/esrijson-test-util';
import { lineStringsToSinglePolygon, linesToSinglePolygon } from '../../src/geometric-operators/lines-to-single-polygon-operator';

describe('lines-to-single-polygon-operator', () => {
  describe('lineStringsToSinglePolygon', () => {
    test('should build a polygon from line strings forming a closed square', () => {
      const lineStrings = [
        makePolylineEsriJson([
          [
            [0, 0],
            [10, 0],
          ],
        ]),
        makePolylineEsriJson([
          [
            [10, 0],
            [10, 10],
          ],
        ]),
        makePolylineEsriJson([
          [
            [10, 10],
            [0, 10],
          ],
        ]),
        makePolylineEsriJson([
          [
            [0, 10],
            [0, 0],
          ],
        ]),
      ];

      const expected = makePolygon(
        [
          [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0],
          ],
        ],
        4326,
      );

      const result = lineStringsToSinglePolygon(lineStrings, 4326);

      expect(result).toEqual(expected);
    });

    test('should build a polygon from a single closed line string', () => {
      const lineStrings = [
        makePolylineEsriJson([
          [
            [0, 0],
            [5, 0],
            [5, 5],
            [0, 5],
            [0, 0],
          ],
        ]),
      ];

      const expected = makePolygon(
        [
          [
            [0, 0],
            [0, 5],
            [5, 5],
            [5, 0],
            [0, 0],
          ],
        ],
        4326,
      );

      const result = lineStringsToSinglePolygon(lineStrings, 4326);

      expect(result).toEqual(expected);
    });

    test('should build a donut polygon from an outer and inner closed line string', () => {
      const lineStrings = [
        makePolylineEsriJson([
          [
            [0, 0],
            [10, 0],
            [10, 10],
            [0, 10],
            [0, 0],
          ],
        ]),
        makePolylineEsriJson([
          [
            [3, 3],
            [3, 7],
            [7, 7],
            [7, 3],
            [3, 3],
          ],
        ]),
      ];

      const expected = makePolygon(
        [
          [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0],
          ],
          [
            [3, 3],
            [7, 3],
            [7, 7],
            [3, 7],
            [3, 3],
          ],
        ],
        4326,
      );

      const result = lineStringsToSinglePolygon(lineStrings, 4326);

      expect(result).toEqual(expected);
    });
  });

  describe('linesToSinglePolygon', () => {
    test('should build a polygon from polylines forming a closed square', () => {
      const polylines = [
        makePolyline(
          [
            [
              [0, 0],
              [10, 0],
            ],
          ],
          4326,
        ),
        makePolyline(
          [
            [
              [10, 0],
              [10, 10],
            ],
          ],
          4326,
        ),
        makePolyline(
          [
            [
              [10, 10],
              [0, 10],
            ],
          ],
          4326,
        ),
        makePolyline(
          [
            [
              [0, 10],
              [0, 0],
            ],
          ],
          4326,
        ),
      ];

      const expected = makePolygon(
        [
          [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0],
          ],
        ],
        4326,
      );

      const result = linesToSinglePolygon(polylines, 4326);

      expect(result).toEqual(expected);
    });

    test('should build a polygon from a single closed polyline', () => {
      const polylines = [
        makePolyline(
          [
            [
              [0, 0],
              [5, 0],
              [5, 5],
              [0, 5],
              [0, 0],
            ],
          ],
          4326,
        ),
      ];

      const expected = makePolygon(
        [
          [
            [0, 0],
            [0, 5],
            [5, 5],
            [5, 0],
            [0, 0],
          ],
        ],
        4326,
      );

      const result = linesToSinglePolygon(polylines, 4326);

      expect(result).toEqual(expected);
    });

    test('should build a donut polygon from an outer and inner closed polyline', () => {
      const polylines = [
        makePolyline(
          [
            [
              [0, 0],
              [10, 0],
              [10, 10],
              [0, 10],
              [0, 0],
            ],
          ],
          4326,
        ),
        makePolyline(
          [
            [
              [3, 3],
              [3, 7],
              [7, 7],
              [7, 3],
              [3, 3],
            ],
          ],
          4326,
        ),
      ];

      const expected = makePolygon(
        [
          [
            [0, 0],
            [0, 10],
            [10, 10],
            [10, 0],
            [0, 0],
          ],
          [
            [3, 3],
            [7, 3],
            [7, 7],
            [3, 7],
            [3, 3],
          ],
        ],
        4326,
      );

      const result = linesToSinglePolygon(polylines, 4326);

      expect(result).toEqual(expected);
    });
  });
});
