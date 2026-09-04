import { describe, expect, it, vi } from "vitest";
import { buildServiceUrl, createPaginatedVectorSource } from "@/nsta-data-source";

// Interface to use with mocks, and to verify we pass the right options to the VectorSource.
interface VectorSourceOptions {
  format: unknown,
  loader: (
    this: {
      getFormat: () => {
        readFeatures: (data: unknown, options: { featureProjection: unknown }) => Array<unknown>,
      } | undefined,
      removeLoadedExtent: (extent: unknown) => void,
    },
    extent: unknown,
    resolution: unknown,
    projection: unknown,
  ) => Promise<Array<unknown>>,
}

const mocks = vi.hoisted(() => {
  const vectorSourceOptions: Array<VectorSourceOptions> = [];

  return {
    vectorSourceOptions,
    vectorSourceConstructor: vi.fn(class VectorSource {
      constructor(options: VectorSourceOptions) {
        vectorSourceOptions.push(options);
      }
    }),
    esriJsonConstructor: vi.fn(class EsriJSON {}),
  };
});

vi.mock("ol/source/Vector", () => ({
  default: mocks.vectorSourceConstructor,
}));

vi.mock("ol/format", () => ({
  EsriJSON: mocks.esriJsonConstructor,
}));

describe("buildServiceUrl", () => {
  it("builds the service URL without out fields", () => {
    expect(buildServiceUrl("UKCS_quadrants_(WGS84)")).toBe(
      "https://services-eu1.arcgis.com/OZMfUznmLTnWccBc/arcgis/rest/services/UKCS_quadrants_(WGS84)/FeatureServer/0/query?where=1%3D1&f=json",
    );
  });

  it("builds the service URL with encoded out fields", () => {
    expect(buildServiceUrl("UKCS_quadrants_(WGS84)", "QUADRANT")).toBe(
      "https://services-eu1.arcgis.com/OZMfUznmLTnWccBc/arcgis/rest/services/UKCS_quadrants_(WGS84)/FeatureServer/0/query?where=1%3D1&f=json&outFields=QUADRANT",
    );
  });
});

describe("createPaginatedVectorSource", () => {
  it("creates a vector source using an EsriJSON format", () => {
    createPaginatedVectorSource("https://example.test/query");

    expect(mocks.esriJsonConstructor).toHaveBeenCalledOnce();

    expect(mocks.vectorSourceConstructor).toHaveBeenCalledWith(
      expect.objectContaining({
        format: expect.anything(),
        loader: expect.any(Function),
      }),
    );
  });

  it("loads all pages while the service says there are more results", async () => {
    createPaginatedVectorSource("https://example.test/query");

    const loader = mocks.vectorSourceOptions[0].loader;

    const readFeatures = vi.fn()
      .mockReturnValueOnce(["feature-1", "feature-2"])
      .mockReturnValueOnce(["feature-3"]);

    const fetchMock = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ exceededTransferLimit: true }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ exceededTransferLimit: false }),
      });

    vi.stubGlobal("fetch", fetchMock);

    const features = await loader.call(
      {
        getFormat: () => ({ readFeatures }),
        removeLoadedExtent: vi.fn(),
      },
      "extent",
      1,
      "projection",
    );

    expect(features).toEqual(["feature-1", "feature-2", "feature-3"]);

    expect(fetchMock.mock.calls[0][0]).toContain("resultOffset=0");
    expect(fetchMock.mock.calls[1][0]).toContain("resultOffset=2");

    expect(readFeatures).toHaveBeenCalledWith(
      { exceededTransferLimit: true },
      { featureProjection: "projection" },
    );
  });
});
