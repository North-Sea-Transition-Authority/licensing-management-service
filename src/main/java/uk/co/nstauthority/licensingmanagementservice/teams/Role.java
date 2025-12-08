package uk.co.nstauthority.licensingmanagementservice.teams;

public enum Role {
  CREATE_MANAGE_ANY_ORGANISATION_TEAM("Organisation access manager", "Create and edit any organisation team"),
  MANAGE_TEAM("Access manager", "Add, remove and update members of this team"),
  VIEW_ANY_LICENCE("Application viewer", "View any licence and licence applications"),
  VIEW_ORGANISATION_LICENCES("Licence viewer", "Can view licences and applications this organisation has access to"),
  APPLICATION_EDITOR("Application editor", "Can view and edit applications on behalf of this organisation"),
  APPLICATION_SUBMITTER("Application submitter", "Can submit applications on behalf of this organisation"),
  EXTERNAL_APPLICATION_VIEWER("External application viewer", "Can view and track this application"),
  EXTERNAL_APPLICATION_EDITOR(
      "External application editor",
          "Can edit this application on behalf of the responsible organisation"
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