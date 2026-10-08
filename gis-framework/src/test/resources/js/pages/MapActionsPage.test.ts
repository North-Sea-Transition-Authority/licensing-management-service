import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import MapActionsPage from "@/pages/MapActionsPage.vue";

const { getFeaturesMock, getOutlineNodesMock } = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
  getOutlineNodesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getFeatures: getFeaturesMock,
  getOutlineNodes: getOutlineNodesMock,
  getTextualDescription: vi.fn(),
}));

function feature(featureId: string, startDepth: number, endDepth: number): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("startDepth", startDepth);
  created.set("endDepth", endDepth);
  return created;
}

const baseMapStub = {
  props: ["features", "refitOnFeaturesChange"],
  template: `<p>Map of {{ features.map(f => f.get("featureId")).join(",") }}, refits: {{ refitOnFeaturesChange }}</p>`,
};

const textualDescriptionStub = {
  props: ["textualDescriptionUrl", "commandJourneyId"],
  template: `<p>Description from {{ textualDescriptionUrl }} for {{ commandJourneyId }}</p>`,
};

function renderPage() {
  return render(MapActionsPage, {
    props: {
      commandJourneyId: "journey-1",
      srsWkid: SupportedWkid.ED50_WKID,
      featuresBaseUrl: "/api/gis-framework/command-journey-features",
      outlineNodesBaseUrl: "/api/gis-framework/command-journey-outline-nodes",
      textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
    },
    global: {
      stubs: {
        BaseMap: baseMapStub,
        TextualDescription: textualDescriptionStub,
      },
    },
  });
}

describe("depthActionsPage", () => {
  beforeEach(() => {
    getFeaturesMock.mockReset().mockResolvedValue([feature("shallow", 0, -100), feature("deep", -100, -200)]);
    getOutlineNodesMock.mockReset().mockResolvedValue([]);
  });

  it("loads the features and outline nodes from the urls built from the command journey id", async () => {
    renderPage();

    await waitFor(() => {
      expect(getFeaturesMock).toHaveBeenCalledWith("/api/gis-framework/command-journey-features/journey-1");
      expect(getOutlineNodesMock).toHaveBeenCalledWith("/api/gis-framework/command-journey-outline-nodes/journey-1");
    });
  });

  it.each([
    {
      description: "a single feature split across depths",
      features: [feature("feature-1", 0, -100), feature("feature-1", -100, -200)],
      expectedLabels: ["Split", "Add strata", "Rename"],
    },
    {
      description: "two features",
      features: [feature("feature-1", 0, -100), feature("feature-2", -100, -200)],
      expectedLabels: ["Split", "Add strata", "Merge", "Rename"],
    },
  ])("offers $expectedLabels for $description", async ({ features, expectedLabels }) => {
    getFeaturesMock.mockReset().mockResolvedValue(features);

    renderPage();

    await screen.findByText(/^Map of feature-1/);
    expect(screen.getAllByRole("radio").map(radio => (radio as HTMLInputElement).labels?.[0]?.textContent?.trim()))
      .toEqual(expectedLabels);
  });

  it("passes the map only the features at the selected depth", async () => {
    renderPage();
    expect(await screen.findByText("Map of shallow, refits: false")).toBeInTheDocument();

    await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), "1");

    expect(await screen.findByText("Map of deep, refits: false")).toBeInTheDocument();
  });

  it("shows the depth slider when the features have depths", async () => {
    renderPage();

    expect(await screen.findByRole("slider", { name: "Select depth" })).toBeInTheDocument();
  });

  it("hides the depth slider when the features have no depths", async () => {
    const withoutDepths = new Feature();
    withoutDepths.set("featureId", "feature-1");
    getFeaturesMock.mockReset().mockResolvedValue([withoutDepths]);

    renderPage();

    await screen.findByText("Map of feature-1, refits: false");
    expect(screen.queryByRole("slider", { name: "Select depth" })).not.toBeInTheDocument();
  });

  it("passes the textual description url and command journey id to the description", () => {
    renderPage();

    expect(screen.getByText("Description from /api/gis-framework/command-journey-textual-description for journey-1"))
      .toBeInTheDocument();
  });

  it("shows the error summary above the map linking to the first action when continuing without a selection", async () => {
    renderPage();
    await screen.findByRole("radio", { name: "Split" });

    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    const summaryLink = screen.getByRole("link", { name: "Select what action you would like to perform" });
    expect(summaryLink).toHaveAttribute("href", `#${screen.getByRole("radio", { name: "Split" }).id}`);
    expect(summaryLink.compareDocumentPosition(screen.getByText(/^Map of/)))
      .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it("removes the error summary when continuing with a selection after an error", async () => {
    renderPage();
    await screen.findByRole("radio", { name: "Split" });
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    await fireEvent.click(screen.getByRole("radio", { name: "Split" }));
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    expect(screen.queryByRole("link", { name: "Select what action you would like to perform" })).not.toBeInTheDocument();
  });

  it("shows an error when the features request fails", async () => {
    getFeaturesMock.mockReset().mockRejectedValue(new Error("network error"));

    renderPage();

    expect(await screen.findByText("Unable to load the features.")).toBeInTheDocument();
  });
});
