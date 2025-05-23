package uk.co.nstauthority.licensingmanagementservice.teams;

// TODO XYZ - Replace with your roles
public enum Role {
  CREATE_MANAGE_ANY_ORGANISATION_TEAM("Organisation access manager", "Create and edit any organisation team"),
  MANAGE_TEAM("Access manager", "Add, remove and update members of this team"),
  VIEW_ANY_APPLICATION("Application viewer", "View any application"),
  VIEW_APPLICATION("Application viewer", "View applications"),
  EDIT_APPLICATION("Application editor", "Edit applications");

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
