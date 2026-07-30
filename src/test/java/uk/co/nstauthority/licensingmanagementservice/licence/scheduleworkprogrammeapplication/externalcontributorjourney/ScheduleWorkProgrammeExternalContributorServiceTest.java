package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeExternalContributorServiceTest {

  @Mock
  private ScheduleWorkProgrammeExternalContributorRequestRepository repository;

  @Mock
  private ExternalContributorService externalContributorService;

  @InjectMocks
  private ScheduleWorkProgrammeExternalContributorService scheduleWorkProgrammeExternalContributorService;

  @Captor
  private ArgumentCaptor<ScheduleWorkProgrammeExternalContributorRequest> requestArgumentCaptor;

  private ScheduleWorkProgrammeApplication application;
  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    application = new ScheduleWorkProgrammeApplication();
    application.setId(UUID.randomUUID());

    applicationDetail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());
    applicationDetail.setScheduleWorkProgrammeApplication(application);
  }

  @Test
  void saveExternalContributorForm_persistsAnswerAgainstApplication() {
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(true);

    when(repository.findByScheduleWorkProgrammeApplication(application))
        .thenReturn(Optional.of(new ScheduleWorkProgrammeExternalContributorRequest()));

    scheduleWorkProgrammeExternalContributorService.saveExternalContributorForm(form, applicationDetail);

    verify(repository).save(requestArgumentCaptor.capture());

    var savedRequest = requestArgumentCaptor.getValue();
    assertThat(savedRequest.getAddExternalContributors()).isTrue();
    assertThat(savedRequest.getScheduleWorkProgrammeApplication()).isEqualTo(application);

    verify(externalContributorService, never()).clearExternalContributors(any(TeamScopeReference.class));
  }

  @Test
  void saveExternalContributorForm_whenAnswerIsNo_clearsExistingContributors() {
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(false);

    when(repository.findByScheduleWorkProgrammeApplication(application))
        .thenReturn(Optional.of(new ScheduleWorkProgrammeExternalContributorRequest()));

    scheduleWorkProgrammeExternalContributorService.saveExternalContributorForm(form, applicationDetail);

    verify(externalContributorService).clearExternalContributors(any(TeamScopeReference.class));
  }

  @Test
  void getExternalContributorForm_whenExists_returnsMappedForm() {
    var request = new ScheduleWorkProgrammeExternalContributorRequest();
    request.setAddExternalContributors(false);

    when(repository.findByScheduleWorkProgrammeApplication(application)).thenReturn(Optional.of(request));

    var result = scheduleWorkProgrammeExternalContributorService.getExternalContributorForm(applicationDetail);

    assertThat(result.getAddExternalContributors()).isFalse();
  }

  @Test
  void getExternalContributorForm_whenNotExists_returnsEmptyForm() {
    when(repository.findByScheduleWorkProgrammeApplication(application)).thenReturn(Optional.empty());

    var result = scheduleWorkProgrammeExternalContributorService.getExternalContributorForm(applicationDetail);

    assertThat(result).isNotNull();
    assertThat(result.getAddExternalContributors()).isNull();
  }

  @Test
  void isExternalContributorSectionComplete_delegatesWithStoredAnswer() {
    var request = new ScheduleWorkProgrammeExternalContributorRequest();
    request.setAddExternalContributors(true);

    when(repository.findByScheduleWorkProgrammeApplication(application)).thenReturn(Optional.of(request));
    when(externalContributorService.isSectionComplete(eq(true), any(TeamScopeReference.class))).thenReturn(true);

    assertThat(scheduleWorkProgrammeExternalContributorService
        .isExternalContributorSectionComplete(applicationDetail)).isTrue();
  }

  @Test
  void isExternalContributorSectionComplete_whenNoRequest_delegatesNullAnswer() {
    when(repository.findByScheduleWorkProgrammeApplication(application)).thenReturn(Optional.empty());
    when(externalContributorService.isSectionComplete(isNull(), any(TeamScopeReference.class))).thenReturn(false);

    assertThat(scheduleWorkProgrammeExternalContributorService
        .isExternalContributorSectionComplete(applicationDetail)).isFalse();
  }

  @Test
  void getExternalContributorsTeam_delegatesToSharedService() {
    var team = new Team(UUID.randomUUID());

    when(externalContributorService.getExternalContributorsTeam(any(TeamScopeReference.class))).thenReturn(team);

    assertThat(scheduleWorkProgrammeExternalContributorService.getExternalContributorsTeam(applicationDetail))
        .isEqualTo(team);
  }
}
