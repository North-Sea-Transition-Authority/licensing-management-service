import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, ref } from "vue";
import StrataSplitPanel from "@/components/mapActions/StrataSplitPanel.vue";

const { strataSplitFeaturesMock } = vi.hoisted(() => ({
  strataSplitFeaturesMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  strataSplitFeatures: strataSplitFeaturesMock,
}));

const featureOptions = [
  {
    featureId: "feature-1",
    featureName: "Block A",
    depthRanges: [{ startDepth: 0, endDepth: Number.NEGATIVE_INFINITY }],
  },
  {
    featureId: "feature-2",
    featureName: "Block B",
    depthRanges: [
      { startDepth: 0, endDepth: -1000 },
      { startDepth: -1000, endDepth: -3000 },
    ],
  },
];

const host = defineComponent({
  components: { StrataSplitPanel },
  emits: ["success", "error", "back", "validation-error"],
  setup() {
    const panel = ref<InstanceType<typeof StrataSplitPanel> | null>(null);
    const selectedFeatureIds = ref<string[]>([]);
    return { panel, selectedFeatureIds, featureOptions };
  },
  template: `
    <strata-split-panel
      ref="panel"
      v-model:selected-feature-ids="selectedFeatureIds"
      base-url="/api/gis-framework"
      command-journey-id="journey-1"
      csrf-header-name="X-CSRF-TOKEN"
      csrf-token="csrf-token"
      :feature-options="featureOptions"
      @success="$emit('success')"
      @error="$emit('error', $event)"
      @back="$emit('back')"
      @validation-error="$emit('validation-error', $event)"
    />
    <p>Selected: {{ selectedFeatureIds.join(",") }}</p>
    <button @click="panel?.focus()">Focus panel</button>`,
});

const depthLabel = "What depth is the new stratigraphic boundary at?";
const depthHint = "Enter a whole number below 0, for example -2000";
const featuresLegend = "Select the subareas to add strata";

async function enterDepth(depth: string) {
  await fireEvent.update(screen.getByRole("textbox", { name: depthLabel }), depth);
}

async function tick(...featureNames: string[]) {
  for (const featureName of featureNames) {
    await fireEvent.click(screen.getByRole("checkbox", { name: featureName }));
  }
}

async function addStrata() {
  await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));
}

describe("strataSplitPanel", () => {
  beforeEach(() => {
    strataSplitFeaturesMock.mockReset();
  });

  it("lists a checkbox for each feature option described by its depth ranges", () => {
    render(host);

    expect(screen.getByRole("group", { name: featuresLegend })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Block A" })).toHaveAccessibleDescription("0 to -infinity");
    expect(screen.getByRole("checkbox", { name: "Block B" }))
      .toHaveAccessibleDescription("0 to -1000, -1000 to -3000");
  });

  it("updates the selected feature ids when features are ticked", async () => {
    render(host);

    await tick("Block A", "Block B");

    expect(screen.getByText("Selected: feature-1,feature-2")).toBeInTheDocument();
  });

  it.each([
    { depth: "", error: "Enter the depth of the new stratigraphic boundary" },
    { depth: "abc", error: "Depth must be a whole number" },
    { depth: "-10.5", error: "Depth must be a whole number" },
    { depth: "0", error: "Depth must be below 0" },
    { depth: "100", error: "Depth must be below 0" },
  ])("shows '$error' for the depth '$depth'", async ({ depth, error }) => {
    const { emitted } = render(host);
    await enterDepth(depth);
    await tick("Block A");

    await addStrata();

    expect(screen.getByRole("textbox", { name: depthLabel })).toHaveAccessibleDescription(`${depthHint} Error:${error}`);
    expect(emitted("validation-error")).toEqual([
      [[{ message: error, fieldId: screen.getByRole("textbox", { name: depthLabel }).id }]],
    ]);
    expect(strataSplitFeaturesMock).not.toHaveBeenCalled();
  });

  it("shows an error when no features are selected", async () => {
    const { emitted } = render(host);
    await enterDepth("-2000");

    await addStrata();

    const error = "Select at least one subarea to split";
    expect(screen.getByRole("group", { name: featuresLegend })).toHaveAccessibleDescription(`Error:${error}`);
    expect(emitted("validation-error")).toEqual([
      [[{ message: error, fieldId: screen.getByRole("checkbox", { name: "Block A" }).id }]],
    ]);
    expect(strataSplitFeaturesMock).not.toHaveBeenCalled();
  });

  it("shows an error naming the selected features that cannot be split at the depth", async () => {
    render(host);
    await enterDepth("-1000");
    await tick("Block A", "Block B");

    await addStrata();

    expect(screen.getByRole("group", { name: featuresLegend })).toHaveAccessibleDescription(
      "Error:Block B cannot be split at -1000 as it is outside the subarea or is already a boundary",
    );
    expect(strataSplitFeaturesMock).not.toHaveBeenCalled();
  });

  it("emits both errors in field order when the features and depth are invalid", async () => {
    const { emitted } = render(host);

    await addStrata();

    expect(emitted("validation-error")).toEqual([
      [[
        {
          message: "Select at least one subarea to split",
          fieldId: screen.getByRole("checkbox", { name: "Block A" }).id,
        },
        {
          message: "Enter the depth of the new stratigraphic boundary",
          fieldId: screen.getByRole("textbox", { name: depthLabel }).id,
        },
      ]],
    ]);
  });

  it("strata splits the selected features at the depth, clears the errors and emits success", async () => {
    strataSplitFeaturesMock.mockResolvedValue({ outputFeatureIds: ["feature-3", "feature-4"] });
    const { emitted } = render(host);
    await enterDepth(" -2000 ");
    await tick("Block A", "Block B");

    await addStrata();

    await waitFor(() => {
      expect(emitted("success")).toHaveLength(1);
    });
    expect(emitted("validation-error")).toEqual([[[]]]);
    expect(strataSplitFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/strata-split",
      ["feature-1", "feature-2"],
      -2000,
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
  });

  it("disables both buttons while the strata split request is in progress", async () => {
    strataSplitFeaturesMock.mockReturnValue(new Promise(() => {}));
    render(host);
    await enterDepth("-2000");
    await tick("Block A");

    await addStrata();

    expect(screen.getByRole("button", { name: "Add strata" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Back to action select" })).toBeDisabled();
  });

  it("emits the error message when the strata split request fails", async () => {
    strataSplitFeaturesMock.mockRejectedValue(new Error("network error"));
    const { emitted } = render(host);
    await enterDepth("-2000");
    await tick("Block A");

    await addStrata();

    await waitFor(() => {
      expect(emitted("error")).toEqual([
        ["An error occurred while attempting to stratigraphically split the features. Please try again."],
      ]);
    });
  });

  it("emits back when going back to the action select", async () => {
    const { emitted } = render(host);

    await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

    expect(emitted("back")).toHaveLength(1);
  });

  it("focuses the heading when focused", async () => {
    render(host);

    await fireEvent.click(screen.getByRole("button", { name: "Focus panel" }));

    expect(screen.getByRole("heading", { name: "Add strata" })).toHaveFocus();
  });
});
