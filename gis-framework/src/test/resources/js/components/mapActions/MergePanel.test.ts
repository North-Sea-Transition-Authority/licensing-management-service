import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, ref } from "vue";
import MergePanel from "@/components/mapActions/MergePanel.vue";

const { mergeFeaturesMock } = vi.hoisted(() => ({
  mergeFeaturesMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  mergeFeatures: mergeFeaturesMock,
}));

const featureOptions = [
  { featureId: "feature-1", featureName: "Block A", depthRanges: [{ startDepth: 0, endDepth: -1000 }] },
  {
    featureId: "feature-2",
    featureName: "Block B",
    depthRanges: [
      { startDepth: 0, endDepth: -1000 },
      { startDepth: -1000, endDepth: Number.NEGATIVE_INFINITY },
    ],
  },
  { featureId: "feature-3", featureName: "Block C", depthRanges: [{ startDepth: -1000, endDepth: -3000 }] },
];

const host = defineComponent({
  components: { MergePanel },
  props: { hasDepths: { type: Boolean, default: false } },
  emits: ["success", "error", "back"],
  setup() {
    const panel = ref<InstanceType<typeof MergePanel> | null>(null);
    const selectedFeatureIds = ref<string[]>([]);
    return { panel, selectedFeatureIds, featureOptions };
  },
  template: `
    <merge-panel
      ref="panel"
      v-model:selected-feature-ids="selectedFeatureIds"
      base-url="/api/gis-framework"
      command-journey-id="journey-1"
      csrf-header-name="X-CSRF-TOKEN"
      csrf-token="csrf-token"
      :feature-options="featureOptions"
      :has-depths="hasDepths"
      @success="$emit('success')"
      @error="$emit('error', $event)"
      @back="$emit('back')"
    />
    <p>Selected: {{ selectedFeatureIds.join(",") }}</p>
    <button @click="panel?.focus()">Focus panel</button>`,
});

const warningText = "The attributes for the merged subarea will need to be defined again.";

async function tick(...featureNames: string[]) {
  for (const featureName of featureNames) {
    await fireEvent.click(screen.getByRole("checkbox", { name: featureName }));
  }
}

describe("mergePanel", () => {
  beforeEach(() => {
    mergeFeaturesMock.mockReset();
  });

  it("lists a checkbox for each feature option under the legend", () => {
    render(host);

    expect(screen.getByRole("group", { name: "Select the subareas to merge" })).toBeInTheDocument();
    expect(screen.getAllByRole("checkbox").map(checkbox => (checkbox as HTMLInputElement).labels?.[0]?.textContent?.trim()))
      .toEqual(["Block A", "Block B", "Block C"]);
  });

  it("describes each feature option by its depth ranges", () => {
    render(host);

    expect(screen.getByRole("checkbox", { name: "Block A" })).toHaveAccessibleDescription("0 to -1000");
    expect(screen.getByRole("checkbox", { name: "Block B" }))
      .toHaveAccessibleDescription("0 to -1000, -1000 to -infinity");
    expect(screen.getByRole("checkbox", { name: "Block C" })).toHaveAccessibleDescription("-1000 to -3000");
  });

  it.each([
    { hasDepths: true, expected: true },
    { hasDepths: false, expected: false },
  ])("shows the merge across depths hint is $expected when hasDepths is $hasDepths", ({ hasDepths, expected }) => {
    render(host, { props: { hasDepths } });

    const hint = screen.queryByText(/Subareas can be merged across depths/);

    expect(hint !== null).toBe(expected);
  });

  it("updates the selected feature ids when features are ticked", async () => {
    render(host);

    await tick("Block A", "Block C");

    expect(screen.getByText("Selected: feature-1,feature-3")).toBeInTheDocument();
  });

  it.each([
    { ticked: [], expected: { warningShown: false, mergeEnabled: false } },
    { ticked: ["Block A"], expected: { warningShown: false, mergeEnabled: false } },
    { ticked: ["Block A", "Block B"], expected: { warningShown: true, mergeEnabled: true } },
  ])("shows the warning and enables merge as $expected with $ticked ticked", async ({ ticked, expected }) => {
    render(host);

    await tick(...ticked);

    expect({
      warningShown: screen.queryByText(warningText) !== null,
      mergeEnabled: !(screen.getByRole("button", { name: "Merge" }) as HTMLButtonElement).disabled,
    }).toEqual(expected);
  });

  it("merges the selected features and emits success", async () => {
    mergeFeaturesMock.mockResolvedValue({ outputFeatureId: "feature-4" });
    const { emitted } = render(host);
    await tick("Block A", "Block B");

    await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

    await waitFor(() => {
      expect(emitted("success")).toHaveLength(1);
    });
    expect(mergeFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/merge",
      ["feature-1", "feature-2"],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
  });

  it("disables both buttons while the merge request is in progress", async () => {
    mergeFeaturesMock.mockReturnValue(new Promise(() => {}));
    render(host);
    await tick("Block A", "Block B");

    await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

    expect(screen.getByRole("button", { name: "Merge" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Back to action select" })).toBeDisabled();
  });

  it("emits the error message when the merge request fails", async () => {
    mergeFeaturesMock.mockRejectedValue(new Error("network error"));
    const { emitted } = render(host);
    await tick("Block A", "Block B");

    await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

    await waitFor(() => {
      expect(emitted("error")).toEqual([
        ["An error occurred while attempting to merge the features. Please try again."],
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

    expect(screen.getByRole("heading", { name: "Merge" })).toHaveFocus();
  });
});
