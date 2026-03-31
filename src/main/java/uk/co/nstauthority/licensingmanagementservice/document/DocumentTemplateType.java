package uk.co.nstauthority.licensingmanagementservice.document;

public enum DocumentTemplateType {

  CONTINUATION_LETTER(
      "Continuation Letter",
      "Letter to confirm the continuation of a licence into a subsequent term."
  ),
  EXTENSION_APPROVAL_LETTER(
      "Extension Approval Letter",
      "Letter to approve an extension to a licence schedule."
  );

  private final String title;
  private final String description;
  private final String documentInstancePdfTemplatePath;

  DocumentTemplateType(
      String title,
      String description
  ) {
    this.title = title;
    this.description = description;
    this.documentInstancePdfTemplatePath = "lms/document/document.ftl";
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getDocumentInstancePdfTemplatePath() {
    return documentInstancePdfTemplatePath;
  }

}

