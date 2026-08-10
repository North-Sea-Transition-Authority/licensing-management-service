import path from "node:path";
import grpc from "@grpc/grpc-js";
import protoLoader from "@grpc/proto-loader";
import express from "express";
import {beforeEach, describe, expect, it, vi} from "vitest";
import {main} from "../src/grpc-server";
import {buildPolygonHandler} from "../src/handlers/build-polygon-handler.ts";
import {calculateAreaHandler} from "../src/handlers/calculate-area-operator-handler.ts";
import {coordinatesToPolylineHandler} from "../src/handlers/coordinates-to-polyline-handler.ts";
import {explodePolygonHandler} from "../src/handlers/explode-polygon-handler.ts";
import {findNorthwestMostLineHandler} from "../src/handlers/find-northwest-most-line-handler.ts";
import {findParentLinesHandler} from "../src/handlers/find-parent-lines-handler.ts";
import {getLineStartAndEndPointsHandler} from "../src/handlers/get-line-start-and-end-points-handler.ts";
import {splitPolygonHandler} from "../src/handlers/split-polygon-handler.ts";
import {
  validatePolygonReconstructionFromPolylinesHandler,
} from "../src/handlers/validate-polygon-reconstruction-from-polylines-handler.ts";
import {migrateBlockOrSubarea} from "../src/migration/handlers/migrate-block-or-sub-area";
import {migrateReferenceBlockHandler} from "../src/migration/handlers/migrate-reference-block.ts";
import {validateBlockAndSubarea} from "../src/migration/handlers/validate-block-and-subarea.ts";
import {validateReferenceBlock} from "../src/migration/handlers/validate-reference-block.ts";
import {validateTopologicallyEqual} from "../src/migration/handlers/validate-topologically-equal.ts";

const MOCK_DIRNAME = "/mock/arcgis-node/src";

vi.mock("url", () => ({
  fileURLToPath: vi.fn(() => "/mock/arcgis-node/src/grpc-server.ts"),
}));
vi.mock("@grpc/grpc-js", () => ({
  default: {
    // eslint-disable-next-line prefer-arrow-callback
    Server: vi.fn(function () {
      return {
        addService: vi.fn(),
        bindAsync: vi.fn((_addr, _creds, cb) => cb(null)),
      };
    }),
    ServerCredentials: { createInsecure: vi.fn(() => "insecure-creds") },
    loadPackageDefinition: vi.fn(() => ({
      uk: {
        co: {
          fivium: {
            grpc: { gis: { ArcGisService: { service: "mock-service-def" } } },
          },
        },
      },
    })),
  },
}));
vi.mock("@grpc/proto-loader", () => ({ default: { loadSync: vi.fn() } }));
vi.mock("../src/migration/handlers/migrate-block-or-sub-area", () => ({
  migrateBlockOrSubarea: vi.fn(),
}));
vi.mock("express", () => {
  const app = { use: vi.fn(), listen: vi.fn((_p, cb) => cb?.()), disable: vi.fn() };
  return {
    default: Object.assign(
      vi.fn(() => app),
      { static: vi.fn() },
    ),
  };
});

function getGrpcServerInstance() {
  return vi.mocked(grpc.Server).mock.results[0].value;
}

function getExpressApp() {
  return vi.mocked(express).mock.results[0].value;
}

describe("main()", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("asset-server", () => {
    it("should serve assets on the expected route", () => {
      main();

      const expectedAssetFolder = path.resolve(MOCK_DIRNAME, "../public/assets");
      const app = getExpressApp();
      expect(app.use).toHaveBeenCalledWith("/assets", undefined);
      expect(express.static).toHaveBeenCalledWith(expectedAssetFolder);
      expect(app.listen).toHaveBeenCalledWith(3000, expect.any(Function));
    });
  });

  describe("proto-loading", () => {
    it("should load proto definition with expected options", () => {
      main();

      const expectedProtoPath = path.resolve(MOCK_DIRNAME, "../../src/main/proto", "ArcGisJs.proto");
      const expectedIncludeDir = path.resolve(MOCK_DIRNAME, "../../src/main/proto");

      expect(vi.mocked(protoLoader.loadSync)).toHaveBeenCalledWith(
        expectedProtoPath,
        expect.objectContaining({
          keepCase: true,
          longs: String,
          enums: String,
          defaults: true,
          oneofs: true,
          includeDirs: [expectedIncludeDir],
        }),
      );
    });
  });

  describe("grpc-server", () => {
    it("should register correct services", () => {
      main();

      const server = getGrpcServerInstance();
      expect(server.addService).toHaveBeenCalledWith("mock-service-def", {
        splitPolygon: splitPolygonHandler,
        buildPolygon: buildPolygonHandler,
        explodePolygon: explodePolygonHandler,
        findParentLines: findParentLinesHandler,
        getLineStartAndEndPoints: getLineStartAndEndPointsHandler,
        findNorthwestMostLine: findNorthwestMostLineHandler,
        validatePolygonReconstructionFromPolylines: validatePolygonReconstructionFromPolylinesHandler,
        calculateArea: calculateAreaHandler,
        coordinatesToPolyline: coordinatesToPolylineHandler,
        migrateBlockOrSubarea,
        validateBlockAndSubarea,
        validateTopologicallyEqual,
        migrateReferenceBlock: migrateReferenceBlockHandler,
        validateReferenceBlock,
      });
    });

    it("should bind to 0.0.0.0:8082", () => {
      main();

      const server = getGrpcServerInstance();
      expect(server.bindAsync).toHaveBeenCalledWith("0.0.0.0:8082", "insecure-creds", expect.any(Function));
    });
  });
});
