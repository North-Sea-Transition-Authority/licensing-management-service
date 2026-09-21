import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvHint from "@/components/govukVue/hint/GvHint.vue";

describe("gvHint", () => {
  it("renders the text prop", () => {
    render(GvHint, { props: { text: "Enter your full name" } });

    expect(screen.getByText("Enter your full name")).toBeInTheDocument();
  });

  it("prefers the default slot over the text prop", () => {
    render(GvHint, {
      props: { text: "Ignored text" },
      slots: { default: "Slotted hint" },
    });

    expect(screen.getByText("Slotted hint")).toBeInTheDocument();
    expect(screen.queryByText("Ignored text")).not.toBeInTheDocument();
  });

  it("adds the given id to the hint element", () => {
    const { container } = render(GvHint, { props: { id: "name-hint", text: "Hint" } });

    expect(container.querySelector("div#name-hint.govuk-hint")).toBeInTheDocument();
  });
});
