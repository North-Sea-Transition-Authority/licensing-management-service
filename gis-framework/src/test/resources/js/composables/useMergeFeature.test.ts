import { beforeEach, describe, expect, it, vi } from "vitest";
import { useMergeFeature } from "@/composables/useMergeFeature";

const { mergeFeaturesMock } = vi.hoisted(() => ({
  mergeFeaturesMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  mergeFeatures: mergeFeaturesMock,
}));

const request = {
  mergeUrl: "/api/gis-framework/merge",
  commandJourneyId: "journey-1",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token",
};

describe("useMergeFeature", () => {
  beforeEach(() => {
    mergeFeaturesMock.mockReset();
  });

  it.each([
    { description: "no features", featureIds: [] },
    { description: "a single feature", featureIds: ["feature-1"] },
  ])("does not merge when given $description", async ({ featureIds }) => {
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { merge } = useMergeFeature(request, featureIds, { onSuccess, onError });

    await merge();

    expect(mergeFeaturesMock).not.toHaveBeenCalled();
    expect(onSuccess).not.toHaveBeenCalled();
    expect(onError).not.toHaveBeenCalled();
  });

  it("merges the features and calls onSuccess", async () => {
    mergeFeaturesMock.mockResolvedValue({ outputFeatureId: "feature-3" });
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { merge } = useMergeFeature(() => request, () => ["feature-1", "feature-2"], { onSuccess, onError });

    await merge();

    expect(mergeFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/merge",
      ["feature-1", "feature-2"],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
  });

  it("reports processing until the merge request completes", async () => {
    let resolveMerge: (value: unknown) => void = () => {};
    mergeFeaturesMock.mockReturnValue(new Promise((resolve) => {
      resolveMerge = resolve;
    }));
    const { isProcessing, merge } = useMergeFeature(request, ["feature-1", "feature-2"], {
      onSuccess: vi.fn(),
      onError: vi.fn(),
    });

    const merging = merge();

    expect(isProcessing.value).toBe(true);
    resolveMerge({ outputFeatureId: "feature-3" });
    await merging;
    expect(isProcessing.value).toBe(false);
  });

  it("calls onError and stops processing when the merge request fails", async () => {
    mergeFeaturesMock.mockRejectedValue(new Error("network error"));
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { isProcessing, merge } = useMergeFeature(request, ["feature-1", "feature-2"], { onSuccess, onError });

    await merge();

    expect(onError).toHaveBeenCalledWith("An error occurred while attempting to merge the features. Please try again.");
    expect(onSuccess).not.toHaveBeenCalled();
    expect(isProcessing.value).toBe(false);
  });
});
