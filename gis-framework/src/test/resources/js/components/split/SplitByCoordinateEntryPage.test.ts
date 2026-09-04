import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import SplitByCoordinateEntryPage
  from "@/pages/SplitByCoordinateEntryPage.vue";

const {
  getTextualDescriptionMock,
  getSplitHistoryStatusMock,
  splitFeatureMock,
} = vi.hoisted(() => ({
  getTextualDescriptionMock: vi.fn(),
  getSplitHistoryStatusMock: vi.fn(),
  splitFeatureMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getTextualDescription: getTextualDescriptionMock,
  getOutlineNodes: vi.fn(),
}));

vi.mock("@/api/split-history.api", () => ({
  getSplitHistoryStatus: getSplitHistoryStatusMock,
}));

vi.mock("@/api/split.api", () => ({
  splitFeature: splitFeatureMock,
  undoSplit: vi.fn(),
  redoSplit: vi.fn(),
}));

// Stubs BaseMap entirely, exposing the props the page wires into it, so tests can assert the
// command-journey wiring without exercising OpenLayers.
const baseMapStub = {
  props: [
    "srsWkid",
    "featuresUrl",
    "outlineNodesUrl",
    "includeNstaQuadrants",
    "includeNstaBlocks",
    "includeSnapPoints",
    "includeDrawLine",
    "selectedPoints",
    "refreshCounter",
  ],
  template: `
    <div>
      <p data-testid="features-url">{{ featuresUrl }}</p>
      <p data-testid="refresh-counter">{{ refreshCounter }}</p>
    </div>
  `,
};

const baseProps = {
  commandJourneyId: "journey-1",
  srsWkid: SupportedWkid.BNG_WKID,
  featuresBaseUrl: "/api/gis-framework/features",
  outlineNodesBaseUrl: "/api/gis-framework/outline-nodes",
  splitUrl: "/api/gis-framework/split",
  historyBaseUrl: "/api/gis-framework/split-history",
  undoBaseUrl: "/api/gis-framework/undo",
  redoBaseUrl: "/api/gis-framework/redo",
  textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
};

function renderPage() {
  return render(SplitByCoordinateEntryPage, {
    props: { ...baseProps },
    global: { stubs: { BaseMap: baseMapStub } },
  });
}

async function enterTwoPoints() {
  await fireEvent.update(screen.getByLabelText("Grid reference"), "TQ 389 773");
  await fireEvent.click(screen.getByRole("button", { name: "Add after" }));

  await waitFor(() => expect(screen.getAllByLabelText("Grid reference")).toHaveLength(2));
  await fireEvent.update(screen.getAllByLabelText("Grid reference")[1], "TQ 400 800");
}

function cardHeadings(): string[] {
  return screen.getAllByRole("heading", { level: 3 }).map(heading => heading.textContent!.trim());
}

describe("splitByCoordinateEntryPage", () => {
  beforeEach(() => {
    getTextualDescriptionMock.mockReset().mockResolvedValue("");
    getSplitHistoryStatusMock.mockReset().mockResolvedValue({ canUndo: false, canRedo: false });
    splitFeatureMock.mockReset();
  });

  it("enables the split button once two valid coordinates are entered", async () => {
    renderPage();

    await waitFor(() => expect(screen.getByRole("button", { name: "Split" })).toBeDisabled());

    await enterTwoPoints();

    await waitFor(() => expect(screen.getByRole("button", { name: "Split" })).toBeEnabled());
  });

  it("clears the entered points and refreshes the description after a successful split", async () => {
    getTextualDescriptionMock
      .mockResolvedValueOnce("Original description")
      .mockResolvedValue("Updated description");
    splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2", "feature-3"] });
    renderPage();

    await waitFor(() => expect(screen.getByText("Original description")).toBeInTheDocument());

    await enterTwoPoints();
    await fireEvent.click(screen.getByRole("button", { name: "Split" }));

    await waitFor(() => {
      expect(screen.getByText("Updated description")).toBeInTheDocument();
      expect(cardHeadings()).toEqual(["Point 1"]);
      expect(screen.getByLabelText<HTMLInputElement>("Grid reference").value).toBe("");
    });
  });

  it("shows an error summary when a split action fails", async () => {
    splitFeatureMock.mockRejectedValue(new Error("network error"));
    renderPage();

    await enterTwoPoints();
    await fireEvent.click(screen.getByRole("button", { name: "Split" }));

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent)
        .toContain("An error occurred while attempting to split the feature. Please try again.");
    });
  });

  it("clears a previous error after a successful split", async () => {
    splitFeatureMock
      .mockResolvedValueOnce({ outputFeatureIds: [] })
      .mockResolvedValue({ outputFeatureIds: ["feature-2"] });
    renderPage();

    await enterTwoPoints();
    await fireEvent.click(screen.getByRole("button", { name: "Split" }));

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent)
        .toContain("No split took place. Make sure your line crosses the feature boundary.");
    });

    await fireEvent.click(screen.getByRole("button", { name: "Split" }));

    await waitFor(() => expect(screen.queryByRole("alert")).toBeNull());
  });
});
