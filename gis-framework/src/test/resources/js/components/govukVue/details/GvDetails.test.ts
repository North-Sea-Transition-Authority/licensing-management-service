import { fireEvent, render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvDetails from "../../../../../../main/resources/js/components/govukVue/details/GvDetails.vue";

describe("gvDetails", () => {
  it("renders the summary and text props", () => {
    render(GvDetails, {
      props: { summary: "More detail", text: "The hidden content" },
    });

    expect(screen.getByText("More detail")).toBeInTheDocument();
    expect(screen.getByText("The hidden content")).toBeInTheDocument();
  });

  it("prefers the summary slot over the summary prop", () => {
    render(GvDetails, {
      props: { summary: "Ignored summary" },
      slots: { summary: "Slotted summary" },
    });

    expect(screen.getByText("Slotted summary")).toBeInTheDocument();
    expect(screen.queryByText("Ignored summary")).not.toBeInTheDocument();
  });

  it("prefers the default slot over the text prop", () => {
    render(GvDetails, {
      props: { text: "Ignored text" },
      slots: { default: "Slotted text" },
    });

    expect(screen.getByText("Slotted text")).toBeInTheDocument();
    expect(screen.queryByText("Ignored text")).not.toBeInTheDocument();
  });

  it("adds the given id to the details element", () => {
    const { container } = render(GvDetails, { props: { id: "my-details" } });

    expect(container.querySelector("details#my-details")).toBeInTheDocument();
  });

  it("is closed by default", () => {
    const { container } = render(GvDetails, { props: { summary: "Toggle" } });

    expect(container.querySelector("details")).not.toHaveAttribute("open");
  });

  it("is open when the open prop is true", () => {
    const { container } = render(GvDetails, { props: { summary: "Toggle", open: true } });

    expect(container.querySelector("details")).toHaveAttribute("open");
  });

  it("toggles open and emits update:open when the summary is clicked", async () => {
    const { container, emitted } = render(GvDetails, { props: { summary: "Toggle" } });

    await fireEvent.click(screen.getByText("Toggle"));

    expect(container.querySelector("details")).toHaveAttribute("open");
    expect(emitted("update:open")).toEqual([[true]]);
  });

  it("syncs the open state when the open prop changes", async () => {
    const { container, rerender } = render(GvDetails, { props: { summary: "Toggle", open: false } });

    await rerender({ open: true });

    expect(container.querySelector("details")).toHaveAttribute("open");
  });
});
