import type Map from "ol/Map";
import type { SnapPoint } from "../../../../../main/resources/js/grid-utils";
import { render, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SnapPointsLayer from "../../../../../main/resources/js/components/baseMap/SnapPointsLayer.vue";
import { SupportedWkid } from "../../../../../main/resources/js/coordinate-system-utils";

const mocks = vi.hoisted(() => ({
  generateSnapPoints: vi.fn(() => [] as SnapPoint[]),
}));

vi.mock("../../../../../main/resources/js/grid-utils", async (importOriginal) => {
  const original = await importOriginal<typeof import("../../../../../main/resources/js/grid-utils")>();
  return {
    ...original,
    generateSnapPoints: mocks.generateSnapPoints,
  };
});

describe("snapPointsLayer", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  function renderSnapPointsLayer(srsWkid: SupportedWkid, zoom: number) {
    const view = {
      calculateExtent: vi.fn(() => [1, 2, 3, 4]),
      getZoom: vi.fn(() => zoom),
      on: vi.fn(),
      un: vi.fn(),
    };

    const viewport = {
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    };

    const olMap = {
      map: {
        getSize: vi.fn(() => [100, 100]),
        getView: vi.fn(() => view),
        getViewport: vi.fn(() => viewport),
        getPixelFromCoordinate: vi.fn(() => [0, 0]),
        on: vi.fn(),
        un: vi.fn(),
      } as unknown as Map,
    };

    return render(SnapPointsLayer, {
      props: {
        olMap,
        srsWkid,
      },
      global: {
        stubs: {
          "ol-feature": true,
          "ol-geom-point": true,
          "ol-overlay": true,
          "ol-source-vector": { template: "<slot />" },
          "ol-style": true,
          "ol-style-circle": true,
          "ol-style-fill": true,
          "ol-vector-layer": { template: "<slot />" },
        },
      },
    });
  }

  it("calls generateSnapPoints with ED50 wkid and renders the returned points", async () => {
    mocks.generateSnapPoints.mockReturnValue([
      { id: "3600,187200", coordinates: [1.0, 52.0], originalSrsCoordinates: [42, 43], displayName: "coord1" },
      { id: "3630,187200", coordinates: [1.0083, 52.0], originalSrsCoordinates: [44, 45], displayName: "coord2" },
    ]);

    // getSpacingForZoom(11, ED50) = 60
    const { container } = renderSnapPointsLayer(SupportedWkid.ED50_WKID, 11);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).toHaveBeenCalledWith(1, 2, 3, 4, SupportedWkid.ED50_WKID, 60);
      expect(container.querySelectorAll("ol-feature-stub")).toHaveLength(2);
    });
  });

  it("calls generateSnapPoints with BNG wkid and renders the returned points", async () => {
    mocks.generateSnapPoints.mockReturnValue([
      { id: "12120,4750", coordinates: [0.9993, 51.9977], originalSrsCoordinates: [606000, 237500], displayName: "coord1" },
      { id: "12130,4750", coordinates: [1.0066, 51.9975], originalSrsCoordinates: [606500, 237500], displayName: "coord2" },
    ]);

    // getSpacingForZoom(12, BNG) = 1000
    const { container } = renderSnapPointsLayer(SupportedWkid.BNG_WKID, 12);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).toHaveBeenCalledWith(1, 2, 3, 4, SupportedWkid.BNG_WKID, 1000);
      expect(container.querySelectorAll("ol-feature-stub")).toHaveLength(2);
    });
  });

  it("does not call generateSnapPoints below the minimum ED50 zoom", async () => {
    renderSnapPointsLayer(SupportedWkid.ED50_WKID, 10);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).not.toHaveBeenCalled();
    });
  });

  it("does not call generateSnapPoints below the minimum BNG zoom", async () => {
    renderSnapPointsLayer(SupportedWkid.BNG_WKID, 11);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).not.toHaveBeenCalled();
    });
  });
});
