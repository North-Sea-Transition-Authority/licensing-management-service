package uk.co.nstauthority.licensingmanagementservice.document;

import static uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldService.MAIL_MERGE_FIELD_PATTERN;
import static uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldService.MANUAL_FIELD_PATTERN;

import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.DigitalSignatureMailMergeField;

public class MailMergeValidationUtil {

  private MailMergeValidationUtil() {
    throw new IllegalStateException("This is a util class and should not be instantiated");
  }

  public static boolean sectionsContainInvalidMailMergeFields(DocumentInstanceSectionsSummaryView summarySectionsView) {
    return summarySectionsView.topLevelDocumentInstanceSectionSummaryViews().stream()
        .anyMatch(documentInstanceSectionSummaryView ->
            contentContainsManualMailMergeFields(documentInstanceSectionSummaryView.content())
            || contentContainsNonApplicableMailMergeFields(documentInstanceSectionSummaryView.content()));
  }

  public static boolean contentContainsNonApplicableMailMergeFields(String content) {
    var contentWithoutSignature = content.replace(DigitalSignatureMailMergeField.SIGNATURE_PLACEHOLDER_TEXT, "");
    return MAIL_MERGE_FIELD_PATTERN.matcher(contentWithoutSignature).results().findFirst().isPresent();
  }

  public static boolean contentContainsManualMailMergeFields(String content) {
    return MANUAL_FIELD_PATTERN.matcher(content).results().findFirst().isPresent();
  }
}
