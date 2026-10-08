import { fireEvent, render, screen, waitFor } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import { ref } from "vue";
import GvRadio from "@/components/govukVue/radios/GvRadio.vue";
import GvRadios from "@/components/govukVue/radios/GvRadios.vue";

const components = { GvRadios, GvRadio };

function renderGroup(initial?: string, name?: string) {
  return render({
    components,
    setup() {
      return { selected: ref(initial), name };
    },
    template: `
      <gv-radios v-model="selected" legend="Change" :name="name">
        <gv-radio value="SPLIT" label="Split" />
        <gv-radio value="MERGE" label="Merge" />
      </gv-radios>
      <p>Selected: {{ selected }}</p>
      <button @click="selected = 'MERGE'">Select merge</button>
    `,
  });
}

function radioNames() {
  return screen.getAllByRole("radio").map(radio => radio.getAttribute("name"));
}

describe("radios components working together", () => {
  it("sets the v-model to the value of the chosen radio", async () => {
    renderGroup();

    await fireEvent.click(screen.getByRole("radio", { name: "Merge" }));

    expect(await screen.findByText("Selected: MERGE")).toBeInTheDocument();
  });

  it("checks the radio whose value is the initial v-model", async () => {
    renderGroup("SPLIT");

    await waitFor(() => {
      expect(screen.getByRole("radio", { name: "Split" })).toBeChecked();
      expect(screen.getByRole("radio", { name: "Merge" })).not.toBeChecked();
    });
  });

  it("checks the radio whose value the v-model is changed to", async () => {
    renderGroup("SPLIT");

    await fireEvent.click(screen.getByRole("button", { name: "Select merge" }));

    await waitFor(() => {
      expect(screen.getByRole("radio", { name: "Merge" })).toBeChecked();
      expect(screen.getByRole("radio", { name: "Split" })).not.toBeChecked();
    });
  });

  it("gives every radio the group's name", () => {
    renderGroup(undefined, "action");

    expect(radioNames()).toEqual(["action", "action"]);
  });

  it("gives every radio the same generated name when the group has no name", () => {
    renderGroup();

    const [firstName, secondName] = radioNames();
    expect(firstName).toMatch(/^gv-radios-/);
    expect(secondName).toBe(firstName);
  });
});
