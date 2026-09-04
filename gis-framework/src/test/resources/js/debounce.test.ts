import { afterEach, describe, expect, it, vi } from "vitest";
import { debounce } from "@/debounce";

describe("debounce", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("waits before calling the debounced function", () => {
    vi.useFakeTimers();
    const callback = vi.fn();
    const debouncedCallback = debounce(callback, 100);

    debouncedCallback("value");

    expect(callback).not.toHaveBeenCalled();

    vi.advanceTimersByTime(99);

    expect(callback).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);

    expect(callback).toHaveBeenCalledOnce();
    expect(callback).toHaveBeenCalledWith("value");
  });

  it("calls the debounced function once with the latest arguments", () => {
    vi.useFakeTimers();
    const callback = vi.fn();
    const debouncedCallback = debounce(callback, 100);

    debouncedCallback("first", 1);
    vi.advanceTimersByTime(50);
    debouncedCallback("second", 2);
    vi.advanceTimersByTime(50);

    expect(callback).not.toHaveBeenCalled();

    vi.advanceTimersByTime(50);

    expect(callback).toHaveBeenCalledOnce();
    expect(callback).toHaveBeenCalledWith("second", 2);
  });
});
