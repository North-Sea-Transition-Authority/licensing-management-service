import { fireEvent, render, screen, within } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import CoordinateDmsInput from "../../../../../main/resources/js/components/coordinateInput/CoordinateDmsInput.vue";

function renderInput(secondsPrecision?: number, latitude: number = 0, longitude: number = 0) {
  const latEmits: number[] = [];
  const lonEmits: number[] = [];
  const { container } = render(CoordinateDmsInput, {
    props: {
      "index": 0,
      longitude,
      latitude,
      secondsPrecision,
      "onUpdate:latitude": (value: number) => latEmits.push(value),
      "onUpdate:longitude": (value: number) => lonEmits.push(value),
    },
  });

  const latGroup = () => screen.getByRole("group", { name: "Latitude" });
  const lonGroup = () => screen.getByRole("group", { name: "Longitude" });
  const latField = (label: string) => within(latGroup()).getByLabelText<HTMLInputElement>(label);
  const lonField = (label: string) => within(lonGroup()).getByLabelText<HTMLInputElement>(label);
  const errorText = (id: string) => container.querySelector(`#${id}`)?.textContent?.trim() ?? "";

  async function leaveLatGroup(): Promise<void> {
    await fireEvent.focusOut(latGroup(), { relatedTarget: null });
  }

  async function leaveLonGroup(): Promise<void> {
    await fireEvent.focusOut(lonGroup(), { relatedTarget: null });
  }

  return { container, latEmits, lonEmits, latGroup, latField, lonField, errorText, leaveLatGroup, leaveLonGroup };
}

describe("coordinateDmsInput", () => {
  it("emits decimal degrees for a valid latitude", async () => {
    const { latEmits, latField } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await fireEvent.update(latField("Minutes"), "49");
    await fireEvent.update(latField("Seconds"), "30");

    expect(latEmits.at(-1)).toBeCloseTo(53.825, 5);
  });

  it("starts with blank fields even when finite coordinates are supplied", () => {
    const { latField, lonField } = renderInput(undefined, 53.825, 1.5);

    expect(latField("Degrees").value).toBe("");
    expect(latField("Minutes").value).toBe("");
    expect(latField("Seconds").value).toBe("");
    expect(lonField("Degrees").value).toBe("");
    expect(lonField("Minutes").value).toBe("");
    expect(lonField("Seconds").value).toBe("");
  });

  it("shows an error when seconds exceed the configured precision", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput(3);

    await fireEvent.update(latField("Degrees"), "0");
    await fireEvent.update(latField("Minutes"), "0");
    await fireEvent.update(latField("Seconds"), "30.1234");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("decimal place");
  });

  it("shows an error when latitude degrees are out of range", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "91");
    await fireEvent.update(latField("Minutes"), "0");
    await fireEvent.update(latField("Seconds"), "0");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("between 0 and 90");
  });

  it("shows an error when longitude degrees are out of range", async () => {
    const { lonField, errorText, leaveLonGroup } = renderInput();

    await fireEvent.update(lonField("Degrees"), "181");
    await fireEvent.update(lonField("Minutes"), "0");
    await fireEvent.update(lonField("Seconds"), "0");
    await leaveLonGroup();

    expect(errorText("lon-error-0")).toContain("between 0 and 180");
  });

  it("shows an error when latitude degrees is not a whole number", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "53.5");
    await fireEvent.update(latField("Minutes"), "0");
    await fireEvent.update(latField("Seconds"), "0");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("Degrees must be a whole number");
  });

  it("shows an error when latitude minutes is not a whole number", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await fireEvent.update(latField("Minutes"), "30.5");
    await fireEvent.update(latField("Seconds"), "0");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("Minutes must be a whole number");
  });

  it("shows an error when latitude minutes are out of range", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await fireEvent.update(latField("Minutes"), "60");
    await fireEvent.update(latField("Seconds"), "0");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("Minutes must be between 0 and 59");
  });

  it("shows an error when latitude seconds are out of range", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await fireEvent.update(latField("Minutes"), "0");
    await fireEvent.update(latField("Seconds"), "60");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("Seconds must be between 0 and 59");
  });

  it("shows no error while a partly entered latitude is still being typed", async () => {
    const { latField, errorText } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");

    expect(errorText("lat-error-0")).toBe("");
  });

  it("shows no error when focus moves between fields of the same group", async () => {
    const { latField, errorText } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await fireEvent.focusOut(latField("Degrees"), { relatedTarget: latField("Minutes") });

    expect(errorText("lat-error-0")).toBe("");
  });

  it("shows an error when focus leaves the group with the entry incomplete", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await leaveLatGroup();

    expect(errorText("lat-error-0")).toContain("Enter degrees, minutes and seconds");
  });

  it("clears a displayed error as soon as the entry is corrected", async () => {
    const { latField, errorText, leaveLatGroup } = renderInput();

    await fireEvent.update(latField("Degrees"), "91");
    await fireEvent.update(latField("Minutes"), "0");
    await fireEvent.update(latField("Seconds"), "0");
    await leaveLatGroup();
    expect(errorText("lat-error-0")).not.toBe("");

    await fireEvent.update(latField("Degrees"), "53");

    expect(errorText("lat-error-0")).toBe("");
  });

  it("emits NaN while the entry is invalid", async () => {
    const { latEmits, latField } = renderInput();

    await fireEvent.update(latField("Degrees"), "91");

    expect(latEmits.at(-1)).toBeNaN();
  });

  it("emits NaN when a previously valid entry is cleared", async () => {
    const { latEmits, latField } = renderInput();

    await fireEvent.update(latField("Degrees"), "53");
    await fireEvent.update(latField("Minutes"), "49");
    await fireEvent.update(latField("Seconds"), "30");
    await fireEvent.update(latField("Degrees"), "");

    expect(latEmits.at(-1)).toBeNaN();
  });
});
