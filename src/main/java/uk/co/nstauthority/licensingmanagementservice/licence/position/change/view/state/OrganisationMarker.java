package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

public enum OrganisationMarker {

  NO_BENEFICIAL_INTEREST("No beneficial interest", "govuk-tag--yellow"),
  NOT_A_LICENSEE("Not a licensee", "govuk-tag--yellow");

  private final String label;
  private final String tagClass;

  OrganisationMarker(String label, String tagClass) {
    this.label = label;
    this.tagClass = tagClass;
  }

  public String getLabel() {
    return label;
  }

  public String getTagClass() {
    return tagClass;
  }
}