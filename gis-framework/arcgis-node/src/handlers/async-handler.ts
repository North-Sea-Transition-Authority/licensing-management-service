import type { handleUnaryCall, ServerUnaryCall } from "@grpc/grpc-js";
import { logger } from "../config/logger";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Adapts an async unary handler to the void-returning signature gRPC expects ({@link handleUnaryCall}).
 */
export function asyncHandler<Req, Res>(fn: (call: ServerUnaryCall<Req, Res>) => Promise<Res>): handleUnaryCall<Req, Res> {
  return (call, callback) => {
    void (async () => {
      try {
        const response = await fn(call);
        callback(null, response);
      } catch (error: unknown) {
        logger.error({ error }, "Unhandled error in gRPC handler");
        callback(toGrpcInternalError(error), null);
      }
    })();
  };
}
