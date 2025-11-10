package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;

@Service
public class LicenceScheduleSupportingInformationFormValidator {

  private final LicenceScheduleExtensionService licenceScheduleExtensionService;
  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public LicenceScheduleSupportingInformationFormValidator(
      LicenceScheduleExtensionService licenceScheduleExtensionService,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService
  ) {
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  public boolean isValid(
      BindingResult bindingResult,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    ValidationUtils.rejectIfEmptyOrWhitespace(
        bindingResult,
        "reasonForAmendment",
        "reasonForAmendment.empty",
        "Enter reason for requesting amendment or extension");

    ValidationUtils.rejectIfEmptyOrWhitespace(
        bindingResult,
        "licenceProgress",
        "licenceProgress.empty",
        "Enter the progress on the licence work programme to date");


    if (licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)
        || licenceWorkProgrammeAmendmentService.isAmendmentRequested(scheduleWorkProgrammeApplicationDetail)) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          bindingResult,
          "planDuringExtension",
          "planDuringExtension.empty",
          "Enter what you plan to do during the period of extension");
    }

    ValidationUtils.rejectIfEmptyOrWhitespace(
        bindingResult,
        "impactOnDeliverables",
        "impactOnDeliverables.empty",
        "Enter how the requested changes impact current or future deliverables");

    return !bindingResult.hasErrors();
  }
}