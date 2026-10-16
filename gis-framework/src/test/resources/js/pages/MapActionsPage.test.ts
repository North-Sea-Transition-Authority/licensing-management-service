import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import MapActionsPage from "@/pages/MapActionsPage.vue";

const { getFeaturesMock, getOutlineNodesMock, getHistoryStatusMock, splitFeatureMock, mergeFeaturesMock } = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
  getOutlineNodesMock: vi.fn(),
  getHistoryStatusMock: vi.fn(),
  splitFeatureMock: vi.fn(),
  mergeFeaturesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getFeatures: getFeaturesMock,
  getOutlineNodes: getOutlineNodesMock,
  getTextualDescription: vi.fn(),
}));

vi.mock("@/api/history.api", () => ({
  getHistoryStatus: getHistoryStatusMock,
}));

vi.mock("@/api/operator.api", () => ({
  splitFeature: splitFeatureMock,
  mergeFeatures: mergeFeaturesMock,
}));

const firstPoint = { id: "point-1", displayName: "Point one", coordinates: [0, 0], originalSrsCoordinates: [1, 2] };
const secondPoint = { id: "point-2", displayName: "Point two", coordinates: [0, 0], originalSrsCoordinates: [3, 4] };

function feature(featureId: string, startDepth: number, endDepth: number): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("featureName", `${featureId} block`);
  created.set("startDepth", startDepth);
  created.set("endDepth", endDepth);
  return created;
}

const baseMapStub = defineComponent({
  props: ["features", "refitOnFeaturesChange", "includeDrawLine", "selectedFeatureIds"],
  emits: ["update:points"],
  setup: () => ({ firstPoint, secondPoint }),
  data: () => ({ drawnPoints: [] as typeof firstPoint[] }),
  methods: {
    drawPoints(points: typeof firstPoint[]) {
      this.drawnPoints = points;
      this.$emit("update:points", points);
    },
    removeLastPoint() {
      this.drawPoints(this.drawnPoints.slice(0, -1));
    },
  },
  template: `
    <div>
      <p>Map of {{ features.map(f => f.get("featureId")).join(",") }}, refits: {{ refitOnFeaturesChange }}</p>
      <p>Highlighted on map: {{ selectedFeatureIds.join(",") }}</p>
      <template v-if="includeDrawLine">
        <button @click="drawPoints([firstPoint])">Draw first point</button>
        <button @click="drawPoints([firstPoint, secondPoint])">Draw line</button>
      </template>
    </div>`,
});

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
      baseUrl: "/api/gis-framework",
      csrfHeaderName: "X-CSRF-TOKEN",
      csrfToken: "csrf-token",
    },
    global: {
      stubs: {
        BaseMap: baseMapStub,
        TextualDescription: textualDescriptionStub,
      },
    },
  });
}

describe("mapActionsPage", () => {
  beforeEach(() => {
    getFeaturesMock.mockReset().mockResolvedValue([feature("shallow", 0, -100), feature("deep", -100, -200)]);
    getOutlineNodesMock.mockReset().mockResolvedValue([]);
    getHistoryStatusMock.mockReset().mockResolvedValue({ canUndo: false, canRedo: false });
    splitFeatureMock.mockReset();
    mergeFeaturesMock.mockReset();
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

  it("loads the history status from the url built from the base url and command journey id", async () => {
    renderPage();

    await waitFor(() => {
      expect(getHistoryStatusMock).toHaveBeenCalledWith("/api/gis-framework/history/journey-1");
    });
  });

  it("asks for the next action when the journey already has actions", async () => {
    getHistoryStatusMock.mockReset().mockResolvedValue({ canUndo: true, canRedo: false });

    renderPage();

    expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
      .toBeInTheDocument();
  });

  it("shows an error when the history status request fails", async () => {
    getHistoryStatusMock.mockReset().mockRejectedValue(new Error("network error"));

    renderPage();

    expect(await screen.findByText("Unable to load the actions taken.")).toBeInTheDocument();
  });

  describe("when split is chosen", () => {
    async function chooseSplit() {
      await fireEvent.click(await screen.findByRole("radio", { name: "Split" }));
      await fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    }

    it("replaces the action select with the split panel and focuses its heading", async () => {
      renderPage();

      await chooseSplit();

      expect(screen.getByRole("heading", { name: "Split" })).toHaveFocus();
      expect(screen.queryByRole("radio", { name: "Split" })).not.toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Draw line" })).toBeInTheDocument();
    });

    it("splits the features at the selected depth when a line is drawn", async () => {
      splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2"] });
      renderPage();
      await screen.findByText("Map of shallow, refits: false");
      await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), "1");
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      await waitFor(() => {
        expect(splitFeatureMock).toHaveBeenCalledWith(
          "/api/gis-framework/split",
          [firstPoint, secondPoint],
          "journey-1",
          "X-CSRF-TOKEN",
          "csrf-token",
          -100,
        );
      });
    });

    it("splits the features without a depth when the features have no depths", async () => {
      const withoutDepths = new Feature();
      withoutDepths.set("featureId", "feature-1");
      getFeaturesMock.mockReset().mockResolvedValue([withoutDepths]);
      splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2"] });
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      await waitFor(() => {
        expect(splitFeatureMock).toHaveBeenCalledWith(
          "/api/gis-framework/split",
          [firstPoint, secondPoint],
          "journey-1",
          "X-CSRF-TOKEN",
          "csrf-token",
          undefined,
        );
      });
    });

    it("does not split until the line has at least two points", async () => {
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw first point" }));

      expect(splitFeatureMock).not.toHaveBeenCalled();
    });

    it("reloads the map and returns to the action select with the first action focused after a split", async () => {
      getHistoryStatusMock.mockReset()
        .mockResolvedValueOnce({ canUndo: false, canRedo: false })
        .mockResolvedValue({ canUndo: true, canRedo: false });
      splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2"] });
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
        .toBeInTheDocument();
      expect(screen.getByRole("radio", { name: "Split" })).toHaveFocus();
      expect(screen.queryByRole("heading", { name: "Split" })).not.toBeInTheDocument();
      expect(getFeaturesMock).toHaveBeenCalledTimes(2);
    });

    it("keeps showing the split depth band after the map reloads", async () => {
      getFeaturesMock.mockReset()
        .mockResolvedValueOnce([feature("shallow", 0, -100), feature("deep", -100, -200)])
        .mockResolvedValue([feature("shallow", 0, -100), feature("deep-west", -100, -200), feature("deep-east", -100, -200)]);
      splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["deep-west", "deep-east"] });
      renderPage();
      await screen.findByText("Map of shallow, refits: false");
      await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), "1");
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      expect(await screen.findByText("Map of deep-west,deep-east, refits: false")).toBeInTheDocument();
    });

    it("stays on the split panel without an error when the line does not split anything", async () => {
      vi.spyOn(console, "warn").mockImplementation(() => {});
      splitFeatureMock.mockResolvedValue({ outputFeatureIds: [] });
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      await waitFor(() => {
        expect(console.warn).toHaveBeenCalledWith("No split took place. Make sure your line crosses the feature boundary.");
      });
      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
      expect(screen.getByRole("heading", { name: "Split" })).toBeInTheDocument();
    });

    it("shows an error and stays on the split panel when the split request fails", async () => {
      splitFeatureMock.mockRejectedValue(new Error("network error"));
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      expect(await screen.findByText("An error occurred while attempting to split the feature. Please try again."))
        .toBeInTheDocument();
      expect(screen.getByRole("heading", { name: "Split" })).toBeInTheDocument();
    });

    it("locks the depth slider while the line has points", async () => {
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw first point" }));

      expect(screen.getByRole("slider", { name: "Select depth" })).toBeDisabled();
    });

    it("unlocks the depth slider when the last line point is removed", async () => {
      renderPage();
      await chooseSplit();
      await fireEvent.click(screen.getByRole("button", { name: "Draw first point" }));

      await fireEvent.click(screen.getByRole("link", { name: /Remove/ }));

      expect(screen.getByRole("slider", { name: "Select depth" })).toBeEnabled();
    });

    it("returns to the action select with the first action focused and the depth unlocked when going back", async () => {
      renderPage();
      await chooseSplit();
      await fireEvent.click(screen.getByRole("button", { name: "Draw first point" }));

      await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

      expect(screen.getByRole("radio", { name: "Split" })).toHaveFocus();
      expect(screen.getByRole("slider", { name: "Select depth" })).toBeEnabled();
      expect(screen.queryByRole("heading", { name: "Split" })).not.toBeInTheDocument();
    });
  });

  describe("when merge is chosen", () => {
    const warningText = "The attributes for the merged subarea will need to be defined again.";

    async function chooseMerge() {
      await fireEvent.click(await screen.findByRole("radio", { name: "Merge" }));
      await fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    }

    async function tick(...featureNames: string[]) {
      for (const featureName of featureNames) {
        await fireEvent.click(screen.getByRole("checkbox", { name: featureName }));
      }
    }

    function highlightedOnMap() {
      return screen.getByText(/^Highlighted on map:/).textContent?.trim();
    }

    it("replaces the action select with the merge panel and focuses its heading", async () => {
      renderPage();

      await chooseMerge();

      expect(screen.getByRole("heading", { name: "Merge" })).toHaveFocus();
      expect(screen.queryByRole("radio", { name: "Merge" })).not.toBeInTheDocument();
      expect(screen.getByRole("group", { name: "Select the features to merge" })).toBeInTheDocument();
    });

    it("lists every feature, including those outside the selected depth band", async () => {
      renderPage();

      await chooseMerge();

      expect(screen.getByText("Map of shallow, refits: false")).toBeInTheDocument();
      expect(screen.getAllByRole("checkbox").map(checkbox => (checkbox as HTMLInputElement).labels?.[0]?.textContent?.trim()))
        .toEqual(["deep block", "shallow block"]);
    });

    it.each([
      { ticked: ["shallow block"], expected: { warningShown: false, highlighted: "Highlighted on map: shallow" } },
      {
        ticked: ["shallow block", "deep block"],
        expected: { warningShown: true, highlighted: "Highlighted on map: shallow,deep" },
      },
    ])("highlights the ticked features and shows the warning as $expected with $ticked ticked", async ({
      ticked,
      expected,
    }) => {
      renderPage();
      await chooseMerge();

      await tick(...ticked);

      expect({
        warningShown: screen.queryByText(warningText) !== null,
        highlighted: highlightedOnMap(),
      }).toEqual(expected);
    });

    it("merges the selected features, then reloads the map and returns to the action select with the first action focused", async () => {
      getHistoryStatusMock.mockReset()
        .mockResolvedValueOnce({ canUndo: false, canRedo: false })
        .mockResolvedValue({ canUndo: true, canRedo: false });
      mergeFeaturesMock.mockResolvedValue({ outputFeatureId: "merged" });
      renderPage();
      await chooseMerge();
      await tick("shallow block", "deep block");

      await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

      expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
        .toBeInTheDocument();
      expect(mergeFeaturesMock).toHaveBeenCalledWith(
        "/api/gis-framework/merge",
        ["shallow", "deep"],
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token",
      );
      expect(screen.getByRole("radio", { name: "Split" })).toHaveFocus();
      expect(screen.queryByRole("heading", { name: "Merge" })).not.toBeInTheDocument();
      expect(getFeaturesMock).toHaveBeenCalledTimes(2);
    });

    it("clears the selection after a merge", async () => {
      mergeFeaturesMock.mockResolvedValue({ outputFeatureId: "merged" });
      renderPage();
      await chooseMerge();
      await tick("shallow block", "deep block");
      await fireEvent.click(screen.getByRole("button", { name: "Merge" }));
      await screen.findByRole("radio", { name: "Split" });

      await chooseMerge();

      expect(screen.getAllByRole("checkbox").filter(checkbox => (checkbox as HTMLInputElement).checked)).toEqual([]);
      expect(highlightedOnMap()).toBe("Highlighted on map:");
    });

    it("shows an error and stays on the merge panel when the merge request fails", async () => {
      mergeFeaturesMock.mockRejectedValue(new Error("network error"));
      renderPage();
      await chooseMerge();
      await tick("shallow block", "deep block");

      await fireEvent.click(screen.getByRole("button", { name: "Merge" }));

      expect(await screen.findByText("An error occurred while attempting to merge the features. Please try again."))
        .toBeInTheDocument();
      expect(screen.getByRole("heading", { name: "Merge" })).toBeInTheDocument();
    });

    it("returns to the action select with the first action focused and no features highlighted when going back", async () => {
      renderPage();
      await chooseMerge();
      await tick("shallow block", "deep block");

      await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

      expect(screen.getByRole("radio", { name: "Split" })).toHaveFocus();
      expect(screen.queryByRole("heading", { name: "Merge" })).not.toBeInTheDocument();
      expect(highlightedOnMap()).toBe("Highlighted on map:");
    });

    it("clears the selection when going back", async () => {
      renderPage();
      await chooseMerge();
      await tick("shallow block", "deep block");
      await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

      await chooseMerge();

      expect(screen.getAllByRole("checkbox").filter(checkbox => (checkbox as HTMLInputElement).checked)).toEqual([]);
      expect(highlightedOnMap()).toBe("Highlighted on map:");
    });
  });
});
