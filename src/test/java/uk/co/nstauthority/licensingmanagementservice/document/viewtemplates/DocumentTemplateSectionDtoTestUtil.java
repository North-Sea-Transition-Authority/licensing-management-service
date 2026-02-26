package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import java.util.List;
import java.util.UUID;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;

public class DocumentTemplateSectionDtoTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private DocumentTemplateDto documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    private UUID parentId;
    private String title = "Test title";
    private String content = "Test content";
    private String conditionMnemonic;
    private boolean numbered = true;
    private boolean hasPageBreakBefore = false;
    private int displayOrder = 1;
    private List<DocumentTemplateSectionDto> children = List.of();

    private Builder() {
    }

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withDocumentTemplateDto(DocumentTemplateDto documentTemplateDto) {
      this.documentTemplateDto = documentTemplateDto;
      return this;
    }

    public Builder withParentId(UUID parentId) {
      this.parentId = parentId;
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

    public Builder withConditionMnemonic(String conditionMnemonic) {
      this.conditionMnemonic = conditionMnemonic;
      return this;
    }

    public Builder withNumbered(boolean numbered) {
      this.numbered = numbered;
      return this;
    }

    public Builder withPageBreakBefore(boolean hasPageBreakBefore) {
      this.hasPageBreakBefore = hasPageBreakBefore;
      return this;
    }

    public Builder withDisplayOrder(int displayOrder) {
      this.displayOrder = displayOrder;
      return this;
    }

    public Builder withChildren(List<DocumentTemplateSectionDto> children) {
      this.children = children;
      return this;
    }

    public DocumentTemplateSectionDto build() {
      return new DocumentTemplateSectionDto(
          id,
          documentTemplateDto,
          parentId,
          title,
          content,
          conditionMnemonic,
          numbered,
          hasPageBreakBefore,
          displayOrder,
          children
      );
    }
  }
}
