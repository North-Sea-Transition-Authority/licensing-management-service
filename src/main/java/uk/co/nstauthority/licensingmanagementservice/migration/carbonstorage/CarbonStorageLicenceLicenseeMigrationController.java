package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Profile("migration")
@Controller
@RequestMapping("/migration/carbon-storage-licence-licensee")
public class CarbonStorageLicenceLicenseeMigrationController {

  private final CarbonStorageLicenceLicenseeMigrationService carbonStorageLicenceLicenseeMigrationService;

  public CarbonStorageLicenceLicenseeMigrationController(
      CarbonStorageLicenceLicenseeMigrationService carbonStorageLicenceLicenseeMigrationService
  ) {
    this.carbonStorageLicenceLicenseeMigrationService = carbonStorageLicenceLicenseeMigrationService;
  }

  @GetMapping
  public ResponseEntity<String> migrate() {
    carbonStorageLicenceLicenseeMigrationService.migrate();
    return ResponseEntity.ok("migrated");
  }
}
