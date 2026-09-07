package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;

/**
 * Replays one licence's PEARS history into this application on demand, so a licence can be rebuilt
 * without waiting on a release or a whole-service migration run.
 *
 * <p>The endpoint only exists where the PEARS datasource is configured, since that is what the
 * history is read out of.
 */
@Component
@Endpoint(id = "pears-licence-writeback")
@ConditionalOnPearsDataSource
class PearsLicenceWritebackEndpoint {

  private final LicenceService licenceService;
  private final LicenceWritebackService licenceWritebackService;

  PearsLicenceWritebackEndpoint(LicenceService licenceService, LicenceWritebackService licenceWritebackService) {
    this.licenceService = licenceService;
    this.licenceWritebackService = licenceWritebackService;
  }

  /**
   * Deletes the licence's positions and rebuilds them out of PEARS.
   *
   * @param licenceReference the licence to rebuild, for example {@code P1}, matched case insensitively
   */
  @WriteOperation(produces = MediaType.APPLICATION_JSON_VALUE)
  LicenceWritebackResult overwriteLicencePositionsFromPears(String licenceReference) {
    var licence = licenceService.findByLicenceReferenceOrThrow(normalise(licenceReference));
    return licenceWritebackService.overwriteLicencePositionsFromPears(licence);
  }

  /**
   * Licence references are held upper case, and this endpoint is reached by hand, so a reference
   * typed as {@code p1} should find the same licence as one typed as {@code P1}.
   */
  private static String normalise(String licenceReference) {
    if (StringUtils.isBlank(licenceReference)) {
      throw new IllegalArgumentException("A licence reference is required");
    }
    return licenceReference.strip().toUpperCase();
  }
}
