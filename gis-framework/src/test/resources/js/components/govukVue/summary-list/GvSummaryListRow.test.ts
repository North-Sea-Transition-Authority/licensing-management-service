import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvSummaryListRow from "../../../../../../main/resources/js/components/govukVue/summary-list/GvSummaryListRow.vue";
import GvSummaryListRowAction from "../../../../../../main/resources/js/components/govukVue/summary-list/GvSummaryListRowAction.vue";

describe("gvSummaryListRow", () => {
  it("renders the key and value text props", () => {
    render(GvSummaryListRow, {
      props: { keyText: "Name", valueText: "Anne" },
    });

    expect(screen.getByText("Name")).toBeInTheDocument();
    expect(screen.getByText("Anne")).toBeInTheDocument();
  });

  it("prefers the key-text and value slots over the props", () => {
    render(GvSummaryListRow, {
      props: { keyText: "Ignored key", valueText: "Ignored value" },
      slots: { "key-text": "Slotted key", "value": "Slotted value" },
    });

    expect(screen.getByText("Slotted key")).toBeInTheDocument();
    expect(screen.getByText("Slotted value")).toBeInTheDocument();
    expect(screen.queryByText("Ignored key")).not.toBeInTheDocument();
    expect(screen.queryByText("Ignored value")).not.toBeInTheDocument();
  });

  it("does not render an actions cell when no actions slot is given", () => {
    const { container } = render(GvSummaryListRow, {
      props: { keyText: "Name", valueText: "Anne" },
    });

    expect(container.querySelector(".govuk-summary-list__actions")).not.toBeInTheDocument();
  });

  it("renders an action link provided in the actions slot", () => {
    render(GvSummaryListRow, {
      global: { components: { GvSummaryListRowAction } },
      props: { keyText: "Name", valueText: "Anne" },
      slots: {
        actions: `<gv-summary-list-row-action text="Change" href="/change" />`,
      },
    });

    expect(screen.getByRole("link", { name: /Change/ })).toBeInTheDocument();
  });
});
