import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import UndoRedoActions from "@/components/undoRedo/UndoRedoActions.vue";

const { getHistoryStatusMock, undoMock, redoMock } = vi.hoisted(() => ({
  getHistoryStatusMock: vi.fn(),
  undoMock: vi.fn(),
  redoMock: vi.fn(),
}));

vi.mock("@/api/history.api", () => ({
  getHistoryStatus: getHistoryStatusMock,
}));

vi.mock("@/api/operator.api", () => ({
  undo: undoMock,
  redo: redoMock,
}));

const baseProps = {
  refreshCounter: 0,
  baseUrl: "/api/gis-framework",
  commandJourneyId: "journey-1",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
  operationName: "merge",
};

function renderComponent(props = {}) {
  return render(UndoRedoActions, { props: { ...baseProps, ...props } });
}

describe("undoRedoActions", () => {
  beforeEach(() => {
    getHistoryStatusMock.mockReset();
    undoMock.mockReset();
    redoMock.mockReset();
  });

  it("disables the undo and redo buttons when there is nothing to undo or redo", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: false });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Undo merge" })).toBeDisabled();
      expect(screen.getByRole("button", { name: "Redo merge" })).toBeDisabled();
    });
  });

  it("enables the undo button when there is a command to undo", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: true, canRedo: false });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Undo merge" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "Redo merge" })).toBeDisabled();
    });
  });

  it("enables the redo button when there is a command to redo", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: true });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Redo merge" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "Undo merge" })).toBeDisabled();
    });
  });

  it("fetches the history status from the built command journey url", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: false });
    renderComponent();

    await waitFor(() => {
      expect(getHistoryStatusMock).toHaveBeenCalledWith("/api/gis-framework/history/journey-1");
    });
  });

  it("refetches the history status when the refresh counter changes", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: false });
    const { rerender } = renderComponent();

    await waitFor(() => expect(getHistoryStatusMock).toHaveBeenCalledTimes(1));

    await rerender({ ...baseProps, refreshCounter: 1 });

    await waitFor(() => expect(getHistoryStatusMock).toHaveBeenCalledTimes(2));
  });

  it("emits action-error when the history status cannot be loaded", async () => {
    getHistoryStatusMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent();

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([["Unable to load undo/redo status."]]);
    });
  });

  it("undoes against the built url and emits action-success after a successful undo", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: true, canRedo: false });
    undoMock.mockResolvedValue(undefined);
    const { emitted } = renderComponent();

    await waitFor(() => expect(screen.getByRole("button", { name: "Undo merge" })).toBeEnabled());

    await fireEvent.click(screen.getByRole("button", { name: "Undo merge" }));

    await waitFor(() => {
      expect(undoMock).toHaveBeenCalledWith("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token-1");
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits an operation-specific action-error when the undo request fails", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: true, canRedo: false });
    undoMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent({ operationName: "split" });

    await waitFor(() => expect(screen.getByRole("button", { name: "Undo split" })).toBeEnabled());

    await fireEvent.click(screen.getByRole("button", { name: "Undo split" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to undo the last split. Please try again."],
      ]);
    });
  });

  it("redoes against the built url and emits action-success after a successful redo", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: true });
    redoMock.mockResolvedValue(undefined);
    const { emitted } = renderComponent();

    await waitFor(() => expect(screen.getByRole("button", { name: "Redo merge" })).toBeEnabled());

    await fireEvent.click(screen.getByRole("button", { name: "Redo merge" }));

    await waitFor(() => {
      expect(redoMock).toHaveBeenCalledWith("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token-1");
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits an operation-specific action-error when the redo request fails", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: true });
    redoMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent({ operationName: "split" });

    await waitFor(() => expect(screen.getByRole("button", { name: "Redo split" })).toBeEnabled());

    await fireEvent.click(screen.getByRole("button", { name: "Redo split" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to redo the last split. Please try again."],
      ]);
    });
  });

  it("renders slotted content alongside the undo and redo buttons", async () => {
    getHistoryStatusMock.mockResolvedValue({ canUndo: false, canRedo: false });
    render(UndoRedoActions, {
      props: { ...baseProps },
      slots: { default: `<button type="button">Merge</button>` },
    });

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Merge" })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Undo merge" })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Redo merge" })).toBeInTheDocument();
    });
  });
});
