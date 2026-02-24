package uk.co.nstauthority.licensingmanagementservice.document;

import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionUrls;

public class DocumentTemplateSectionUrlsTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilderWithUrlSuffix(String urlSuffix) {
    return newBuilder()
        .withAddSectionBeforeUrl("test-add-section-before-url%s".formatted(urlSuffix))
        .withAddSectionAfterUrl("test-add-section-after-url%s".formatted(urlSuffix))
        .withAddSubsectionUrl("test-add-subsection-url%s".formatted(urlSuffix))
        .withEditUrl("test-edit-url%s".formatted(urlSuffix))
        .withRemoveUrl("test-remove-url%s".formatted(urlSuffix));
  }

  public static class Builder {

    private String addSectionBeforeUrl = "test-add-section-before-url";
    private String addSectionAfterUrl = "test-add-section-after-url";
    private String addSubsectionUrl = "test-add-subsection-url";
    private String editUrl = "test-edit-url";
    private String removeUrl = "test-remove-url";

    public Builder withAddSectionBeforeUrl(String addSectionBeforeUrl) {
      this.addSectionBeforeUrl = addSectionBeforeUrl;
      return this;
    }

    public Builder withAddSectionAfterUrl(String addSectionAfterUrl) {
      this.addSectionAfterUrl = addSectionAfterUrl;
      return this;
    }

    public Builder withAddSubsectionUrl(String addSubsectionUrl) {
      this.addSubsectionUrl = addSubsectionUrl;
      return this;
    }

    public Builder withEditUrl(String editUrl) {
      this.editUrl = editUrl;
      return this;
    }

    public Builder withRemoveUrl(String removeUrl) {
      this.removeUrl = removeUrl;
      return this;
    }

    public DocumentTemplateSectionUrls build() {
      return new DocumentTemplateSectionUrls(addSectionBeforeUrl, addSectionAfterUrl, addSubsectionUrl, editUrl, removeUrl);
    }
  }
}
