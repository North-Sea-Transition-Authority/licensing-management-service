package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;

@Service
public class ScheduleWorkProgrammeExternalContributorService {

  private final ScheduleWorkProgrammeExternalContributorRequestRepository repository;
  private final ExternalContributorService externalContributorService;

  public ScheduleWorkProgrammeExternalContributorService(
      ScheduleWorkProgrammeExternalContributorRequestRepository repository,
      ExternalContributorService externalContributorService
  ) {
    this.repository = repository;
    this.externalContributorService = externalContributorService;
  }

  @Transactional
  public void saveExternalContributorForm(
      ExternalContributorForm form,
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var application = applicationDetail.getScheduleWorkProgrammeApplication();
    var request = repository.findByScheduleWorkProgrammeApplication(application)
        .orElseGet(ScheduleWorkProgrammeExternalContributorRequest::new);

    request.setScheduleWorkProgrammeApplication(application);
    request.setAddExternalContributors(form.getAddExternalContributors());

    repository.save(request);

    if (BooleanUtils.isFalse(form.getAddExternalContributors())) {
      externalContributorService.clearExternalContributors(scopeReference(applicationDetail));
    }
  }

  public ExternalContributorForm getExternalContributorForm(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var form = new ExternalContributorForm();
    getExternalContributorRequest(applicationDetail)
        .ifPresent(request -> form.setAddExternalContributors(request.getAddExternalContributors()));
    return form;
  }

  public Optional<ScheduleWorkProgrammeExternalContributorRequest> getExternalContributorRequest(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    return repository.findByScheduleWorkProgrammeApplication(applicationDetail.getScheduleWorkProgrammeApplication());
  }

  public boolean isExternalContributorSectionComplete(
      ScheduleWorkProgrammeApplicationDetail applicationDetail
  ) {
    var addExternalContributors = getExternalContributorRequest(applicationDetail)
        .map(ScheduleWorkProgrammeExternalContributorRequest::getAddExternalContributors)
        .orElse(null);

    return externalContributorService.isSectionComplete(addExternalContributors, scopeReference(applicationDetail));
  }

  public Team getExternalContributorsTeam(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return externalContributorService.getExternalContributorsTeam(scopeReference(applicationDetail));
  }

  private TeamScopeReference scopeReference(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return TeamScopeReference.from(
        applicationDetail.getScheduleWorkProgrammeApplication().getId().toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name()
    );
  }
}
