import type { EventsKey } from "ol/events";
import type { Extent } from "ol/extent";
import type { Style } from "ol/style";
import type OlMap from "vue3-openlayers/map/OlMap";
import { render, screen, waitFor } from "@testing-library/vue";
import { unByKey } from "ol/Observable";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, h, onMounted } from "vue";
import FeatureLayer from "@/components/baseMap/FeatureLayer.vue";

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

vi.mock("ol/Observable", () => ({
  unByKey: vi.fn(),
}));

interface VectorLayerProps {
  style?: (feature: { get: (property: string) => string | undefined }) => Style,
  declutter?: boolean,
}

const vectorLayerProps: VectorLayerProps = {};

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

function createOlSourceVectorStub(extent: Extent) {
  return defineComponent({
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
            getExtent: () => extent,
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
}

function createOlMapStub(size: Extent | undefined) {
  const fit = vi.fn();
  const listenerKey = { key: "change:size" } as unknown as EventsKey;
  const on = vi.fn().mockReturnValue(listenerKey);
  let currentSize = size;

  const olMap = {
    map: {
      getSize: () => currentSize,
      getView: () => ({ fit }),
      on,
    },
  } as unknown as InstanceType<typeof OlMap>;

  return {
    olMap,
    fit,
    on,
    listenerKey,
    setSize: (newSize: Extent) => {
      currentSize = newSize;
    },
    triggerSizeChange: () => on.mock.calls[0][1](),
  };
}

function renderFeatureLayer(olMap: InstanceType<typeof OlMap>, extent: Extent) {
  return render(FeatureLayer, {
    props: {
      featuresUrl: "https://example.test/features",
      olMap,
      fillColor: [10, 20, 30],
      strokeColor: [40, 50, 60, 0.75],
    },
    global: {
      stubs: {
        "ol-vector-layer": OlVectorLayerStub,
        "ol-source-vector": createOlSourceVectorStub(extent),
      },
    },
  });
}

describe("featureLayer", () => {
  const sourceExtent = [1, 2, 3, 4];
  const expectedFitOptions = { padding: [50, 50, 50, 50] };

  beforeEach(() => {
    vi.mocked(unByKey).mockClear();
  });

  it("renders the feature vector source and fits the map when features load", async () => {
    const { olMap, fit } = createOlMapStub([1280, 1024]);

    renderFeatureLayer(olMap, sourceExtent);

    expect(screen.getByTestId("ol-vector-layer")).toBeInTheDocument();
    expect(screen.getByTestId("ol-source-vector")).toHaveAttribute(
      "data-url",
      "https://example.test/features",
    );
    expect(screen.getByTestId("ol-source-vector")).toHaveAttribute("data-format", "esri-json");
    expect(mocks.esriJsonConstructor).toHaveBeenCalled();
    expect(vectorLayerProps.declutter).toBe(true);

    const style = vectorLayerProps.style?.({
      get: property => property === "featureName" ? "Test feature" : undefined,
    });

    expect(style?.getStroke()?.getColor()).toEqual([40, 50, 60, 0.75]);
    expect(style?.getFill()?.getColor()).toEqual([10, 20, 30, 0.5]);
    expect(style?.getText()?.getText()).toBe("Test feature");
    expect(style?.getText()?.getFont()).toBe("18px \"GDS Transport\"");

    await waitFor(() => {
      expect(fit).toHaveBeenCalledWith(sourceExtent, expectedFitOptions);
    });
  });

  it("defers the fit until the map has a size", async () => {
    const { olMap, fit, on, setSize, triggerSizeChange } = createOlMapStub(undefined);

    renderFeatureLayer(olMap, sourceExtent);

    await waitFor(() => {
      expect(on).toHaveBeenCalledWith("change:size", expect.any(Function));
    });
    expect(fit).not.toHaveBeenCalled();

    setSize([1280, 1024]);
    triggerSizeChange();

    expect(fit).toHaveBeenCalledExactlyOnceWith(sourceExtent, expectedFitOptions);

    triggerSizeChange();

    expect(fit).toHaveBeenCalledOnce();
  });

  it("does not fit while the map size is still zero", async () => {
    const { olMap, fit, on, setSize, triggerSizeChange } = createOlMapStub([0, 0]);

    renderFeatureLayer(olMap, sourceExtent);

    await waitFor(() => {
      expect(on).toHaveBeenCalledWith("change:size", expect.any(Function));
    });

    triggerSizeChange();

    expect(fit).not.toHaveBeenCalled();

    setSize([1280, 1024]);
    triggerSizeChange();

    expect(fit).toHaveBeenCalledExactlyOnceWith(sourceExtent, expectedFitOptions);
  });

  it("does not fit when the loaded features have an empty extent", async () => {
    const { olMap, fit, on } = createOlMapStub(undefined);

    renderFeatureLayer(olMap, [Infinity, Infinity, -Infinity, -Infinity]);

    await waitFor(() => {
      expect(screen.getByTestId("ol-source-vector")).toBeInTheDocument();
    });

    expect(fit).not.toHaveBeenCalled();
    expect(on).not.toHaveBeenCalled();
  });

  it("removes the size listener when unmounted", async () => {
    const { olMap, on, listenerKey } = createOlMapStub(undefined);

    const { unmount } = renderFeatureLayer(olMap, sourceExtent);

    await waitFor(() => {
      expect(on).toHaveBeenCalledWith("change:size", expect.any(Function));
    });

    unmount();

    expect(unByKey).toHaveBeenCalledExactlyOnceWith(listenerKey);
  });
});
