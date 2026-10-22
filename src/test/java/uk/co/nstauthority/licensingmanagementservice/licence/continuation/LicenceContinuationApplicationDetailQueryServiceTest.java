package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationDetailQueryServiceTest {

  @Mock
  private LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  @InjectMocks
  private LicenceContinuationApplicationDetailQueryService licenceContinuationApplicationDetailQueryService;

  @Test
  void getLatestByApplication() {
    var application = new LicenceContinuationApplication();
    application.setId(UUID.randomUUID());

    var applicationDetail = new LicenceContinuationApplicationDetail();
    applicationDetail.setId(UUID.randomUUID());

    when(licenceContinuationApplicationDetailRepository
        .findFirstByLicenceContinuationApplicationOrderByVersionNumberDesc(application))
        .thenReturn(Optional.of(applicationDetail));

    var result = licenceContinuationApplicationDetailQueryService.getLatestByApplication(application);

    assertThat(result).contains(applicationDetail);
  }

  @Test
  void getLatestSummariesByApplicationIds() {
    var applicationId = UUID.randomUUID();

    var summary = new LatestApplicationDetailSummary(applicationId, UUID.randomUUID(), ApplicationStatus.SUBMITTED, "LMS/CA/2040/1");

    when(licenceContinuationApplicationDetailRepository.findLatestSummariesByLicenceContinuationApplication_IdIn(Set.of(applicationId)))
        .thenReturn(List.of(summary));

    var result = licenceContinuationApplicationDetailQueryService.getLatestSummariesByApplicationIds(Set.of(applicationId));

    assertThat(result).containsExactly(summary);
  }
}
