import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";
import GvSummaryList from "@/components/govukVue/summary-list/GvSummaryList.vue";
import GvSummaryListRow from "@/components/govukVue/summary-list/GvSummaryListRow.vue";
import GvSummaryListRowAction from "@/components/govukVue/summary-list/GvSummaryListRowAction.vue";

const components = { GvSummaryList, GvSummaryListRow, GvSummaryListRowAction };

describe("summaryList components working together", () => {
  it("derives an action's visually hidden text from its row key", async () => {
    render({
      components,
      template: `
        <gv-summary-list>
          <gv-summary-list-row key-text="Name" value-text="Anne">
            <template #actions>
              <gv-summary-list-row-action text="Change" href="/change" />
            </template>
          </gv-summary-list-row>
        </gv-summary-list>
      `,
    });

    expect(await screen.findByRole("link", { name: /Change/ })).toHaveTextContent("Change Name");
  });

  it("wraps multiple actions in a row inside a list of items", async () => {
    render({
      components,
      template: `
        <gv-summary-list>
          <gv-summary-list-row key-text="Name" value-text="Anne">
            <template #actions>
              <gv-summary-list-row-action text="Change" href="/change" />
              <gv-summary-list-row-action text="Remove" href="/remove" />
            </template>
          </gv-summary-list-row>
        </gv-summary-list>
      `,
    });

    expect(await screen.findByRole("list")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
  });

  it("marks a row with no actions when a sibling row has actions", async () => {
    const { container } = render({
      components,
      template: `
        <gv-summary-list>
          <gv-summary-list-row key-text="Name" value-text="Anne">
            <template #actions>
              <gv-summary-list-row-action text="Change" href="/change" />
            </template>
          </gv-summary-list-row>
          <gv-summary-list-row key-text="Date of birth" value-text="1 January 2000" />
        </gv-summary-list>
      `,
    });

    await screen.findByRole("link", { name: /Change/ });

    expect(container.querySelectorAll(".govuk-summary-list__row--no-actions")).toHaveLength(1);
  });

  it("fires the composed click handler when an action is clicked", async () => {
    const onRemove = vi.fn();

    render({
      components,
      setup() {
        return { onRemove };
      },
      template: `
        <gv-summary-list>
          <gv-summary-list-row key-text="Name" value-text="Anne">
            <template #actions>
              <gv-summary-list-row-action text="Remove" @click.prevent="onRemove" />
            </template>
          </gv-summary-list-row>
        </gv-summary-list>
      `,
    });

    await fireEvent.click(await screen.findByRole("link", { name: /Remove/ }));

    expect(onRemove).toHaveBeenCalledOnce();
  });
});
