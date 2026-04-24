import { describe, expect, test } from 'vitest';
import { getCoordinateSystemWkid } from '../../../src/util/coordinate-system-utils.ts';
import { CoordinateSystem } from '../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts';
import { fixDirectionOfAllLines } from '../../../src/migration/utils/fix-direction-of-all-lines.ts';
import { makePolyline, makeLineWithNavigationAndId } from '../../test-utils/esrijson-test-util.ts';

const ED50_WKID = getCoordinateSystemWkid(CoordinateSystem.ED50);

describe('fixDirectionOfAllLines', () => {
  test('reverses a middle line that is going the wrong way', () => {
    // 4 lines forming a ring: line1 -> line2 (reversed) -> line3 -> line4
    // line1: (0,0) -> (1,0)
    // line2: (2,0) -> (1,0)  <-- wrong direction, should be (1,0) -> (2,0)
    // line3: (2,0) -> (3,0)
    // line4: (3,0) -> (0,0)
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [2, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    ); // wrong way
    const line3 = makePolyline(
      [
        [
          [2, 0],
          [3, 0],
        ],
      ],
      ED50_WKID,
    );
    const line4 = makePolyline(
      [
        [
          [3, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));
    idToLineWithNavigationWrapper.set(4, makeLineWithNavigationAndId(line4, LineNavigationType.LOXODROME, 4));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 3, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 4, connectionOrder: 4, ringNumber: 1 },
    ];

    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    // line2 should be reversed to (1,0) -> (2,0)
    const expectedLine2 = makePolyline(
      [
        [
          [1, 0],
          [2, 0],
        ],
      ],
      ED50_WKID,
    );

    const expectedMap = new Map();
    expectedMap.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    expectedMap.set(2, makeLineWithNavigationAndId(expectedLine2, LineNavigationType.LOXODROME, 2));
    expectedMap.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));
    expectedMap.set(4, makeLineWithNavigationAndId(line4, LineNavigationType.LOXODROME, 4));

    expect(idToLineWithNavigationWrapper).toEqual(expectedMap);
  });

  test('reverses the last line that is going the wrong way', () => {
    // 4 lines forming a ring: line1 -> line2 -> line3 -> line4 (reversed)
    // line1: (0,0) -> (1,0)
    // line2: (1,0) -> (2,0)
    // line3: (2,0) -> (3,0)
    // line4: (0,0) -> (3,0)  <-- wrong direction, should be (3,0) -> (0,0)
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [1, 0],
          [2, 0],
        ],
      ],
      ED50_WKID,
    );
    const line3 = makePolyline(
      [
        [
          [2, 0],
          [3, 0],
        ],
      ],
      ED50_WKID,
    );
    const line4 = makePolyline(
      [
        [
          [0, 0],
          [3, 0],
        ],
      ],
      ED50_WKID,
    ); // wrong way

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));
    idToLineWithNavigationWrapper.set(4, makeLineWithNavigationAndId(line4, LineNavigationType.LOXODROME, 4));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 3, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 4, connectionOrder: 4, ringNumber: 1 },
    ];

    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    // line4 should be reversed to (3,0) -> (0,0)
    const expectedLine4 = makePolyline(
      [
        [
          [3, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );

    const expectedMap = new Map();
    expectedMap.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    expectedMap.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    expectedMap.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));
    expectedMap.set(4, makeLineWithNavigationAndId(expectedLine4, LineNavigationType.LOXODROME, 4));

    expect(idToLineWithNavigationWrapper).toEqual(expectedMap);
  });

  test('does not reverse any lines when all directions are correct', () => {
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [1, 0],
          [2, 0],
        ],
      ],
      ED50_WKID,
    );
    const line3 = makePolyline(
      [
        [
          [2, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 3, ringNumber: 1 },
    ];

    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    // All lines should remain unchanged
    const expectedMap = new Map();
    expectedMap.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    expectedMap.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    expectedMap.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    expect(idToLineWithNavigationWrapper).toEqual(expectedMap);
  });

  test('handles multiple rings independently', () => {
    // Ring 1: line1 -> line2 (reversed)
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    ); // wrong way, should be (1,0) -> (0,0)

    // Ring 2: line3 -> line4 (reversed)
    const line3 = makePolyline(
      [
        [
          [5, 5],
          [6, 5],
        ],
      ],
      ED50_WKID,
    );
    const line4 = makePolyline(
      [
        [
          [5, 5],
          [6, 5],
        ],
      ],
      ED50_WKID,
    ); // wrong way, should be (6,5) -> (5,5)

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));
    idToLineWithNavigationWrapper.set(4, makeLineWithNavigationAndId(line4, LineNavigationType.LOXODROME, 4));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 1, ringNumber: 2 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 4, connectionOrder: 2, ringNumber: 2 },
    ];

    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    // Both line2 and line4 should be reversed
    const expectedLine2 = makePolyline(
      [
        [
          [1, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );
    const expectedLine4 = makePolyline(
      [
        [
          [6, 5],
          [5, 5],
        ],
      ],
      ED50_WKID,
    );

    const expectedMap = new Map();
    expectedMap.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    expectedMap.set(2, makeLineWithNavigationAndId(expectedLine2, LineNavigationType.LOXODROME, 2));
    expectedMap.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));
    expectedMap.set(4, makeLineWithNavigationAndId(expectedLine4, LineNavigationType.LOXODROME, 4));

    expect(idToLineWithNavigationWrapper).toEqual(expectedMap);
  });

  test('throws error when lines do not connect even after reversing', () => {
    // line1: (0,0) -> (1,0)
    // line2: (5,5) -> (6,6) -- completely disconnected, reversing won't help
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [5, 5],
          [6, 6],
        ],
      ],
      ED50_WKID,
    );

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
    ];

    expect(() => fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType)).toThrow();
  });

  test('throws error when line id is not found in idToLineWithNavigationWrapper', () => {
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );

    // Only line 1 in the map, but linesWithType references line 2
    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
    ];

    expect(() => fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType)).toThrow();
  });

  test('reverses the first line when it is going the wrong way', () => {
    // line1: (1,0) -> (0,0)  <-- wrong direction, should be (0,0) -> (1,0)
    // line2: (1,0) -> (2,0)
    // line3: (2,0) -> (0,0)
    const line1 = makePolyline(
      [
        [
          [1, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    ); // wrong way
    const line2 = makePolyline(
      [
        [
          [1, 0],
          [2, 0],
        ],
      ],
      ED50_WKID,
    );
    const line3 = makePolyline(
      [
        [
          [2, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 3, ringNumber: 1 },
    ];

    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    // line1 should be reversed to (0,0) -> (1,0)
    const expectedLine1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );

    const expectedMap = new Map();
    expectedMap.set(1, makeLineWithNavigationAndId(expectedLine1, LineNavigationType.LOXODROME, 1));
    expectedMap.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    expectedMap.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    expect(idToLineWithNavigationWrapper).toEqual(expectedMap);
  });

  test('reverses the first line and the next line when both are wrong', () => {
    // Reproduces the original bug: first line and next line both need reversing
    // line1: (1,0) -> (0,0)  <-- wrong
    // line2: (2,0) -> (1,0)  <-- also wrong
    // line3: (2,0) -> (0,0)
    const line1 = makePolyline(
      [
        [
          [1, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [2, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line3 = makePolyline(
      [
        [
          [2, 0],
          [0, 0],
        ],
      ],
      ED50_WKID,
    );

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 3, ringNumber: 1 },
    ];

    fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType);

    // line1 reversed: (0,0) -> (1,0), line2 reversed: (1,0) -> (2,0), line3 stays: (2,0) -> (0,0)
    const expectedLine1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const expectedLine2 = makePolyline(
      [
        [
          [1, 0],
          [2, 0],
        ],
      ],
      ED50_WKID,
    );

    const expectedMap = new Map();
    expectedMap.set(1, makeLineWithNavigationAndId(expectedLine1, LineNavigationType.LOXODROME, 1));
    expectedMap.set(2, makeLineWithNavigationAndId(expectedLine2, LineNavigationType.LOXODROME, 2));
    expectedMap.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    expect(idToLineWithNavigationWrapper).toEqual(expectedMap);
  });

  test('throws error when three lines form a broken chain that cannot be fixed by reversing', () => {
    // line1: (0,0) -> (1,0)
    // line2: (1,0) -> (2,0)  -- connects to line1 correctly
    // line3: (5,5) -> (6,6)  -- completely disconnected from line2, reversing gives (6,6) -> (5,5) which also doesn't connect
    const line1 = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      ED50_WKID,
    );
    const line2 = makePolyline(
      [
        [
          [1, 0],
          [2, 0],
        ],
      ],
      ED50_WKID,
    );
    const line3 = makePolyline(
      [
        [
          [5, 5],
          [6, 6],
        ],
      ],
      ED50_WKID,
    );

    const idToLineWithNavigationWrapper = new Map();
    idToLineWithNavigationWrapper.set(1, makeLineWithNavigationAndId(line1, LineNavigationType.LOXODROME, 1));
    idToLineWithNavigationWrapper.set(2, makeLineWithNavigationAndId(line2, LineNavigationType.LOXODROME, 2));
    idToLineWithNavigationWrapper.set(3, makeLineWithNavigationAndId(line3, LineNavigationType.LOXODROME, 3));

    const linesWithType = [
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 1, connectionOrder: 1, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 2, connectionOrder: 2, ringNumber: 1 },
      { geoJsonString: '', isGeodesic: false, oracleLineSsid: 3, connectionOrder: 3, ringNumber: 1 },
    ];

    expect(() => fixDirectionOfAllLines(idToLineWithNavigationWrapper, linesWithType)).toThrow();
  });
});
