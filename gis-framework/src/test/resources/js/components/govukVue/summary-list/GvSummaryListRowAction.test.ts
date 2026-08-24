import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";
import GvSummaryListRowAction from "../../../../../../main/resources/js/components/govukVue/summary-list/GvSummaryListRowAction.vue";

describe("gvSummaryListRowAction", () => {
  it("renders a link with the text prop and href", () => {
    render(GvSummaryListRowAction, {
      props: { text: "Change", href: "/change" },
    });

    expect(screen.getByRole("link", { name: "Change" })).toHaveAttribute("href", "/change");
  });

  it("prefers the default slot over the text prop", () => {
    render(GvSummaryListRowAction, {
      props: { text: "Ignored", href: "/change" },
      slots: { default: "Slotted change" },
    });

    expect(screen.getByRole("link", { name: "Slotted change" })).toBeInTheDocument();
    expect(screen.queryByText("Ignored")).not.toBeInTheDocument();
  });

  it("renders the visually hidden text alongside the link text", () => {
    render(GvSummaryListRowAction, {
      props: { text: "Change", href: "/change", visuallyHiddenText: "name" },
    });

    expect(screen.getByRole("link", { name: /Change/ })).toHaveTextContent("Change name");
  });

  it("falls back to a hash href and fires the click handler when there is a handler but no href", async () => {
    const onClick = vi.fn();

    render(GvSummaryListRowAction, {
      props: { text: "Remove" },
      attrs: { onClick },
    });

    const link = screen.getByRole("link", { name: "Remove" });
    expect(link).toHaveAttribute("href", "#");

    await fireEvent.click(link);

    expect(onClick).toHaveBeenCalledOnce();
  });
});
