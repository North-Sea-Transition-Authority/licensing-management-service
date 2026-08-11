import { render, screen } from "@testing-library/vue";
import { describe, expect, it } from "vitest";
import SingleErrorSummary from "../../../../main/resources/js/components/SingleErrorSummary.vue";

describe("singleErrorSummary", () => {
  it("renders nothing when there is no message", () => {
    render(SingleErrorSummary, { props: { message: null } });

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("renders the message inside a govuk-style error summary", () => {
    render(SingleErrorSummary, { props: { message: "Something went wrong" } });

    const alert = screen.getByRole("alert");
    expect(alert).toHaveClass("govuk-error-summary");
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
  });
});
