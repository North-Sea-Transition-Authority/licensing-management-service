package uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
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

  @Test
  void saveExternalContributorForm_persistsAnswer() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var form = new ExternalContributorForm();
    form.setAddExternalContributors(true);

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationExternalContributorRequest()));

    licenceContinuationExternalContributorService.saveExternalContributorForm(form, applicationDetail);

    verify(licenceContinuationExternalContributorRepository).save(requestArgumentCaptor.capture());

    var savedRequest = requestArgumentCaptor.getValue();
    assertThat(savedRequest.getAddExternalContributors()).isTrue();
    assertThat(savedRequest.getLicenceContinuationApplicationDetail()).isEqualTo(applicationDetail);
  }

  @Test
  void getExternalContributorForm_whenExists_returnsMappedForm() {
    var applicationDetail = new LicenceContinuationApplicationDetail();
    var request = new LicenceContinuationExternalContributorRequest();
    request.setAddExternalContributors(false);

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(request));

    var result = licenceContinuationExternalContributorService.getExternalContributorForm(applicationDetail);

    assertThat(result.getAddExternalContributors()).isFalse();
  }

  @Test
  void getExternalContributorForm_whenNotExists_returnsEmptyForm() {
    var applicationDetail = new LicenceContinuationApplicationDetail();

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());

    var result = licenceContinuationExternalContributorService.getExternalContributorForm(applicationDetail);

    assertThat(result).isNotNull();
    assertThat(result.getAddExternalContributors()).isNull();
  }

  @Test
  void isExternalContributorSectionComplete_delegatesWithStoredAnswer() {
    var applicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    var request = new LicenceContinuationExternalContributorRequest();
    request.setAddExternalContributors(true);

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.of(request));
    when(externalContributorService.isSectionComplete(eq(true), any(TeamScopeReference.class)))
        .thenReturn(true);

    assertThat(licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(applicationDetail)).isTrue();
  }

  @Test
  void isExternalContributorSectionComplete_whenNoRequest_delegatesNullAnswer() {
    var applicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());

    when(licenceContinuationExternalContributorRepository.findByLicenceContinuationApplicationDetail(applicationDetail))
        .thenReturn(Optional.empty());
    when(externalContributorService.isSectionComplete(isNull(), any(TeamScopeReference.class)))
        .thenReturn(false);

    assertThat(licenceContinuationExternalContributorService
        .isExternalContributorSectionComplete(applicationDetail)).isFalse();
  }

  @Test
  void getExternalContributorsTeam_delegatesToSharedService() {
    var applicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    var team = new Team(UUID.randomUUID());

    when(externalContributorService.getExternalContributorsTeam(any(TeamScopeReference.class)))
        .thenReturn(team);

    assertThat(licenceContinuationExternalContributorService.getExternalContributorsTeam(applicationDetail))
        .isEqualTo(team);
  }
}
