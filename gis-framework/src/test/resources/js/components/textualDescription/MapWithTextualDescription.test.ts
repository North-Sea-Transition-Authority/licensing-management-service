import { render, screen, waitFor } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";

import MapWithTextualDescription
  from "../../../../../main/resources/js/components/textualDescription/MapWithTextualDescription.vue";

const { getTextualDescriptionMock } = vi.hoisted(() => ({
  getTextualDescriptionMock: vi.fn(),
}));

vi.mock("../../../../../main/resources/js/api/features.api", () => ({
  getTextualDescription: getTextualDescriptionMock,
}));

// Registers <ol-map> as a slotless labelled <div>. Because the stub does not render its default slot,
// none of BaseMap's OpenLayers layer children mount (so no canvas/WebGL code runs under jsdom), while
// the fall-through aria-label still lands on the rendered element.
const openLayersStub = {
  install(app: { component: (name: string, component: object) => void }) {
    app.component("ol-map", { inheritAttrs: true, template: "<div><!-- ol-map stub --></div>" });
  },
};

const baseProps = {
  srsWkid: 4230,
  featuresUrl: "/api/gis-framework/features?featureIds=feature-1",
  outlineNodesUrl: "/api/gis-framework/outline-nodes?featureIds=feature-1",
  textualDescriptionUrl: "/api/gis-framework/textual-description?featureId=feature-1",
} as const;

describe("mapWithTextualDescription", () => {
  it("renders the textual description text and the map", async () => {
    getTextualDescriptionMock.mockResolvedValue(
      "<div class=\"gis-textual-description\"><p>Subarea 30/1a is bounded by the following coordinates:</p></div>",
    );

    render(MapWithTextualDescription, {
      props: { ...baseProps },
      global: { plugins: [openLayersStub] },
    });

    await waitFor(() => {
      expect(
        screen.getByText("Subarea 30/1a is bounded by the following coordinates:"),
      ).toBeInTheDocument();
    });

    expect(
      screen.getByLabelText("A map displaying TODO: EPGF-78 insert displayed shape names here"),
    ).toBeInTheDocument();
  });
});
