package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class WorkProgrammeActivityFormValidator {

  LicenceStartDateService licenceStartDateService;

  public WorkProgrammeActivityFormValidator(LicenceStartDateService licenceStartDateService) {
    this.licenceStartDateService = licenceStartDateService;
  }

  boolean isValid(
      WorkProgrammeActivityForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "workProgrammeActivityCategory",
        "workProgrammeActivityCategory.required",
        "Select a category"
    );

    if (form.getWorkProgrammeActivityCategory() != null
        && form.getWorkProgrammeActivityCategory().equals(WorkProgrammeActivityCategory.OTHER_ACTIVITY)) {
      ValidationUtils.rejectIfEmpty(
          errors,
          "otherCategoryName",
          "otherCategoryName.required",
          "Enter a category"
      );
    }

    ValidationUtils.rejectIfEmpty(
        errors,
        "description",
        "description.required",
        "Enter the description of the activity"
    );

    ValidationUtils.rejectIfEmpty(
        errors,
        "workProgrammeActivityCommitment",
        "workProgrammeActivityCommitment.required",
        "Select the commitment of the activity"
    );

    ValidationUtils.rejectIfEmpty(
        errors,
        "workProgrammeActivityDateOption",
        "workProgrammeActivityDateOption.required",
        "Select an option"
    );

    if (form.getWorkProgrammeActivityDateOption() != null) {
      if (form.getWorkProgrammeActivityDateOption().equals(WorkProgrammeActivityDateOption.FIXED_DATE)) {
        var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

        ThreeFieldDateInputValidator.builder()
            .emptyInputErrorMessage("Provide the due date")
            .mustBeAfterDate(licenceStartDate.getStartDate())
            .mustBeAfterDateErrorMessage("The due date must be after the licence start date")
            .validate(form.getDueDateInput(), errors);
      }

      if (form.getWorkProgrammeActivityDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceScheduleTermId",
            "licenceScheduleTermId.required",
            "Select a term"
        );
      }

      if (form.getWorkProgrammeActivityDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceSchedulePhaseId",
            "licenceSchedulePhaseId.required",
            "Select a phase"
        );
      }
    }

    return !errors.hasErrors();
  }
}
