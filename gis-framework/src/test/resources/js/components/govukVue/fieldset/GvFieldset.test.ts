import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvFieldset from "@/components/govukVue/fieldset/GvFieldset.vue";

describe("gvFieldset", () => {
  it("renders the legend prop and the default slot content", () => {
    render(GvFieldset, {
      props: { legend: "Contact details" },
      slots: { default: "Fieldset body" },
    });

    expect(screen.getByRole("group", { name: "Contact details" })).toBeInTheDocument();
    expect(screen.getByText("Fieldset body")).toBeInTheDocument();
  });

  it("prefers the legend slot over the legend prop", () => {
    render(GvFieldset, {
      props: { legend: "Ignored legend" },
      slots: { legend: "Slotted legend" },
    });

    expect(screen.getByText("Slotted legend")).toBeInTheDocument();
    expect(screen.queryByText("Ignored legend")).not.toBeInTheDocument();
  });

  it("renders no legend when neither the prop nor the slot is provided", () => {
    const { container } = render(GvFieldset);

    expect(container.querySelector("legend")).not.toBeInTheDocument();
  });

  it("applies the legendClass to the legend", () => {
    const { container } = render(GvFieldset, {
      props: { legend: "Contact details", legendClass: "govuk-fieldset__legend--l" },
    });

    expect(container.querySelector("legend")).toHaveClass("govuk-fieldset__legend--l");
  });

  it("renders the legend as an h1 heading when legendIsPageHeading is set", () => {
    render(GvFieldset, { props: { legend: "Contact details", legendIsPageHeading: true } });

    expect(screen.getByRole("heading", { level: 1, name: "Contact details" })).toBeInTheDocument();
  });

  it("adds the role and aria-describedby attributes to the fieldset", () => {
    render(GvFieldset, {
      props: { legend: "Contact details", role: "radiogroup", describedBy: "contact-hint" },
    });

    const fieldset = screen.getByRole("radiogroup", { name: "Contact details" });
    expect(fieldset).toHaveAttribute("aria-describedby", "contact-hint");
  });
});
