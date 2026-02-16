package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class SelectLicenceAmendmentFormValidator {

  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public SelectLicenceAmendmentFormValidator(
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService
  ) {
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  boolean isValid(
      SelectLicenceAmendmentForm form,
      Errors errors,
      ScheduleWorkProgrammeApplicationDetail detail
  ) {
    ValidationUtils.rejectIfEmpty(errors, "selectedWorkProgrammeActivityAmendmentId",
        "selectedWorkProgrammeActivityAmendmentId.required",
        "Select a work programme activity to amend");

    if (form.getSelectedWorkProgrammeActivityAmendmentId() == null) {
      return !errors.hasErrors();
    }

    if (isAlreadyBeingAmended(form, detail)) {
      errors.rejectValue("selectedWorkProgrammeActivityAmendmentId",
          "selectedWorkProgrammeActivityAmendmentId.required",
          "Selected work programme activity is already being amended");
    }

    return !errors.hasErrors();
  }

  private boolean isAlreadyBeingAmended(
      SelectLicenceAmendmentForm form,
      ScheduleWorkProgrammeApplicationDetail detail
  ) {
    return licenceWorkProgrammeAmendmentService.existsByWorkProgrammeActivityIdAndSwpApplicationDetail(
        form.getSelectedWorkProgrammeActivityAmendmentId(),
        detail
    );
  }
}