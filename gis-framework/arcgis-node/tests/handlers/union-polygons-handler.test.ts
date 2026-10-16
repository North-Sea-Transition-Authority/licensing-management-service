import Polygon from "@arcgis/core/geometry/Polygon.js";
import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as unionPolygonsOperatorModule from "../../src/geometric-operators/union-polygons-operator";
import { unionPolygonsHandler } from "../../src/handlers/union-polygons-handler";

vi.mock("../../src/geometric-operators/union-polygons-operator");

const SQUARE = { rings: [[[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]]], spatialReference: { wkid: 4230 } };
const OFFSET_SQUARE = { rings: [[[5, 5], [15, 5], [15, 15], [5, 15], [5, 5]]], spatialReference: { wkid: 4230 } };
const UNIONED = {
  rings: [[[0, 0], [10, 0], [10, 5], [15, 5], [15, 15], [5, 15], [5, 10], [0, 10], [0, 0]]],
  spatialReference: { wkid: 4230 },
};

describe("unionPolygonsHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn();
    mockCall = {
      request: {
        esriJsonPolygons: [JSON.stringify(SQUARE), JSON.stringify(OFFSET_SQUARE)],
      },
    };
  });

  it("unionPolygonsHandler_whenPolygonsProvided_thenReturnsUnionedEsriJson", async () => {
    const unioned = Polygon.fromJSON(UNIONED);
    vi.mocked(unionPolygonsOperatorModule.unionPolygonsOperator).mockReturnValue(unioned);

    unionPolygonsHandler(mockCall, mockCallback);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, {
      esriJsonPolygon: JSON.stringify(unioned.toJSON()),
    }));
    expect(unionPolygonsOperatorModule.unionPolygonsOperator).toHaveBeenCalledWith([
      Polygon.fromJSON(SQUARE),
      Polygon.fromJSON(OFFSET_SQUARE),
    ]);
  });

  it("unionPolygonsHandler_whenOperatorThrows_thenCallsBackWithInternalError", async () => {
    const testError = new Error("No polygons provided to union");
    vi.mocked(unionPolygonsOperatorModule.unionPolygonsOperator).mockImplementation(() => {
      throw testError;
    });

    unionPolygonsHandler(mockCall, mockCallback);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(testError, null));
    expect(mockCallback.mock.calls[0][0].code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
