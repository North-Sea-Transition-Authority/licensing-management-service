package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationLicenceOperatorsSubmissionService {

  private final LicenceContinuationLicenceOperatorsService licenceContinuationLicenceOperatorsService;
  private final LicenceContinuationLicenceOperatorsValidator licenceContinuationLicenceOperatorsValidator;

  public LicenceContinuationLicenceOperatorsSubmissionService(
      LicenceContinuationLicenceOperatorsService licenceContinuationLicenceOperatorsService,
      LicenceContinuationLicenceOperatorsValidator licenceContinuationLicenceOperatorsValidator
  ) {
    this.licenceContinuationLicenceOperatorsService = licenceContinuationLicenceOperatorsService;
    this.licenceContinuationLicenceOperatorsValidator = licenceContinuationLicenceOperatorsValidator;
  }

  public boolean isSectionSubmittable(LicenceContinuationApplicationDetail applicationDetail) {
    var subareas = licenceContinuationLicenceOperatorsService.getSubareasForApplication(applicationDetail);
    boolean isMissingOperators = licenceContinuationLicenceOperatorsService.hasMissingOperators(subareas);

    var form = licenceContinuationLicenceOperatorsService.getLicenceContinuationLicenceOperatorsForm(
        applicationDetail);

    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceContinuationLicenceOperatorsValidator.isValid(bindingResult, isMissingOperators
    );
  }
}