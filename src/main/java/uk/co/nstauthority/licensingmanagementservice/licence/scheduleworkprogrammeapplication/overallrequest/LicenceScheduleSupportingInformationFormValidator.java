package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.file.FileValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleSupportingInformationFormValidator {

  private final LicenceScheduleSupportingInformationHelperService  licenceScheduleSupportingInformationHelperService;

  public LicenceScheduleSupportingInformationFormValidator(
      LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService
  ) {
    this.licenceScheduleSupportingInformationHelperService = licenceScheduleSupportingInformationHelperService;
  }

  public boolean isValid(
      LicenceScheduleSupportingInformationForm form,
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


    if (licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(scheduleWorkProgrammeApplicationDetail)) {
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

    FileValidationUtil.validator()
        .withMandatoryDescriptions(true)
        .withMinimumNumberOfFiles(0, "")
        .validate(bindingResult, form.getDocuments(), "documents");

    return !bindingResult.hasErrors();
  }
}