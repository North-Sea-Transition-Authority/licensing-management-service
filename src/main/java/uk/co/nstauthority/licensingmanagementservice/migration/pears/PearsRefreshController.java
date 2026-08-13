package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduledJobService;

/**
 * Manually triggers the same PEARS licence and responsible organisation refresh that
 * {@link LicenceScheduledJobService#retrieveAndSavePearsLicences()} runs on an hourly schedule, so the data can be
 * updated on demand rather than waiting for the next scheduled run.
 */
@Profile("migration")
@RestController
@RequestMapping("/migration/pears-refresh")
public class PearsRefreshController {

  private final LicenceScheduledJobService licenceScheduledJobService;

  public PearsRefreshController(LicenceScheduledJobService licenceScheduledJobService) {
    this.licenceScheduledJobService = licenceScheduledJobService;
  }

  /**
   * GET is supported alongside POST purely so the refresh can be triggered by pasting the URL into a logged in browser,
   * which is the only practical way to reach this endpoint by hand given it sits behind SAML authentication.
   */
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> refreshPearsLicences() {
    licenceScheduledJobService.retrieveAndSavePearsLicences();
    return ResponseEntity.ok("PEARS licences and responsible organisations refreshed");
  }
}
