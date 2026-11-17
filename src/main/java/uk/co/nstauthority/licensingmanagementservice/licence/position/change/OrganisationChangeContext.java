package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

public interface OrganisationChangeContext {
  Integer currentId();

  Integer previousId();

  String currentName();

  String previousName();
}
