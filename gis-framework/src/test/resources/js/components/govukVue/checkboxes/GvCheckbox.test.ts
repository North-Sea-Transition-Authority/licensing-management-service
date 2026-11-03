import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import GvCheckbox from "@/components/govukVue/checkboxes/GvCheckbox.vue";

describe("gvCheckbox", () => {
  it("renders an unchecked checkbox with its label", () => {
    render(GvCheckbox, { props: { label: "Accept" } });

    expect(screen.getByRole("checkbox", { name: "Accept" })).not.toBeChecked();
  });

  it("checks the box and emits update:modelValue when clicked", async () => {
    const { emitted } = render(GvCheckbox, { props: { label: "Accept" } });

    await fireEvent.click(screen.getByRole("checkbox", { name: "Accept" }));

    expect(screen.getByRole("checkbox", { name: "Accept" })).toBeChecked();
    expect(emitted("update:modelValue")).toEqual([[true]]);
  });

  it("reflects a direct v-model into the checked state", async () => {
    render({
      components: { GvCheckbox },
      setup() {
        return { value: ref(true) };
      },
      template: `<gv-checkbox v-model="value" label="Accept" />`,
    });

    expect(await screen.findByRole("checkbox", { name: "Accept" })).toBeChecked();
  });

  it("disables the checkbox when the disabled prop is set", () => {
    render(GvCheckbox, { props: { label: "Accept", disabled: true } });

    expect(screen.getByRole("checkbox", { name: "Accept" })).toBeDisabled();
  });

  it("renders the hint text", () => {
    render(GvCheckbox, { props: { label: "Accept", hint: "Read this first" } });

    expect(screen.getByText("Read this first")).toBeInTheDocument();
  });

  it("renders the divider text", () => {
    render(GvCheckbox, { props: { label: "None", divider: "or" } });

    expect(screen.getByText("or")).toBeInTheDocument();
  });

  it("reveals conditional content only when the box is checked", async () => {
    const { container } = render(GvCheckbox, {
      props: { label: "Other" },
      slots: { conditional: "Please specify" },
    });

    const conditional = container.querySelector(".govuk-checkboxes__conditional");
    expect(conditional).toHaveClass("govuk-checkboxes__conditional--hidden");

    await fireEvent.click(screen.getByRole("checkbox", { name: "Other" }));

    expect(conditional).not.toHaveClass("govuk-checkboxes__conditional--hidden");
  });

  it("updates reactive conditional content without errors", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    render({
      components: { GvCheckbox },
      setup() {
        return { message: ref("First message") };
      },
      template: `
        <gv-checkbox label="Other">
          <template #conditional><p>{{ message }}</p></template>
        </gv-checkbox>
        <button @click="message = 'Second message'">Change message</button>`,
    });
    await fireEvent.click(screen.getByRole("checkbox", { name: "Other" }));

    await fireEvent.click(screen.getByRole("button", { name: "Change message" }));

    expect(screen.getByText("Second message")).toBeInTheDocument();
    expect(warn).not.toHaveBeenCalled();
    warn.mockRestore();
  });
});
