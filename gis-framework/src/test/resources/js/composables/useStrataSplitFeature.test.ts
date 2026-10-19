import { beforeEach, describe, expect, it, vi } from "vitest";
import { useStrataSplitFeature } from "@/composables/useStrataSplitFeature";

const { strataSplitFeaturesMock } = vi.hoisted(() => ({
  strataSplitFeaturesMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  strataSplitFeatures: strataSplitFeaturesMock,
}));

const request = {
  strataSplitUrl: "/api/gis-framework/strata-split",
  commandJourneyId: "journey-1",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token",
};

describe("useStrataSplitFeature", () => {
  beforeEach(() => {
    strataSplitFeaturesMock.mockReset();
  });

  it("strata splits the features at the depth and calls onSuccess", async () => {
    strataSplitFeaturesMock.mockResolvedValue({ outputFeatureIds: ["feature-2", "feature-3"] });
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { strataSplit } = useStrataSplitFeature(() => request, { onSuccess, onError });

    await strataSplit(["feature-1"], -2000);

    expect(strataSplitFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/strata-split",
      ["feature-1"],
      -2000,
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
  });

  it("reports processing until the strata split request completes", async () => {
    let resolveStrataSplit: (value: unknown) => void = () => {};
    strataSplitFeaturesMock.mockReturnValue(new Promise((resolve) => {
      resolveStrataSplit = resolve;
    }));
    const { isProcessing, strataSplit } = useStrataSplitFeature(request, { onSuccess: vi.fn(), onError: vi.fn() });

    const splitting = strataSplit(["feature-1"], -2000);

    expect(isProcessing.value).toBe(true);
    resolveStrataSplit({ outputFeatureIds: [] });
    await splitting;
    expect(isProcessing.value).toBe(false);
  });

  it("calls onError and stops processing when the strata split request fails", async () => {
    strataSplitFeaturesMock.mockRejectedValue(new Error("network error"));
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { isProcessing, strataSplit } = useStrataSplitFeature(request, { onSuccess, onError });

    await strataSplit(["feature-1"], -2000);

    expect(onError).toHaveBeenCalledWith(
      "An error occurred while attempting to stratigraphically split the features. Please try again.",
    );
    expect(onSuccess).not.toHaveBeenCalled();
    expect(isProcessing.value).toBe(false);
  });
});
