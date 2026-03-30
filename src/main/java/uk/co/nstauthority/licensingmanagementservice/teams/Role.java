package uk.co.nstauthority.licensingmanagementservice.teams;

public enum Role {

  MANAGE_TEAM(
      "Access manager",
      "Add, remove and update members of this team"
  ),
  CREATE_MANAGE_ANY_ORGANISATION_TEAM(
      "Organisation access manager",
      "Create and edit any organisation team"
  ),
  VIEW_ANY_LICENCE(
      "Application viewer",
      "View any licence and licence applications"
  ),
  VIEW_ORGANISATION_LICENCES(
      "Licence viewer",
      "Can view licences and applications this organisation has access to"
  ),
  APPLICATION_EDITOR(
      "Application editor",
      "Can create, edit and view applications on behalf of this organisation"
  ),
  APPLICATION_SUBMITTER(
      "Application submitter",
      "Can create, edit, submit and view applications on behalf of this organisation"
  ),
  EXTERNAL_APPLICATION_VIEWER(
      "External application viewer",
      "Can view and track this application"
  ),
  EXTERNAL_APPLICATION_EDITOR(
      "External application editor",
          "Can edit this application on behalf of the responsible organisation"
  ),
  OFFLINE_LICENCE_ADMINISTRATOR(
      "Offline licence administrator",
      "Add, remove and update carbon storage, gas storage, exploration and methane drainage licences"
  ),
  SCHEDULE_ADMINISTRATOR(
      "Schedule administrator",
      "Add and update licence schedules"
  ),
  WORK_PROGRAMME_ADMINISTRATOR(
      "Work programme administrator",
      "Add, remove and update work programmes"
  ),
  WORK_PROGRAMME_STATUS_ADMINISTRATOR(
      "Work programme status administrator",
      "Update the status of a work programme activity"
  ),
  LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER(
      "Licence schedule and work programme view",
      "Can view the licence schedule and work programme. Users in any other role automatically have view access"
  ),
  DOCUMENT_TEMPLATE_MANAGER(
      "Document template administrator",
      "Create and edit document templates"
  ),
  CASE_MANAGER_NEW_VENTURES(
      "Case manager (New Ventures)",
      "Receive and manage extension and amendment applications for licences in the initial term"
  ),
  CASE_MANAGER_OPERATIONS(
      "Case manager (Operations)",
      "Receive and manage extension and amendment applications for licences post initial term"
  ),
  STEWARD_NEW_VENTURES(
      "Steward (New Ventures)",
      "Frame, consult and prepare decision support paper for licences in the initial term"
  ),
  STEWARD_OPERATIONS(
      "Steward (Operations)",
      "Frame, consult and prepare decision support paper for licences post initial term"
  ),
  DECISION_ISSUER_NEW_VENTURES(
      "Decision issuer (New Ventures)",
      "Issue the final decision to the applicant for licences in the initial term"
  ),
  DECISION_ISSUER_OPERATIONS(
      "Decision issuer (Operations)",
      "Issue the final decision to the applicant for licences post initial term"
  ),
  CONTINUATION_REVIEWER_NEW_VENTURES(
      "Continuation reviewer (New Ventures)",
      "Review continuation requests for licences in the initial term"
  ),
  CONTINUATION_REVIEWER_OPERATIONS(
      "Continuation reviewer (Operations)",
      "Review continuation requests for licences post initial term"
  ),
  CASE_MANAGER_CS_NEW_VENTURES(
      "Case manager (New Ventures)",
      "Receive and manage extension and amendment applications for licences in appraise phase"
  ),
  CASE_MANAGER_CS_CTS(
      "Case manager (Carbon Transport and Storage)",
      "Receive and manage extension and amendment applications for licences post appraise phase"
  ),
  STEWARD_CS_NEW_VENTURES(
      "Steward (New Ventures)",
      "Frame, consult and prepare decision support paper for licences in appraise phase"
  ),
  STEWARD_CS_CTS(
      "Steward (Carbon Transport and Storage)",
      "Frame, consult and prepare decision support paper for licences post appraise phase"
  ),
  DECISION_ISSUER_CS_NEW_VENTURES(
      "Decision issuer (New Ventures)",
      "Issue the final decision to the applicant for licences in appraise phase"
  ),
  DECISION_ISSUER_CS_CTS(
      "Decision issuer (Carbon Transport and Storage)",
      "Issue the final decision to the applicant for licences post appraise phase"
  ),
  CASE_MANAGER_ONSHORE(
      "Case manager",
      "Receive and manage extension and amendment applications for onshore production licences"
  ),
  STEWARD_ONSHORE(
      "Steward",
      "Frame, consult and prepare decision support paper for onshore production licences"
  ),
  DECISION_ISSUER_ONSHORE(
      "Decision issuer",
      "Issue the final decision to the applicant for onshore production licences"
  ),
  CONTINUATION_ISSUER(
      "Continuation issuer",
      "Issue confirmation of continuation to the applicant for production licences"
  ),
  DECISION_EXECUTOR(
      "Decision executor",
      "Prepare deeds and execute decisions on extensions and work programme amendments for all licence types"
  );

  private final String name;

  private final String description;

  Role(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }
}