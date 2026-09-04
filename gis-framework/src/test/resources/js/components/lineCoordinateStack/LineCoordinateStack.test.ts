import type { SnapPoint } from "@/grid-utils";
import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import LineCoordinateStack from "@/components/lineCoordinateStack/LineCoordinateStack.vue";
import { SupportedWkid } from "@/coordinate-system-utils";

function renderCard(srsWkid: SupportedWkid, points: SnapPoint[] = []) {
  return render(LineCoordinateStack, {
    props: {
      points,
      srsWkid,
    },
  });
}

function makePoint(displayName: string, id = displayName): SnapPoint {
  return {
    id,
    displayName,
    coordinates: [0, 0],
    originalSrsCoordinates: [0, 0],
  };
}

describe("lineCoordinateStack", () => {
  it("shows the empty message and no remove action when there are no points", () => {
    renderCard(SupportedWkid.ED50_WKID, []);

    expect(screen.getByText("No points added yet")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /RemovePoint/ })).not.toBeInTheDocument();
  });

  it("makes only the most recently added point removable", () => {
    renderCard(SupportedWkid.ED50_WKID, [makePoint("1"), makePoint("2")]);

    expect(screen.getByRole("link", { name: "RemovePoint 2" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "RemovePoint 1" })).not.toBeInTheDocument();
  });

  it("emits undo-last-point when the remove action is clicked", async () => {
    const { emitted } = renderCard(SupportedWkid.ED50_WKID, [makePoint("1"), makePoint("2")]);

    await fireEvent.click(screen.getByRole("link", { name: "RemovePoint 2" }));

    expect(emitted()).toHaveProperty("undo-last-point");
  });
});
