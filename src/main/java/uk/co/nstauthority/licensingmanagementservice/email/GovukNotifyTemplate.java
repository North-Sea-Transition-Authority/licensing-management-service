package uk.co.nstauthority.licensingmanagementservice.email;

public enum GovukNotifyTemplate {
  SEND_CONTINUATION_ISSUED_DOCUMENT_V1("5209730e-37d9-40cc-838b-6686c7fcbf61"),
  APPLICATION_WITHDRAWAL_V1("554b8f6e-44e3-4a6e-9edf-917f3f0f5d3d");

  private final String templateId;

  GovukNotifyTemplate(String templateId) {
    this.templateId = templateId;
  }

  String getTemplateId() {
    return templateId;
  }
}
