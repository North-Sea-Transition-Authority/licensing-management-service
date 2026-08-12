package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("migration")
@RestController
@RequestMapping("/migration/carbon-storage-licences")
public class CarbonStorageLicenceMigrationController {

  private final CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService;

  public CarbonStorageLicenceMigrationController(
      CarbonStorageLicenceMigrationService carbonStorageLicenceMigrationService
  ) {
    this.carbonStorageLicenceMigrationService = carbonStorageLicenceMigrationService;
  }

  @PostMapping("/licences")
  public ResponseEntity<String> migrateLicences() {
    carbonStorageLicenceMigrationService.migrateLicences();
    return ResponseEntity.ok("licences migrated");
  }

  @PostMapping("/schedules")
  public ResponseEntity<String> migrateSchedules() {
    carbonStorageLicenceMigrationService.migrateSchedules();
    return ResponseEntity.ok("schedules migrated");
  }
}
