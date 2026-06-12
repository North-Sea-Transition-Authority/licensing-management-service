import type { ServerErrorResponse } from "@grpc/grpc-js";
import { status } from "@grpc/grpc-js";

export class GrpcError extends Error {
  constructor(public readonly code: status, message: string) {
    super(message);
    this.name = "GrpcError";
  }
}

export function toGrpcInternalError(error: unknown): ServerErrorResponse {
  const grpcError = error instanceof Error ? error : new Error("Unknown error");
  const serverError = grpcError as ServerErrorResponse;

  if (typeof serverError.code !== "number" || !Number.isInteger(serverError.code)) {
    serverError.code = status.INTERNAL;
  }

  return serverError;
}
