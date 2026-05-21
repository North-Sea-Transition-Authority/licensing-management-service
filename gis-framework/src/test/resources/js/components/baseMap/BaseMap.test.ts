import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import { defineComponent } from "vue";
import BaseMap from "../../../../../main/resources/js/components/baseMap/BaseMap.vue";

const OlMapStub = defineComponent({
  template: `<div data-testid="ol-map"><slot /></div>`,
});
const NstaQuadrantLayerStub = defineComponent({
  props: {
    olMap: {
      type: Object,
      required: false,
    },
  },
  template: `<div data-testid="nsta-quadrant-layer" />`,
});

describe("baseMap", () => {
  it("renders the base OpenStreetMap map", () => {
    render(BaseMap, {
      props: {
        includeNstaQuadrants: false,
      },
      global: {
        stubs: {
          "ol-map": OlMapStub,
          "ol-view": { template: `<div data-testid="ol-view" />` },
          "ol-tile-layer": { template: `<div data-testid="ol-tile-layer"><slot /></div>` },
          "ol-source-osm": { template: `<div data-testid="ol-source-osm" />` },
          "NstaQuadrantLayer": NstaQuadrantLayerStub,
        },
      },
    });

    expect(screen.getByTestId("ol-map")).toBeInTheDocument();
    expect(screen.getByTestId("ol-view")).toBeInTheDocument();
    expect(screen.getByTestId("ol-tile-layer")).toBeInTheDocument();
    expect(screen.getByTestId("ol-source-osm")).toBeInTheDocument();
    expect(screen.queryByTestId("nsta-quadrant-layer")).not.toBeInTheDocument();
  });

  it("renders the NSTA quadrant layer when requested", async () => {
    render(BaseMap, {
      props: {
        includeNstaQuadrants: true,
      },
      global: {
        stubs: {
          "ol-map": OlMapStub,
          "ol-view": { template: `<div data-testid="ol-view" />` },
          "ol-tile-layer": { template: `<div data-testid="ol-tile-layer"><slot /></div>` },
          "ol-source-osm": { template: `<div data-testid="ol-source-osm" />` },
          "NstaQuadrantLayer": NstaQuadrantLayerStub,
        },
      },
    });

    expect(screen.getByTestId("ol-map")).toBeInTheDocument();
    expect(screen.getByTestId("ol-view")).toBeInTheDocument();
    expect(screen.getByTestId("ol-tile-layer")).toBeInTheDocument();
    expect(screen.getByTestId("ol-source-osm")).toBeInTheDocument();
    expect(await screen.findByTestId("nsta-quadrant-layer")).toBeInTheDocument();
  });
});
