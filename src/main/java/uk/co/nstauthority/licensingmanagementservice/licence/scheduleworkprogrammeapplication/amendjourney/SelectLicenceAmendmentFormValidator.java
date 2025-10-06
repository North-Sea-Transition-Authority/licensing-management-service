package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectLicenceAmendmentFormValidator {

  LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository;

  public SelectLicenceAmendmentFormValidator(
      LicenceWorkProgrammeAmendmentRepository licenceWorkProgrammeAmendmentRepository) {
    this.licenceWorkProgrammeAmendmentRepository = licenceWorkProgrammeAmendmentRepository;
  }

  boolean isValid(SelectLicenceAmendmentForm form, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "selectedWorkProgrammeActivityAmendmentId",
        "selectedWorkProgrammeActivityAmendmentId.required",
        "Select a work programme activity to amend");


    if (licenceWorkProgrammeAmendmentRepository
        .existsByWorkProgrammeActivityId(form.getSelectedWorkProgrammeActivityAmendmentId())) {
      errors.rejectValue("selectedWorkProgrammeActivityAmendmentId",
          "selectedWorkProgrammeActivityAmendmentId.required",
          "Selected work programme activity is already being amended");
    }

    return !errors.hasErrors();
  }
}