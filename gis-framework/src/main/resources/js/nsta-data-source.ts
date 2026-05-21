import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import { EsriJSON } from "ol/format";
import VectorSource from "ol/source/Vector";

const esriJson = new EsriJSON();

const nstaArcGisServerBaseUrl = "https://services-eu1.arcgis.com/OZMfUznmLTnWccBc/arcgis/rest/services/";

/**
 * Builds a URL for a NSTA ArcGIS REST API resource.
 * @param resourcePath The path of the resource we want to query. A list of options can be found by navigating to the
 *        arcGIS baseUrl.
 * @param outFields The fields of the layer we want to query.
 */
export function buildServiceUrl(resourcePath: string, outFields = ""): string {
  const querySuffix = "/FeatureServer/0/query?where=1%3D1&f=json";
  const encodedOutFields = outFields ? `&outFields=${encodeURIComponent(outFields)}` : "";
  return `${nstaArcGisServerBaseUrl}${resourcePath}${querySuffix}${encodedOutFields}`;
}

/**
 * Creates a paginated vector source for a NSTA ArcGIS REST API resource.
 * Will populate a VectorSource with features from the given serviceUrl.
 * There is a 2000 feature limit on each API call, so the method will make multiple calls until all results are loaded.
 * @param serviceUrl The URL of the NSTA ArcGIS REST API resource.
 */
export function createPaginatedVectorSource(serviceUrl: string) {
  return new VectorSource({
    format: esriJson,
    async loader(this: VectorSource<Feature<Geometry>>, extent, resolution, projection) {
      const urlObj = new URL(serviceUrl);
      const format = this.getFormat();
      if (!format) {
        throw new Error("Vector source format is not configured");
      }

      const loadedFeatures: Array<Feature<Geometry>> = [];
      let offset = 0;
      let hasMore = true;

      try {
        while (hasMore) {
          urlObj.searchParams.set("resultOffset", String(offset));
          const response = await fetch(urlObj.toString());
          if (!response.ok) {
            throw new Error(`Failed to fetch: ${response.status}`);
          }
          const data = await response.json();
          const features = format.readFeatures(data, { featureProjection: projection });
          loadedFeatures.push(...features);
          offset += features.length;
          hasMore = data?.exceededTransferLimit === true;
        }

        return loadedFeatures;
      } catch (error) {
        console.error("Error loading vector source:", error);
        this.removeLoadedExtent(extent);
        throw error;
      }
    },
  });
}
