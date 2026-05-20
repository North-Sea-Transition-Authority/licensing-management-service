import Polyline from "@arcgis/core/geometry/Polyline.js";
import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as findParentLinesModule from "../../src/geometric-operators/find-parent-lines";
import { findParentLinesHandler } from "../../src/handlers/find-parent-lines-handler";
import * as esriJsonUtil from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/util/esrijson-util");
vi.mock("../../src/geometric-operators/find-parent-lines");

describe("findParentLinesHandler", () => {
  let mockCallback: any;
  let mockCall: any;
  const testWkid = 4326;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        parentLines: [],
        childrenEsriJsonPolylines: [],
      },
    };
  });

  it("should return a successful callback with parent matches and orphaned children", () => {
    const parentLineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [10, 0],
      ],
    ]);
    const childLineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [5, 0],
      ],
    ]);
    const orphanedChildEsriJson = makePolylineEsriJson([
      [
        [20, 20],
        [30, 20],
      ],
    ]);

    mockCall.request.parentLines = [{ id: "parent-1", esriJsonPolyline: parentLineEsriJson }];
    mockCall.request.childrenEsriJsonPolylines = [childLineEsriJson, orphanedChildEsriJson];

    const mockParentLine = new Polyline({
      paths: [
        [
          [0, 0],
          [10, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });
    const mockChildLine = new Polyline({
      paths: [
        [
          [0, 0],
          [5, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });
    const mockOrphanedChildLine = new Polyline({
      paths: [
        [
          [20, 20],
          [30, 20],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    vi.mocked(esriJsonUtil.esriJsonToPolyline)
      .mockReturnValueOnce(mockParentLine)
      .mockReturnValueOnce(mockChildLine)
      .mockReturnValueOnce(mockOrphanedChildLine);
    vi.mocked(findParentLinesModule.findParentLines).mockReturnValue({
      lines: [{ id: "parent-1", polyline: mockChildLine }],
      orphanedLines: [mockOrphanedChildLine],
    });

    findParentLinesHandler(mockCall, mockCallback as any);

    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenCalledTimes(3);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(1, parentLineEsriJson);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(2, childLineEsriJson);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(3, orphanedChildEsriJson);
    expect(findParentLinesModule.findParentLines).toHaveBeenCalledWith(
      [{ id: "parent-1", polyline: mockParentLine }],
      [mockChildLine, mockOrphanedChildLine],
    );
    expect(mockCallback).toHaveBeenCalledWith(null, {
      linesWithParentMatch: [
        {
          parentId: "parent-1",
          childEsriJsonPolyline: JSON.stringify(mockChildLine.toJSON()),
        },
      ],
      orphanedChildrenEsriJsonPolylines: [JSON.stringify(mockOrphanedChildLine.toJSON())],
    });
  });

  it("should call callback with error when findParentLines throws", () => {
    const parentLineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [10, 0],
      ],
    ]);
    mockCall.request.parentLines = [{ id: "parent-1", esriJsonPolyline: parentLineEsriJson }];

    const mockParentLine = new Polyline({
      paths: [
        [
          [0, 0],
          [10, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const testError = new Error("Failed to find parent lines");
    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockParentLine);
    vi.mocked(findParentLinesModule.findParentLines).mockImplementation(() => {
      throw testError;
    });

    findParentLinesHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to find parent lines");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
