package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.fivium.energyportalapi.generated.types.Subarea;
import uk.co.nstauthority.licensingmanagementservice.energyportal.subarea.SubareaQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationLicenceOperatorsService {

  // The Energy Portal records some subareas against this placeholder organisation rather than leaving the operator empty
  static final String NO_OPERATOR_ORGANISATION_UNIT_NAME = "NO OPERATOR";

  private final LicenceContinuationLicenceOperatorsRepository licenceContinuationLicenceOperatorsRepository;
  private final SubareaQueryService subareaQueryService;

  public LicenceContinuationLicenceOperatorsService(
      LicenceContinuationLicenceOperatorsRepository licenceContinuationLicenceOperatorsRepository,
      SubareaQueryService subareaQueryService
  ) {
    this.licenceContinuationLicenceOperatorsRepository = licenceContinuationLicenceOperatorsRepository;
    this.subareaQueryService = subareaQueryService;
  }

  public List<Subarea> getSubareasForApplication(LicenceContinuationApplicationDetail detail) {
    var licenceId = detail.getLicence().getId();
    return subareaQueryService.searchSubareasByLicenceIds(List.of(licenceId));
  }

  public boolean hasMissingOperators(List<Subarea> subareas) {
    return subareas.stream()
        .map(Subarea::getOperator)
        .anyMatch(this::isMissingOperator);
  }

  private boolean isMissingOperator(OrganisationUnit operator) {
    return operator == null
        || NO_OPERATOR_ORGANISATION_UNIT_NAME.equals(operator.getName());
  }

  @Transactional
  public void saveLicenceContinuationLicenceOperatorsForm(
      LicenceContinuationLicenceOperatorsForm form,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    var request = licenceContinuationLicenceOperatorsRepository.findByLicenceContinuationApplicationDetail(
        applicationDetail
    ).orElse(new LicenceContinuationLicenceOperatorsRequest());

    request.setLicenceContinuationApplicationDetail(applicationDetail);
    request.setPendingActionsExplanation(form.getPendingActionsExplanation());

    licenceContinuationLicenceOperatorsRepository.save(request);
  }

  public LicenceContinuationLicenceOperatorsForm getLicenceContinuationLicenceOperatorsForm(
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    return licenceContinuationLicenceOperatorsRepository
        .findByLicenceContinuationApplicationDetail(applicationDetail)
        .map(this::requestToForm)
        .orElseGet(LicenceContinuationLicenceOperatorsForm::new);
  }

  private LicenceContinuationLicenceOperatorsForm requestToForm(
      LicenceContinuationLicenceOperatorsRequest request
  ) {
    var form = new LicenceContinuationLicenceOperatorsForm();
    form.setPendingActionsExplanation(request.getPendingActionsExplanation());
    return form;
  }

}