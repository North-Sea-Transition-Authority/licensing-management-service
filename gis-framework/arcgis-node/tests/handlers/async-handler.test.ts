import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { asyncHandler } from "../../src/handlers/async-handler";
import { GrpcError } from "../../src/handlers/grpc-error";

vi.mock("../../src/config/logger", () => ({
  logger: {
    error: vi.fn(),
  },
}));

describe("asyncHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = { request: {} };
  });

  it("should call the callback with the resolved response", async () => {
    const handler = asyncHandler(async () => ({ result: "ok" }));

    handler(mockCall, mockCallback);
    await vi.waitFor(() => {
      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(null, { result: "ok" });
    });
  });

  it("should call callback with an internal error when the handler rejects", async () => {
    const testError = new Error("Something went wrong");
    const handler = asyncHandler(async () => {
      throw testError;
    });

    handler(mockCall, mockCallback);
    await vi.waitFor(() => {
      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(testError, null);
    });

    expect(mockCallback.mock.calls[0][0].code).toBe(status.INTERNAL);
  });

  it("should preserve the grpc status code when the handler throws a GrpcError", async () => {
    const testError = new GrpcError(status.INVALID_ARGUMENT, "Bad input");
    const handler = asyncHandler(async () => {
      throw testError;
    });

    handler(mockCall, mockCallback);
    await vi.waitFor(() => {
      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(testError, null);
    });

    expect(mockCallback.mock.calls[0][0].code).toBe(status.INVALID_ARGUMENT);
  });
});
