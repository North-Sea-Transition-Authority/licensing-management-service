package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;

@Service
public class LicenceContinuationExternalContributorService {

  private final LicenceContinuationExternalContributorRepository licenceContinuationExternalContributorRepository;
  private final ExternalContributorService externalContributorService;

  public LicenceContinuationExternalContributorService(
      LicenceContinuationExternalContributorRepository licenceContinuationExternalContributorRepository,
      ExternalContributorService externalContributorService
  ) {
    this.licenceContinuationExternalContributorRepository = licenceContinuationExternalContributorRepository;
    this.externalContributorService = externalContributorService;
  }

  @Transactional
  public void saveExternalContributorForm(
      ExternalContributorForm form,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    var request =
        licenceContinuationExternalContributorRepository.findByLicenceContinuationApplicationDetail(applicationDetail)
            .orElse(new LicenceContinuationExternalContributorRequest());

    request.setLicenceContinuationApplicationDetail(applicationDetail);
    request.setAddExternalContributors(form.getAddExternalContributors());

    licenceContinuationExternalContributorRepository.save(request);

    if (BooleanUtils.isFalse(form.getAddExternalContributors())) {
      externalContributorService.clearExternalContributors(scopeReference(applicationDetail));
    }
  }

  public ExternalContributorForm getExternalContributorForm(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var form = new ExternalContributorForm();
    getExternalContributorRequest(licenceContinuationApplicationDetail)
        .ifPresent(request -> form.setAddExternalContributors(request.getAddExternalContributors()));
    return form;
  }

  public Optional<LicenceContinuationExternalContributorRequest> getExternalContributorRequest(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return licenceContinuationExternalContributorRepository
        .findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail);
  }

  public boolean isExternalContributorSectionComplete(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var addExternalContributors = getExternalContributorRequest(licenceContinuationApplicationDetail)
        .map(LicenceContinuationExternalContributorRequest::getAddExternalContributors)
        .orElse(null);

    return externalContributorService.isSectionComplete(
        addExternalContributors,
        scopeReference(licenceContinuationApplicationDetail)
    );
  }

  public Team getExternalContributorsTeam(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    return externalContributorService.getExternalContributorsTeam(scopeReference(licenceContinuationApplicationDetail));
  }

  private TeamScopeReference scopeReference(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    return TeamScopeReference.from(
        licenceContinuationApplicationDetail.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION.name()
    );
  }
}
