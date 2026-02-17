package uk.co.nstauthority.licensingmanagementservice.document;

import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;

public record DocumentTemplateSection(
    String title,
    String contentFreemarker,
    String content,
    String conditionMnemonic,
    int displayOrder,
    DocumentTemplateSectionDto parentDto,
    boolean isNumbered,
    boolean hasPageBreakBefore
) {

  public static Builder newBuilder(String title, DocumentTemplate.Builder builder) {
    return new Builder().withTitle(title).forTemplate(builder);
  }

  public static class Builder {

    private DocumentTemplate.Builder templateBuilder;
    private String title;
    private String contentFreemarker;
    private int displayOrder;
    private String content;

    private DocumentTemplateSectionDto parentDto = null;
    private boolean isNumbered = false;
    private boolean hasPageBreakBefore = false;
    private String conditionMnemonic = null;

    private Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    // We either want a freemarker file or html text to define the content.
    // Either content or freemarkerContent should always be null.
    public Builder withContentFreemarker(String contentFreemarker) {
      this.contentFreemarker = contentFreemarker;
      this.content = null;
      return this;
    }

    public Builder withContent(String content) {
      this.contentFreemarker = null;
      this.content = content;
      return this;
    }

    public Builder withConditionMnemonic(String conditionMnemonic) {
      this.conditionMnemonic = conditionMnemonic;
      return this;
    }

    public Builder withDisplayOrder(int displayOrder) {
      this.displayOrder = displayOrder;
      return this;
    }

    public Builder hasPageBreakBefore(boolean hasPageBreakBefore) {
      this.hasPageBreakBefore = hasPageBreakBefore;
      return this;
    }

    // This method adds the new section to the template that we're building and returns the builder for the relevant template
    public DocumentTemplate.Builder completeSection() {
      validateContent();
      var section = new DocumentTemplateSection(
          title,
          contentFreemarker,
          content,
          conditionMnemonic,
          displayOrder,
          parentDto,
          isNumbered,
          hasPageBreakBefore
      );
      return templateBuilder.withSection(section);
    }

    // This method is used to build the final section of a template. It returns the section object to be added to the builder.
    // This should never be called in a consumer, it should only be used within the DocumentTemplate class.
    DocumentTemplateSection buildLastSection() {
      validateContent();
      return new DocumentTemplateSection(
          title,
          contentFreemarker,
          content,
          conditionMnemonic,
          displayOrder,
          parentDto,
          isNumbered,
          hasPageBreakBefore
      );
    }

    // This stores the template builder that we're currently using to build sections for.
    private Builder forTemplate(DocumentTemplate.Builder template) {
      this.templateBuilder = template;
      return this;
    }

    private void validateContent() {
      if (content == null && contentFreemarker == null) {
        throw new IllegalStateException(
            "DocumentTemplateSection '" + title + "' must have either content or contentFreemarker defined."
        );
      }
      if (content != null && contentFreemarker != null) {
        throw new IllegalStateException(
            "DocumentTemplateSection '" + title + "' cannot have both content and contentFreemarker defined."
        );
      }
    }
  }
}
