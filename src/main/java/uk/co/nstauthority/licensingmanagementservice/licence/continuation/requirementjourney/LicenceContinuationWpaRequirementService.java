package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationWpaRequirementService {

  private final LicenceContinuationWpaRequirementRepository licenceContinuationWpaRequirementRepository;

  public LicenceContinuationWpaRequirementService(
      LicenceContinuationWpaRequirementRepository licenceContinuationWpaRequirementRepository
  ) {
    this.licenceContinuationWpaRequirementRepository = licenceContinuationWpaRequirementRepository;
  }

  @Transactional
  public void saveLicenceContinuationWorkProgrammeActivitiesRequirementForm(
      LicenceContinuationWpaRequirementForm form,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    var wpaRequirementRequest =
        licenceContinuationWpaRequirementRepository.findByLicenceContinuationApplicationDetail(applicationDetail)
            .orElse(new LicenceContinuationWpaRequirementRequest());

    wpaRequirementRequest.setWorkProgrammeActivitiesCompletionStatus(form.getWorkProgrammeActivitiesCompletionStatus());
    wpaRequirementRequest.setLicenceContinuationApplicationDetail(applicationDetail);

    if (BooleanUtils.isTrue(form.getWorkProgrammeActivitiesCompletionStatus())) {
      wpaRequirementRequest.setActionsToCompleteWorkProgrammeActivities(null);
      wpaRequirementRequest.setFurtherInformation(form.getFurtherInformation());
    } else {
      wpaRequirementRequest.setActionsToCompleteWorkProgrammeActivities(form.getActionsToCompleteWorkProgrammeActivities());
      wpaRequirementRequest.setFurtherInformation(null);
    }

    licenceContinuationWpaRequirementRepository.save(wpaRequirementRequest);
  }

  public LicenceContinuationWpaRequirementForm licenceContinuationWorkProgrammeActivitiesRequirementRequestToForm(
      LicenceContinuationWpaRequirementRequest request
  ) {
    LicenceContinuationWpaRequirementForm form = new LicenceContinuationWpaRequirementForm();
    form.setWorkProgrammeActivitiesCompletionStatus(request.getWorkProgrammeActivitiesCompletionStatus());
    form.setActionsToCompleteWorkProgrammeActivities(request.getActionsToCompleteWorkProgrammeActivities());
    form.setFurtherInformation(request.getFurtherInformation());
    return form;
  }

  public LicenceContinuationWpaRequirementForm getLicenceContinuationWorkProgrammeActivitiesRequirementForm(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return  licenceContinuationWpaRequirementRepository.findByLicenceContinuationApplicationDetail(
        licenceContinuationApplicationDetail
        )
       .map(this::licenceContinuationWorkProgrammeActivitiesRequirementRequestToForm)
       .orElse(new LicenceContinuationWpaRequirementForm());
  }

  public Optional<LicenceContinuationWpaRequirementRequest> getWorkProgrammeActivitiesRequirementRequest(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return licenceContinuationWpaRequirementRepository
        .findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail);
  }
}
