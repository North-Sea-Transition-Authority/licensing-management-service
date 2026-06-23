import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import { defineComponent } from "vue";
import BaseMap from "../../../../../main/resources/js/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "../../../../../main/resources/js/coordinate-system-utils";

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
const FeatureLayerStub = defineComponent({
  props: {
    featuresUrl: {
      type: String,
      required: true,
    },
    olMap: {
      type: Object,
      required: false,
    },
  },
  template: `<div data-testid="feature-layer" :data-features-url="featuresUrl" />`,
});
const SnapPointsLayerStub = defineComponent({
  props: {
    olMap: {
      type: Object,
      required: false,
    },
    srsWkid: {
      type: Number,
      required: true,
    },
  },
  template: `<div data-testid="snap-points-layer" :data-srs-wkid="srsWkid" />`,
});

describe("baseMap", () => {
  it("renders the base OpenStreetMap map and feature layer", async () => {
    render(BaseMap, {
      props: {
        includeNstaQuadrants: false,
        includeSnapPoints: false,
        featuresUrl: "dummyUrl",
        srsWkid: SupportedWkid.ED50_WKID,
      },
      global: {
        stubs: {
          "ol-map": OlMapStub,
          "ol-view": { template: `<div data-testid="ol-view" />` },
          "ol-tile-layer": { template: `<div data-testid="ol-tile-layer"><slot /></div>` },
          "ol-source-osm": { template: `<div data-testid="ol-source-osm" />` },
          "NstaQuadrantLayer": NstaQuadrantLayerStub,
          "FeatureLayer": FeatureLayerStub,
          "SnapPointsLayer": SnapPointsLayerStub,
        },
      },
    });

    expect(screen.getByTestId("ol-map")).toBeInTheDocument();
    expect(screen.getByTestId("ol-view")).toBeInTheDocument();
    expect(screen.getByTestId("ol-tile-layer")).toBeInTheDocument();
    expect(screen.getByTestId("ol-source-osm")).toBeInTheDocument();
    expect(await screen.findByTestId("feature-layer")).toHaveAttribute("data-features-url", "dummyUrl");
    expect(screen.queryByTestId("nsta-quadrant-layer")).not.toBeInTheDocument();
    expect(screen.queryByTestId("snap-points-layer")).not.toBeInTheDocument();
  });

  it("renders the NSTA quadrant layer when requested", async () => {
    render(BaseMap, {
      props: {
        includeNstaQuadrants: true,
        featuresUrl: "dummyUrl",
        srsWkid: SupportedWkid.ED50_WKID,
      },
      global: {
        stubs: {
          "ol-map": OlMapStub,
          "ol-view": { template: `<div data-testid="ol-view" />` },
          "ol-tile-layer": { template: `<div data-testid="ol-tile-layer"><slot /></div>` },
          "ol-source-osm": { template: `<div data-testid="ol-source-osm" />` },
          "NstaQuadrantLayer": NstaQuadrantLayerStub,
          "FeatureLayer": FeatureLayerStub,
          "SnapPointsLayer": SnapPointsLayerStub,
        },
      },
    });

    expect(screen.getByTestId("ol-map")).toBeInTheDocument();
    expect(screen.getByTestId("ol-view")).toBeInTheDocument();
    expect(screen.getByTestId("ol-tile-layer")).toBeInTheDocument();
    expect(screen.getByTestId("ol-source-osm")).toBeInTheDocument();
    expect(await screen.findByTestId("feature-layer")).toHaveAttribute("data-features-url", "dummyUrl");
    expect(await screen.findByTestId("nsta-quadrant-layer")).toBeInTheDocument();
  });

  it("render snap point layer when requested", async () => {
    render(BaseMap, {
      props: {
        featuresUrl: "dummyUrl",
        srsWkid: SupportedWkid.ED50_WKID,
      },
      global: {
        stubs: {
          "ol-map": OlMapStub,
          "ol-view": { template: `<div data-testid="ol-view" />` },
          "ol-tile-layer": { template: `<div data-testid="ol-tile-layer"><slot /></div>` },
          "ol-source-osm": { template: `<div data-testid="ol-source-osm" />` },
          "NstaQuadrantLayer": NstaQuadrantLayerStub,
          "FeatureLayer": FeatureLayerStub,
          "SnapPointsLayer": SnapPointsLayerStub,
        },
      },
    });

    const snapPointsLayer = await screen.findByTestId("snap-points-layer");
    expect(snapPointsLayer).toHaveAttribute("data-srs-wkid", SupportedWkid.ED50_WKID.toString());
  });
});
