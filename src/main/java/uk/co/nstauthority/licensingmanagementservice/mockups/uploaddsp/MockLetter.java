package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.List;

// Public so the real letter templates can call the accessors; they read the same shape as the document library views
public final class MockLetter {

  private static final SectionUrls URLS = new SectionUrls("#", "#", "#", "#", "#");

  private MockLetter() {
  }

  public record SectionUrls(
      String addSectionBeforeUrl,
      String addSectionAfterUrl,
      String addSubsectionUrl,
      String editUrl,
      String removeUrl
  ) {
  }

  public record Section(
      String id,
      String titleWithSectionNumber,
      boolean hasPageBreakBefore,
      SectionUrls documentInstanceSectionUrls,
      String content,
      List<Section> children
  ) {
  }

  public record Sections(List<Section> topLevelDocumentInstanceSectionSummaryViews) {
  }

  static Sections decisionLetter() {
    return new Sections(List.of(
        section("1", "1. Decision", "<p>[Decision wording generated from the recorded decision]</p>"),
        section("2", "2. Licence details", "<p>[Licence, licensees and blocks from the application]</p>"),
        section("3", "3. Conditions", "<p>[Conditions generated from the decision details]</p>")
    ));
  }

  static Sections directionLetter() {
    return new Sections(List.of(
        section("1", "1. Direction", "<p>[Direction wording generated from the recorded decision]</p>"),
        section("2", "2. Licence details", "<p>[Licence, licensees and blocks from the application]</p>"),
        section("3", "3. Effect of this direction", "<p>[Standard wording from the direction letter template]</p>")
    ));
  }

  private static Section section(String id, String title, String content) {
    return new Section(id, title, false, URLS, content, List.of());
  }
}
