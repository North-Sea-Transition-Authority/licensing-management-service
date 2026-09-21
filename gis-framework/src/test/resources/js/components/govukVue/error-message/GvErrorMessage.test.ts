import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import GvErrorMessage from "@/components/govukVue/error-message/GvErrorMessage.vue";

describe("gvErrorMessage", () => {
  it("renders the text prop", () => {
    render(GvErrorMessage, { props: { text: "Enter a valid date" } });

    expect(screen.getByText("Enter a valid date")).toBeInTheDocument();
  });

  it("prefers the default slot over the text prop", () => {
    render(GvErrorMessage, {
      props: { text: "Ignored text" },
      slots: { default: "Slotted error" },
    });

    expect(screen.getByText("Slotted error")).toBeInTheDocument();
    expect(screen.queryByText("Ignored text")).not.toBeInTheDocument();
  });

  it("renders the default visually hidden prefix", () => {
    render(GvErrorMessage, { props: { text: "Enter a valid date" } });

    const prefix = screen.getByText("Error:");
    expect(prefix).toHaveClass("govuk-visually-hidden");
  });

  it("renders a custom visually hidden prefix", () => {
    render(GvErrorMessage, { props: { text: "Enter a valid date", visuallyHiddenText: "Problem" } });

    expect(screen.getByText("Problem:")).toBeInTheDocument();
  });

  it("omits the visually hidden prefix when visuallyHiddenText is empty", () => {
    render(GvErrorMessage, { props: { text: "Enter a valid date", visuallyHiddenText: "" } });

    expect(screen.queryByText(":")).not.toBeInTheDocument();
  });

  it("adds the given id to the error message element", () => {
    const { container } = render(GvErrorMessage, { props: { id: "date-error", text: "Error" } });

    expect(container.querySelector("p#date-error.govuk-error-message")).toBeInTheDocument();
  });
});
