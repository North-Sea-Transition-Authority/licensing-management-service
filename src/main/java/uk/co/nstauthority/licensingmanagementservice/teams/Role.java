package uk.co.nstauthority.licensingmanagementservice.teams;

import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhaseGated;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;

public enum Role implements PhaseGated {

  MANAGE_TEAM(
      "Access manager",
      "Add, remove and update members of this team",
      ReleaseFeature.TEAM_ROLE
  ),
  CREATE_MANAGE_ANY_ORGANISATION_TEAM(
      "Organisation access manager",
      "Create and edit any organisation team",
      ReleaseFeature.TEAM_ROLE
  ),
  VIEW_ANY_LICENCE(
      "Application viewer",
      "View any licence and licence applications",
      ReleaseFeature.TEAM_ROLE
  ),
  VIEW_ORGANISATION_LICENCES(
      "Licence viewer",
      "Can view licences this organisation is an active licensee on",
      ReleaseFeature.INDUSTRY_LICENCE_AND_APPLICATION_ROLE
  ),
  APPLICATION_EDITOR(
      "Application editor",
      "Can create, edit and view applications on behalf of this organisation",
      ReleaseFeature.INDUSTRY_LICENCE_AND_APPLICATION_ROLE
  ),
  APPLICATION_SUBMITTER(
      "Application submitter",
      "Can create, edit, submit and view applications on behalf of this organisation",
      ReleaseFeature.INDUSTRY_LICENCE_AND_APPLICATION_ROLE
  ),

  LICENSEE_CONTACTS_MANAGER(
      "Licensee contacts manager",
      "Can add and update the contact email for each licensee this organisation is responsible for",
      ReleaseFeature.TEAM_ROLE
  ),
  EXTERNAL_APPLICATION_VIEWER(
      "External application viewer",
      "Can view and track this application",
      ReleaseFeature.TEAM_ROLE
  ),
  EXTERNAL_APPLICATION_EDITOR(
      "External application editor",
      "Can edit this application on behalf of the responsible organisation",
      ReleaseFeature.TEAM_ROLE
  ),
  OFFLINE_LICENCE_ADMINISTRATOR(
      "Offline licence administrator",
      "Add, remove and update carbon storage, gas storage, exploration and methane drainage licences",
      ReleaseFeature.TEAM_ROLE
  ),
  SCHEDULE_ADMINISTRATOR(
      "Schedule administrator",
      "Add and update licence schedules",
      ReleaseFeature.TEAM_ROLE
  ),
  WORK_PROGRAMME_ADMINISTRATOR(
      "Work programme administrator",
      "Add, remove and update work programme activities",
      ReleaseFeature.TEAM_ROLE
  ),
  WORK_PROGRAMME_STATUS_ADMINISTRATOR(
      "Work programme status administrator",
      "Update the status of a work programme activity",
      ReleaseFeature.TEAM_ROLE
  ),
  LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER(
      "Licence schedule and work programme view",
      "Can view the licence schedule and work programme. Users in any other role automatically have view access",
      ReleaseFeature.TEAM_ROLE
  ),
  DOCUMENT_TEMPLATE_MANAGER(
      "Document template administrator",
      "Create and edit document templates",
      ReleaseFeature.TEAM_ROLE
  ),
  LICENCE_CONTACTS_MANAGER(
      "Licence contacts manager",
      "Can add and update the contact email for any licensee on any licence",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_NEW_VENTURES(
      "Case manager (New Ventures)",
      "Receive and manage extension and amendment applications for licences in the initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_OPERATIONS(
      "Case manager (Operations)",
      "Receive and manage extension and amendment applications for licences post initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_NEW_VENTURES(
      "Steward (New Ventures)",
      "Frame, consult and prepare decision support paper for licences in the initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_OPERATIONS(
      "Steward (Operations)",
      "Frame, consult and prepare decision support paper for licences post initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_NEW_VENTURES(
      "Decision issuer (New Ventures)",
      "Issue the final decision to the applicant for licences in the initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_OPERATIONS(
      "Decision issuer (Operations)",
      "Issue the final decision to the applicant for licences post initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  CONTINUATION_REVIEWER_NEW_VENTURES(
      "Continuation reviewer (New Ventures)",
      "Review continuation requests for licences in the initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  CONTINUATION_REVIEWER_OPERATIONS(
      "Continuation reviewer (Operations)",
      "Review continuation requests for licences post initial term",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_CS_NEW_VENTURES(
      "Case manager (New Ventures)",
      "Receive and manage extension and amendment applications for licences in appraise phase",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_CS_CTS(
      "Case manager (Carbon Transport and Storage)",
      "Receive and manage extension and amendment applications for licences post appraise phase",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_CS_NEW_VENTURES(
      "Steward (New Ventures)",
      "Frame, consult and prepare decision support paper for licences in appraise phase",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_CS_CTS(
      "Steward (Carbon Transport and Storage)",
      "Frame, consult and prepare decision support paper for licences post appraise phase",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_CS_NEW_VENTURES(
      "Decision issuer (New Ventures)",
      "Issue the final decision to the applicant for licences in appraise phase",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_CS_CTS(
      "Decision issuer (Carbon Transport and Storage)",
      "Issue the final decision to the applicant for licences post appraise phase",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_ONSHORE(
      "Case manager",
      "Receive and manage extension and amendment applications for onshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_ONSHORE(
      "Steward",
      "Frame, consult and prepare decision support paper for onshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_ONSHORE(
      "Decision issuer",
      "Issue the final decision to the applicant for onshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CONTINUATION_ISSUER(
      "Continuation issuer",
      "Issue confirmation of continuation to the applicant for production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_EXECUTOR(
      "Decision executor",
      "Prepare deeds and execute decisions on extensions and work programme amendments for all licence types",
      ReleaseFeature.TEAM_ROLE
  );

  private final String name;

  private final String description;

  private final ReleaseFeature releaseFeature;

  Role(String name, String description, ReleaseFeature releaseFeature) {
    this.name = name;
    this.description = description;
    this.releaseFeature = releaseFeature;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return releaseFeature;
  }
}
