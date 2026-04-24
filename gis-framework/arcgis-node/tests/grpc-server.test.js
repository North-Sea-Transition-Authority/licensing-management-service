import { beforeEach, describe, expect, test, vi } from 'vitest';
import { main } from '../src/grpc-server';
import { migrateBlockOrSubarea } from '../src/migration/handlers/migrate-block-or-sub-area';
import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import express from 'express';
import path from 'path';
import process from 'node:process';

const MOCK_DIRNAME = '/mock/arcgis-node/src';

vi.mock('url', () => ({
  fileURLToPath: vi.fn(() => '/mock/arcgis-node/src/grpc-server.ts'),
}));
vi.mock('@grpc/grpc-js', () => ({
  default: {
    Server: vi.fn(function () {
      return {
        addService: vi.fn(),
        bindAsync: vi.fn((_addr, _creds, cb) => cb(null)),
      };
    }),
    ServerCredentials: { createInsecure: vi.fn(() => 'insecure-creds') },
    loadPackageDefinition: vi.fn(() => ({
      uk: {
        co: {
          fivium: {
            grpc: { gis: { ArcGisService: { service: 'mock-service-def' } } },
          },
        },
      },
    })),
  },
}));
vi.mock('@grpc/proto-loader', () => ({ default: { loadSync: vi.fn() } }));
vi.mock('../src/migration/handlers/migrate-block-or-sub-area', () => ({
  migrateBlockOrSubarea: vi.fn(),
}));
vi.mock('express', () => {
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

describe('main()', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('asset-server', () => {
    test('should serve assets on the expected route', () => {
      main();

      const expectedAssetFolder = path.resolve(process.cwd(), '../public/assets');
      const app = getExpressApp();
      expect(app.use).toHaveBeenCalledWith('/assets', undefined);
      expect(express.static).toHaveBeenCalledWith(expectedAssetFolder);
      expect(app.listen).toHaveBeenCalledWith(3000, expect.any(Function));
    });
  });

  describe('proto-loading', () => {
    test('should load proto definition with expected options', () => {
      main();

      const expectedProtoPath = path.resolve(MOCK_DIRNAME, '../../src/main/proto', 'ArcGisJs.proto');
      const expectedIncludeDir = path.resolve(MOCK_DIRNAME, '../../src/main/proto');

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

  describe('grpc-server', () => {
    test('should register correct services', () => {
      main();

      const server = getGrpcServerInstance();
      expect(server.addService).toHaveBeenCalledWith('mock-service-def', {
        migrateBlockOrSubarea,
      });
    });

    test('should bind to 0.0.0.0:8082', () => {
      main();

      const server = getGrpcServerInstance();
      expect(server.bindAsync).toHaveBeenCalledWith('0.0.0.0:8082', 'insecure-creds', expect.any(Function));
    });
  });
});
