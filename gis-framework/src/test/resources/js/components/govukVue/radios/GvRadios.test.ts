import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvRadios from "@/components/govukVue/radios/GvRadios.vue";

describe("gvRadios", () => {
  it("renders the legend as an accessible group", () => {
    render(GvRadios, { props: { legend: "Change" } });

    expect(screen.getByRole("group", { name: "Change" })).toBeInTheDocument();
  });

  it("renders the hint text and describes the group with it", () => {
    render(GvRadios, { props: { legend: "Change", hint: "Choose one" } });

    expect(screen.getByRole("group", { name: "Change" })).toHaveAccessibleDescription("Choose one");
  });

  it("shows the error message and adds the error modifier class when errorMessage is set", () => {
    const { container } = render(GvRadios, { props: { legend: "Change", errorMessage: "Select a change" } });

    expect(screen.getByRole("group", { name: "Change" })).toHaveAccessibleDescription(/Select a change/);
    expect(container.querySelector(".govuk-form-group--error")).toBeInTheDocument();
  });

  it("does not add the error modifier class when there is no error", () => {
    const { container } = render(GvRadios, { props: { legend: "Change" } });

    expect(container.querySelector(".govuk-form-group--error")).not.toBeInTheDocument();
  });

  it.each([
    { props: { size: "small" }, expectedClass: "govuk-radios--small" },
    { props: { direction: "inline" }, expectedClass: "govuk-radios--inline" },
  ])("adds the $expectedClass modifier class", ({ props, expectedClass }) => {
    const { container } = render(GvRadios, { props: { legend: "Change", ...props } });

    expect(container.querySelector(`.${expectedClass}`)).toBeInTheDocument();
  });
});
