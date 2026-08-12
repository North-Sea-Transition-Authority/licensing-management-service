import { render, screen } from "@testing-library/vue";
import { afterEach, describe, expect, it, vi } from "vitest";
import ErrorSummary from "../../../../../../main/resources/js/components/gdsComponents/error/ErrorSummary.vue";

describe("errorSummary", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the default title", () => {
    render(ErrorSummary);

    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(screen.getByText("There is a problem")).toBeInTheDocument();
  });

  it("renders a custom title provided via the title prop", () => {
    render(ErrorSummary, { props: { title: "Custom title" } });

    expect(screen.getByText("Custom title")).toBeInTheDocument();
  });

  it("renders a title provided via the title slot instead of the prop", () => {
    render(ErrorSummary, {
      props: { title: "Ignored title" },
      slots: { title: "Slotted title" },
    });

    expect(screen.getByText("Slotted title")).toBeInTheDocument();
    expect(screen.queryByText("Ignored title")).not.toBeInTheDocument();
  });

  it("renders the description when provided via the description prop", () => {
    render(ErrorSummary, { props: { description: "Something went wrong" } });

    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
  });

  it("renders the description when provided via the description slot", () => {
    render(ErrorSummary, { slots: { description: "Slotted description" } });

    expect(screen.getByText("Slotted description")).toBeInTheDocument();
  });

  it("does not render a description paragraph when no description is provided", () => {
    const { container } = render(ErrorSummary);

    expect(container.querySelector(".govuk-error-summary__body p")).not.toBeInTheDocument();
  });

  it("renders default slot content inside the error list", () => {
    render(ErrorSummary, { slots: { default: "<li>Field is required</li>" } });

    expect(screen.getByText("Field is required")).toBeInTheDocument();
  });

  it("focuses the summary when mounted by default", () => {
    const focusSpy = vi.spyOn(HTMLElement.prototype, "focus");

    render(ErrorSummary);

    expect(focusSpy).toHaveBeenCalled();
  });

  it("does not focus the summary when disableAutoFocus is true", () => {
    const focusSpy = vi.spyOn(HTMLElement.prototype, "focus");

    render(ErrorSummary, { props: { disableAutoFocus: true } });

    expect(focusSpy).not.toHaveBeenCalled();
  });
});
