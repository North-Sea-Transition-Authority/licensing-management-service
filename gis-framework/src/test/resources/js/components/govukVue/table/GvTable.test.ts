import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvTable from "@/components/govukVue/table/GvTable.vue";

describe("gvTable", () => {
  it("renders as a table containing its slotted content", () => {
    render(GvTable, {
      slots: { default: `<tbody><tr><td>Cell content</td></tr></tbody>` },
    });

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Cell content" })).toBeInTheDocument();
  });

  it("renders no caption when neither the caption prop nor slot is given", () => {
    const { container } = render(GvTable);

    expect(container.querySelector("caption")).not.toBeInTheDocument();
  });

  it("renders the caption prop text when given", () => {
    const { container } = render(GvTable, { props: { caption: "Monthly totals" } });

    expect(container.querySelector("caption")).toHaveTextContent("Monthly totals");
  });

  it("prefers the caption slot over the caption prop", () => {
    render(GvTable, {
      props: { caption: "Ignored caption" },
      slots: { caption: "Slotted caption" },
    });

    expect(screen.getByText("Slotted caption")).toBeInTheDocument();
    expect(screen.queryByText("Ignored caption")).not.toBeInTheDocument();
  });

  it("applies the captionClass to the caption", () => {
    const { container } = render(GvTable, {
      props: { caption: "Monthly totals", captionClass: "govuk-table__caption--l" },
    });

    expect(container.querySelector("caption")).toHaveClass("govuk-table__caption--l");
  });
});
