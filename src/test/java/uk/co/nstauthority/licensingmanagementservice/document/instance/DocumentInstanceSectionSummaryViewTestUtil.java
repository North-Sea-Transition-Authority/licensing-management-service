package uk.co.nstauthority.licensingmanagementservice.document.instance;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionSummaryView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionUrls;

public class DocumentInstanceSectionSummaryViewTestUtil {

  private DocumentInstanceSectionSummaryViewTestUtil() {
    throw new IllegalStateException("Cannot instantiate static utils class");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id =  UUID.randomUUID();
    private String sectionNumber = "1";
    private String title = "title";
    private String content = "content";
    private boolean hasPageBreakBefore = false;
    private List<String> errorMessages = List.of();
    private Map<String, String> mailMergeResolvedValuesByMnemonic = Map.of();
    private DocumentInstanceSectionUrls documentInstanceSectionUrls = DocumentInstanceSectionUrlsTestUtil.newBuilder().build();
    private List<DocumentInstanceSectionSummaryView> children = List.of();

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withSectionNumber(String sectionNumber) {
      this.sectionNumber = sectionNumber;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withContent(String content) {
      this.content = content;
      return this;
    }

    public Builder withHasPageBreakBefore(boolean hasPageBreakBefore) {
      this.hasPageBreakBefore = hasPageBreakBefore;
      return this;
    }

    public Builder withErrorMessages(List<String> errorMessages) {
      this.errorMessages = errorMessages;
      return this;
    }

    public Builder withMailMergeResolvedValuesByMnemonic(Map<String, String> mailMergeResolvedValuesByMnemonic) {
      this.mailMergeResolvedValuesByMnemonic = mailMergeResolvedValuesByMnemonic;
      return this;
    }

    public Builder withDocumentInstanceSectionUrls(DocumentInstanceSectionUrls documentInstanceSectionUrls) {
      this.documentInstanceSectionUrls = documentInstanceSectionUrls;
      return this;
    }

    public Builder withChildren(List<DocumentInstanceSectionSummaryView> children) {
      this.children = children;
      return this;
    }

    public DocumentInstanceSectionSummaryView build() {
      return new DocumentInstanceSectionSummaryView(
          this.id,
          this.sectionNumber,
          this.title,
          this.content,
          this.hasPageBreakBefore,
          this.errorMessages,
          this.mailMergeResolvedValuesByMnemonic,
          this.documentInstanceSectionUrls,
          this.children
      );
    }
  }
}
