import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvSummaryList from "@/components/govukVue/summary-list/GvSummaryList.vue";
import GvSummaryListRow from "@/components/govukVue/summary-list/GvSummaryListRow.vue";

const globalComponents = {
  global: { components: { GvSummaryListRow } },
};

describe("gvSummaryList", () => {
  it("renders as a summary list containing its rows", () => {
    const { container } = render(GvSummaryList, {
      ...globalComponents,
      slots: {
        default: `<gv-summary-list-row key-text="Name" value-text="Anne" />`,
      },
    });

    expect(container.querySelector("dl.govuk-summary-list")).toBeInTheDocument();
    expect(screen.getByText("Name")).toBeInTheDocument();
    expect(screen.getByText("Anne")).toBeInTheDocument();
  });

  it("adds the no-border modifier when showBorders is false", () => {
    const { container } = render(GvSummaryList, {
      props: { showBorders: false },
    });

    expect(container.querySelector(".govuk-summary-list--no-border")).toBeInTheDocument();
  });

  it("renders as a summary card with a level 2 heading when a card title is given", () => {
    render(GvSummaryList, { props: { cardTitle: "Personal details" } });

    expect(screen.getByRole("heading", { level: 2, name: "Personal details" })).toBeInTheDocument();
  });

  it("renders the card title at the requested heading level", () => {
    render(GvSummaryList, {
      props: { cardTitle: "Personal details", cardTitleHeadingLevel: 3 },
    });

    expect(screen.getByRole("heading", { level: 3, name: "Personal details" })).toBeInTheDocument();
  });

  it("prefers the card-title slot over the cardTitle prop", () => {
    render(GvSummaryList, {
      props: { cardTitle: "Ignored title" },
      slots: { "card-title": "Slotted title" },
    });

    expect(screen.getByRole("heading", { name: "Slotted title" })).toBeInTheDocument();
    expect(screen.queryByText("Ignored title")).not.toBeInTheDocument();
  });

  it("renders content provided in the card-actions slot", () => {
    render(GvSummaryList, {
      props: { cardTitle: "Personal details" },
      slots: { "card-actions": `<a href="/change">Change</a>` },
    });

    expect(screen.getByRole("link", { name: "Change" })).toBeInTheDocument();
  });
});
