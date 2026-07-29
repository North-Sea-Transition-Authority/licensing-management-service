import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import PointCard from "../../../../../main/resources/js/components/coordinateInput/PointCard.vue";
import { SupportedWkid } from "../../../../../main/resources/js/coordinate-system-utils";

function renderCard(srsWkid: SupportedWkid, index = 0) {
  return render(PointCard, {
    props: {
      index,
      srsWkid,
      longitudeOriginalSrs: 0,
      latitudeOriginalSrs: 0,
    },
  });
}

describe("pointCard", () => {
  it("shows the DMS input for offshore systems (ED50)", () => {
    renderCard(SupportedWkid.ED50_WKID);

    expect(screen.getByRole("group", { name: "Latitude" })).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Longitude" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Grid reference")).not.toBeInTheDocument();
  });

  it("shows the DMS input for offshore systems (ETRS89)", () => {
    renderCard(SupportedWkid.ETRS89_WKID);

    expect(screen.getByRole("group", { name: "Latitude" })).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Longitude" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Grid reference")).not.toBeInTheDocument();
  });

  it("shows the grid input for onshore systems (BNG)", () => {
    renderCard(SupportedWkid.BNG_WKID);

    expect(screen.getByLabelText("Grid reference")).toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Latitude" })).not.toBeInTheDocument();
  });

  it("shows Add before, Add after and Clear on the first card", () => {
    renderCard(SupportedWkid.BNG_WKID, 0);

    expect(screen.getByRole("heading", { name: "Point 1" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add before" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add after" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Clear" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Remove" })).not.toBeInTheDocument();
  });

  it("hides Add before and replaces Clear with Remove on subsequent cards", () => {
    renderCard(SupportedWkid.BNG_WKID, 1);

    expect(screen.getByRole("heading", { name: "Point 2" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Add before" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Clear" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add after" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove" })).toBeInTheDocument();
  });

  it("emits clear when the first card's Clear button is clicked", async () => {
    const { emitted } = renderCard(SupportedWkid.BNG_WKID, 0);

    await fireEvent.click(screen.getByRole("button", { name: "Clear" }));

    expect(emitted()).toHaveProperty("clear");
    expect(emitted()).not.toHaveProperty("remove");
  });

  it("emits remove when a subsequent card's Remove button is clicked", async () => {
    const { emitted } = renderCard(SupportedWkid.BNG_WKID, 1);

    await fireEvent.click(screen.getByRole("button", { name: "Remove" }));

    expect(emitted()).toHaveProperty("remove");
    expect(emitted()).not.toHaveProperty("clear");
  });

  it("emits add-before and add-after when those buttons are clicked", async () => {
    const { emitted } = renderCard(SupportedWkid.BNG_WKID, 0);

    await fireEvent.click(screen.getByRole("button", { name: "Add before" }));
    await fireEvent.click(screen.getByRole("button", { name: "Add after" }));

    expect(emitted()).toHaveProperty("add-before");
    expect(emitted()).toHaveProperty("add-after");
  });
});
