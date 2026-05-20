import { status } from "@grpc/grpc-js";
import { describe, expect, it } from "vitest";
import { toGrpcInternalError } from "../../src/handlers/grpc-error";

describe("toGrpcInternalError", () => {
  it("should add an internal status code to an error without a gRPC status code", () => {
    const error = new Error("Something failed");

    const result = toGrpcInternalError(error);

    expect(result).toBe(error);
    expect(result.message).toBe("Something failed");
    expect(result.code).toBe(status.INTERNAL);
  });

  it("should preserve an existing gRPC status code", () => {
    const error = Object.assign(new Error("Invalid request"), { code: status.INVALID_ARGUMENT });

    const result = toGrpcInternalError(error);

    expect(result).toBe(error);
    expect(result.message).toBe("Invalid request");
    expect(result.code).toBe(status.INVALID_ARGUMENT);
  });

  it("should replace an invalid status code with internal", () => {
    const error = Object.assign(new Error("Invalid code"), { code: "not a status code" });

    const result = toGrpcInternalError(error);

    expect(result).toBe(error);
    expect(result.message).toBe("Invalid code");
    expect(result.code).toBe(status.INTERNAL);
  });

  it("should convert a non-error value to an internal gRPC error", () => {
    const result = toGrpcInternalError("Something failed");

    expect(result).toBeInstanceOf(Error);
    expect(result.message).toBe("Unknown error");
    expect(result.code).toBe(status.INTERNAL);
  });
});
