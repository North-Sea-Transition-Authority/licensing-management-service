package uk.co.nstauthority.licensingmanagementservice.document.instance;

import java.util.List;
import java.util.UUID;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;

public class DocumentInstanceSectionDtoTestUtil {

  private DocumentInstanceSectionDtoTestUtil() {
    throw new IllegalStateException("This is a util class and cannot be instantiated");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private DocumentInstanceDto documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder().build();
    private UUID createdFromDocumentTemplateSectionId = UUID.randomUUID();
    private UUID parentId;
    private String title = "Test title";
    private String content = "Test content";
    private boolean numbered = true;
    private boolean hasPageBreakBefore = false;
    private int displayOrder = 1;
    private List<DocumentInstanceSectionDto> children = List.of();

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withDocumentInstanceDto(DocumentInstanceDto documentInstanceDto) {
      this.documentInstanceDto = documentInstanceDto;
      return this;
    }

    public Builder withCreatedFromDocumentTemplateSectionId(UUID createdFromDocumentTemplateSectionId) {
      this.createdFromDocumentTemplateSectionId = createdFromDocumentTemplateSectionId;
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

    public Builder withChildren(List<DocumentInstanceSectionDto> children) {
      this.children = children;
      return this;
    }

    public DocumentInstanceSectionDto build() {
      return new DocumentInstanceSectionDto(
          id,
          documentInstanceDto,
          createdFromDocumentTemplateSectionId,
          parentId,
          title,
          content,
          numbered,
          hasPageBreakBefore,
          displayOrder,
          children
      );
    }
  }
}
