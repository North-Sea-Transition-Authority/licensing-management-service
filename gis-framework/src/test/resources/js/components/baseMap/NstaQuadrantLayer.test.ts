import type OlMap from "vue3-openlayers/map/OlMap";
import { render } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";
import NstaQuadrantLayer from "../../../../../main/resources/js/components/baseMap/NstaQuadrantLayer.vue";

const mocks = vi.hoisted(() => {
  const quadrantLayer = { layer: "quadrants" };
  const vectorSource = { source: "quadrants" };
  const quadrantStyle = { style: "quadrants" };

  return {
    addLayer: vi.fn(),
    quadrantLayer,
    vectorSource,
    quadrantStyle,
    buildServiceUrl: vi.fn(() => "https://example.test/query"),
    createPaginatedVectorSource: vi.fn(() => vectorSource),
    vectorLayerConstructor: vi.fn(class VectorLayer {
      constructor() {
        return quadrantLayer;
      }
    }),
    fillConstructor: vi.fn(class Fill {}),
    strokeConstructor: vi.fn(class Stroke {}),
    styleConstructor: vi.fn(class Style {
      constructor() {
        return quadrantStyle;
      }
    }),
  };
});

vi.mock("ol/layer/Vector", () => ({
  default: mocks.vectorLayerConstructor,
}));

vi.mock("ol/style", () => ({
  Fill: mocks.fillConstructor,
  Stroke: mocks.strokeConstructor,
  Style: mocks.styleConstructor,
}));

vi.mock("../../../../../main/resources/js/nsta-data-source", () => ({
  buildServiceUrl: mocks.buildServiceUrl,
  createPaginatedVectorSource: mocks.createPaginatedVectorSource,
}));

describe("nstaQuadrantLayer", () => {
  it("adds the quadrant vector layer to the OpenLayers map on mount", () => {
    const olMap = {
      map: {
        addLayer: mocks.addLayer,
      },
    } as unknown as InstanceType<typeof OlMap>;

    render(NstaQuadrantLayer, {
      props: {
        olMap,
      },
    });

    expect(mocks.buildServiceUrl).toHaveBeenCalledWith(
      "UKCS_quadrants_(WGS84)",
      "QUADRANT",
    );

    expect(mocks.createPaginatedVectorSource).toHaveBeenCalledWith(
      "https://example.test/query",
    );

    expect(mocks.vectorLayerConstructor).toHaveBeenCalledWith(
      expect.objectContaining({
        source: mocks.vectorSource,
        style: mocks.quadrantStyle,
        declutter: true,
      }),
    );

    expect(mocks.addLayer).toHaveBeenCalledWith(mocks.quadrantLayer);
  });
});
