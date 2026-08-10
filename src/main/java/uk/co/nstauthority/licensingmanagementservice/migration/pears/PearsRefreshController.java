package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduledJobService;

/**
 * Manually triggers the same PEARS licence and responsible organisation refresh that
 * {@link LicenceScheduledJobService#retrieveAndSavePearsLicences()} runs on an hourly schedule, so the data can be
 * updated on demand rather than waiting for the next scheduled run.
 */
@Profile("migration")
@Controller
@RequestMapping("/migration/pears-refresh")
public class PearsRefreshController {

  private final LicenceScheduledJobService licenceScheduledJobService;

  public PearsRefreshController(LicenceScheduledJobService licenceScheduledJobService) {
    this.licenceScheduledJobService = licenceScheduledJobService;
  }

  @GetMapping
  public ResponseEntity<String> refreshPearsLicences() {
    licenceScheduledJobService.retrieveAndSavePearsLicences();
    return ResponseEntity.ok("PEARS licences and responsible organisations refreshed");
  }
}
