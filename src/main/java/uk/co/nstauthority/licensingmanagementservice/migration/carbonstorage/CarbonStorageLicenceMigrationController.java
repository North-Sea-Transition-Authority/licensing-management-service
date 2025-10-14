package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Profile("migration")
@Controller
@RequestMapping("/migration/carbon-storage-licences")
public class CarbonStorageLicenceMigrationController {

  private final CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService;

  public CarbonStorageLicenceMigrationController(
      CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService
  ) {
    this.carbonStorageLicenceMigrationService = carbonStorageLicenceMigrationService;
  }

  @GetMapping
  public ResponseEntity<String> migrate() {
    carbonStorageLicenceMigrationService.migrate();
    return ResponseEntity.ok("migrated");
  }
}
