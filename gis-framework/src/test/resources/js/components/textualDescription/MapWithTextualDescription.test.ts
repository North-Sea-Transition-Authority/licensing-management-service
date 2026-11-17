import userEvent from "@testing-library/user-event";
import { render, screen, waitFor, within } from "@testing-library/vue";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import MapWithTextualDescription
  from "@/components/textualDescription/MapWithTextualDescription.vue";

const { getTextualDescriptionMock, getFeaturesMock, getOutlineNodesMock } = vi.hoisted(() => ({
  getTextualDescriptionMock: vi.fn(),
  getFeaturesMock: vi.fn(),
  getOutlineNodesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getTextualDescription: getTextualDescriptionMock,
  getFeatures: getFeaturesMock,
  getOutlineNodes: getOutlineNodesMock,
}));

// Registers <ol-map> as a slotless labelled <div>. Because the stub does not render its default slot,
// none of BaseMap's OpenLayers layer children mount (so no canvas/WebGL code runs under jsdom), while
// the fall-through aria-label still lands on the rendered element.
const openLayersStub = {
  install(app: { component: (name: string, component: object) => void }) {
    app.component("ol-map", { inheritAttrs: true, template: "<div><!-- ol-map stub --></div>" });
  },
};

function expectShown(element: HTMLElement | null, shown: boolean) {
  const assertion = expect(element);
  (shown ? assertion : assertion.not).toBeInTheDocument();
}

const baseProps = {
  srsWkid: 4230,
  featuresUrl: "/api/gis-framework/features?featureIds=feature-1",
  outlineNodesUrl: "/api/gis-framework/outline-nodes?featureIds=feature-1",
  textualDescriptionUrl: "/api/gis-framework/textual-description?featureId=feature-1",
} as const;

const MAP_LABEL = "A map displaying TODO: EPGF-78 insert displayed shape names here";
const DESCRIPTION = "Subarea 30/1a is bounded by the following coordinates:";

describe("mapWithTextualDescription", () => {
  beforeEach(() => {
    getTextualDescriptionMock.mockResolvedValue(`<p>${DESCRIPTION}</p>`);
    getFeaturesMock.mockResolvedValue([]);
    getOutlineNodesMock.mockResolvedValue([]);
  });

  it("when the toggle is off, then the textual description and the map are shown without view options", async () => {
    render(MapWithTextualDescription, {
      props: { ...baseProps },
      global: { plugins: [openLayersStub] },
    });

    expect(await screen.findByText(DESCRIPTION)).toBeInTheDocument();
    expect(screen.getByLabelText(MAP_LABEL)).toBeInTheDocument();
    expect(screen.queryByRole("radio")).not.toBeInTheDocument();
    expect(getFeaturesMock).toHaveBeenCalledWith(baseProps.featuresUrl);
    expect(getOutlineNodesMock).toHaveBeenCalledWith(baseProps.outlineNodesUrl);
  });

  it("passes the described feature to the map", async () => {
    getTextualDescriptionMock.mockResolvedValue("");
    getFeaturesMock.mockResolvedValue([]);
    getOutlineNodesMock.mockResolvedValue([]);

    render(MapWithTextualDescription, {
      props: { ...baseProps, describedFeatureId: "feature-1" },
      global: {
        plugins: [openLayersStub],
        stubs: {
          BaseMap: {
            props: ["describedFeatureId"],
            template: "<p>Described feature: {{ describedFeatureId }}</p>",
          },
        },
      },
    });

    expect(await screen.findByText("Described feature: feature-1")).toBeInTheDocument();
  });

  it.each([
    { clicks: [], checked: "Map", mapShown: true, descriptionShown: false },
    { clicks: ["Description"], checked: "Description", mapShown: false, descriptionShown: true },
    { clicks: ["Map and description"], checked: "Map and description", mapShown: true, descriptionShown: true },
    { clicks: ["Map and description", "Map"], checked: "Map", mapShown: true, descriptionShown: false },
  ])("when the toggle is on and $clicks is selected, then $checked is shown", async ({
    clicks,
    checked,
    mapShown,
    descriptionShown,
  }) => {
    render(MapWithTextualDescription, {
      props: { ...baseProps, toggle: true },
      global: { plugins: [openLayersStub] },
    });

    for (const name of clicks) {
      await userEvent.click(screen.getByRole("radio", { name }));
    }

    expect(screen.getByRole("radio", { name: checked })).toBeChecked();
    expectShown(screen.queryByLabelText(MAP_LABEL), mapShown);
    await waitFor(() => expectShown(screen.queryByText(DESCRIPTION), descriptionShown));
  });

  describe("highlighting selected features", () => {
    const baseMapStub = {
      props: ["selectedFeatureIds"],
      template: "<ul aria-label=\"Highlighted features\"><li v-for=\"id in selectedFeatureIds\" :key=\"id\">{{ id }}</li></ul>",
    };

    let hostCheckboxes: HTMLElement;

    function renderHostCheckboxes(checkedFeatureIds: string[]) {
      hostCheckboxes = document.createElement("div");
      hostCheckboxes.innerHTML = ["feature-1", "feature-2"].map(featureId => `
        <input type="checkbox" id="${featureId}" name="surrenderedFeatureIds" value="${featureId}"
          ${checkedFeatureIds.includes(featureId) ? "checked" : ""}>
        <label for="${featureId}">Select ${featureId}</label>`).join("");
      document.body.appendChild(hostCheckboxes);
    }

    function renderComponent(selectionInputName?: string) {
      render(MapWithTextualDescription, {
        props: { ...baseProps, selectionInputName },
        global: { plugins: [openLayersStub], stubs: { BaseMap: baseMapStub } },
      });
    }

    function highlightedFeatureIds() {
      return within(screen.getByRole("list", { name: "Highlighted features" }))
        .queryAllByRole("listitem")
        .map(item => item.textContent);
    }

    afterEach(() => hostCheckboxes.remove());

    it("highlights features already checked when mounted", async () => {
      renderHostCheckboxes(["feature-2"]);

      renderComponent("surrenderedFeatureIds");

      await waitFor(() => expect(highlightedFeatureIds()).toEqual(["feature-2"]));
    });

    it("updates highlighted features as checkboxes are checked and unchecked", async () => {
      renderHostCheckboxes(["feature-2"]);
      renderComponent("surrenderedFeatureIds");

      await userEvent.click(screen.getByLabelText("Select feature-1"));
      await userEvent.click(screen.getByLabelText("Select feature-2"));

      expect(highlightedFeatureIds()).toEqual(["feature-1"]);
    });

    it("highlights nothing when no selection input name is given", async () => {
      renderHostCheckboxes(["feature-2"]);
      renderComponent();

      await userEvent.click(screen.getByLabelText("Select feature-1"));

      expect(highlightedFeatureIds()).toEqual([]);
    });
  });
});
