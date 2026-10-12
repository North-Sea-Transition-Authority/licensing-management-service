import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GrpcError } from "../../src/handlers/grpc-error";
import { syncHandler } from "../../src/handlers/sync-handler";

vi.mock("../../src/config/logger", () => ({
  logger: {
    error: vi.fn(),
  },
}));

describe("syncHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = { request: {} };
  });

  it("should call the callback with the returned response", () => {
    const handler = syncHandler(() => ({ result: "ok" }));

    handler(mockCall, mockCallback);

    expect(mockCallback).toHaveBeenCalledOnce();
    expect(mockCallback).toHaveBeenCalledWith(null, { result: "ok" });
  });

  it("should call callback with an internal error when the handler throws", () => {
    const testError = new Error("Something went wrong");
    const handler = syncHandler(() => {
      throw testError;
    });

    handler(mockCall, mockCallback);

    expect(mockCallback).toHaveBeenCalledOnce();
    expect(mockCallback).toHaveBeenCalledWith(testError, null);
    expect(mockCallback.mock.calls[0][0].code).toBe(status.INTERNAL);
  });

  it("should preserve the grpc status code when the handler throws a GrpcError", () => {
    const testError = new GrpcError(status.INVALID_ARGUMENT, "Bad input");
    const handler = syncHandler(() => {
      throw testError;
    });

    handler(mockCall, mockCallback);

    expect(mockCallback).toHaveBeenCalledOnce();
    expect(mockCallback).toHaveBeenCalledWith(testError, null);
    expect(mockCallback.mock.calls[0][0].code).toBe(status.INVALID_ARGUMENT);
  });
});
