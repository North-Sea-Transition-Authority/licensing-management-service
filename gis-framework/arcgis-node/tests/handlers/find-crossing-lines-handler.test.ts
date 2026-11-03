import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as findCrossingLinesModule from "../../src/geometric-operators/find-crossing-lines-operator";
import { findCrossingLinesHandler } from "../../src/handlers/find-crossing-lines-handler";
import { makePolygonEsriJson, makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/find-crossing-lines-operator");

const polygonEsriJson = makePolygonEsriJson([
  [
    [0, 0],
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
  ],
]);

const lines = [{ id: "line", polyLineEsriJson: makePolylineEsriJson([[[5, 5], [15, 5]]]) }];

describe("findCrossingLinesHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonPolygons: [polygonEsriJson],
        lines,
      },
    };
  });

  it("should return the ids of the crossing lines", () => {
    vi.mocked(findCrossingLinesModule.findCrossingLines).mockReturnValue(["line"]);

    findCrossingLinesHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledWith(null, { crossingLineIds: ["line"] });
    expect(findCrossingLinesModule.findCrossingLines).toHaveBeenCalledWith([polygonEsriJson], lines);
  });

  it("should call callback with error when findCrossingLines throws", () => {
    const testError = new Error("Failed to find crossing lines");
    vi.mocked(findCrossingLinesModule.findCrossingLines).mockImplementation(() => {
      throw testError;
    });

    findCrossingLinesHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledOnce();
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
