import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import MergeActions from "@/components/merge/MergeActions.vue";

const { getHistoryStatusMock, mergeFeaturesMock, undoMock, redoMock } = vi.hoisted(() => ({
  getHistoryStatusMock: vi.fn(),
  mergeFeaturesMock: vi.fn(),
  undoMock: vi.fn(),
  redoMock: vi.fn(),
}));

vi.mock("@/api/history.api", () => ({
  getHistoryStatus: getHistoryStatusMock,
}));

vi.mock("@/api/operator.api", () => ({
  mergeFeatures: mergeFeaturesMock,
  undo: undoMock,
  redo: redoMock,
}));

const baseProps = {
  refreshCounter: 0,
  baseUrl: "/api/gis-framework",
  mergeUrl: "/api/gis-framework/merge",
  commandJourneyId: "journey-1",
  featureIds: [],
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
};

function renderComponent(props = {}) {
  return render(MergeActions, { props: { ...baseProps, ...props } });
}

describe("mergeActions", () => {
  beforeEach(() => {
    getHistoryStatusMock.mockReset().mockResolvedValue({ canUndo: false, canRedo: false });
    mergeFeaturesMock.mockReset();
    undoMock.mockReset();
    redoMock.mockReset();
  });

  it("disables the merge button until at least two features are selected", async () => {
    const { rerender } = renderComponent({ featureIds: ["feature-1"] });

    await waitFor(() => expect(screen.getByRole("button", { name: "Merge" })).toBeDisabled());

    await rerender({ ...baseProps, featureIds: ["feature-1", "feature-2"] });

    expect(screen.getByRole("button", { name: "Merge" })).toBeEnabled();
  });

  it("merges the selected features and emits action-success when the merge button is clicked", async () => {
    mergeFeaturesMock.mockResolvedValue({ outputFeatureId: "feature-3" });
    const { emitted } = renderComponent({ featureIds: ["feature-1", "feature-2"] });

    await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

    await waitFor(() => {
      expect(mergeFeaturesMock).toHaveBeenCalledWith(
        "/api/gis-framework/merge",
        ["feature-1", "feature-2"],
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      );
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits action-error when the merge request fails", async () => {
    mergeFeaturesMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent({ featureIds: ["feature-1", "feature-2"] });

    await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to merge the features. Please try again."],
      ]);
    });
  });
});
