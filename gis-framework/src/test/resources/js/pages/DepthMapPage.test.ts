import { render, screen, waitFor } from "@testing-library/vue";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import DepthMapPage from "@/pages/DepthMapPage.vue";

const { getFeaturesMock, getOutlineNodesMock } = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
  getOutlineNodesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getFeatures: getFeaturesMock,
  getOutlineNodes: getOutlineNodesMock,
  getTextualDescription: vi.fn(),
}));

function feature(startDepth: number, endDepth: number): Feature {
  const created = new Feature();
  created.set("startDepth", startDepth);
  created.set("endDepth", endDepth);
  return created;
}

// Stubs BaseMap, exposing the props the page wires in without exercising OpenLayers.
const baseMapStub = {
  props: ["srsWkid", "features", "outlineNodes", "refitOnFeaturesChange"],
  template: `<div><p data-testid="refit-on-features-change">{{ refitOnFeaturesChange }}</p></div>`,
};

// Stubs TextualDescription, exposing the props the page wires in without fetching the description.
const textualDescriptionStub = {
  props: ["textualDescriptionUrl", "commandJourneyId"],
  template: `
    <div>
      <p data-testid="description-url">{{ textualDescriptionUrl }}</p>
      <p data-testid="description-journey">{{ commandJourneyId }}</p>
    </div>
  `,
};

const baseProps = {
  commandJourneyId: "journey-1",
  srsWkid: SupportedWkid.ED50_WKID,
  featuresBaseUrl: "/api/gis-framework/command-journey-features",
  outlineNodesBaseUrl: "/api/gis-framework/command-journey-outline-nodes",
  textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
};

function renderPage() {
  return render(DepthMapPage, {
    props: { ...baseProps },
    global: {
      stubs: {
        BaseMap: baseMapStub,
        TextualDescription: textualDescriptionStub,
      },
    },
  });
}

describe("depthMapPage", () => {
  beforeEach(() => {
    getFeaturesMock.mockReset().mockResolvedValue([feature(0, -100), feature(-100, -200)]);
    getOutlineNodesMock.mockReset().mockResolvedValue([]);
  });

  it("loads the features and outline nodes from the urls built from the command journey id", async () => {
    renderPage();

    await waitFor(() => {
      expect(getFeaturesMock).toHaveBeenCalledWith("/api/gis-framework/command-journey-features/journey-1");
      expect(getOutlineNodesMock).toHaveBeenCalledWith("/api/gis-framework/command-journey-outline-nodes/journey-1");
    });
  });

  it("renders a depth band for each distinct start depth", async () => {
    renderPage();

    expect(await screen.findByText("-infinity to -100")).toBeInTheDocument();
    expect(screen.getByText("-100 to 0")).toBeInTheDocument();
  });

  it("renders a single all depths band when there are no features", async () => {
    getFeaturesMock.mockReset().mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("All depths")).toBeInTheDocument();
  });

  it("stops the map refitting when the visible features change", () => {
    renderPage();

    expect(screen.getByTestId("refit-on-features-change").textContent).toBe("false");
  });

  it("passes the textual description url and command journey id to the description", () => {
    renderPage();

    expect(screen.getByTestId("description-url").textContent)
      .toBe("/api/gis-framework/command-journey-textual-description");
    expect(screen.getByTestId("description-journey").textContent).toBe("journey-1");
  });
});
