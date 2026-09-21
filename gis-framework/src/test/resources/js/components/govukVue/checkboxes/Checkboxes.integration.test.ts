import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import { ref } from "vue";
import GvCheckbox from "@/components/govukVue/checkboxes/GvCheckbox.vue";
import GvCheckboxes from "@/components/govukVue/checkboxes/GvCheckboxes.vue";

const components = { GvCheckboxes, GvCheckbox };

function renderGroup(initial: string[] = []) {
  return render({
    components,
    setup() {
      return { selected: ref(initial) };
    },
    template: `
      <gv-checkboxes v-model="selected" legend="Features">
        <gv-checkbox value="a" label="Alpha" />
        <gv-checkbox value="b" label="Bravo" />
        <gv-checkbox value="c" label="Charlie" />
      </gv-checkboxes>
      <p>Selected: {{ selected.join(",") }}</p>
    `,
  });
}

describe("checkboxes components working together", () => {
  it("renders the group as a fieldset with its legend", async () => {
    renderGroup();

    expect(await screen.findByRole("group", { name: "Features" })).toBeInTheDocument();
  });

  it("adds a checkbox's value to the v-model array when ticked", async () => {
    renderGroup();

    await fireEvent.click(screen.getByRole("checkbox", { name: "Alpha" }));
    await fireEvent.click(screen.getByRole("checkbox", { name: "Charlie" }));

    expect(await screen.findByText("Selected: a,c")).toBeInTheDocument();
  });

  it("removes a checkbox's value from the v-model array when unticked", async () => {
    renderGroup(["a", "b"]);

    await waitFor(() => expect(screen.getByRole("checkbox", { name: "Alpha" })).toBeChecked());

    await fireEvent.click(screen.getByRole("checkbox", { name: "Alpha" }));

    expect(await screen.findByText("Selected: b")).toBeInTheDocument();
  });

  it("pre-ticks the checkboxes whose values are in the initial v-model", async () => {
    renderGroup(["b"]);

    await waitFor(() => {
      expect(screen.getByRole("checkbox", { name: "Bravo" })).toBeChecked();
      expect(screen.getByRole("checkbox", { name: "Alpha" })).not.toBeChecked();
      expect(screen.getByRole("checkbox", { name: "Charlie" })).not.toBeChecked();
    });
  });

  it("applies a shared name to every checkbox in the group", async () => {
    const { container } = renderGroup();

    await screen.findByRole("group", { name: "Features" });

    const names = [...container.querySelectorAll<HTMLInputElement>("input[type=checkbox]")]
      .map(input => input.name);

    expect(new Set(names).size).toBe(1);
    expect(names[0]).toBeTruthy();
  });
});
