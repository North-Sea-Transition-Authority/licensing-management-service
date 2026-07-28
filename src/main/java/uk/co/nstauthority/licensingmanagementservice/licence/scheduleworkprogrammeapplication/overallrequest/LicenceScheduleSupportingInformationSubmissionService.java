package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleSupportingInformationSubmissionService {
  private final LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;
  private final LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator;

  public LicenceScheduleSupportingInformationSubmissionService(
      LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService,
      LicenceScheduleSupportingInformationFormValidator licenceScheduleSupportingInformationFormValidator
  ) {
    this.licenceScheduleSupportingInformationService = licenceScheduleSupportingInformationService;
    this.licenceScheduleSupportingInformationFormValidator = licenceScheduleSupportingInformationFormValidator;
  }

  public boolean isSectionSubmittable(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    var form = licenceScheduleSupportingInformationService.getLicenceScheduleRequestForm(
        scheduleWorkProgrammeApplicationDetail);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceScheduleSupportingInformationFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail);
  }
}