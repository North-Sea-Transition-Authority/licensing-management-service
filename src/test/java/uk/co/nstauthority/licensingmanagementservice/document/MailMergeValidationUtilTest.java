package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.nstauthority.licensingmanagementservice.document.instance.DocumentInstanceSectionSummaryViewTestUtil;

class MailMergeValidationUtilTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
          "((unresolved-field))",
          "??manual field??"
      }
  )
  void sectionsContainInvalidMailMergeFields_whenTrue(String content) {
    var sectionSummaryView = DocumentInstanceSectionSummaryViewTestUtil
        .newBuilder()
        .withContent(content)
        .build();
    var sectionsSummaryView = DocumentInstanceSectionsSummaryView.from(List.of(sectionSummaryView));

    assertThat(MailMergeValidationUtil.sectionsContainInvalidMailMergeFields(sectionsSummaryView)).isTrue();
  }

  @Test
  void sectionsContainInvalidMailMergeFields_whenFalse() {
    var sectionSummaryView = DocumentInstanceSectionSummaryViewTestUtil
        .newBuilder()
        .build();
    var sectionsSummaryView = DocumentInstanceSectionsSummaryView.from(List.of(sectionSummaryView));

    assertThat(MailMergeValidationUtil.sectionsContainInvalidMailMergeFields(sectionsSummaryView)).isFalse();
  }

  @Test
  void contentContainsNonApplicableMailMergeFields() {
    assertThat(MailMergeValidationUtil.contentContainsNonApplicableMailMergeFields("((unresolved-field))")).isTrue();
  }

  @Test
  void contentContainsManualMailMergeFields() {
    assertThat(MailMergeValidationUtil.contentContainsManualMailMergeFields("??manual field??")).isTrue();
  }
}