import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvLabel from "@/components/govukVue/label/GvLabel.vue";

describe("gvLabel", () => {
  it("renders the text prop and links the label to forId", () => {
    render(GvLabel, { props: { text: "Full name", forId: "name-input" } });

    const label = screen.getByText("Full name");
    expect(label).toHaveClass("govuk-label");
    expect(label).toHaveAttribute("for", "name-input");
  });

  it("prefers the default slot over the text prop", () => {
    render(GvLabel, {
      props: { text: "Ignored text", forId: "name-input" },
      slots: { default: "Slotted label" },
    });

    expect(screen.getByText("Slotted label")).toBeInTheDocument();
    expect(screen.queryByText("Ignored text")).not.toBeInTheDocument();
  });

  it("does not wrap the label when it is not the page heading", () => {
    const { container } = render(GvLabel, { props: { text: "Full name", forId: "name-input" } });

    expect(container.querySelector("h1")).not.toBeInTheDocument();
    expect(container.querySelector(".govuk-label-wrapper")).not.toBeInTheDocument();
  });

  it("wraps the label in an h1 with the wrapper class when it is the page heading", () => {
    const { container } = render(GvLabel, {
      props: { text: "Full name", forId: "name-input", isPageHeading: true },
    });

    const heading = container.querySelector("h1.govuk-label-wrapper");
    expect(heading).toBeInTheDocument();
    expect(heading?.querySelector("label.govuk-label")).toBeInTheDocument();
  });

  it("passes through fallthrough attributes to the label", () => {
    render(GvLabel, { props: { text: "Full name", forId: "name-input" }, attrs: { id: "name-label" } });

    expect(screen.getByText("Full name")).toHaveAttribute("id", "name-label");
  });
});
