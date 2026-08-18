import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SplitActions from "../../../../../main/resources/js/components/splitByPointAndClick/SplitActions.vue";

const { getSplitHistoryStatusMock, undoSplitMock, redoSplitMock } = vi.hoisted(() => ({
  getSplitHistoryStatusMock: vi.fn(),
  undoSplitMock: vi.fn(),
  redoSplitMock: vi.fn(),
}));

vi.mock("../../../../../main/resources/js/api/split-history.api", () => ({
  getSplitHistoryStatus: getSplitHistoryStatusMock,
}));

vi.mock("../../../../../main/resources/js/api/split.api", () => ({
  undoSplit: undoSplitMock,
  redoSplit: redoSplitMock,
}));

const baseProps = {
  refreshCounter: 0,
  historyUrl: "/api/gis-framework/split-history/journey-1",
  undoUrl: "/api/gis-framework/undo/journey-1",
  redoUrl: "/api/gis-framework/redo/journey-1",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
};

function renderComponent(props = {}) {
  return render(SplitActions, { props: { ...baseProps, ...props } });
}

describe("splitActions", () => {
  beforeEach(() => {
    getSplitHistoryStatusMock.mockReset();
    undoSplitMock.mockReset();
    redoSplitMock.mockReset();
  });

  it("disables the undo and redo buttons when there is nothing to undo or redo", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: false });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Undo split" })).toBeDisabled();
      expect(screen.getByRole("button", { name: "Redo split" })).toBeDisabled();
    });
  });

  it("enables the undo button when there is a command to undo", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: true, canRedo: false });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Undo split" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "Redo split" })).toBeDisabled();
    });
  });

  it("enables the redo button when there is a command to redo", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: true });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Redo split" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "Undo split" })).toBeDisabled();
    });
  });

  it("refetches the history status when the refresh counter changes", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: false });
    const { rerender } = renderComponent();

    await waitFor(() => {
      expect(getSplitHistoryStatusMock).toHaveBeenCalledTimes(1);
    });

    await rerender({ ...baseProps, refreshCounter: 1 });

    await waitFor(() => {
      expect(getSplitHistoryStatusMock).toHaveBeenCalledTimes(2);
    });
  });

  it("emits action-error when the history status cannot be loaded", async () => {
    getSplitHistoryStatusMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent();

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([["Unable to load undo/redo status."]]);
    });
  });

  it("emits action-success after a successful undo", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: true, canRedo: false });
    undoSplitMock.mockResolvedValue({ outputFeatureIds: ["feature-1"] });
    const { emitted } = renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Undo split" })).toBeEnabled();
    });

    await fireEvent.click(screen.getByRole("button", { name: "Undo split" }));

    await waitFor(() => {
      expect(undoSplitMock).toHaveBeenCalledWith(
        "/api/gis-framework/undo/journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      );
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits action-error when the undo request fails", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: true, canRedo: false });
    undoSplitMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Undo split" })).toBeEnabled();
    });

    await fireEvent.click(screen.getByRole("button", { name: "Undo split" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to undo the last split. Please try again."],
      ]);
    });
  });

  it("emits action-success after a successful redo", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: true });
    redoSplitMock.mockResolvedValue({ outputFeatureIds: ["feature-1"] });
    const { emitted } = renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Redo split" })).toBeEnabled();
    });

    await fireEvent.click(screen.getByRole("button", { name: "Redo split" }));

    await waitFor(() => {
      expect(redoSplitMock).toHaveBeenCalledWith(
        "/api/gis-framework/redo/journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      );
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits action-error when the redo request fails", async () => {
    getSplitHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: true });
    redoSplitMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Redo split" })).toBeEnabled();
    });

    await fireEvent.click(screen.getByRole("button", { name: "Redo split" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to redo the last split. Please try again."],
      ]);
    });
  });
});
