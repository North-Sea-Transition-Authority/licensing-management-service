import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import CoordinateGridInput from "../../../../../main/resources/js/components/coordinateInput/CoordinateGridInput.vue";
import { SupportedWkid } from "../../../../../main/resources/js/coordinate-system-utils";

// TQ 389 773 (100 m resolution) parses to these BNG metres (SW corner of the 100 m square).
const TQ_389_773_EASTING = 538900;
const TQ_389_773_NORTHING = 177300;

function renderInput(overrides: { longitude?: number, latitude?: number, maxFiguresPerAxis?: number } = {}) {
  const coordEmits: [number, number][] = [];
  const { container } = render(CoordinateGridInput, {
    props: {
      "index": 0,
      "srsWkid": SupportedWkid.BNG_WKID,
      "longitude": overrides.longitude ?? Number.NaN,
      "latitude": overrides.latitude ?? Number.NaN,
      "maxFiguresPerAxis": overrides.maxFiguresPerAxis,
      "onUpdate:coordinates": (value: [number, number]) => coordEmits.push(value),
    },
  });

  const input = () => screen.getByLabelText<HTMLInputElement>("Grid reference");
  const errorText = () => container.querySelector("#grid-error-0")?.textContent?.trim() ?? "";

  async function enter(gridReference: string): Promise<void> {
    await fireEvent.update(input(), gridReference);
  }

  async function blur(): Promise<void> {
    await fireEvent.blur(input());
  }

  return { container, coordEmits, input, errorText, enter, blur };
}

describe("coordinateGridInput", () => {
  it("emits easting and northing metres for a valid spaced grid reference", async () => {
    const { coordEmits, enter, errorText } = renderInput();

    await enter("TQ 389 773");

    expect(errorText()).toBe("");
    expect(coordEmits.at(-1)).toEqual([TQ_389_773_EASTING, TQ_389_773_NORTHING]);
  });

  it("accepts a compact grid reference with no spaces", async () => {
    const { coordEmits, enter, errorText } = renderInput();

    await enter("TQ389773");

    expect(errorText()).toBe("");
    expect(coordEmits.at(-1)).toEqual([TQ_389_773_EASTING, TQ_389_773_NORTHING]);
  });

  it("accepts a lower-case grid reference", async () => {
    const { coordEmits, enter } = renderInput();

    await enter("tq389773");

    expect(coordEmits.at(-1)).toEqual([TQ_389_773_EASTING, TQ_389_773_NORTHING]);
  });

  it("accepts irregular whitespace", async () => {
    const { coordEmits, enter } = renderInput();

    await enter("  TQ   389    773 ");

    expect(coordEmits.at(-1)).toEqual([TQ_389_773_EASTING, TQ_389_773_NORTHING]);
  });

  it("normalises the entered value to XX 999 999 format on blur", async () => {
    const { input, enter, blur } = renderInput();

    await enter("tq389773");
    await blur();

    expect(input().value).toBe("TQ 389 773");
  });

  it("clears the coordinates without showing an error while an incomplete reference is typed", async () => {
    const { coordEmits, enter, errorText } = renderInput();

    await enter("TQ 3");

    expect(errorText()).toBe("");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("shows an error on blur when the figures are unequal length", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput();

    await enter("TQ 38 773");
    await blur();

    expect(errorText()).toContain("same number of figures");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("shows an error on blur when figures exceed the configured maximum per axis", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput({ maxFiguresPerAxis: 3 });

    await enter("TQ 3890 7730");
    await blur();

    expect(errorText()).toContain("figure");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("shows an error on blur for an invalid grid square", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput();

    await enter("ZZ 389 773");
    await blur();

    expect(errorText()).toContain("valid OS grid reference");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("shows an error on blur when the square is not two letters", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput();

    await enter("T389773");
    await blur();

    expect(errorText()).toContain("two letters");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("shows an error on blur when the body is not numbers only", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput();

    await enter("TQ 38A 773");
    await blur();

    expect(errorText()).toContain("numbers only");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("clears the coordinates when a previously valid reference is emptied", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput();

    await enter("TQ 389 773");
    await enter("");
    await blur();

    expect(errorText()).toContain("Enter a grid reference");
    expect(coordEmits.at(-1)).toEqual([Number.NaN, Number.NaN]);
  });

  it("clears a displayed error as soon as the reference is corrected", async () => {
    const { coordEmits, enter, blur, errorText } = renderInput();

    await enter("TQ 38 773");
    await blur();
    expect(errorText()).not.toBe("");

    await enter("TQ 389 773");

    expect(errorText()).toBe("");
    expect(coordEmits.at(-1)).toEqual([TQ_389_773_EASTING, TQ_389_773_NORTHING]);
  });

  it("starts with an empty box even when finite easting/northing metres are supplied", () => {
    const { input } = renderInput({
      longitude: TQ_389_773_EASTING,
      latitude: TQ_389_773_NORTHING,
      maxFiguresPerAxis: 3,
    });

    expect(input().value).toBe("");
  });
});
