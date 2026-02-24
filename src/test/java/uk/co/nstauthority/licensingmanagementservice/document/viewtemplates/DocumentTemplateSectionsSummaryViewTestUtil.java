package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import java.util.List;
import java.util.Map;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionsSummaryView;

public class DocumentTemplateSectionsSummaryViewTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private List<DocumentTemplateSectionSummaryView> topLevelDocumentTemplateSectionSummaryViews = List.of();
    private List<String> allErrorMessages = List.of();
    private Map<String, String> allMailMergeResolvedValuesByMnemonic = Map.of();

    private Builder() {
    }

    public Builder withDocumentTemplateSectionSummaryViews(List<DocumentTemplateSectionSummaryView> topLevelDocumentTemplateSectionSummaryViews) {
      this.topLevelDocumentTemplateSectionSummaryViews = topLevelDocumentTemplateSectionSummaryViews;
      return this;
    }

    public Builder withErrorMessages(List<String> allErrorMessages) {
      this.allErrorMessages = allErrorMessages;
      return this;
    }

    public Builder withResolvedMailMergeFields(Map<String, String>  allMailMergeResolvedValuesByMnemonic) {
      this.allMailMergeResolvedValuesByMnemonic = allMailMergeResolvedValuesByMnemonic;
      return this;
    }

    public DocumentTemplateSectionsSummaryView build() {
      return new DocumentTemplateSectionsSummaryView(
          topLevelDocumentTemplateSectionSummaryViews,
          allErrorMessages,
          allMailMergeResolvedValuesByMnemonic
      );
    }
  }
}
