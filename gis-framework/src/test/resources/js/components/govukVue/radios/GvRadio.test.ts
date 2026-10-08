import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvRadio from "@/components/govukVue/radios/GvRadio.vue";

describe("gvRadio", () => {
  it("renders a radio labelled by its label", () => {
    render(GvRadio, { props: { value: "SPLIT", label: "Split" } });

    expect(screen.getByRole("radio", { name: "Split" })).toHaveAttribute("value", "SPLIT");
  });

  it("uses the given id for the radio", () => {
    render(GvRadio, { props: { id: "first-radio", value: "SPLIT", label: "Split" } });

    expect(screen.getByRole("radio", { name: "Split" })).toHaveAttribute("id", "first-radio");
  });

  it("renders the hint text and describes the radio with it", () => {
    render(GvRadio, { props: { value: "SPLIT", label: "Split", hint: "Cut the shape in two" } });

    expect(screen.getByRole("radio", { name: "Split" })).toHaveAccessibleDescription("Cut the shape in two");
  });

  it("renders the divider text above the radio", () => {
    render(GvRadio, { props: { value: "SPLIT", label: "Split", divider: "or" } });

    expect(screen.getByText("or")).toBeInTheDocument();
  });

  it("disables the radio when disabled is set", () => {
    render(GvRadio, { props: { value: "SPLIT", label: "Split", disabled: true } });

    expect(screen.getByRole("radio", { name: "Split" })).toBeDisabled();
  });
});
