package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@Service
public class SelectLicenceFormValidator {

  private final LicenceService licenceService;
  private final LicenceScheduleService licenceScheduleService;

  public SelectLicenceFormValidator(LicenceService licenceService,
                                    LicenceScheduleService licenceScheduleService
  ) {
    this.licenceService = licenceService;
    this.licenceScheduleService = licenceScheduleService;
  }

  boolean isValid(SelectLicenceForm form, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "licenceId", "licenceId.required");

    if (StringUtils.isNotBlank(form.getLicenceId())) {
      var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getLicenceId()));

      if (licenceScheduleService.doesLicenceScheduleExistForLicence(licence)) {
        errors.rejectValue("licenceId", "licenceId.invalid", "A schedule already exists for the selected licence");
      }
    }

    return !errors.hasErrors();
  }

}
