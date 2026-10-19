import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import { ref } from "vue";
import GvInput from "@/components/govukVue/input/GvInput.vue";

describe("gvInput", () => {
  it("renders a text input labelled by its label", () => {
    render(GvInput, { props: { label: "Depth" } });

    expect(screen.getByRole("textbox", { name: "Depth" })).toHaveAttribute("type", "text");
  });

  it("emits update:modelValue when typed into", async () => {
    const { emitted } = render(GvInput, { props: { label: "Depth" } });

    await fireEvent.update(screen.getByRole("textbox", { name: "Depth" }), "-2000");

    expect(emitted("update:modelValue")).toEqual([["-2000"]]);
  });

  it("reflects a v-model into the input value", async () => {
    render({
      components: { GvInput },
      setup() {
        return { value: ref("-1000") };
      },
      template: `<gv-input v-model="value" label="Depth" />`,
    });

    expect(await screen.findByRole("textbox", { name: "Depth" })).toHaveValue("-1000");
  });

  it("describes the input by its hint and error message", () => {
    render(GvInput, { props: { label: "Depth", hint: "For example, -2000", errorMessage: "Enter a depth" } });

    expect(screen.getByRole("textbox", { name: "Depth" }))
      .toHaveAccessibleDescription("For example, -2000 Error:Enter a depth");
  });

  it("marks the input as errored only when there is an error message", async () => {
    const { rerender } = render(GvInput, { props: { label: "Depth" } });

    expect(screen.getByRole("textbox", { name: "Depth" })).not.toHaveClass("govuk-input--error");

    await rerender({ label: "Depth", errorMessage: "Enter a depth" });

    expect(screen.getByRole("textbox", { name: "Depth" })).toHaveClass("govuk-input--error");
  });

  it("uses the given id so links can target the input", () => {
    render(GvInput, { props: { label: "Depth", id: "depth-input" } });

    expect(screen.getByRole("textbox", { name: "Depth" })).toHaveAttribute("id", "depth-input");
  });

  it("passes attributes such as inputmode and classes to the input", () => {
    render(GvInput, { props: { label: "Depth", inputmode: "numeric" }, attrs: { class: "govuk-input--width-10" } });

    const input = screen.getByRole("textbox", { name: "Depth" });
    expect(input).toHaveAttribute("inputmode", "numeric");
    expect(input).toHaveClass("govuk-input--width-10");
  });

  it("renders a prefix and suffix in a wrapper", () => {
    render(GvInput, { props: { label: "Depth", prefix: "-", suffix: "m" } });

    expect(screen.getByText("-")).toHaveClass("govuk-input__prefix");
    expect(screen.getByText("m")).toHaveClass("govuk-input__suffix");
  });
});
