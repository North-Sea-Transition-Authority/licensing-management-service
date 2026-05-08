import { type ServerErrorResponse, status } from '@grpc/grpc-js';

export function toGrpcInternalError(error: unknown): ServerErrorResponse {
  const grpcError = error instanceof Error ? error : new Error('Unknown error');
  const serverError = grpcError as ServerErrorResponse;

  if (typeof serverError.code !== 'number' || !Number.isInteger(serverError.code)) {
    serverError.code = status.INTERNAL;
  }

  return serverError;
}
