import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvWarningText from "@/components/govukVue/warning-text/GvWarningText.vue";

describe("gvWarningText", () => {
  it("renders the text prop", () => {
    render(GvWarningText, { props: { text: "You can be fined" } });

    expect(screen.getByText("You can be fined")).toBeInTheDocument();
  });

  it("prefers the default slot over the text prop", () => {
    render(GvWarningText, {
      props: { text: "Ignored text" },
      slots: { default: "Slotted text" },
    });

    expect(screen.getByText("Slotted text")).toBeInTheDocument();
    expect(screen.queryByText("Ignored text")).not.toBeInTheDocument();
  });

  it.each([
    { description: "the default", props: {}, expected: "Warning" },
    { description: "the given", props: { iconFallbackText: "Important" }, expected: "Important" },
  ])("renders $description icon fallback text visually hidden", ({ props, expected }) => {
    render(GvWarningText, { props: { text: "You can be fined", ...props } });

    expect(screen.getByText(expected)).toHaveClass("govuk-visually-hidden");
  });

  it("hides the icon from assistive technologies", () => {
    render(GvWarningText, { props: { text: "You can be fined" } });

    expect(screen.getByText("!")).toHaveAttribute("aria-hidden", "true");
  });
});
