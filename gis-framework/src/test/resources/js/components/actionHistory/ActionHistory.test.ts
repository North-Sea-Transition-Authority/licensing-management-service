import type { JsonHistoryAction } from "@/api/history.api";
import { render, screen, within } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import ActionHistory from "@/components/actionHistory/ActionHistory.vue";

const mergeAction: JsonHistoryAction = {
  number: 2,
  transformationType: "MERGE",
  inputFeatureNames: ["A", "B"],
  outputFeatureNames: ["mergeResult_ABCD"],
};

const splitAction: JsonHistoryAction = {
  number: 1,
  transformationType: "SPLIT",
  inputFeatureNames: ["C"],
  outputFeatureNames: ["C_1", "C_2"],
};

function renderComponent(actions: JsonHistoryAction[]) {
  return render(ActionHistory, { props: { actions } });
}

function dataRowCells() {
  return screen.getAllByRole("row")
    .slice(1)
    .map(row => within(row).getAllByRole("cell").map(cell => cell.textContent?.replace(/\s+/g, " ").trim()));
}

describe("actionHistory", () => {
  it("shows that no actions have been taken when there are none", () => {
    renderComponent([]);

    expect(screen.getByText("No actions taken yet.")).toBeInTheDocument();
  });

  it.each([
    { actions: [], summary: "Show action history (0 actions)" },
    { actions: [splitAction], summary: "Show action history (1 action)" },
    { actions: [mergeAction, splitAction], summary: "Show action history (2 actions)" },
  ])("summarises the action history as \"$summary\"", ({ actions, summary }) => {
    renderComponent(actions);

    expect(screen.getByText(summary)).toBeInTheDocument();
  });

  it("lists each action by number and type in the order given", () => {
    renderComponent([mergeAction, splitAction]);

    expect(dataRowCells()).toEqual([
      ["2.", "Merge", "A, B", "mergeResult_ABCD"],
      ["1.", "Split", "C", "C_1, C_2"],
    ]);
  });

  it.each([
    { action: mergeAction, inputs: "A, B", outputs: "mergeResult_ABCD" },
    { action: splitAction, inputs: "C", outputs: "C_1, C_2" },
  ])("lists the input and output feature names of a $action.transformationType", ({ action, inputs, outputs }) => {
    renderComponent([action]);

    const cellTexts = within(screen.getAllByRole("row")[1])
      .getAllByRole("cell")
      .map(cell => cell.textContent?.trim());

    expect(cellTexts).toEqual(expect.arrayContaining([inputs, outputs]));
  });
});
