import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import SplitByPointAndClickPage
  from "@/pages/SplitByPointAndClickPage.vue";

// Stubs BaseMap entirely, exposing a button that emits update:points with a fixed two-point array, so
// tests can drive the page's point wiring without simulating real map clicks/OpenLayers internals.
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
      <button data-testid="emit-two-points" @click="$emit('update:points', [
        { coordinates: [0, 0], originalSrsCoordinates: [1, 2] },
        { coordinates: [0, 0], originalSrsCoordinates: [3, 4] },
      ])">two</button>
    </div>
  `,
};

// Stubs SplitActions entirely, exposing the props the page wires into it plus buttons that emit
// action-success/action-error, so tests can assert the wiring and drive the shared success/error handling
// without exercising the real split/undo/history behaviour.
const splitActionsStub = {
  props: [
    "autoSplit",
    "points",
    "splitUrl",
    "commandJourneyId",
    "refreshCounter",
    "historyUrl",
    "undoUrl",
    "redoUrl",
    "csrfHeaderName",
    "csrfToken",
  ],
  emits: ["action-success", "action-error"],
  template: `
    <div>
      <p data-testid="auto-split">{{ autoSplit }}</p>
      <p data-testid="split-url">{{ splitUrl }}</p>
      <p data-testid="split-journey">{{ commandJourneyId }}</p>
      <p data-testid="split-points">{{ JSON.stringify(points) }}</p>
      <p data-testid="history-url">{{ historyUrl }}</p>
      <p data-testid="undo-url">{{ undoUrl }}</p>
      <p data-testid="redo-url">{{ redoUrl }}</p>
      <button data-testid="emit-action-success" @click="$emit('action-success')">success</button>
      <button data-testid="emit-action-error" @click="$emit('action-error', 'undo failed')">error</button>
    </div>
  `,
};

// Stubs TextualDescription entirely, exposing the props the page wires into it, so tests can assert the
// command-journey wiring without exercising the real description fetch.
const textualDescriptionStub = {
  props: ["textualDescriptionUrl", "commandJourneyId", "refreshCounter"],
  template: `
    <div>
      <p data-testid="description-url">{{ textualDescriptionUrl }}</p>
      <p data-testid="description-journey">{{ commandJourneyId }}</p>
      <p data-testid="description-refresh">{{ refreshCounter }}</p>
    </div>
  `,
};

const baseProps = {
  commandJourneyId: "journey-1",
  srsWkid: SupportedWkid.ED50_WKID,
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
  return render(SplitByPointAndClickPage, {
    props: { ...baseProps },
    global: {
      stubs: {
        BaseMap: baseMapStub,
        SplitActions: splitActionsStub,
        TextualDescription: textualDescriptionStub,
      },
    },
  });
}

describe("splitByPointAndClickPage", () => {
  it("builds the initial features url from the given command journey id", () => {
    renderPage();

    expect(screen.getByTestId("features-url").textContent)
      .toBe("/api/gis-framework/features/journey-1");
  });

  it("passes the textual description url and command journey id to the description", () => {
    renderPage();

    expect(screen.getByTestId("description-url").textContent)
      .toBe("/api/gis-framework/command-journey-textual-description");
    expect(screen.getByTestId("description-journey").textContent).toBe("journey-1");
  });

  it("builds the history, undo and redo urls from the given command journey id", () => {
    renderPage();

    expect(screen.getByTestId("history-url").textContent)
      .toBe("/api/gis-framework/split-history/journey-1");
    expect(screen.getByTestId("undo-url").textContent)
      .toBe("/api/gis-framework/undo/journey-1");
    expect(screen.getByTestId("redo-url").textContent)
      .toBe("/api/gis-framework/redo/journey-1");
  });

  it("wires the split url, command journey id and auto-split into the split actions", () => {
    renderPage();

    expect(screen.getByTestId("auto-split").textContent).toBe("true");
    expect(screen.getByTestId("split-url").textContent).toBe("/api/gis-framework/split");
    expect(screen.getByTestId("split-journey").textContent).toBe("journey-1");
  });

  it("passes the drawn points to the split actions", async () => {
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-two-points"));

    await waitFor(() => {
      expect(JSON.parse(screen.getByTestId("split-points").textContent!)).toEqual([
        { coordinates: [0, 0], originalSrsCoordinates: [1, 2] },
        { coordinates: [0, 0], originalSrsCoordinates: [3, 4] },
      ]);
    });
  });

  it("refreshes the description when split actions succeed", async () => {
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-action-success"));

    await waitFor(() => {
      expect(screen.getByTestId("description-refresh").textContent).toBe("1");
    });
  });

  it("refreshes the map and clears any error when split actions succeed", async () => {
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-action-success"));

    await waitFor(() => {
      expect(screen.getByTestId("refresh-counter").textContent).toBe("1");
    });
  });

  it("clears the drawn points after a successful split", async () => {
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-two-points"));

    await waitFor(() => {
      expect(JSON.parse(screen.getByTestId("split-points").textContent!)).toHaveLength(2);
    });

    await fireEvent.click(screen.getByTestId("emit-action-success"));

    await waitFor(() => {
      expect(JSON.parse(screen.getByTestId("split-points").textContent!)).toHaveLength(0);
    });
  });

  it("shows an error message when split actions fail", async () => {
    renderPage();

    await fireEvent.click(screen.getByTestId("emit-action-error"));

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toContain("undo failed");
    });
  });
});
