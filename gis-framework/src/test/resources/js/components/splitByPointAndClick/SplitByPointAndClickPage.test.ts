import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SplitByPointAndClickPage
  from "../../../../../main/resources/js/components/splitByPointAndClick/SplitByPointAndClickPage.vue";
import { SupportedWkid } from "../../../../../main/resources/js/coordinate-system-utils";

const { splitFeatureMock } = vi.hoisted(() => ({
  splitFeatureMock: vi.fn(),
}));

vi.mock("../../../../../main/resources/js/api/split.api", () => ({
  splitFeature: splitFeatureMock,
}));

// Stubs BaseMap entirely, exposing buttons that emit update:points with a fixed points array, so tests
// can drive the page's auto-split watcher without simulating real map clicks/OpenLayers internals.
const baseMapStub = {
  props: [
    "srsWkid",
    "featuresUrl",
    "outlineNodesUrl",
    "includeNstaQuadrants",
    "includeNstaBlocks",
    "refreshCounter",
  ],
  emits: ["update:points"],
  template: `
    <div>
      <p data-testid="features-url">{{ featuresUrl }}</p>
      <p data-testid="refresh-counter">{{ refreshCounter }}</p>
      <button data-testid="emit-one-point" @click="$emit('update:points', [{ coordinates: [0, 0], originalSrsCoordinates: [1, 2] }])">one</button>
      <button data-testid="emit-two-points" @click="$emit('update:points', [
        { coordinates: [0, 0], originalSrsCoordinates: [1, 2] },
        { coordinates: [0, 0], originalSrsCoordinates: [3, 4] },
      ])">two</button>
    </div>
  `,
};

const baseProps = {
  commandJourneyId: "journey-1",
  srsWkid: SupportedWkid.ED50_WKID,
  featuresBaseUrl: "/api/gis-framework/features",
  outlineNodesBaseUrl: "/api/gis-framework/outline-nodes",
  splitUrl: "/api/gis-framework/split",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
};

function renderPage() {
  return render(SplitByPointAndClickPage, {
    props: { ...baseProps },
    global: { stubs: { BaseMap: baseMapStub } },
  });
}

describe("splitByPointAndClickPage", () => {
  beforeEach(() => {
    splitFeatureMock.mockReset();
  });

  it("builds the initial features url from the given command journey id", () => {
    renderPage();

    expect(screen.getByTestId("features-url").textContent)
      .toBe("/api/gis-framework/features/journey-1");
  });

  it("does not attempt a split with fewer than two points", async () => {
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-one-point"));

    expect(splitFeatureMock).not.toHaveBeenCalled();
  });

  it("automatically splits once two points are drawn, and refreshes the map on success", async () => {
    splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2", "feature-3"] });
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-two-points"));

    await waitFor(() => {
      expect(splitFeatureMock).toHaveBeenCalledWith(
        "/api/gis-framework/split",
        [
          { coordinates: [0, 0], originalSrsCoordinates: [1, 2] },
          { coordinates: [0, 0], originalSrsCoordinates: [3, 4] },
        ],
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId("refresh-counter").textContent).toBe("1");
    });
  });

  it("signals the map to clear the drawn line after a successful split", async () => {
    splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2", "feature-3"] });
    renderPage();

    expect(screen.getByTestId("refresh-counter").textContent).toBe("0");

    await fireEvent.click(screen.getByTestId("emit-two-points"));

    await waitFor(() => {
      expect(screen.getByTestId("refresh-counter").textContent).toBe("1");
    });
  });

  it("does not show an error when the cutter line doesn't cross the feature boundary", async () => {
    const consoleWarnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    splitFeatureMock.mockResolvedValue({ outputFeatureIds: [] });
    const { container } = renderPage();

    await fireEvent.click(screen.getByTestId("emit-two-points"));

    await waitFor(() => {
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        "No split took place. Make sure your line crosses the feature boundary.",
      );
    });

    expect(container.querySelector(".govuk-error-summary")).not.toBeInTheDocument();
    expect(screen.getByTestId("refresh-counter").textContent).toBe("0");
  });

  it("shows an error message when the split request fails", async () => {
    splitFeatureMock.mockRejectedValue(new Error("network error"));
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-two-points"));

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent)
        .toContain("An error occurred while attempting to split the feature. Please try again.");
    });
  });
});
