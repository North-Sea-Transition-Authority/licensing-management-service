package uk.co.nstauthority.licensingmanagementservice.email;

public enum GovukNotifyTemplate {
  CONTINUATION_LETTER_ISSUED("5209730e-37d9-40cc-838b-6686c7fcbf61");

  private final String templateId;

  GovukNotifyTemplate(String templateId) {
    this.templateId = templateId;
  }

  String getTemplateId() {
    return templateId;
  }
}
