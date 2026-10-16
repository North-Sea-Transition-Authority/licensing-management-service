import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, ref } from "vue";
import SplitPanel from "@/components/mapActions/SplitPanel.vue";
import { SupportedWkid } from "@/coordinate-system-utils";

const { splitFeatureMock } = vi.hoisted(() => ({
  splitFeatureMock: vi.fn(),
}));

vi.mock("@/api/operator.api", () => ({
  splitFeature: splitFeatureMock,
}));

const firstPoint = { id: "point-1", displayName: "Point one", coordinates: [0, 0], originalSrsCoordinates: [1, 2] };
const secondPoint = { id: "point-2", displayName: "Point two", coordinates: [0, 0], originalSrsCoordinates: [3, 4] };

const host = defineComponent({
  components: { SplitPanel },
  props: { withoutDepth: { type: Boolean, default: false } },
  emits: ["success", "error", "back", "undo-last-point"],
  setup() {
    const panel = ref<InstanceType<typeof SplitPanel> | null>(null);
    const points = ref<typeof firstPoint[]>([]);
    return { panel, points, firstPoint, secondPoint, srsWkid: SupportedWkid.ED50_WKID };
  },
  template: `
    <split-panel
      ref="panel"
      base-url="/api/gis-framework"
      command-journey-id="journey-1"
      csrf-header-name="X-CSRF-TOKEN"
      csrf-token="csrf-token"
      :points="points"
      :srs-wkid="srsWkid"
      :depth="withoutDepth ? undefined : -100"
      @success="$emit('success')"
      @error="$emit('error', $event)"
      @back="$emit('back')"
      @undo-last-point="$emit('undo-last-point')"
    />
    <button @click="points = [firstPoint]">Draw first point</button>
    <button @click="points = [firstPoint, secondPoint]">Draw line</button>
    <button @click="panel?.focus()">Focus panel</button>`,
});

describe("splitPanel", () => {
  beforeEach(() => {
    splitFeatureMock.mockReset();
  });

  it("splits at the given depth and emits success once a line is drawn", async () => {
    splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2"] });
    const { emitted } = render(host);

    await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

    await waitFor(() => {
      expect(emitted("success")).toHaveLength(1);
    });
    expect(splitFeatureMock).toHaveBeenCalledWith(
      "/api/gis-framework/split",
      [firstPoint, secondPoint],
      "journey-1",
      "X-CSRF-TOKEN",
      "csrf-token",
      -100,
    );
  });

  it.each([
    { withoutDepth: false, expected: true },
    { withoutDepth: true, expected: false },
  ])("shows the depth slider locked hint is $expected when withoutDepth is $withoutDepth", ({ withoutDepth, expected }) => {
    render(host, { props: { withoutDepth } });

    const hint = screen.queryByText(/The current depth slider is locked while you draw/);

    expect(hint !== null).toBe(expected);
  });

  it("emits the error message when the split request fails", async () => {
    splitFeatureMock.mockRejectedValue(new Error("network error"));
    const { emitted } = render(host);

    await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

    await waitFor(() => {
      expect(emitted("error")).toEqual([
        ["An error occurred while attempting to split the feature. Please try again."],
      ]);
    });
  });

  it("emits undo-last-point when the last point is removed", async () => {
    const { emitted } = render(host);
    await fireEvent.click(screen.getByRole("button", { name: "Draw first point" }));

    await fireEvent.click(screen.getByRole("link", { name: /Remove/ }));

    expect(emitted("undo-last-point")).toHaveLength(1);
  });

  it("emits back when going back to the action select", async () => {
    const { emitted } = render(host);

    await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

    expect(emitted("back")).toHaveLength(1);
  });

  it("focuses the heading when focused", async () => {
    render(host);

    await fireEvent.click(screen.getByRole("button", { name: "Focus panel" }));

    expect(screen.getByRole("heading", { name: "Split" })).toHaveFocus();
  });
});
