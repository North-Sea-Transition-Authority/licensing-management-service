import { render, screen, waitFor } from "@testing-library/vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

import TextualDescription from "@/components/textualDescription/TextualDescription.vue";

const { getTextualDescriptionMock } = vi.hoisted(() => ({
  getTextualDescriptionMock: vi.fn(),
}));

vi.mock("@/api/features.api", () => ({
  getTextualDescription: getTextualDescriptionMock,
}));

describe("textualDescription", () => {
  beforeEach(() => {
    getTextualDescriptionMock.mockReset();
  });

  it("renders the fetched description as HTML", async () => {
    const description = `<style>.gis-textual-description { font-family: sans-serif; }</style>
<div class="gis-textual-description">
<div class="gis-textual-description__feature">
<p>Subarea 30/1a is bounded by the following coordinates:</p>
<table class="gis-textual-description__coordinates"><tbody>
<tr><td class="gis-textual-description__label">(1)</td><td class="gis-textual-description__ordinate">1E</td><td class="gis-textual-description__ordinate">1N</td></tr>
</tbody></table>
<p>The above coordinates were specified using "British National Grid".<br>
The lines joining coordinates (1) to (2) are navigated as loxodromes.</p>
</div>
</div>`;
    getTextualDescriptionMock.mockResolvedValue(description);

    render(TextualDescription, {
      props: { textualDescriptionUrl: "/api/gis-framework/textual-description?featureId=feature-1" },
    });

    await waitFor(() => {
      expect(screen.getByText("Subarea 30/1a is bounded by the following coordinates:")).toBeInTheDocument();
      expect(screen.getByText("(1)")).toBeInTheDocument();
      expect(screen.getByText("1E")).toBeInTheDocument();
      expect(screen.getByText("1N")).toBeInTheDocument();
      expect(
        screen.getByText(
          "The above coordinates were specified using \"British National Grid\". The lines joining coordinates (1) to (2) are navigated as loxodromes.",
        ),
      ).toBeInTheDocument();
    });
    expect(getTextualDescriptionMock).toHaveBeenCalledWith(
      "/api/gis-framework/textual-description?featureId=feature-1",
    );
  });

  it("builds the url from the command journey id when one is given", async () => {
    getTextualDescriptionMock.mockResolvedValue("");

    render(TextualDescription, {
      props: {
        textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
        commandJourneyId: "journey-1",
      },
    });

    await waitFor(() => {
      expect(getTextualDescriptionMock).toHaveBeenCalledWith(
        "/api/gis-framework/command-journey-textual-description/journey-1",
      );
    });
  });

  it("refetches the description when the refresh counter changes", async () => {
    getTextualDescriptionMock.mockResolvedValue("");

    const { rerender } = render(TextualDescription, {
      props: {
        textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
        commandJourneyId: "journey-1",
        refreshCounter: 0,
      },
    });

    await waitFor(() => {
      expect(getTextualDescriptionMock).toHaveBeenCalledTimes(1);
    });

    await rerender({
      textualDescriptionUrl: "/api/gis-framework/command-journey-textual-description",
      commandJourneyId: "journey-1",
      refreshCounter: 1,
    });

    await waitFor(() => {
      expect(getTextualDescriptionMock).toHaveBeenCalledTimes(2);
    });
  });
});
