import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import DepthSlider from "@/components/depthSlider/DepthSlider.vue";

const options = new Map<number, string>([
  [0, "-100 to 0"],
  [-200, "-infinity to -200"],
  [-100, "-200 to -100"],
]);

function renderSlider(modelValue: number, disabled = false) {
  return render(DepthSlider, {
    props: { options, modelValue, disabled },
  });
}

describe("depthSlider", () => {
  it("renders a label for each depth option", () => {
    renderSlider(0);

    expect(screen.getByText("-100 to 0")).toBeInTheDocument();
    expect(screen.getByText("-200 to -100")).toBeInTheDocument();
    expect(screen.getByText("-infinity to -200")).toBeInTheDocument();
    expect(screen.getByRole("slider", { name: "Select depth" })).toHaveAttribute("max", "2");
  });

  it.each([
    { modelValue: 0, expectedSliderValue: "0" },
    { modelValue: -100, expectedSliderValue: "1" },
    { modelValue: -200, expectedSliderValue: "2" },
  ])("positions the slider at $expectedSliderValue when the selected depth is $modelValue", ({ modelValue, expectedSliderValue }) => {
    renderSlider(modelValue);

    expect(screen.getByRole("slider", { name: "Select depth" })).toHaveValue(expectedSliderValue);
  });

  it.each([
    { selectedDepth: -200, sliderValue: "0", expectedDepth: 0 },
    { selectedDepth: 0, sliderValue: "1", expectedDepth: -100 },
    { selectedDepth: 0, sliderValue: "2", expectedDepth: -200 },
  ])("selects depth $expectedDepth when the slider is moved to $sliderValue", async ({ selectedDepth, sliderValue, expectedDepth }) => {
    const { emitted } = renderSlider(selectedDepth);

    await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), sliderValue);

    expect(emitted("update:modelValue")).toEqual([[expectedDepth]]);
  });

  it("selects the depth of a clicked label", async () => {
    const { emitted } = renderSlider(0);

    await fireEvent.click(screen.getByText("-200 to -100"));

    expect(emitted("update:modelValue")).toEqual([[-100]]);
  });

  it("does not select a depth when a label is clicked while disabled", async () => {
    const { emitted } = renderSlider(0, true);

    await fireEvent.click(screen.getByText("-200 to -100"));

    expect(screen.getByRole("slider", { name: "Select depth" })).toBeDisabled();
    expect(emitted("update:modelValue")).toBeUndefined();
  });
});
