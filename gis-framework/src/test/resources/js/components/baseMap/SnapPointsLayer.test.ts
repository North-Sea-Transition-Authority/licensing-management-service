import type Map from "ol/Map";
import type { SnapPoint } from "../../../../../main/resources/js/grid-utils";
import { render, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SnapPointsLayer from "../../../../../main/resources/js/components/baseMap/SnapPointsLayer.vue";
import { SupportedWkid } from "../../../../../main/resources/js/coordinate-system-utils";

const mocks = vi.hoisted(() => ({
  generateSnapPoints: vi.fn(() => [] as SnapPoint[]),
}));

vi.mock("../../../../../main/resources/js/grid-utils", () => ({
  generateSnapPoints: mocks.generateSnapPoints,
}));

describe("snapPointsLayer", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  function renderSnapPointsLayer(srsWkid: number, zoom: number, snapPointSpacing?: number) {
    const view = {
      calculateExtent: vi.fn(() => [1, 2, 3, 4]),
      getZoom: vi.fn(() => zoom),
      on: vi.fn(),
      un: vi.fn(),
    };

    const olMap = {
      map: {
        getSize: vi.fn(() => [100, 100]),
        getView: vi.fn(() => view),
      } as unknown as Map,
    };

    return render(SnapPointsLayer, {
      props: {
        olMap,
        srsWkid,
        snapPointSpacing,
      },
      global: {
        stubs: {
          "ol-feature": true,
          "ol-geom-point": true,
          "ol-source-vector": { template: "<slot />" },
          "ol-style": true,
          "ol-style-circle": true,
          "ol-style-fill": true,
          "ol-vector-layer": { template: "<slot />" },
        },
      },
    });
  }

  it("calls generateSnapPoints with the WGS84 extent and ED50 wkid", async () => {
    renderSnapPointsLayer(SupportedWkid.ED50_WKID, 11);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).toHaveBeenCalledWith(1, 2, 3, 4, SupportedWkid.ED50_WKID, undefined);
    });
  });

  it("calls generateSnapPoints with the WGS84 extent and BNG wkid", async () => {
    renderSnapPointsLayer(SupportedWkid.BNG_WKID, 12);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).toHaveBeenCalledWith(1, 2, 3, 4, SupportedWkid.BNG_WKID, undefined);
    });
  });

  it("passes custom snap point spacing to generateSnapPoints", async () => {
    renderSnapPointsLayer(SupportedWkid.ED50_WKID, 11, 60);

    await waitFor(() => {
      expect(mocks.generateSnapPoints).toHaveBeenCalledWith(1, 2, 3, 4, SupportedWkid.ED50_WKID, 60);
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

  it("renders the snap points returned by generateSnapPoints", async () => {
    mocks.generateSnapPoints.mockReturnValue([
      { id: "120,6240", coordinates: [1.0, 52.0], originalSrsCoordinates: [42, 43] },
      { id: "121,6240", coordinates: [1.0083, 52.0], originalSrsCoordinates: [44, 45] },
    ]);

    const { container } = renderSnapPointsLayer(SupportedWkid.ED50_WKID, 11);

    await waitFor(() => {
      expect(container.querySelectorAll("ol-feature-stub")).toHaveLength(2);
    });
  });
});
