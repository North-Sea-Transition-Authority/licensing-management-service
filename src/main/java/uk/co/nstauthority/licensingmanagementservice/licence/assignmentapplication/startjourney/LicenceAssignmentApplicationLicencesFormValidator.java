package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@Service
class LicenceAssignmentApplicationLicencesFormValidator {

  private final LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService;

  LicenceAssignmentApplicationLicencesFormValidator(
      LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService
  ) {
    this.licenceAssignmentApplicationStartJourneyService = licenceAssignmentApplicationStartJourneyService;
  }

  boolean isValid(
      LicenceAssignmentApplicationLicencesForm form,
      Errors errors,
      LicenceType licenceType
  ) {
    var selectedLicences = licenceAssignmentApplicationStartJourneyService.getLicencesOfType(
        form.getLicenceIds(),
        licenceType
    );

    if (selectedLicences.isEmpty()) {
      errors.rejectValue("licenceSelector", "licenceSelector.required", "Add at least one licence");
    }

    return !errors.hasErrors();
  }

}
