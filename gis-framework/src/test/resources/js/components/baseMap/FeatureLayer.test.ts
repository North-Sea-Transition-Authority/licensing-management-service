import type { Style } from "ol/style";
import type OlMap from "vue3-openlayers/map/OlMap";
import { render, screen, waitFor } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";
import { defineComponent, h, onMounted } from "vue";
import FeatureLayer from "../../../../../main/resources/js/components/baseMap/FeatureLayer.vue";

const mocks = vi.hoisted(() => {
  const esriJson = { format: "esri-json" };

  return {
    esriJson,
    esriJsonConstructor: vi.fn(class EsriJSON {
      constructor() {
        return esriJson;
      }
    }),
  };
});

vi.mock("ol/format", () => ({
  EsriJSON: mocks.esriJsonConstructor,
}));

interface VectorLayerProps {
  style?: (feature: { get: (property: string) => string | undefined }) => Style,
  declutter?: boolean,
}

describe("featureLayer", () => {
  it("renders the feature vector source and fits the map when features load", async () => {
    const vectorLayerProps: VectorLayerProps = {};
    const sourceExtent = [1, 2, 3, 4];
    const fit = vi.fn();

    const olMap = {
      map: {
        getView: () => ({
          fit,
        }),
      },
    } as unknown as InstanceType<typeof OlMap>;

    const OlVectorLayerStub = defineComponent({
      props: {
        style: {
          type: Function,
          required: false,
        },
        declutter: {
          type: Boolean,
          required: false,
        },
      },
      setup(props, { slots }) {
        vectorLayerProps.style = props.style as VectorLayerProps["style"];
        vectorLayerProps.declutter = props.declutter;

        return () => h("div", { "data-testid": "ol-vector-layer" }, slots.default?.());
      },
    });

    const OlSourceVectorStub = defineComponent({
      props: {
        url: {
          type: String,
          required: true,
        },
        format: {
          type: Object,
          required: true,
        },
      },
      emits: ["featuresloadend"],
      setup(props, { emit }) {
        onMounted(() => {
          emit("featuresloadend", {
            target: {
              getExtent: () => sourceExtent,
            },
          });
        });

        return () => h(
          "div",
          {
            "data-testid": "ol-source-vector",
            "data-url": props.url,
            "data-format": props.format === mocks.esriJson ? "esri-json" : "unknown",
          },
        );
      },
    });

    render(FeatureLayer, {
      props: {
        featuresUrl: "https://example.test/features",
        olMap,
        fillColor: [10, 20, 30],
        strokeColor: [40, 50, 60, 0.75],
      },
      global: {
        stubs: {
          "ol-vector-layer": OlVectorLayerStub,
          "ol-source-vector": OlSourceVectorStub,
        },
      },
    });

    expect(screen.getByTestId("ol-vector-layer")).toBeInTheDocument();
    expect(screen.getByTestId("ol-source-vector")).toHaveAttribute(
      "data-url",
      "https://example.test/features",
    );
    expect(screen.getByTestId("ol-source-vector")).toHaveAttribute("data-format", "esri-json");
    expect(mocks.esriJsonConstructor).toHaveBeenCalledOnce();
    expect(vectorLayerProps.declutter).toBe(true);

    const style = vectorLayerProps.style?.({
      get: property => property === "featureName" ? "Test feature" : undefined,
    });

    expect(style?.getStroke()?.getColor()).toEqual([40, 50, 60, 0.75]);
    expect(style?.getFill()?.getColor()).toEqual([10, 20, 30, 0.5]);
    expect(style?.getText()?.getText()).toBe("Test feature");
    expect(style?.getText()?.getFont()).toBe("18px \"GDS Transport\"");

    await waitFor(() => {
      expect(fit).toHaveBeenCalledWith(sourceExtent, {
        padding: [50, 50, 50, 50],
      });
    });
  });
});
