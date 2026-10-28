import type { JsonHistoryStatus } from "@/api/history.api";
import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import UndoRedoActions from "@/components/undoRedo/UndoRedoActions.vue";

const { undoMock, redoMock } = vi.hoisted(() => ({
  undoMock: vi.fn(),
  redoMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  undo: undoMock,
  redo: redoMock,
}));

const nothingToUndoOrRedo: JsonHistoryStatus = {
  undoTransformationType: null,
  redoTransformationType: null,
};

const undoMergeRedoAddStrata: JsonHistoryStatus = {
  undoTransformationType: "MERGE",
  redoTransformationType: "STRATA_SPLIT",
};

const baseProps = {
  historyStatus: nothingToUndoOrRedo,
  baseUrl: "/api/gis-framework",
  commandJourneyId: "journey-1",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
};

function renderComponent(props = {}) {
  return render(UndoRedoActions, { props: { ...baseProps, ...props } });
}

function buttonNames() {
  return screen.getAllByRole("button").map(button => button.textContent?.trim());
}

describe("undoRedoActions", () => {
  beforeEach(() => {
    undoMock.mockReset();
    redoMock.mockReset();
  });

  it.each([
    { historyStatus: null, expected: { undoEnabled: false, redoEnabled: false } },
    { historyStatus: nothingToUndoOrRedo, expected: { undoEnabled: false, redoEnabled: false } },
    {
      historyStatus: { ...nothingToUndoOrRedo, undoTransformationType: "SPLIT" },
      expected: { undoEnabled: true, redoEnabled: false },
    },
    {
      historyStatus: { ...nothingToUndoOrRedo, redoTransformationType: "SPLIT" },
      expected: { undoEnabled: false, redoEnabled: true },
    },
  ])("enables the buttons as $expected for history status $historyStatus", ({ historyStatus, expected }) => {
    renderComponent({ historyStatus });

    const [undoButton, redoButton] = screen.getAllByRole("button");
    expect({
      undoEnabled: !(undoButton as HTMLButtonElement).disabled,
      redoEnabled: !(redoButton as HTMLButtonElement).disabled,
    }).toEqual(expected);
  });

  it.each([
    { transformationType: "SPLIT", expectedNames: ["Undo split", "Redo split"] },
    { transformationType: "MERGE", expectedNames: ["Undo merge", "Redo merge"] },
    { transformationType: "STRATA_SPLIT", expectedNames: ["Undo add strata", "Redo add strata"] },
  ])("names the buttons $expectedNames for a $transformationType command", ({ transformationType, expectedNames }) => {
    renderComponent({
      historyStatus: {
        undoTransformationType: transformationType,
        redoTransformationType: transformationType,
      },
    });

    expect(buttonNames()).toEqual(expectedNames);
  });

  it("names the undo and redo buttons after their own commands", () => {
    renderComponent({ historyStatus: undoMergeRedoAddStrata });

    expect(buttonNames()).toEqual(["Undo merge", "Redo add strata"]);
  });

  it("names the buttons plainly when there is nothing to undo or redo", () => {
    renderComponent();

    expect(buttonNames()).toEqual(["Undo", "Redo"]);
  });

  it("names the buttons after the operation name over the command types", () => {
    renderComponent({ historyStatus: undoMergeRedoAddStrata, operationName: "split" });

    expect(buttonNames()).toEqual(["Undo split", "Redo split"]);
  });

  it("undoes against the built url and emits action-success after a successful undo", async () => {
    undoMock.mockResolvedValue(undefined);
    const { emitted } = renderComponent({ historyStatus: undoMergeRedoAddStrata });

    await fireEvent.click(screen.getByRole("button", { name: "Undo merge" }));

    await waitFor(() => {
      expect(undoMock).toHaveBeenCalledWith("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token-1");
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits an action-error naming the command when the undo request fails", async () => {
    undoMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent({ historyStatus: undoMergeRedoAddStrata });

    await fireEvent.click(screen.getByRole("button", { name: "Undo merge" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to undo the last merge. Please try again."],
      ]);
    });
  });

  it("redoes against the built url and emits action-success after a successful redo", async () => {
    redoMock.mockResolvedValue(undefined);
    const { emitted } = renderComponent({ historyStatus: undoMergeRedoAddStrata });

    await fireEvent.click(screen.getByRole("button", { name: "Redo add strata" }));

    await waitFor(() => {
      expect(redoMock).toHaveBeenCalledWith("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token-1");
      expect(emitted("action-success")).toHaveLength(1);
    });
  });

  it("emits an action-error naming the command when the redo request fails", async () => {
    redoMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderComponent({ historyStatus: undoMergeRedoAddStrata });

    await fireEvent.click(screen.getByRole("button", { name: "Redo add strata" }));

    await waitFor(() => {
      expect(emitted("action-error")).toEqual([
        ["An error occurred while attempting to redo the last add strata. Please try again."],
      ]);
    });
  });

  it("disables both buttons and reports processing while a request is in flight", async () => {
    let resolveUndo!: () => void;
    undoMock.mockReturnValue(new Promise<void>((resolve) => {
      resolveUndo = resolve;
    }));
    const { emitted } = renderComponent({ historyStatus: undoMergeRedoAddStrata });

    await fireEvent.click(screen.getByRole("button", { name: "Undo merge" }));

    expect(screen.getByRole("button", { name: "Undo merge" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Redo add strata" })).toBeDisabled();

    resolveUndo();

    await waitFor(() => expect(emitted("update:processing")).toEqual([[true], [false]]));
  });

  it("disables both buttons when disabled even though the history allows undo and redo", () => {
    renderComponent({ historyStatus: undoMergeRedoAddStrata, disabled: true });

    expect(screen.getByRole("button", { name: "Undo merge" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Redo add strata" })).toBeDisabled();
  });

  it("renders slotted content alongside the undo and redo buttons", () => {
    render(UndoRedoActions, {
      props: { ...baseProps, operationName: "merge" },
      slots: { default: `<button type="button">Merge</button>` },
    });

    expect(buttonNames()).toEqual(["Merge", "Undo merge", "Redo merge"]);
  });
});
