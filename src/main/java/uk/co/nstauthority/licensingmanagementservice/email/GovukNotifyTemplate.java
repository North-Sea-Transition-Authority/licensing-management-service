package uk.co.nstauthority.licensingmanagementservice.email;

public enum GovukNotifyTemplate {
  SEND_CONTINUATION_ISSUED_DOCUMENT_V1("5209730e-37d9-40cc-838b-6686c7fcbf61"),
  APPLICATION_WITHDRAWAL_V1("554b8f6e-44e3-4a6e-9edf-917f3f0f5d3d"),
  LICENCE_CONTACT_UPDATED_V1("51973850-af1c-4130-8a80-9f8b38ea28f7"),
  TERM_OR_PHASE_END_REMINDER_V1("6d8e144d-adeb-45ca-9cc3-60e3d4fc7d70"),
  NEW_SCHEDULE_AMENDMENT_APPLICATION_SUBMITTED_V1("0d07824b-5b09-4ab9-9999-bbdee063d46a"),
  STEWARD_ASSIGNED_TO_APPLICATION_V1("d30ae587-2611-435a-9689-095753eb8fd2"),
  LICENCE_EXPIRY_REMINDER_V1("46060456-64fa-45ae-9981-e21742c308d6"),
  WORK_PROGRAMME_ACTIVITY_REMINDER_V1("ebf7f00a-f8ad-4463-8ead-bda988e82bf7"),
  OTHER_SCHEDULE_EVENT_REMINDER_V1("88d2eaa9-2d66-4d85-bf40-f3dba4fb539f");

  private final String templateId;

  GovukNotifyTemplate(String templateId) {
    this.templateId = templateId;
  }

  String getTemplateId() {
    return templateId;
  }
}
