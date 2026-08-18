package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;

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

  /**
   * GET is supported alongside POST purely so the migration can be triggered by pasting the URL into a logged in
   * browser, which is the only practical way to reach this endpoint by hand given it sits behind SAML authentication.
   */
  @RequestMapping(value = "/licences", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> migrateLicences() {
    var result = carbonStorageLicenceMigrationService.migrateLicences();
    return ResponseEntity.ok(result.describe("carbon storage licences"));
  }

  /**
   * GET is supported alongside POST for the same reason as {@link #migrateLicences()}.
   */
  @RequestMapping(value = "/schedules", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> migrateSchedules() {
    var result = carbonStorageLicenceMigrationService.migrateSchedules();
    return ResponseEntity.ok(result.describe("carbon storage licence schedules"));
  }

  /**
   * Returned when the migration was triggered before the data it needs is in place. The migration has done nothing, so
   * it can be triggered again once the extract tables are loaded.
   */
  @ExceptionHandler(MigrationPreconditionException.class)
  public ResponseEntity<String> handleMigrationPreconditionException(MigrationPreconditionException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
  }
}
