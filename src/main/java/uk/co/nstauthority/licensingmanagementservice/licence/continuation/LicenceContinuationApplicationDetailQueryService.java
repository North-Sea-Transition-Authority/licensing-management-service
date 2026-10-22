package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.application.LatestApplicationDetailSummary;

/**
 * Read-only lookups over {@link LicenceContinuationApplicationDetail}, kept free of any dependency on
 * {@link LicenceContinuationService} so other services can query application details without risking a
 * circular dependency with anything that service itself depends on.
 */
@Service
public class LicenceContinuationApplicationDetailQueryService {

  private final LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  public LicenceContinuationApplicationDetailQueryService(
      LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository
  ) {
    this.licenceContinuationApplicationDetailRepository = licenceContinuationApplicationDetailRepository;
  }

  public Optional<LicenceContinuationApplicationDetail> getLatestByApplication(
      LicenceContinuationApplication licenceContinuationApplication
  ) {
    return licenceContinuationApplicationDetailRepository
        .findFirstByLicenceContinuationApplicationOrderByVersionNumberDesc(licenceContinuationApplication);
  }

  public List<LatestApplicationDetailSummary> getLatestSummariesByApplicationIds(
      Collection<UUID> licenceContinuationApplicationIds
  ) {
    return licenceContinuationApplicationDetailRepository
        .findLatestSummariesByLicenceContinuationApplication_IdIn(licenceContinuationApplicationIds);
  }
}
