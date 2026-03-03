package uk.co.nstauthority.licensingmanagementservice.document.instance;

import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionUrls;

public class DocumentInstanceSectionUrlsTestUtil {

  private DocumentInstanceSectionUrlsTestUtil() {
    throw new IllegalStateException("This is a utility class and should not be instantiated");
  }

  public static Builder newBuilder() {
    return new Builder();
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

    public DocumentInstanceSectionUrls build() {
      return new DocumentInstanceSectionUrls(addSectionBeforeUrl, addSectionAfterUrl, addSubsectionUrl, editUrl, removeUrl);
    }
  }
}
