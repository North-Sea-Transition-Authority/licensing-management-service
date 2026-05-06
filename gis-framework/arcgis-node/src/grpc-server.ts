import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import esriConfig from '@arcgis/core/config.js';
import express from 'express';
import type { ProtoGrpcType } from '../generated/ArcGisJs.ts';
import { logger } from './config/logger';
import { splitPolygonHandler } from './handlers/split-polygon-handler';
import { buildPolygonHandler } from './handlers/build-polygon-handler';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

//We need to host a version of the ESRI CDN so the library can run offline.
//https://developers.arcgis.com/javascript/latest/faq/#can-i-host-the-arcgis-cdn-modules-locally
//https://developers.arcgis.com/javascript/latest/working-with-assets/
const assetApp = express();
assetApp.disable('x-powered-by');
const ASSET_PORT = 3000;
const assetFolder = path.resolve(process.cwd(), '../public/assets');
logger.info(`[Asset Server] serving files in: ${assetFolder}`);
assetApp.use('/assets', express.static(assetFolder));
assetApp.listen(ASSET_PORT, () => {
  logger.info(`[Asset Server] Running at http://localhost:${ASSET_PORT}/assets`);
});
esriConfig.assetsPath = `http://localhost:${ASSET_PORT}/assets`;

const PROTO_PATH = path.resolve(__dirname, '../../src/main/proto', 'ArcGisJs.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
  includeDirs: [path.resolve(__dirname, '../../src/main/proto')],
});

const arcGisJsProto: ProtoGrpcType['uk']['co']['fivium']['grpc']['gis'] = (
  grpc.loadPackageDefinition(packageDefinition) as unknown as ProtoGrpcType
).uk.co.fivium.grpc.gis;

function main() {
  const server = new grpc.Server();

  server.addService(arcGisJsProto.ArcGisService.service, {
    splitPolygonHandler,
    buildPolygonHandler,
  });

  const bindAddress = '0.0.0.0:8082';

  server.bindAsync(bindAddress, grpc.ServerCredentials.createInsecure(), (error) => {
    if (error) {
      logger.error(error);
      return;
    }
    logger.info(`gRPC Server running at ${bindAddress}`);
  });
}

main();
