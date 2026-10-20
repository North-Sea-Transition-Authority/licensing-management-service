package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleExtensionSubmissionService {
  private final LicenceScheduleExtensionService licenceScheduleExtensionFormService;
  private final LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  public LicenceScheduleExtensionSubmissionService(
      LicenceScheduleExtensionService licenceScheduleExtensionFormService,
      LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator) {
    this.licenceScheduleExtensionFormService = licenceScheduleExtensionFormService;
    this.licenceScheduleExtensionFormValidator = licenceScheduleExtensionFormValidator;
  }

  public boolean isSectionSubmittable(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    // The form and the validator both need the extendable terms and phases, so they share one set of views
    var extensionRequestViews = licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(
        scheduleWorkProgrammeApplicationDetail,
        licenceScheduleDetail
    );
    var form = licenceScheduleExtensionFormService.buildLicenceScheduleExtensionForm(extensionRequestViews);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceScheduleExtensionFormValidator.isValidAgainstExtensionViews(form, bindingResult, extensionRequestViews);
  }
}