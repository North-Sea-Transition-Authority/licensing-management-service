import type { JsonHistoryAction, JsonHistoryStatus } from "@/api/history.api";
import type { TransformationType } from "@/transformation-type";
import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import MapActionsPage from "@/pages/MapActionsPage.vue";

const {
  getFeaturesMock,
  getOutlineNodesMock,
  getHistoryStatusMock,
  splitFeatureMock,
  mergeFeaturesMock,
  strataSplitFeaturesMock,
  undoMock,
  redoMock,
} = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
  getOutlineNodesMock: vi.fn(),
  getHistoryStatusMock: vi.fn(),
  splitFeatureMock: vi.fn(),
  mergeFeaturesMock: vi.fn(),
  strataSplitFeaturesMock: vi.fn(),
  undoMock: vi.fn(),
  redoMock: vi.fn(),
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
  strataSplitFeatures: strataSplitFeaturesMock,
  undo: undoMock,
  redo: redoMock,
}));

const noHistory: JsonHistoryStatus = {
  undoTransformationType: null,
  redoTransformationType: null,
  actions: [],
};

const mergeAction: JsonHistoryAction = {
  number: 2,
  transformationType: "MERGE",
  inputFeatureNames: ["C_2", "D"],
  outputFeatureNames: ["mergeResult_ABCD"],
};

const splitAction: JsonHistoryAction = {
  number: 1,
  transformationType: "SPLIT",
  inputFeatureNames: ["C"],
  outputFeatureNames: ["C_1", "C_2"],
};

function historyAfter(transformationType: TransformationType): JsonHistoryStatus {
  return { ...noHistory, undoTransformationType: transformationType };
}

function historyAfterUndoing(transformationType: TransformationType): JsonHistoryStatus {
  return { ...noHistory, redoTransformationType: transformationType };
}

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

function featureWithoutDepths(featureId: string): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("featureName", `${featureId} block`);
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
    getHistoryStatusMock.mockReset().mockResolvedValue(noHistory);
    splitFeatureMock.mockReset();
    mergeFeaturesMock.mockReset();
    strataSplitFeaturesMock.mockReset();
    undoMock.mockReset();
    redoMock.mockReset();
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
      expectedLabels: ["Add strata", "Rename", "Split"],
    },
    {
      description: "two features",
      features: [feature("feature-1", 0, -100), feature("feature-2", -100, -200)],
      expectedLabels: ["Add strata", "Merge", "Rename", "Split"],
    },
    {
      description: "a single feature with no depths",
      features: [featureWithoutDepths("feature-1")],
      expectedLabels: ["Rename", "Split"],
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

    await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), "0");

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

  it("shows the error summary above the map linking to the first action, and the inline error, when continuing without a selection", async () => {
    renderPage();
    await screen.findByRole("radio", { name: "Split" });

    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    const summaryLink = screen.getByRole("link", { name: "Select what action you would like to perform" });
    expect(summaryLink).toHaveAttribute("href", `#${screen.getByRole("radio", { name: "Add strata" }).id}`);
    expect(summaryLink.compareDocumentPosition(screen.getByText(/^Map of/)))
      .toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    expect(screen.getByRole("group", { name: "What action would you like to perform first?" }))
      .toHaveAccessibleDescription("You can perform multiple actions before finalising. Error:Select what action you would like to perform");
  });

  it("removes the error summary and inline error when continuing with a selection after an error", async () => {
    renderPage();
    await screen.findByRole("radio", { name: "Rename" });
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    await fireEvent.click(screen.getByRole("radio", { name: "Rename" }));
    await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

    expect(screen.queryByRole("link", { name: "Select what action you would like to perform" })).not.toBeInTheDocument();
    expect(screen.getByRole("group", { name: "What action would you like to perform first?" }))
      .toHaveAccessibleDescription("You can perform multiple actions before finalising.");
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
    getHistoryStatusMock.mockReset().mockResolvedValue(historyAfter("SPLIT"));

    renderPage();

    expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
      .toBeInTheDocument();
  });

  it("shows an error when the history status request fails", async () => {
    getHistoryStatusMock.mockReset().mockRejectedValue(new Error("network error"));

    renderPage();

    expect(await screen.findByText("Unable to load the actions taken.")).toBeInTheDocument();
  });

  it("shows the actions taken in the action history with their count", async () => {
    getHistoryStatusMock.mockReset().mockResolvedValue({ ...historyAfter("MERGE"), actions: [mergeAction, splitAction] });

    renderPage();

    expect(await screen.findByText("Show action history (2 actions)")).toBeInTheDocument();
    expect(screen.getByText("mergeResult_ABCD")).toBeInTheDocument();
    expect(screen.getByText("C_1, C_2")).toBeInTheDocument();
  });

  describe("undo and redo", () => {
    it("removes the undone action from the action history", async () => {
      getHistoryStatusMock.mockReset()
        .mockResolvedValueOnce({ ...historyAfter("MERGE"), actions: [mergeAction, splitAction] })
        .mockResolvedValue({ ...historyAfter("SPLIT"), canRedo: true, redoTransformationType: "MERGE", actions: [splitAction] });
      undoMock.mockResolvedValue(undefined);
      renderPage();

      await fireEvent.click(await screen.findByRole("button", { name: "Undo merge" }));

      expect(await screen.findByText("Show action history (1 action)")).toBeInTheDocument();
      expect(screen.queryByText("mergeResult_ABCD")).not.toBeInTheDocument();
      expect(screen.getByText("C_1, C_2")).toBeInTheDocument();
    });

    it("names the undo and redo buttons after the commands they would reverse", async () => {
      getHistoryStatusMock.mockReset().mockResolvedValue({
        undoTransformationType: "STRATA_SPLIT",
        redoTransformationType: "MERGE",
        actions: [],
      });

      renderPage();

      expect(await screen.findByRole("button", { name: "Undo add strata" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "Redo merge" })).toBeEnabled();
    });

    it.each(["Split", "Merge", "Add strata"])("hides undo and redo while the %s panel is open", async (action) => {
      getHistoryStatusMock.mockReset().mockResolvedValue(historyAfter("SPLIT"));
      renderPage();
      await screen.findByRole("button", { name: "Undo split" });

      await fireEvent.click(screen.getByRole("radio", { name: action }));
      await fireEvent.click(screen.getByRole("button", { name: "Continue" }));

      expect(screen.queryByRole("button", { name: /^(Undo|Redo)/ })).not.toBeInTheDocument();
    });

    it("undoes the last action, then reloads the map and offers only the actions still available", async () => {
      getFeaturesMock.mockReset()
        .mockResolvedValueOnce([feature("shallow", 0, -100), feature("deep", -100, -200)])
        .mockResolvedValue([feature("merged", 0, -200)]);
      getHistoryStatusMock.mockReset()
        .mockResolvedValueOnce(historyAfter("SPLIT"))
        .mockResolvedValue(historyAfterUndoing("SPLIT"));
      undoMock.mockResolvedValue(undefined);
      renderPage();

      await fireEvent.click(await screen.findByRole("button", { name: "Undo split" }));

      expect(await screen.findByRole("group", { name: "What action would you like to perform first?" }))
        .toBeInTheDocument();
      expect(undoMock).toHaveBeenCalledWith("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token");
      expect(screen.getByText("Map of merged, refits: false")).toBeInTheDocument();
      expect(screen.queryByRole("radio", { name: "Merge" })).not.toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Redo split" })).toBeEnabled();
    });

    it("redoes the last undone action, then reloads the map and asks for the next action", async () => {
      getHistoryStatusMock.mockReset()
        .mockResolvedValueOnce(historyAfterUndoing("MERGE"))
        .mockResolvedValue(historyAfter("MERGE"));
      redoMock.mockResolvedValue(undefined);
      renderPage();

      await fireEvent.click(await screen.findByRole("button", { name: "Redo merge" }));

      expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
        .toBeInTheDocument();
      expect(redoMock).toHaveBeenCalledWith("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token");
      expect(getFeaturesMock).toHaveBeenCalledTimes(2);
      expect(screen.getByRole("button", { name: "Undo merge" })).toBeEnabled();
    });

    it("shows an error and stays on the action select when the undo request fails", async () => {
      getHistoryStatusMock.mockReset().mockResolvedValue(historyAfter("STRATA_SPLIT"));
      undoMock.mockRejectedValue(new Error("network error"));
      renderPage();

      await fireEvent.click(await screen.findByRole("button", { name: "Undo add strata" }));

      expect(await screen.findByText("An error occurred while attempting to undo the last add strata. Please try again."))
        .toBeInTheDocument();
      expect(screen.getByRole("radio", { name: "Split" })).toBeInTheDocument();
    });

    it.each([
      { pendingReload: "history", pendingMock: getHistoryStatusMock },
      { pendingReload: "features", pendingMock: getFeaturesMock },
    ])("keeps undo, redo and continue disabled until the $pendingReload reload after an undo finishes", async ({ pendingMock }) => {
      const history: JsonHistoryStatus = {
        undoTransformationType: "SPLIT",
        redoTransformationType: "MERGE",
        actions: [],
      };
      const features = [feature("shallow", 0, -100), feature("deep", -100, -200)];
      getHistoryStatusMock.mockReset().mockResolvedValue(history);
      getFeaturesMock.mockReset().mockResolvedValue(features);
      undoMock.mockResolvedValue(undefined);
      renderPage();
      await screen.findByRole("button", { name: "Undo split" });
      let finishReload!: () => void;
      pendingMock.mockReturnValueOnce(new Promise((resolve) => {
        finishReload = () => resolve(pendingMock === getHistoryStatusMock ? history : features);
      }));

      await fireEvent.click(screen.getByRole("button", { name: "Undo split" }));
      await waitFor(() => expect(pendingMock).toHaveBeenCalledTimes(2));

      expect(screen.getByRole("button", { name: "Undo split" })).toBeDisabled();
      expect(screen.getByRole("button", { name: "Redo merge" })).toBeDisabled();
      expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
      finishReload();
      await waitFor(() => expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled());
      expect(screen.getByRole("button", { name: "Undo split" })).toBeEnabled();
    });

    it("removes the validation error summary and inline error after a successful undo", async () => {
      getHistoryStatusMock.mockReset().mockResolvedValue(historyAfter("SPLIT"));
      undoMock.mockResolvedValue(undefined);
      renderPage();
      await screen.findByRole("button", { name: "Undo split" });
      await fireEvent.click(screen.getByRole("button", { name: "Continue" }));
      await screen.findByRole("link", { name: "Select what action you would like to perform" });

      await fireEvent.click(screen.getByRole("button", { name: "Undo split" }));

      await waitFor(() => {
        expect(screen.queryByRole("link", { name: "Select what action you would like to perform" })).not.toBeInTheDocument();
      });
      expect(screen.getByRole("group", { name: "What action would you like to perform next?" }))
        .toHaveAccessibleDescription("");
    });

    it("removes the error from a failed undo after a successful retry", async () => {
      const undoError = "An error occurred while attempting to undo the last split. Please try again.";
      getHistoryStatusMock.mockReset().mockResolvedValue(historyAfter("SPLIT"));
      undoMock.mockRejectedValueOnce(new Error("network error")).mockResolvedValue(undefined);
      renderPage();
      await fireEvent.click(await screen.findByRole("button", { name: "Undo split" }));
      await screen.findByText(undoError);

      await fireEvent.click(screen.getByRole("button", { name: "Undo split" }));

      await waitFor(() => expect(screen.queryByText(undoError)).not.toBeInTheDocument());
    });

    it("disables continue while an undo is in flight", async () => {
      getHistoryStatusMock.mockReset().mockResolvedValue(historyAfter("SPLIT"));
      undoMock.mockReturnValue(new Promise(() => {}));
      renderPage();

      await fireEvent.click(await screen.findByRole("button", { name: "Undo split" }));

      expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
    });
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
      await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), "0");
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
        .mockResolvedValueOnce(noHistory)
        .mockResolvedValue(historyAfter("SPLIT"));
      splitFeatureMock.mockResolvedValue({ outputFeatureIds: ["feature-2"] });
      renderPage();
      await chooseSplit();

      await fireEvent.click(screen.getByRole("button", { name: "Draw line" }));

      expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
        .toBeInTheDocument();
      expect(screen.getByRole("radio", { name: "Add strata" })).toHaveFocus();
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
      await fireEvent.update(screen.getByRole("slider", { name: "Select depth" }), "0");
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

      expect(screen.getByRole("radio", { name: "Add strata" })).toHaveFocus();
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
      expect(screen.getByRole("group", { name: "Select the subareas to merge" })).toBeInTheDocument();
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
        .mockResolvedValueOnce(noHistory)
        .mockResolvedValue(historyAfter("MERGE"));
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
      expect(screen.getByRole("radio", { name: "Add strata" })).toHaveFocus();
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

      expect(screen.getByRole("radio", { name: "Add strata" })).toHaveFocus();
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

  describe("when add strata is chosen", () => {
    const depthLabel = "What depth is the new stratigraphic boundary at?";

    async function chooseAddStrata() {
      await fireEvent.click(await screen.findByRole("radio", { name: "Add strata" }));
      await fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    }

    async function enterDepth(depth: string) {
      await fireEvent.update(screen.getByRole("textbox", { name: depthLabel }), depth);
    }

    async function tick(...featureNames: string[]) {
      for (const featureName of featureNames) {
        await fireEvent.click(screen.getByRole("checkbox", { name: featureName }));
      }
    }

    function highlightedOnMap() {
      return screen.getByText(/^Highlighted on map:/).textContent?.trim();
    }

    it("replaces the action select with the add strata panel and focuses its heading", async () => {
      renderPage();

      await chooseAddStrata();

      expect(screen.getByRole("heading", { name: "Add strata" })).toHaveFocus();
      expect(screen.queryByRole("radio", { name: "Add strata" })).not.toBeInTheDocument();
      expect(screen.getByRole("textbox", { name: depthLabel })).toBeInTheDocument();
    });

    it("lists every feature with its depth range, including those outside the selected depth band", async () => {
      renderPage();

      await chooseAddStrata();

      expect(screen.getByText("Map of shallow, refits: false")).toBeInTheDocument();
      expect(screen.getByRole("checkbox", { name: "deep block" })).toHaveAccessibleDescription("-100m to -200m");
      expect(screen.getByRole("checkbox", { name: "shallow block" })).toHaveAccessibleDescription("0m to -100m");
    });

    it("highlights the ticked features on the map", async () => {
      renderPage();
      await chooseAddStrata();

      await tick("deep block");

      expect(highlightedOnMap()).toBe("Highlighted on map: deep");
    });

    it("shows each validation error in the error summary linking to its field", async () => {
      renderPage();
      await chooseAddStrata();

      await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));

      expect(screen.getByRole("link", { name: "Enter the depth of the new stratigraphic boundary" }))
        .toHaveAttribute("href", `#${screen.getByRole("textbox", { name: depthLabel }).id}`);
      expect(screen.getByRole("link", { name: "Select at least one subarea to split" }))
        .toHaveAttribute("href", `#${screen.getByRole("checkbox", { name: "deep block" }).id}`);
      expect(strataSplitFeaturesMock).not.toHaveBeenCalled();
    });

    it("strata splits the selected features, then reloads the map and returns to the action select with the first action focused", async () => {
      getHistoryStatusMock.mockReset()
        .mockResolvedValueOnce(noHistory)
        .mockResolvedValue(historyAfter("STRATA_SPLIT"));
      strataSplitFeaturesMock.mockResolvedValue({ outputFeatureIds: ["deep_above", "deep_below"] });
      renderPage();
      await chooseAddStrata();
      await enterDepth("-150");
      await tick("deep block");

      await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));

      expect(await screen.findByRole("group", { name: "What action would you like to perform next?" }))
        .toBeInTheDocument();
      expect(strataSplitFeaturesMock).toHaveBeenCalledWith(
        "/api/gis-framework/strata-split",
        ["deep"],
        -150,
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token",
      );
      expect(screen.getByRole("radio", { name: "Add strata" })).toHaveFocus();
      expect(screen.queryByRole("heading", { name: "Add strata" })).not.toBeInTheDocument();
      expect(getFeaturesMock).toHaveBeenCalledTimes(2);
    });

    it("shows an error and stays on the add strata panel when the strata split request fails", async () => {
      strataSplitFeaturesMock.mockRejectedValue(new Error("network error"));
      renderPage();
      await chooseAddStrata();
      await enterDepth("-150");
      await tick("deep block");

      await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));

      expect(await screen.findByText(
        "An error occurred while attempting to stratigraphically split the features. Please try again.",
      )).toBeInTheDocument();
      expect(screen.getByRole("heading", { name: "Add strata" })).toBeInTheDocument();
    });

    it("replaces a failed request error with the validation errors on the next invalid attempt", async () => {
      const requestError = "An error occurred while attempting to stratigraphically split the features. Please try again.";
      strataSplitFeaturesMock.mockRejectedValue(new Error("network error"));
      renderPage();
      await chooseAddStrata();
      await enterDepth("-150");
      await tick("deep block");
      await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));
      await screen.findByText(requestError);
      await enterDepth("");

      await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));

      expect(screen.getByRole("link", { name: "Enter the depth of the new stratigraphic boundary" }))
        .toHaveAttribute("href", `#${screen.getByRole("textbox", { name: depthLabel }).id}`);
      expect(screen.queryByText(requestError)).not.toBeInTheDocument();
    });

    it("removes the error summary and highlighting when going back", async () => {
      renderPage();
      await chooseAddStrata();
      await tick("deep block");
      await fireEvent.click(screen.getByRole("button", { name: "Add strata" }));

      await fireEvent.click(screen.getByRole("button", { name: "Back to action select" }));

      expect(screen.getByRole("radio", { name: "Add strata" })).toHaveFocus();
      expect(screen.queryByRole("link", { name: "Enter the depth of the new stratigraphic boundary" })).not.toBeInTheDocument();
      expect(highlightedOnMap()).toBe("Highlighted on map:");
    });
  });
});
