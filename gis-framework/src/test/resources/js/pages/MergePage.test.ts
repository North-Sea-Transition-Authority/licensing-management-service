import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import MergePage from "@/pages/MergePage.vue";

const { getCommandJourneyFeaturesMock } = vi.hoisted(() => ({
  getCommandJourneyFeaturesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getCommandJourneyFeatures: getCommandJourneyFeaturesMock,
  getTextualDescription: vi.fn(),
  getOutlineNodes: vi.fn(),
}));

// Stubs BaseMap, exposing the wired features url and refresh counter without exercising OpenLayers.
const baseMapStub = {
  props: ["srsWkid", "featuresUrl", "refreshCounter"],
  template: `<div><p data-testid="features-url">{{ featuresUrl }}</p></div>`,
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
    getCommandJourneyFeaturesMock.mockReset().mockResolvedValue([
      { featureId: "feature-1", featureName: "Block A" },
      { featureId: "feature-2", featureName: "Block B" },
    ]);
  });

  it("renders a checkbox for each loaded feature", async () => {
    renderPage();

    expect(await screen.findByRole("checkbox", { name: "Block A" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Block B" })).toBeInTheDocument();
  });

  it("builds the features url from the command journey id", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId("features-url").textContent)
        .toBe("/api/gis-framework/command-journey-features/journey-1");
    });
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
      expect(getCommandJourneyFeaturesMock).toHaveBeenCalledTimes(2);
    });
  });

  it("shows an error message when the features cannot be loaded", async () => {
    getCommandJourneyFeaturesMock.mockReset().mockRejectedValue(new Error("network error"));
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
