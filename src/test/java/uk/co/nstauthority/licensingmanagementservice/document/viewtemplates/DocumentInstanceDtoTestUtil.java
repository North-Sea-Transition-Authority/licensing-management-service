package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import java.util.UUID;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;

public class DocumentInstanceDtoTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private String itemReference = "TEST_ITEM_REFERENCE";
    private String itemType = "TEST_ITEM_TYPE";
    private String title = "Test title";
    private String description = "Test description";
    private DocumentTemplateDto documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();

    private Builder() {
    }

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withItemReference(String itemReference) {
      this.itemReference = itemReference;
      return this;
    }

    public Builder withItemType(String itemType) {
      this.itemType = itemType;
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

    public Builder withDocumentTemplate(DocumentTemplateDto documentTemplateDto) {
      this.documentTemplateDto = documentTemplateDto;
      return this;
    }

    public DocumentInstanceDto build() {
      return new DocumentInstanceDto(id, itemReference, itemType, title, description, documentTemplateDto);
    }
  }
}