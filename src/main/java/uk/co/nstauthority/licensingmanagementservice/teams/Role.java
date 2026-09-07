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
  PRODUCTION_LICENCE_CORRECTOR(
      "Production licence corrector",
      "Grants permission to make corrections to historical data on Production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CARBON_STORAGE_LICENCE_CORRECTOR(
      "Carbon Storage licence corrector",
      "Grants permission to make corrections to historical data on Carbon Storage licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_OFFSHORE(
      "Case manager (Offshore production)",
      "Receive and manage extension and amendment applications for offshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_OFFSHORE(
      "Steward (Offshore production)",
      "Frame, consult and prepare decision support paper for offshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_OFFSHORE(
      "Decision issuer (Offshore production)",
      "Issue the final decision to the applicant for offshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CONTINUATION_REVIEWER_OFFSHORE(
      "Continuation reviewer (Offshore production)",
      "Review continuation requests for offshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_CARBON_STORAGE(
      "Case manager (Carbon storage)",
      "Receive and manage extension and amendment applications for carbon storage licences",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_CARBON_STORAGE(
      "Steward (Carbon storage)",
      "Frame, consult and prepare decision support paper for carbon storage licences",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_CARBON_STORAGE(
      "Decision issuer (Carbon storage)",
      "Issue the final decision to the applicant for carbon storage licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CASE_MANAGER_ONSHORE(
      "Case manager (Onshore production)",
      "Receive and manage extension and amendment applications for onshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  STEWARD_ONSHORE(
      "Steward (Onshore production)",
      "Frame, consult and prepare decision support paper for onshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  DECISION_ISSUER_ONSHORE(
      "Decision issuer (Onshore production)",
      "Issue the final decision to the applicant for onshore production licences",
      ReleaseFeature.TEAM_ROLE
  ),
  CONTINUATION_REVIEWER_ONSHORE(
      "Continuation reviewer (Onshore production)",
      "Review continuation requests for onshore production licences",
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
