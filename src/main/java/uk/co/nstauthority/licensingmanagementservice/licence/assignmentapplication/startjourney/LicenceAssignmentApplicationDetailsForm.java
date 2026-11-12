package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class LicenceAssignmentApplicationDetailsForm {

  private Integer organisationUnitId;

  private final StringInput applicationReference = new StringInput(
      "applicationReference",
      "an application reference"
  );

  private final ThreeFieldDateInput plannedDate = new ThreeFieldDateInput(
      "plannedDate",
      "a planned completion date"
  );

  public Integer getOrganisationUnitId() {
    return organisationUnitId;
  }

  public void setOrganisationUnitId(Integer organisationUnitId) {
    this.organisationUnitId = organisationUnitId;
  }

  public StringInput getApplicationReference() {
    return applicationReference;
  }

  public ThreeFieldDateInput getPlannedDate() {
    return plannedDate;
  }
}
