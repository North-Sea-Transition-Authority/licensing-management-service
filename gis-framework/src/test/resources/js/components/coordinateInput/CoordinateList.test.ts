import type { EditablePoint } from "@/components/coordinateInput/CoordinateList.vue";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import CoordinateList from "@/components/coordinateInput/CoordinateList.vue";
import { SupportedWkid } from "@/coordinate-system-utils";

function renderList(initial: EditablePoint[], srsWkid: SupportedWkid = SupportedWkid.BNG_WKID) {
  let model = initial;
  let rerender: (props: Record<string, unknown>) => Promise<void>;
  const result = render(CoordinateList, {
    props: {
      "modelValue": model,
      srsWkid,
      "onUpdate:modelValue": (value: EditablePoint[]) => {
        model = value;
        rerender({ modelValue: value, srsWkid });
      },
    },
  });
  rerender = result.rerender;
  return { ...result, getModel: () => model };
}

function point(id: number): EditablePoint {
  return { id, originalSrsCoordinates: [0, 0], coordinates: undefined };
}

function blankPoint(id: number): EditablePoint {
  return { id, originalSrsCoordinates: [Number.NaN, Number.NaN], coordinates: undefined };
}

function cardHeadings(): string[] {
  return screen.getAllByRole("heading", { level: 3 }).map(heading => heading.textContent!.trim());
}

describe("coordinateList", () => {
  describe("rendering", () => {
    it("renders one card per point", () => {
      renderList([point(100), point(101)]);

      expect(cardHeadings()).toEqual(["Point 1", "Point 2"]);
    });
  });

  describe("adding a point", () => {
    it("adds a point after the given index", async () => {
      const { getModel } = renderList([point(100)]);

      await fireEvent.click(screen.getByRole("button", { name: "Add after" }));

      expect(getModel()).toEqual([point(100), blankPoint(101)]);
      await waitFor(() => expect(cardHeadings()).toEqual(["Point 1", "Point 2"]));
    });

    it("adds a point before the given index", async () => {
      const { getModel } = renderList([point(100)]);

      await fireEvent.click(screen.getByRole("button", { name: "Add before" }));

      expect(getModel()).toEqual([blankPoint(101), point(100)]);
      await waitFor(() => expect(cardHeadings()).toEqual(["Point 1", "Point 2"]));
    });

    it("keeps an existing input's value and shows a blank input for an added point", async () => {
      renderList([point(100)]);

      await fireEvent.update(screen.getByLabelText<HTMLInputElement>("Grid reference"), "TQ 389 773");
      await fireEvent.click(screen.getByRole("button", { name: "Add after" }));

      await waitFor(() => expect(cardHeadings()).toEqual(["Point 1", "Point 2"]));
      const gridValues = screen.getAllByLabelText<HTMLInputElement>("Grid reference").map(input => input.value);
      expect(gridValues).toEqual(["TQ 389 773", ""]);
    });

    it("gives an added point an id distinct from the parent-seeded ids", async () => {
      const { getModel } = renderList([point(0)]);

      await fireEvent.click(screen.getByRole("button", { name: "Add after" }));

      expect(getModel()).toEqual([point(0), blankPoint(1)]);
    });
  });

  describe("removing a point", () => {
    it("removes a point when more than one remains", async () => {
      const { getModel } = renderList([point(100), point(101)]);

      await fireEvent.click(screen.getByRole("button", { name: "Remove" }));

      expect(getModel()).toEqual([point(100)]);
      await waitFor(() => expect(cardHeadings()).toEqual(["Point 1"]));
    });

    it("offers to clear rather than remove the only point", () => {
      renderList([point(100)]);

      expect(screen.queryByRole("button", { name: "Remove" })).toBeNull();
      expect(screen.getByRole("button", { name: "Clear" })).toBeVisible();
    });
  });

  describe("clearing a point", () => {
    it("clears a point's coordinates and empties its input", async () => {
      const { getModel } = renderList([point(100)]);

      await fireEvent.update(screen.getByLabelText<HTMLInputElement>("Grid reference"), "TQ 389 773");
      expect(screen.getByLabelText<HTMLInputElement>("Grid reference").value).toBe("TQ 389 773");

      await fireEvent.click(screen.getByRole("button", { name: "Clear" }));

      expect(getModel()).toEqual([blankPoint(101)]);
      await waitFor(() => expect(screen.getByLabelText<HTMLInputElement>("Grid reference").value).toBe(""));
    });
  });

  describe("editing coordinates", () => {
    // Both offshore systems enter 1°30′0″E / 52°0′0″N as lon/lat degrees; the derived WGS84 differs
    // per datum: ETRS89 is a near-identity transform, ED50 applies a datum shift.
    it.each([
      { srs: "ETRS89", wkid: SupportedWkid.ETRS89_WKID, expectedWgs84: [1.5, 52] },
      { srs: "ED50", wkid: SupportedWkid.ED50_WKID, expectedWgs84: [1.4986067, 51.9991672] },
    ])("recomputes the WGS84 coordinates when an axis is edited ($srs)", async ({ wkid, expectedWgs84 }) => {
      const { getModel } = renderList([point(100)], wkid);

      const longitude = within(screen.getByRole("group", { name: "Longitude" }));
      await fireEvent.update(longitude.getByLabelText("Degrees"), "1");
      await fireEvent.update(longitude.getByLabelText("Minutes"), "30");
      await fireEvent.update(longitude.getByLabelText("Seconds"), "0");

      const latitude = within(screen.getByRole("group", { name: "Latitude" }));
      await fireEvent.update(latitude.getByLabelText("Degrees"), "52");
      await fireEvent.update(latitude.getByLabelText("Minutes"), "0");
      await fireEvent.update(latitude.getByLabelText("Seconds"), "0");

      const updated = getModel()[0];
      expect(updated.originalSrsCoordinates).toEqual([1.5, 52]);
      expect(updated.coordinates![0]).toBeCloseTo(expectedWgs84[0], 4);
      expect(updated.coordinates![1]).toBeCloseTo(expectedWgs84[1], 4);
    });

    it("maps a typed grid reference to the correct WGS84 point", async () => {
      const { getModel } = renderList([point(100)]);

      await fireEvent.update(screen.getByLabelText<HTMLInputElement>("Grid reference"), "TQ 389 773");

      const updated = getModel()[0];
      expect(updated.originalSrsCoordinates).toEqual([538900, 177300]);
      expect(updated.coordinates![0]).toBeCloseTo(0, 1);
      expect(updated.coordinates![1]).toBeCloseTo(51.5, 1);
    });
  });
});
