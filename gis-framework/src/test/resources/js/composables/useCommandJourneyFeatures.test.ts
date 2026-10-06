import type { App } from "vue";
import type { JsonFeatureOutlineNodes } from "@/api/features.api";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApp, ref } from "vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";

const { getFeaturesMock, getOutlineNodesMock } = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
  getOutlineNodesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getFeatures: getFeaturesMock,
  getOutlineNodes: getOutlineNodesMock,
}));

const featuresUrl = "/api/gis-framework/command-journey-features/journey-1";
const outlineNodesUrl = "/api/gis-framework/command-journey-outline-nodes/journey-1";

function feature(featureId: string): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  return created;
}

const loadedOutlineNodes: JsonFeatureOutlineNodes[] = [
  {
    featureId: "feature-1",
    nodes: [{ polygonId: "polygon-1", lineId: "line-1", ringNumber: 1, displayOrder: 1, x: 1, y: 2, mapText: "(1)" }],
  },
];

const flushPromises = () => new Promise(resolve => setTimeout(resolve));

function withSetup<T>(composable: () => T): [T, App] {
  let result!: T;
  const app = createApp({
    setup() {
      result = composable();
      return () => null;
    },
  });
  app.mount(document.createElement("div"));
  return [result, app];
}

describe("useCommandJourneyFeatures", () => {
  beforeEach(() => {
    getFeaturesMock.mockReset().mockResolvedValue([feature("feature-1"), feature("feature-2")]);
    getOutlineNodesMock.mockReset().mockResolvedValue(loadedOutlineNodes);
  });

  it("loads the features and outline nodes from the given urls on mount", async () => {
    const [{ features, outlineNodes, hasError }, app] = withSetup(
      () => useCommandJourneyFeatures(featuresUrl, outlineNodesUrl, ref(0)),
    );

    await flushPromises();

    expect(features.value.map(loaded => loaded.get("featureId"))).toEqual(["feature-1", "feature-2"]);
    expect(outlineNodes.value).toEqual(loadedOutlineNodes);
    expect(hasError.value).toBe(false);
    expect(getFeaturesMock).toHaveBeenCalledWith(featuresUrl);
    expect(getOutlineNodesMock).toHaveBeenCalledWith(outlineNodesUrl);

    app.unmount();
  });

  it.each([
    { failingRequest: "features", failingMock: getFeaturesMock },
    { failingRequest: "outline nodes", failingMock: getOutlineNodesMock },
  ])("flags an error and clears the features and outline nodes when the $failingRequest request fails", async ({ failingMock }) => {
    failingMock.mockReset().mockRejectedValue(new Error("network error"));

    const [{ features, outlineNodes, hasError }, app] = withSetup(
      () => useCommandJourneyFeatures(featuresUrl, outlineNodesUrl, ref(0)),
    );

    await flushPromises();

    expect(features.value).toEqual([]);
    expect(outlineNodes.value).toEqual([]);
    expect(hasError.value).toBe(true);

    app.unmount();
  });

  it("reloads the features and outline nodes when the refresh counter changes", async () => {
    const refreshCounter = ref(0);
    const [, app] = withSetup(() => useCommandJourneyFeatures(featuresUrl, outlineNodesUrl, refreshCounter));
    await flushPromises();

    refreshCounter.value++;
    await flushPromises();

    expect(getFeaturesMock).toHaveBeenCalledTimes(2);
    expect(getOutlineNodesMock).toHaveBeenCalledTimes(2);

    app.unmount();
  });
});
