import type { handleUnaryCall, ServerUnaryCall } from "@grpc/grpc-js";
import { logger } from "../config/logger";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Adapts a synchronous unary handler to the void-returning signature gRPC expects ({@link handleUnaryCall}).
 */
export function syncHandler<Req, Res>(fn: (call: ServerUnaryCall<Req, Res>) => Res): handleUnaryCall<Req, Res> {
  return (call, callback) => {
    try {
      const response = fn(call);
      callback(null, response);
    } catch (error: unknown) {
      logger.error({ error }, "Unhandled error in gRPC handler");
      callback(toGrpcInternalError(error), null);
    }
  };
}
