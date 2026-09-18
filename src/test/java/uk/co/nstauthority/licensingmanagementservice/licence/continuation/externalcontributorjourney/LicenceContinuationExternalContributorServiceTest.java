package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationExternalContributorServiceTest {

  @Mock
  private LicenceContinuationExternalContributorRepository licenceContinuationExternalContributorRepository;

  @Mock
  private ExternalContributorService externalContributorService;

  @InjectMocks
  private LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;

  @Captor
  private ArgumentCaptor<LicenceContinuationExternalContributorRequest> requestArgumentCaptor;

  @Captor
  private ArgumentCaptor<TeamScopeReference> scopeReferenceArgumentCaptor;

  @Test
  void saveExternalContributorForm_persistsAnswer() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();
    var application = applicationDetail.getLicenceContinuationApplication();
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(true);

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplication(application))
        .thenReturn(Optional.of(new LicenceContinuationExternalContributorRequest()));

    licenceContinuationExternalContributorService.saveExternalContributorForm(form, applicationDetail);

    verify(licenceContinuationExternalContributorRepository).save(requestArgumentCaptor.capture());

    var savedRequest = requestArgumentCaptor.getValue();
    assertThat(savedRequest.getAddExternalContributors()).isTrue();
    assertThat(savedRequest.getLicenceContinuationApplication()).isEqualTo(application);

    verify(externalContributorService, never()).clearExternalContributors(any(TeamScopeReference.class));
  }

  @Test
  void saveExternalContributorForm_whenAnswerIsNo_clearsExistingContributors() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();
    var application = applicationDetail.getLicenceContinuationApplication();
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(false);

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplication(application))
        .thenReturn(Optional.of(new LicenceContinuationExternalContributorRequest()));

    licenceContinuationExternalContributorService.saveExternalContributorForm(form, applicationDetail);

    verify(externalContributorService).clearExternalContributors(scopeReferenceArgumentCaptor.capture());

    var scopeReference = scopeReferenceArgumentCaptor.getValue();
    assertThat(scopeReference.getId()).isEqualTo(application.getId().toString());
    assertThat(scopeReference.getType()).isEqualTo(ApplicationType.CONTINUATION_APPLICATION.name());
  }

  @Test
  void getExternalContributorForm_whenExists_returnsMappedForm() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();
    var request = new LicenceContinuationExternalContributorRequest();
    request.setAddExternalContributors(false);

    when(licenceContinuationExternalContributorRepository
        .findByLicenceContinuationApplication(applicationDetail.getLicenceContinuationApplication()))
        .thenReturn(Optional.of(request));

    var result = licenceContinuationExternalContributorService.getExternalContributorForm(applicationDetail);

    assertThat(result.getAddExternalContributors()).isFalse();
  }

  @Test
  void getExternalContributorForm_whenNotExists_returnsEmptyForm() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();

    when(licenceContinuationExternalContributorRepository
        .findByLicenceContinuationApplication(applicationDetail.getLicenceContinuationApplication()))
        .thenReturn(Optional.empty());

    var result = licenceContinuationExternalContributorService.getExternalContributorForm(applicationDetail);

    assertThat(result).isNotNull();
    assertThat(result.getAddExternalContributors()).isNull();
  }

  @Test
  void isExternalContributorSectionComplete_delegatesWithStoredAnswer() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();
    var request = new LicenceContinuationExternalContributorRequest();
    request.setAddExternalContributors(true);

    when(licenceContinuationExternalContributorRepository
        .findByLicenceContinuationApplication(applicationDetail.getLicenceContinuationApplication()))
        .thenReturn(Optional.of(request));
    when(externalContributorService.isSectionComplete(eq(true), any(TeamScopeReference.class)))
        .thenReturn(true);

    assertThat(licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(applicationDetail)).isTrue();
  }

  @Test
  void isExternalContributorSectionComplete_whenNoRequest_delegatesNullAnswer() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();

    when(licenceContinuationExternalContributorRepository
        .findByLicenceContinuationApplication(applicationDetail.getLicenceContinuationApplication()))
        .thenReturn(Optional.empty());
    when(externalContributorService.isSectionComplete(isNull(), any(TeamScopeReference.class)))
        .thenReturn(false);

    assertThat(licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(applicationDetail)).isFalse();
  }

  @Test
  void getExternalContributorsTeam_scopesTeamToTheApplication() {
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder().build();
    var application = applicationDetail.getLicenceContinuationApplication();
    var team = new Team(UUID.randomUUID());

    when(externalContributorService.getExternalContributorsTeam(any(TeamScopeReference.class)))
        .thenReturn(team);

    assertThat(licenceContinuationExternalContributorService.getExternalContributorsTeam(applicationDetail))
        .isEqualTo(team);

    verify(externalContributorService).getExternalContributorsTeam(scopeReferenceArgumentCaptor.capture());

    var scopeReference = scopeReferenceArgumentCaptor.getValue();
    assertThat(scopeReference.getId()).isEqualTo(application.getId().toString());
    assertThat(scopeReference.getType()).isEqualTo(ApplicationType.CONTINUATION_APPLICATION.name());
  }
}
