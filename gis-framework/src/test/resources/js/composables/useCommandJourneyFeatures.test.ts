import type { App } from "vue";
import Feature from "ol/Feature";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApp, ref } from "vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";

const { getFeaturesMock } = vi.hoisted(() => ({
  getFeaturesMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getFeatures: getFeaturesMock,
}));

const featuresUrl = "/api/gis-framework/command-journey-features/journey-1";

function feature(featureId: string): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  return created;
}

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
  });

  it("loads the features from the given url on mount", async () => {
    const [{ features, hasError }, app] = withSetup(() => useCommandJourneyFeatures(featuresUrl, ref(0)));

    await flushPromises();

    expect(features.value.map(loaded => loaded.get("featureId"))).toEqual(["feature-1", "feature-2"]);
    expect(hasError.value).toBe(false);
    expect(getFeaturesMock).toHaveBeenCalledWith(featuresUrl);

    app.unmount();
  });

  it("flags an error and clears the features when the load fails", async () => {
    getFeaturesMock.mockReset().mockRejectedValue(new Error("network error"));

    const [{ features, hasError }, app] = withSetup(() => useCommandJourneyFeatures(featuresUrl, ref(0)));

    await flushPromises();

    expect(features.value).toEqual([]);
    expect(hasError.value).toBe(true);

    app.unmount();
  });

  it("reloads the features when the refresh counter changes", async () => {
    const refreshCounter = ref(0);
    const [, app] = withSetup(() => useCommandJourneyFeatures(featuresUrl, refreshCounter));

    await flushPromises();
    expect(getFeaturesMock).toHaveBeenCalledTimes(1);

    refreshCounter.value++;
    await flushPromises();

    expect(getFeaturesMock).toHaveBeenCalledTimes(2);

    app.unmount();
  });
});
