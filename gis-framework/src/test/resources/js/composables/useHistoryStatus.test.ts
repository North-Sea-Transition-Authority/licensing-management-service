import type { App } from "vue";
import type { JsonHistoryStatus } from "@/api/history.api";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApp, ref } from "vue";
import { useHistoryStatus } from "@/composables/useHistoryStatus";

const { getHistoryStatusMock } = vi.hoisted(() => ({
  getHistoryStatusMock: vi.fn(),
}));

vi.mock("@/api/history.api", () => ({
  getHistoryStatus: getHistoryStatusMock,
}));

const loadedHistoryStatus: JsonHistoryStatus = {
  undoTransformationType: "SPLIT",
  redoTransformationType: "STRATA_SPLIT",
};

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

describe("useHistoryStatus", () => {
  beforeEach(() => {
    getHistoryStatusMock.mockReset().mockResolvedValue(loadedHistoryStatus);
  });

  it("loads the history status from the url built from the base url and command journey id on mount", async () => {
    const onError = vi.fn();

    const [{ historyStatus }, app] = withSetup(
      () => useHistoryStatus("/api/gis-framework", "journey-1", ref(0), { onError }),
    );
    await flushPromises();

    expect(historyStatus.value).toEqual(loadedHistoryStatus);
    expect(getHistoryStatusMock).toHaveBeenCalledWith("/api/gis-framework/history/journey-1");
    expect(onError).not.toHaveBeenCalled();

    app.unmount();
  });

  it("reloads the history status when the refresh counter changes", async () => {
    const refreshCounter = ref(0);
    const [, app] = withSetup(
      () => useHistoryStatus("/api/gis-framework", "journey-1", refreshCounter, { onError: vi.fn() }),
    );
    await flushPromises();

    refreshCounter.value++;
    await flushPromises();

    expect(getHistoryStatusMock).toHaveBeenCalledTimes(2);

    app.unmount();
  });

  it.each([
    { outcome: "succeeds", settle: (resolve: (status: JsonHistoryStatus) => void) => resolve(loadedHistoryStatus) },
    { outcome: "fails", settle: (_: unknown, reject: (error: Error) => void) => reject(new Error("network error")) },
  ])("flags loading until a reload $outcome", async ({ settle }) => {
    const refreshCounter = ref(0);
    const [{ isLoading }, app] = withSetup(
      () => useHistoryStatus("/api/gis-framework", "journey-1", refreshCounter, { onError: vi.fn() }),
    );
    await flushPromises();
    let resolveReload!: (status: JsonHistoryStatus) => void;
    let rejectReload!: (error: Error) => void;
    getHistoryStatusMock.mockReturnValue(new Promise((resolve, reject) => {
      resolveReload = resolve;
      rejectReload = reject;
    }));

    refreshCounter.value++;
    await flushPromises();
    const loadingDuringReload = isLoading.value;
    settle(resolveReload, rejectReload);
    await flushPromises();

    expect([loadingDuringReload, isLoading.value]).toEqual([true, false]);

    app.unmount();
  });

  it("clears the history status and calls onError on every failed request", async () => {
    getHistoryStatusMock.mockReset()
      .mockResolvedValueOnce(loadedHistoryStatus)
      .mockRejectedValue(new Error("network error"));
    const onError = vi.fn();
    const refreshCounter = ref(0);
    const [{ historyStatus }, app] = withSetup(
      () => useHistoryStatus("/api/gis-framework", "journey-1", refreshCounter, { onError }),
    );
    await flushPromises();

    refreshCounter.value++;
    await flushPromises();
    refreshCounter.value++;
    await flushPromises();

    expect(historyStatus.value).toBeNull();
    expect(onError).toHaveBeenCalledTimes(2);

    app.unmount();
  });
});
