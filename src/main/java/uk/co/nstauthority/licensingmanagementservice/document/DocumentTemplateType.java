package uk.co.nstauthority.licensingmanagementservice.document;

public enum DocumentTemplateType {

  CONTINUATION_LETTER(
      "Continuation Letter",
      "Letter to confirm the continuation of a licence into a subsequent term."
  ),
  SCHEDULE_AMENDMENT_LETTER(
      "Schedule Amendment Letter",
      "Letter to confirm the amendment of a schedule and work programme."
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

