package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.mvc.RequestScopedCache;

/**
 * Holds continuation application details loaded by id for the lifetime of a single HTTP request, so the access rules
 * and the controller's argument resolver share one load of the detail named in the URL. Used internally by
 * {@link LicenceContinuationService}; go through that service rather than using this directly.
 */
@Component
public class LicenceContinuationApplicationDetailRequestCache
    extends RequestScopedCache<UUID, Optional<LicenceContinuationApplicationDetail>> {

  private final LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  public LicenceContinuationApplicationDetailRequestCache(
      LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository
  ) {
    this.licenceContinuationApplicationDetailRepository = licenceContinuationApplicationDetailRepository;
  }

  public Optional<LicenceContinuationApplicationDetail> findById(UUID detailId) {
    return get(detailId);
  }

  @Override
  protected Optional<LicenceContinuationApplicationDetail> load(UUID detailId) {
    return licenceContinuationApplicationDetailRepository.findById(detailId);
  }
}
