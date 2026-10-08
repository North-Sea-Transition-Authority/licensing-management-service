import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import ActionRadioGroup from "@/components/actionRadioGroup/ActionRadioGroup.vue";

const validationError = "Select what action you would like to perform";
const hint = "You can perform multiple actions before finalising.";

function renderSelect(hasActions = false, canMerge = true) {
  return render(ActionRadioGroup, { props: { hasActions, canMerge } });
}

function radioLabels() {
  return screen.getAllByRole("radio").map(radio => (radio as HTMLInputElement).labels?.[0]?.textContent?.trim());
}

describe("depthActionSelect", () => {
  it.each([
    {
      hasActions: false,
      legend: "What action would you like to perform first?",
      description: hint,
    },
    {
      hasActions: true,
      legend: "What action would you like to perform next?",
      description: "",
    },
  ])("asks $legend when hasActions is $hasActions", ({ hasActions, legend, description }) => {
    renderSelect(hasActions);

    expect(screen.getByRole("group", { name: legend })).toHaveAccessibleDescription(description);
  });

  it.each([
    { canMerge: true, expectedLabels: ["Split", "Add strata", "Merge", "Rename"] },
    { canMerge: false, expectedLabels: ["Split", "Add strata", "Rename"] },
  ])("offers $expectedLabels when canMerge is $canMerge", ({ canMerge, expectedLabels }) => {
    renderSelect(false, canMerge);

    expect(radioLabels()).toEqual(expectedLabels);
  });

  it("shows the inline error and emits it for the error summary when continuing without a selection", async () => {
    const { emitted } = renderSelect();

    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    expect(screen.getByRole("group", { name: "What action would you like to perform first?" }))
      .toHaveAccessibleDescription(`${hint} Error:${validationError}`);
    expect(emitted()["validation-error"]).toEqual([
      [{ message: validationError, fieldId: screen.getByRole("radio", { name: "Split" }).id }],
    ]);
    expect(emitted().select).toBeUndefined();
  });

  it("emits the selected action when continuing with a selection", async () => {
    const { emitted } = renderSelect();

    await fireEvent.click(screen.getByRole("radio", { name: "Merge" }));
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    expect(emitted().select).toEqual([["MERGE"]]);
  });

  it("clears the error when continuing with a selection after an error", async () => {
    const { emitted } = renderSelect();
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    await fireEvent.click(screen.getByRole("radio", { name: "Split" }));
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    expect(screen.getByRole("group", { name: "What action would you like to perform first?" }))
      .toHaveAccessibleDescription(hint);
    expect(emitted()["validation-error"]?.at(-1)).toEqual([null]);
  });

  it("clears the selection when its option is no longer offered", async () => {
    const { emitted, rerender } = renderSelect(false, true);
    await fireEvent.click(screen.getByRole("radio", { name: "Merge" }));

    await rerender({ hasActions: false, canMerge: false });
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    expect(screen.getByRole("group", { name: "What action would you like to perform first?" }))
      .toHaveAccessibleDescription(`${hint} Error:${validationError}`);
    expect(emitted().select).toBeUndefined();
  });
});
