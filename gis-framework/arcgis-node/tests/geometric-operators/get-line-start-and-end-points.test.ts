import { beforeEach, describe, expect, it, vi } from "vitest";
import { getLineStartAndEndPoints } from "../../src/geometric-operators/get-line-start-and-end-points";
import * as projectPolygon from "../../src/geometric-operators/project-polygon";
import { makePoint, makePolyline } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/project-polygon");

const spatialReferenceWkid = 27700;

describe("getLineStartAndEndPoints", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return the first and last points for each line", async () => {
    const firstLine = makePolyline(
      [
        [
          [0, 0],
          [5, 5],
          [10, 10],
        ],
      ],
      spatialReferenceWkid,
    );
    const secondLine = makePolyline(
      [
        [
          [20, 20],
          [25, 25],
        ],
      ],
      spatialReferenceWkid,
    );

    const result = await getLineStartAndEndPoints(
      [
        { id: "line-1", polyline: firstLine },
        { id: "line-2", polyline: secondLine },
      ],
      false,
    );

    expect(result).toEqual([
      {
        lineId: "line-1",
        startPoint: { x: 0, y: 0 },
        endPoint: { x: 10, y: 10 },
      },
      {
        lineId: "line-2",
        startPoint: { x: 20, y: 20 },
        endPoint: { x: 25, y: 25 },
      },
    ]);
    expect(projectPolygon.projectPointToWgs84).not.toHaveBeenCalled();
  });

  it("should return empty when no lines are provided", async () => {
    expect(await getLineStartAndEndPoints([], false)).toEqual([]);
  });

  it("should throw when a line does not have path", async () => {
    const emptyLine = makePolyline([[]], spatialReferenceWkid);

    await expect(getLineStartAndEndPoints([{ id: "empty-line", polyline: emptyLine }], false))
      .rejects
      .toThrow("Line empty-line has no path");
  });

  it("should project the start and end points to WGS84 when shouldProjectToWgs84 is true", async () => {
    const line = makePolyline(
      [
        [
          [0, 0],
          [10, 10],
        ],
      ],
      spatialReferenceWkid,
    );

    const projectedStartPoint = makePoint(1.1, 2.2, 4326);
    const projectedEndPoint = makePoint(3.3, 4.4, 4326);
    vi.mocked(projectPolygon.projectPointToWgs84)
      .mockResolvedValueOnce(projectedStartPoint)
      .mockResolvedValueOnce(projectedEndPoint);

    const result = await getLineStartAndEndPoints([{ id: "line-1", polyline: line }], true);

    expect(result).toEqual([
      {
        lineId: "line-1",
        startPoint: { x: 1.1, y: 2.2 },
        endPoint: { x: 3.3, y: 4.4 },
      },
    ]);
    expect(projectPolygon.projectPointToWgs84).toHaveBeenCalledTimes(2);
  });
});
