package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleExtensionSubmissionService {
  private final LicenceScheduleExtensionFormService licenceScheduleExtensionFormService;
  private final LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  public LicenceScheduleExtensionSubmissionService(
      LicenceScheduleExtensionFormService licenceScheduleExtensionFormService,
      LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator) {
    this.licenceScheduleExtensionFormService = licenceScheduleExtensionFormService;
    this.licenceScheduleExtensionFormValidator = licenceScheduleExtensionFormValidator;
  }

  public boolean isSectionSubmittable(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    var form = licenceScheduleExtensionFormService.getLicenceScheduleExtensionForm(
        scheduleWorkProgrammeApplicationDetail);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceScheduleExtensionFormValidator.isValid(form, bindingResult);
  }
}