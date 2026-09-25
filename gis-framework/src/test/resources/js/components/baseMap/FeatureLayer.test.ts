import type { EventsKey } from "ol/events";
import type { Extent } from "ol/extent";
import type { Style } from "ol/style";
import type { Mock } from "vitest";
import type OlMap from "vue3-openlayers/map/OlMap";
import { render } from "@testing-library/vue";
import Feature from "ol/Feature";
import { unByKey } from "ol/Observable";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, h } from "vue";
import FeatureLayer from "@/components/baseMap/FeatureLayer.vue";

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
    style: { type: Function, required: false },
    declutter: { type: Boolean, required: false },
  },
  setup(props, { slots }) {
    vectorLayerProps.style = props.style as VectorLayerProps["style"];
    vectorLayerProps.declutter = props.declutter;
    return () => h("div", { "data-testid": "ol-vector-layer" }, slots.default?.());
  },
});

interface SourceMock {
  clear: Mock,
  addFeatures: Mock,
  changed: Mock,
  getExtent: Mock,
}

let sourceMock: SourceMock;

function createOlSourceVectorStub(extent: Extent) {
  sourceMock = {
    clear: vi.fn(),
    addFeatures: vi.fn(),
    changed: vi.fn(),
    getExtent: vi.fn(() => extent),
  };

  return defineComponent({
    setup(_, { expose }) {
      expose({ source: sourceMock });
      return () => h("div", { "data-testid": "ol-source-vector" });
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

function renderFeatureLayer(olMap: InstanceType<typeof OlMap>, extent: Extent, selectedFeatureIds: string[] = []) {
  return render(FeatureLayer, {
    props: {
      features: [],
      olMap,
      fillColor: [10, 20, 30],
      strokeColor: [40, 50, 60, 0.75],
      selectedFeatureIds,
    },
    global: {
      stubs: {
        "ol-vector-layer": OlVectorLayerStub,
        "ol-source-vector": createOlSourceVectorStub(extent),
      },
    },
  });
}

function feature(featureId: string, featureName: string): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("featureName", featureName);
  return created;
}

describe("featureLayer", () => {
  const sourceExtent = [1, 2, 3, 4];
  const expectedFitOptions = { padding: [50, 50, 50, 50] };

  beforeEach(() => {
    vi.mocked(unByKey).mockClear();
  });

  it("populates the source and fits the map when the features change", async () => {
    const { olMap, fit } = createOlMapStub([1280, 1024]);
    const blockA = feature("feature-1", "Block A");

    const { rerender } = renderFeatureLayer(olMap, sourceExtent);
    await rerender({ features: [blockA] });

    expect(sourceMock.clear).toHaveBeenCalled();
    expect(sourceMock.addFeatures).toHaveBeenCalledWith([blockA]);
    expect(vectorLayerProps.declutter).toBe(true);
    expect(fit).toHaveBeenCalledWith(sourceExtent, expectedFitOptions);
  });

  it("styles an unselected feature with the default colours and its name", async () => {
    const { olMap } = createOlMapStub([1280, 1024]);

    renderFeatureLayer(olMap, sourceExtent);

    const style = vectorLayerProps.style?.({
      get: property => property === "featureName" ? "Block A" : undefined,
    });

    expect(style?.getStroke()?.getColor()).toEqual([40, 50, 60, 0.75]);
    expect(style?.getStroke()?.getWidth()).toBe(2);
    expect(style?.getFill()?.getColor()).toEqual([10, 20, 30, 0.5]);
    expect(style?.getText()?.getText()).toBe("Block A");
  });

  it("styles a selected feature with the highlight colours", async () => {
    const { olMap } = createOlMapStub([1280, 1024]);

    renderFeatureLayer(olMap, sourceExtent, ["feature-1"]);

    const style = vectorLayerProps.style?.({
      get: property => property === "featureId" ? "feature-1" : undefined,
    });

    expect(style?.getStroke()?.getColor()).toEqual([212, 53, 28, 1]);
    expect(style?.getStroke()?.getWidth()).toBe(4);
    expect(style?.getFill()?.getColor()).toEqual([212, 53, 28, 0.5]);
  });

  it("redraws the source when the selected feature ids change", async () => {
    const { olMap } = createOlMapStub([1280, 1024]);

    const { rerender } = renderFeatureLayer(olMap, sourceExtent);
    await rerender({ selectedFeatureIds: ["feature-1"] });

    expect(sourceMock.changed).toHaveBeenCalled();
  });

  it("defers the fit until the map has a size", async () => {
    const { olMap, fit, on, setSize, triggerSizeChange } = createOlMapStub(undefined);

    const { rerender } = renderFeatureLayer(olMap, sourceExtent);
    await rerender({ features: [feature("feature-1", "Block A")] });

    expect(on).toHaveBeenCalledWith("change:size", expect.any(Function));
    expect(fit).not.toHaveBeenCalled();

    setSize([1280, 1024]);
    triggerSizeChange();

    expect(fit).toHaveBeenCalledExactlyOnceWith(sourceExtent, expectedFitOptions);

    triggerSizeChange();

    expect(fit).toHaveBeenCalledOnce();
  });

  it("does not fit while the map size is still zero", async () => {
    const { olMap, fit, on, setSize, triggerSizeChange } = createOlMapStub([0, 0]);

    const { rerender } = renderFeatureLayer(olMap, sourceExtent);
    await rerender({ features: [feature("feature-1", "Block A")] });

    expect(on).toHaveBeenCalledWith("change:size", expect.any(Function));

    triggerSizeChange();

    expect(fit).not.toHaveBeenCalled();

    setSize([1280, 1024]);
    triggerSizeChange();

    expect(fit).toHaveBeenCalledExactlyOnceWith(sourceExtent, expectedFitOptions);
  });

  it("does not fit when the features have an empty extent", async () => {
    const { olMap, fit, on } = createOlMapStub(undefined);

    const { rerender } = renderFeatureLayer(olMap, [Infinity, Infinity, -Infinity, -Infinity]);
    await rerender({ features: [feature("feature-1", "Block A")] });

    expect(fit).not.toHaveBeenCalled();
    expect(on).not.toHaveBeenCalled();
  });

  it("removes the size listener when unmounted", async () => {
    const { olMap, on, listenerKey } = createOlMapStub(undefined);

    const { rerender, unmount } = renderFeatureLayer(olMap, sourceExtent);
    await rerender({ features: [feature("feature-1", "Block A")] });

    expect(on).toHaveBeenCalledWith("change:size", expect.any(Function));

    unmount();

    expect(unByKey).toHaveBeenCalledExactlyOnceWith(listenerKey);
  });
});
