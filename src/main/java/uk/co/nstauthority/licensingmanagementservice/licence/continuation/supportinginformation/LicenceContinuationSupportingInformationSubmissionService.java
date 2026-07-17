package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationSupportingInformationSubmissionService {

  private final LicenceContinuationSupportingInformationService licenceContinuationSupportingInformationService;
  private final LicenceContinuationSupportingInformationValidator licenceContinuationSupportingInformationValidator;

  public LicenceContinuationSupportingInformationSubmissionService(
      LicenceContinuationSupportingInformationService licenceContinuationSupportingInformationService,
      LicenceContinuationSupportingInformationValidator licenceContinuationSupportingInformationValidator
  ) {
    this.licenceContinuationSupportingInformationService = licenceContinuationSupportingInformationService;
    this.licenceContinuationSupportingInformationValidator = licenceContinuationSupportingInformationValidator;
  }

  public boolean isSectionSubmittable(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    var form = licenceContinuationSupportingInformationService.getSupportingInformationForm(
        licenceContinuationApplicationDetail
    );

    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

    return licenceContinuationSupportingInformationValidator.isValid(form, bindingResult);
  }
}
