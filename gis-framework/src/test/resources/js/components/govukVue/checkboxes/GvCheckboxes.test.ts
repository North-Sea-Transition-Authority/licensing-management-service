import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvCheckboxes from "@/components/govukVue/checkboxes/GvCheckboxes.vue";

describe("gvCheckboxes", () => {
  it("renders the legend as an accessible group", () => {
    render(GvCheckboxes, { props: { legend: "Features" } });

    expect(screen.getByRole("group", { name: "Features" })).toBeInTheDocument();
  });

  it("renders the hint text", () => {
    render(GvCheckboxes, { props: { legend: "Features", hint: "Choose at least two" } });

    expect(screen.getByText("Choose at least two")).toBeInTheDocument();
  });

  it("shows the error message and adds the error modifier class when errorMessage is set", () => {
    const { container } = render(GvCheckboxes, {
      props: { legend: "Features", errorMessage: "Select at least two features" },
    });

    expect(screen.getByText(/Select at least two features/)).toBeInTheDocument();
    expect(container.querySelector(".govuk-form-group--error")).toBeInTheDocument();
  });

  it("does not add the error modifier class when there is no error", () => {
    const { container } = render(GvCheckboxes, { props: { legend: "Features" } });

    expect(container.querySelector(".govuk-form-group--error")).not.toBeInTheDocument();
  });

  it("adds the small modifier class when size is small", () => {
    const { container } = render(GvCheckboxes, { props: { legend: "Features", size: "small" } });

    expect(container.querySelector(".govuk-checkboxes--small")).toBeInTheDocument();
  });
});
