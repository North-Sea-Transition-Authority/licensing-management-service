package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceWorkProgrammeAmendmentSubmissionService {
  private final LicenceWorkProgrammeAmendmentFormService licenceWorkProgrammeAmendmentFormService;
  private final LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  public LicenceWorkProgrammeAmendmentSubmissionService(
      LicenceWorkProgrammeAmendmentFormService licenceWorkProgrammeAmendmentFormService,
      LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator) {
    this.licenceWorkProgrammeAmendmentFormService = licenceWorkProgrammeAmendmentFormService;
    this.licenceWorkProgrammeAmendmentFormValidator = licenceWorkProgrammeAmendmentFormValidator;
  }

  public boolean isAmendmentSectionSubmittable(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    var form = licenceWorkProgrammeAmendmentFormService.getLicenceWorkProgrammeActivityAmendmentForm(
        scheduleWorkProgrammeApplicationDetail);
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult);
  }
}