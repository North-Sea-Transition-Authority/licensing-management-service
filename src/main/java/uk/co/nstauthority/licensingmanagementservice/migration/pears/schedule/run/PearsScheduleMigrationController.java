package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.annotation.Nullable;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;

/**
 * Starts and follows runs of the PEARS schedule migration, which {@link PearsScheduleMigrationJob} works through in
 * the background.
 *
 * <p>Starting a run accepts GET as well as POST, so it can be triggered by pasting the URL into a logged in browser,
 * which is the only practical way to reach it by hand given it sits behind SAML authentication. Retrying, cancelling
 * and clearing down are POST only.</p>
 */
@Profile("migration")
@ConditionalOnPearsDataSource
@RestController
@RequestMapping("/migration/pears-schedules")
public class PearsScheduleMigrationController extends PearsMigrationRunController {

  private final PearsScheduleMigrationService migrationService;
  private final PearsScheduleMigrationRunService runService;
  private final PearsScheduleCleardownService cleardownService;

  public PearsScheduleMigrationController(
      PearsScheduleMigrationService migrationService,
      PearsScheduleMigrationRunService runService,
      PearsScheduleCleardownService cleardownService,
      @Value("${pears.schedule-migration.cleardown-enabled}") boolean cleardownEnabled
  ) {
    super(runService, PearsMigration.SCHEDULE, "pears-schedule-run", cleardownEnabled);
    this.migrationService = migrationService;
    this.runService = runService;
    this.cleardownService = cleardownService;
  }

  /**
   * Queues a run of every licence with a PEARS schedule, or of just the one given.
   */
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<StartedRun> startRun(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    var licenceReferences = licencesWithAPearsSchedule(licenceReference);
    return started(runService.startRun(licenceReferences, user.wuaId()), licenceReferences.size());
  }

  /**
   * Queues a dry run of every licence with a PEARS schedule, or of just the one given: each is migrated and compared
   * with PEARS as a run would, then rolled back, so nothing in LMS changes. Follow it, and read the licences with issues,
   * at the usual status endpoint. GET as well as POST, as it changes nothing.
   */
  @RequestMapping(value = "/validate", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<StartedRun> startValidation(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    var licenceReferences = licencesWithAPearsSchedule(licenceReference);
    return started(runService.startValidation(licenceReferences, user.wuaId()), licenceReferences.size());
  }

  private List<String> licencesWithAPearsSchedule(@Nullable String licenceReference) {
    return licencesToQueue(
        migrationService.licencesToMigrate().stream().map(PearsLicenceReference::licenceReference).toList(),
        licenceReference,
        "PEARS holds no schedule for the licence"
    );
  }

  /**
   * Queues a cleardown of every schedule the migration created, or of just the given licence's, so the migration can
   * be run again. Not found unless {@code pears.schedule-migration.cleardown-enabled} is set, which it never is in
   * production.
   */
  @PostMapping("/cleardown")
  public ResponseEntity<StartedRun> startCleardown(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    failUnlessCleardownEnabled();
    var licenceReferences = licencesToQueue(cleardownService.licencesToClear(), licenceReference,
        licenceReference == null
            ? "No schedules from the PEARS migration to clear"
            : "The licence has no schedule from the PEARS migration");
    return started(runService.startCleardown(licenceReferences, user.wuaId()), licenceReferences.size());
  }
}
