import { beforeEach, describe, expect, it, vi } from "vitest";
import { useRenameFeature } from "@/composables/useRenameFeature";

const { renameFeaturesMock } = vi.hoisted(() => ({
  renameFeaturesMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  renameFeatures: renameFeaturesMock,
}));

const request = {
  renameUrl: "/api/gis-framework/rename",
  commandJourneyId: "journey-1",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token",
};

const featureRenames = [{ featureId: "feature-1", featureName: "SHAPE 2", name: "Subarea 2" }];

describe("useRenameFeature", () => {
  beforeEach(() => {
    renameFeaturesMock.mockReset();
  });

  it("renames the features and calls onSuccess", async () => {
    renameFeaturesMock.mockResolvedValue({ outputFeatureIds: ["feature-2"] });
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { rename } = useRenameFeature(() => request, { onSuccess, onError });

    await rename(featureRenames);

    expect(renameFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/rename",
      featureRenames,
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
  });

  it("reports processing until the rename request completes", async () => {
    let resolveRename: (value: unknown) => void = () => {};
    renameFeaturesMock.mockReturnValue(new Promise((resolve) => {
      resolveRename = resolve;
    }));
    const { isProcessing, rename } = useRenameFeature(request, { onSuccess: vi.fn(), onError: vi.fn() });

    const renaming = rename(featureRenames);

    expect(isProcessing.value).toBe(true);
    resolveRename({ outputFeatureIds: [] });
    await renaming;
    expect(isProcessing.value).toBe(false);
  });

  it("calls onError and stops processing when the rename request fails", async () => {
    renameFeaturesMock.mockRejectedValue(new Error("network error"));
    const onSuccess = vi.fn();
    const onError = vi.fn();
    const { isProcessing, rename } = useRenameFeature(request, { onSuccess, onError });

    await rename(featureRenames);

    expect(onError).toHaveBeenCalledWith("An error occurred while attempting to rename the subareas. Please try again.");
    expect(onSuccess).not.toHaveBeenCalled();
    expect(isProcessing.value).toBe(false);
  });
});
