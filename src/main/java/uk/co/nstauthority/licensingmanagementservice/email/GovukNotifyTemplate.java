package uk.co.nstauthority.licensingmanagementservice.email;

public enum GovukNotifyTemplate {
  STUB_EMAIL("stub-uuid"); // TODO - Remove when adding a real notify template

  private final String templateId;

  GovukNotifyTemplate(String templateId) {
    this.templateId = templateId;
  }

  String getTemplateId() {
    return templateId;
  }
}
