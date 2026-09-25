import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import MergePage from "@/pages/MergePage.vue";

const { getFeaturesMock } = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getFeatures: getFeaturesMock,
  getTextualDescription: vi.fn(),
  getOutlineNodes: vi.fn(),
}));

function feature(featureId: string, featureName: string): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("featureName", featureName);
  return created;
}

// Stubs BaseMap, exposing the wired features and refresh counter without exercising OpenLayers.
const baseMapStub = {
  props: ["srsWkid", "features", "outlineNodesUrl", "refreshCounter", "selectedFeatureIds"],
  template: `<div><p data-testid="selected-feature-ids">{{ JSON.stringify(selectedFeatureIds) }}</p></div>`,
};

// Stubs MergeActions, exposing the props the page wires in plus buttons that emit action-success/error.
const mergeActionsStub = {
  props: ["featureIds", "mergeUrl", "commandJourneyId", "refreshCounter", "baseUrl"],
  emits: ["action-success", "action-error"],
  template: `
    <div>
      <p data-testid="action-feature-ids">{{ JSON.stringify(featureIds) }}</p>
      <p data-testid="action-merge-url">{{ mergeUrl }}</p>
      <p data-testid="action-base-url">{{ baseUrl }}</p>
      <button data-testid="emit-action-success" @click="$emit('action-success')">success</button>
      <button data-testid="emit-action-error" @click="$emit('action-error', 'merge failed')">error</button>
    </div>
  `,
};

const textualDescriptionStub = {
  props: ["textualDescriptionUrl", "commandJourneyId", "refreshCounter"],
  template: `<div/>`,
};

const baseProps = {
  commandJourneyId: "journey-1",
  srsWkid: SupportedWkid.ED50_WKID,
  featuresBaseUrl: "/api/gis-framework/command-journey-features",
  outlineNodesBaseUrl: "/api/gis-framework/command-journey-outline-nodes",
  mergeUrl: "/api/gis-framework/merge",
  baseUrl: "/api/gis-framework",
  textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
  csrfHeaderName: "X-CSRF-TOKEN",
  csrfToken: "csrf-token-1",
};

function renderPage() {
  return render(MergePage, {
    props: { ...baseProps },
    global: {
      stubs: {
        BaseMap: baseMapStub,
        MergeActions: mergeActionsStub,
        TextualDescription: textualDescriptionStub,
      },
    },
  });
}

describe("mergePage", () => {
  beforeEach(() => {
    getFeaturesMock.mockReset().mockResolvedValue([
      feature("feature-1", "Block A"),
      feature("feature-2", "Block B"),
    ]);
  });

  it("renders a checkbox for each loaded feature", async () => {
    renderPage();

    expect(await screen.findByRole("checkbox", { name: "Block A" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Block B" })).toBeInTheDocument();
  });

  it("loads the features from the url built from the command journey id", async () => {
    renderPage();

    await waitFor(() => {
      expect(getFeaturesMock).toHaveBeenCalledWith("/api/gis-framework/command-journey-features/journey-1");
    });
  });

  it("orders the checkboxes by feature name", async () => {
    getFeaturesMock.mockReset().mockResolvedValue([
      feature("feature-2", "Block 10"),
      feature("feature-1", "Block 2"),
    ]);
    renderPage();

    await screen.findByRole("checkbox", { name: "Block 2" });

    const labels = screen.getAllByRole("checkbox").map(checkbox => checkbox.getAttribute("value"));
    expect(labels).toEqual(["feature-1", "feature-2"]);
  });

  it("renders a single checkbox when a feature spans multiple polygons", async () => {
    getFeaturesMock.mockReset().mockResolvedValue([
      feature("feature-1", "Block A"),
      feature("feature-1", "Block A"),
      feature("feature-2", "Block B"),
    ]);
    renderPage();

    await screen.findByRole("checkbox", { name: "Block A" });

    expect(screen.getAllByRole("checkbox")).toHaveLength(2);
  });

  it("wires the merge url and the base url into the merge actions", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId("action-merge-url").textContent).toBe("/api/gis-framework/merge");
      expect(screen.getByTestId("action-base-url").textContent).toBe("/api/gis-framework");
    });
  });

  it("passes the selected feature ids to the merge actions", async () => {
    renderPage();

    await fireEvent.click(await screen.findByRole("checkbox", { name: "Block A" }));

    await waitFor(() => {
      expect(JSON.parse(screen.getByTestId("action-feature-ids").textContent!)).toEqual(["feature-1"]);
    });
  });

  it("clears the selection and reloads the features after a successful merge", async () => {
    renderPage();

    await fireEvent.click(await screen.findByRole("checkbox", { name: "Block A" }));
    await waitFor(() => {
      expect(JSON.parse(screen.getByTestId("action-feature-ids").textContent!)).toEqual(["feature-1"]);
    });

    await fireEvent.click(screen.getByTestId("emit-action-success"));

    await waitFor(() => {
      expect(JSON.parse(screen.getByTestId("action-feature-ids").textContent!)).toEqual([]);
      expect(getFeaturesMock).toHaveBeenCalledTimes(2);
    });
  });

  it("shows an error message when the features cannot be loaded", async () => {
    getFeaturesMock.mockReset().mockRejectedValue(new Error("network error"));
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toContain("Unable to load the features to merge.");
    });
  });

  it("shows the error message emitted by the merge actions", async () => {
    renderPage();

    await fireEvent.click(await screen.findByTestId("emit-action-error"));

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toContain("merge failed");
    });
  });
});
