package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.UUID;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;

public class DocumentTemplateDtoTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private String mnemonic = "REGULATOR_DIVISION";
    private String title = "Test title";
    private String description = "Test description";
    private String templatePath = "test/template/path";
    private int displayOrder = 1;

    private Builder() {
    }

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withMnemonic(String mnemonic) {
      this.mnemonic = mnemonic;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withTemplatePath(String templatePath) {
      this.templatePath = templatePath;
      return this;
    }

    public Builder withDisplayOrder(int displayOrder) {
      this.displayOrder = displayOrder;
      return this;
    }

    public DocumentTemplateDto build() {
      return new DocumentTemplateDto(id, mnemonic, title, description, templatePath, displayOrder);
    }
  }
}