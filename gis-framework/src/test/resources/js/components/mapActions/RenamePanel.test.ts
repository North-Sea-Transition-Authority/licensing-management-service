import type { FeatureOption } from "@/feature-option-utils";
import { fireEvent, render, screen } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, ref } from "vue";
import RenamePanel from "@/components/mapActions/RenamePanel.vue";

const { renameFeaturesMock } = vi.hoisted(() => ({
  renameFeaturesMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  renameFeatures: renameFeaturesMock,
}));

const allDepths = [{ startDepth: 0, endDepth: Number.NEGATIVE_INFINITY }];

const featureOptions: FeatureOption[] = [
  { featureId: "feature-1", featureName: "SHAPE 1", name: "Alpha", depthRanges: allDepths },
  { featureId: "feature-2", featureName: "SHAPE 2", name: "Bravo", depthRanges: allDepths },
  { featureId: "feature-3", featureName: "SHAPE 3", name: "", depthRanges: allDepths },
];

const featuresLegend = "Select the subareas to rename";
const mapDisplayNameHint = "Must be 15 characters or fewer";

function renderPanel(options: FeatureOption[] = featureOptions) {
  return render(defineComponent({
    components: { RenamePanel },
    emits: ["success", "error", "back", "validation-error"],
    setup() {
      const panel = ref<InstanceType<typeof RenamePanel> | null>(null);
      const selectedFeatureIds = ref<string[]>([]);
      return { panel, selectedFeatureIds, options };
    },
    template: `
      <rename-panel
        ref="panel"
        v-model:selected-feature-ids="selectedFeatureIds"
        base-url="/api/gis-framework"
        command-journey-id="journey-1"
        csrf-header-name="X-CSRF-TOKEN"
        csrf-token="csrf-token"
        :feature-options="options"
        @success="$emit('success')"
        @error="$emit('error', $event)"
        @back="$emit('back')"
        @validation-error="$emit('validation-error', $event)"
      />
      <p>Selected: {{ selectedFeatureIds.join(",") }}</p>
      <button @click="panel?.focus()">Focus panel</button>`,
  }));
}

function subareaNameInput(featureName: string) {
  return screen.getByRole("textbox", { name: `Subarea name for ${featureName}` });
}

function mapDisplayNameInput(featureName: string) {
  return screen.getByRole("textbox", { name: `Map display name for ${featureName}` });
}

async function tick(...featureNames: string[]) {
  for (const featureName of featureNames) {
    await fireEvent.click(screen.getByRole("checkbox", { name: featureName }));
  }
}

async function enterNames(featureName: string, names: { name?: string, featureName?: string }) {
  if (names.name !== undefined) {
    await fireEvent.update(subareaNameInput(featureName), names.name);
  }
  if (names.featureName !== undefined) {
    await fireEvent.update(mapDisplayNameInput(featureName), names.featureName);
  }
}

async function submit() {
  await fireEvent.click(screen.getByRole("button", { name: "Rename" }));
}

describe("renamePanel", () => {
  beforeEach(() => {
    renameFeaturesMock.mockReset().mockResolvedValue({ outputFeatureIds: ["renamed"] });
  });

  it("lists a checkbox for each subarea described by its depth ranges", () => {
    renderPanel();

    expect(screen.getByRole("group", { name: featuresLegend })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "SHAPE 1" })).toHaveAccessibleDescription("0m to -∞m");
    expect(screen.getByRole("checkbox", { name: "SHAPE 2" })).toHaveAccessibleDescription("0m to -∞m");
    expect(screen.getByRole("checkbox", { name: "SHAPE 3" })).toHaveAccessibleDescription("0m to -∞m");
  });

  it("updates the selected feature ids when subareas are ticked", async () => {
    renderPanel();

    await tick("SHAPE 1", "SHAPE 2");

    expect(screen.getByText("Selected: feature-1,feature-2")).toBeInTheDocument();
  });

  it("pre-fills each subarea's inputs with its current names", () => {
    renderPanel();

    expect(subareaNameInput("SHAPE 1")).toHaveValue("Alpha");
    expect(mapDisplayNameInput("SHAPE 1")).toHaveValue("SHAPE 1");
    expect(subareaNameInput("SHAPE 3")).toHaveValue("");
  });

  it("labels each subarea's inputs with the subarea they rename", async () => {
    renderPanel();

    await tick("SHAPE 1", "SHAPE 2");

    expect(screen.getByRole("textbox", { name: "Subarea name for SHAPE 1" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Map display name for SHAPE 1" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Subarea name for SHAPE 2" })).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Map display name for SHAPE 2" })).toBeInTheDocument();
  });

  it("keeps the label's subarea when its map display name is edited", async () => {
    renderPanel();
    await tick("SHAPE 1");

    await fireEvent.update(mapDisplayNameInput("SHAPE 1"), "SHAPE 1A");

    expect(screen.getByRole("textbox", { name: "Map display name for SHAPE 1" })).toHaveValue("SHAPE 1A");
  });

  it("keeps entered names when a subarea is unticked and ticked again", async () => {
    renderPanel();
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", { name: "Alpha 2", featureName: "SHAPE 1A" });

    await tick("SHAPE 1", "SHAPE 1");

    expect(subareaNameInput("SHAPE 1")).toHaveValue("Alpha 2");
    expect(mapDisplayNameInput("SHAPE 1")).toHaveValue("SHAPE 1A");
  });

  it("shows an error when no subareas are selected", async () => {
    const { emitted } = renderPanel();

    await submit();

    const error = "Select at least one subarea to rename";
    expect(screen.getByRole("group", { name: featuresLegend })).toHaveAccessibleDescription(`Error:${error}`);
    expect(emitted("validation-error")).toEqual([
      [[{ message: error, fieldId: screen.getByRole("checkbox", { name: "SHAPE 1" }).id }]],
    ]);
    expect(renameFeaturesMock).not.toHaveBeenCalled();
  });

  it.each([
    { names: { name: " " }, error: "Enter a subarea name for SHAPE 1" },
    { names: { name: " bravo " }, error: "Subarea name for SHAPE 1 must be unique within the block" },
  ])("shows '$error' on the subarea name on the first attempt", async ({ names, error }) => {
    const { emitted } = renderPanel();
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", names);

    await submit();

    expect(subareaNameInput("SHAPE 1")).toHaveAccessibleDescription(`Error:${error}`);
    expect(emitted("validation-error")).toEqual([
      [[{ message: error, fieldId: subareaNameInput("SHAPE 1").id }]],
    ]);
    expect(renameFeaturesMock).not.toHaveBeenCalled();
  });

  it.each([
    { featureName: "", error: "Enter a map display name for SHAPE 1" },
    { featureName: "ABCDEFGHIJKLMNOP", error: "Map display name for SHAPE 1 must be 15 characters or fewer" },
    { featureName: " shape 2 ", error: "Map display name for SHAPE 1 must be unique within the block" },
  ])("shows '$error' on the map display name on the first attempt", async ({ featureName, error }) => {
    const { emitted } = renderPanel();
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", { featureName });

    await submit();

    expect(mapDisplayNameInput("SHAPE 1")).toHaveAccessibleDescription(`${mapDisplayNameHint} Error:${error}`);
    expect(emitted("validation-error")).toEqual([
      [[{ message: error, fieldId: mapDisplayNameInput("SHAPE 1").id }]],
    ]);
    expect(renameFeaturesMock).not.toHaveBeenCalled();
  });

  it("shows an error on every selected subarea given the same new name", async () => {
    const { emitted } = renderPanel();
    await tick("SHAPE 1", "SHAPE 2");
    await enterNames("SHAPE 1", { name: "Delta" });
    await enterNames("SHAPE 2", { name: "delta" });

    await submit();

    expect(emitted("validation-error")).toEqual([[[
      { message: "Subarea name for SHAPE 1 must be unique within the block", fieldId: subareaNameInput("SHAPE 1").id },
      { message: "Subarea name for SHAPE 2 must be unique within the block", fieldId: subareaNameInput("SHAPE 2").id },
    ]]]);
    expect(renameFeaturesMock).not.toHaveBeenCalled();
  });

  it("shows an error for a selected subarea without a current subarea name", async () => {
    const { emitted } = renderPanel();
    await tick("SHAPE 3");
    await enterNames("SHAPE 3", { featureName: "SHAPE 3A" });

    await submit();

    expect(emitted("validation-error")).toEqual([[[
      { message: "Enter a subarea name for SHAPE 3", fieldId: subareaNameInput("SHAPE 3").id },
    ]]]);
    expect(renameFeaturesMock).not.toHaveBeenCalled();
  });

  it("renames only the selected subareas whose names changed, with trimmed names", async () => {
    const { emitted } = renderPanel();
    await tick("SHAPE 1", "SHAPE 2");
    await enterNames("SHAPE 1", { name: " Alpha 2 ", featureName: " SHAPE 1A " });

    await submit();

    expect(renameFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/rename",
      [{ featureId: "feature-1", name: "Alpha 2", featureName: "SHAPE 1A" }],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
    expect(emitted("success")).toHaveLength(1);
  });

  it("renames selected subareas that swap names", async () => {
    renderPanel();
    await tick("SHAPE 1", "SHAPE 2");
    await enterNames("SHAPE 1", { name: "Bravo", featureName: "SHAPE 2" });
    await enterNames("SHAPE 2", { name: "Alpha", featureName: "SHAPE 1" });

    await submit();

    expect(renameFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/rename",
      [
        { featureId: "feature-1", name: "Bravo", featureName: "SHAPE 2" },
        { featureId: "feature-2", name: "Alpha", featureName: "SHAPE 1" },
      ],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
  });

  it("renames a subarea whose names only change case", async () => {
    renderPanel();
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", { name: "ALPHA" });

    await submit();

    expect(renameFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/rename",
      [{ featureId: "feature-1", name: "ALPHA", featureName: "SHAPE 1" }],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
  });

  it("renames a subarea when unselected subareas already share a name", async () => {
    renderPanel([
      { featureId: "feature-1", featureName: "SHAPE 1", name: "Alpha", depthRanges: allDepths },
      { featureId: "feature-2", featureName: "DUPLICATE", name: "Duplicate", depthRanges: allDepths },
      { featureId: "feature-3", featureName: "duplicate", name: " duplicate ", depthRanges: allDepths },
    ]);
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", { name: "Alpha 2" });

    await submit();

    expect(renameFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/rename",
      [{ featureId: "feature-1", name: "Alpha 2", featureName: "SHAPE 1" }],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
  });

  it("renames a selected subarea that resolves an existing shared name", async () => {
    renderPanel([
      { featureId: "feature-1", featureName: "SHAPE 1", name: "Duplicate", depthRanges: allDepths },
      { featureId: "feature-2", featureName: "SHAPE 2", name: "Duplicate", depthRanges: allDepths },
    ]);
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", { name: "Alpha" });

    await submit();

    expect(renameFeaturesMock).toHaveBeenCalledWith(
      "/api/gis-framework/rename",
      [{ featureId: "feature-1", name: "Alpha", featureName: "SHAPE 1" }],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
    );
  });

  it("goes back without renaming when no selected subarea's names changed", async () => {
    const { emitted } = renderPanel();
    await tick("SHAPE 1", "SHAPE 2");

    await submit();

    expect(emitted("back")).toHaveLength(1);
    expect(emitted("validation-error")).toEqual([[[]]]);
    expect(renameFeaturesMock).not.toHaveBeenCalled();
  });

  it("emits the error when the rename request fails", async () => {
    renameFeaturesMock.mockRejectedValue(new Error("network error"));
    const { emitted } = renderPanel();
    await tick("SHAPE 1");
    await enterNames("SHAPE 1", { name: "Alpha 2" });

    await submit();

    expect(emitted("error")).toEqual([["An error occurred while attempting to rename the subareas. Please try again."]]);
    expect(emitted("success")).toBeUndefined();
  });

  it("emits back when going back to the action select", async () => {
    const { emitted } = renderPanel();

    await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

    expect(emitted("back")).toHaveLength(1);
  });

  it("focuses the heading when focused", async () => {
    renderPanel();

    await fireEvent.click(screen.getByRole("button", { name: "Focus panel" }));

    expect(screen.getByRole("heading", { name: "Rename subareas" })).toHaveFocus();
  });
});
