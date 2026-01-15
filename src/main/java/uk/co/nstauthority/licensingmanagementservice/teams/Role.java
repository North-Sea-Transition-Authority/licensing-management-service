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
  LICENCE_SCHEDULE_WORK_PROGRAMME_VIEWER(
      "Licence schedule and work programme view",
      "Can view the licence schedule and work programme. Users in any other role automatically have view access"
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