import type { ProtoGrpcType } from "../generated/ArcGisJs.ts";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import esriConfig from "@arcgis/core/config.js";
import grpc from "@grpc/grpc-js";
import protoLoader from "@grpc/proto-loader";
import express from "express";
import { logger } from "./config/logger";
import { buildPolygonHandler } from "./handlers/build-polygon-handler";
import { calculateAreaHandler } from "./handlers/calculate-area-operator-handler";
import { explodePolygonHandler } from "./handlers/explode-polygon-handler";
import { findNorthwestMostLineHandler } from "./handlers/find-northwest-most-line-handler";
import { findParentLinesHandler } from "./handlers/find-parent-lines-handler";
import { getLineStartAndEndPointsHandler } from "./handlers/get-line-start-and-end-points-handler";
import { splitPolygonHandler } from "./handlers/split-polygon-handler";
import {
  validatePolygonReconstructionFromPolylinesHandler,
} from "./handlers/validate-polygon-reconstruction-from-polylines-handler";
import { migrateBlockOrSubarea } from "./migration/handlers/migrate-block-or-sub-area";
import { migrateReferenceBlockHandler } from "./migration/handlers/migrate-reference-block";
import { validateBlockAndSubarea } from "./migration/handlers/validate-block-and-subarea";
import { validateReferenceBlock } from "./migration/handlers/validate-reference-block";
import { validateTopologicallyEqual } from "./migration/handlers/validate-topologically-equal";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const ASSET_PORT = 3000;
const GRPC_BIND_ADDRESS = "0.0.0.0:8082";
const PROTO_PATH = path.resolve(__dirname, "../../src/main/proto", "ArcGisJs.proto");

/**
 * By default, the ArcGIS SDK operators call out to js.arcgis.com to fetch a WASM from their CDN. So that we don't have to rely on
 * their CDN always being up or compromised, we our hosting a local server which will give us the assets we need.
 */
function startAssetServer() {
  const assetApp = express();
  assetApp.disable("x-powered-by");
  const assetFolder = path.resolve(process.cwd(), "../public/assets");
  logger.info(`[Asset Server] serving files in: ${assetFolder}`);
  assetApp.use("/assets", express.static(assetFolder));
  assetApp.listen(ASSET_PORT, () => {
    logger.info(`[Asset Server] Running at http://localhost:${ASSET_PORT}/assets`);
  });
  esriConfig.assetsPath = `http://localhost:${ASSET_PORT}/assets`;
}

/**
 * The Node.js library dynamically generates service descriptors and client stub definitions from .proto files loaded at runtime.
 * This method loads all of proto files to be used by the server.
 * https://grpc.io/docs/languages/node/basics/#loading-service-descriptors-from-proto-files
 */
function loadProtoDefinition() {
  const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
    includeDirs: [path.resolve(__dirname, "../../src/main/proto")],
  });

  return (grpc.loadPackageDefinition(packageDefinition) as unknown as ProtoGrpcType).uk.co.fivium.grpc.gis;
}

function startGrpcServer(arcGisJsProto: ProtoGrpcType["uk"]["co"]["fivium"]["grpc"]["gis"]) {
  const server = new grpc.Server();

  server.addService(arcGisJsProto.ArcGisService.service, {
    splitPolygon: splitPolygonHandler,
    buildPolygon: buildPolygonHandler,
    explodePolygon: explodePolygonHandler,
    findParentLines: findParentLinesHandler,
    getLineStartAndEndPoints: getLineStartAndEndPointsHandler,
    findNorthwestMostLine: findNorthwestMostLineHandler,
    validatePolygonReconstructionFromPolylines: validatePolygonReconstructionFromPolylinesHandler,
    calculateArea: calculateAreaHandler,
    migrateBlockOrSubarea,
    validateBlockAndSubarea,
    validateTopologicallyEqual,
    migrateReferenceBlock: migrateReferenceBlockHandler,
    validateReferenceBlock,
  });

  server.bindAsync(GRPC_BIND_ADDRESS, grpc.ServerCredentials.createInsecure(), (error) => {
    if (error) {
      logger.error(error);
      return;
    }
    logger.info(`gRPC Server running at ${GRPC_BIND_ADDRESS}`);
  });
}

export function main() {
  startAssetServer();
  const arcGisJsProto = loadProtoDefinition();
  startGrpcServer(arcGisJsProto);
}

if (process.env.VITEST !== "true") {
  main();
}
