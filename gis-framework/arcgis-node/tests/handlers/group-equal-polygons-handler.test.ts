import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as groupEqualPolygonsModule from "../../src/geometric-operators/group-equal-polygons";
import { groupEqualPolygonsHandler } from "../../src/handlers/group-equal-polygons-handler";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/group-equal-polygons");

const polygons = [
  {
    id: "a",
    esriJsonPolygon: makePolygonEsriJson([
      [
        [0, 0],
        [10, 0],
        [10, 10],
        [0, 10],
        [0, 0],
      ],
    ]),
  },
  {
    id: "b",
    esriJsonPolygon: makePolygonEsriJson([
      [
        [20, 20],
        [30, 20],
        [30, 30],
        [20, 30],
        [20, 20],
      ],
    ]),
  },
];

describe("groupEqualPolygonsHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = { request: { polygons } };
  });

  it("groupEqualPolygonsHandler_whenGroupsFound_thenReturnsIdsOfEachGroup", async () => {
    vi.mocked(groupEqualPolygonsModule.groupEqualPolygons).mockReturnValue([["a", "c"], ["b"]]);

    groupEqualPolygonsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, {
      groups: [{ ids: ["a", "c"] }, { ids: ["b"] }],
    }));
    expect(groupEqualPolygonsModule.groupEqualPolygons).toHaveBeenCalledWith(polygons);
  });

  it("groupEqualPolygonsHandler_whenGroupEqualPolygonsThrows_thenCallsCallbackWithInternalError", async () => {
    const testError = new Error("Failed to group equal polygons");
    vi.mocked(groupEqualPolygonsModule.groupEqualPolygons).mockImplementation(() => {
      throw testError;
    });

    groupEqualPolygonsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledOnce());
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
