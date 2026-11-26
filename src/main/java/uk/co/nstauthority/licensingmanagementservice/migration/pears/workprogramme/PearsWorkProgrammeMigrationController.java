package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

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
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsMigration;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsMigrationRunController;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run.PearsScheduleMigrationRunService;

/**
 * Starts and follows runs of the PEARS work programme migration, which the schedule migration's job works through in
 * the background, one run at a time across both migrations (plan, design section 4). A run's status, report, retry and
 * cancel work as for schedules.
 *
 * <p>Starting a run accepts GET as well as POST, so it can be triggered by pasting the URL into a logged in browser.
 * Retrying, cancelling and clearing down are POST only.</p>
 */
@Profile("migration")
@ConditionalOnPearsDataSource
@RestController
@RequestMapping("/migration/pears-work-programmes")
public class PearsWorkProgrammeMigrationController extends PearsMigrationRunController {

  private final PearsWorkProgrammeMigrationService migrationService;
  private final PearsScheduleMigrationRunService runService;
  private final PearsWorkProgrammeCleardownService cleardownService;

  public PearsWorkProgrammeMigrationController(
      PearsWorkProgrammeMigrationService migrationService,
      PearsScheduleMigrationRunService runService,
      PearsWorkProgrammeCleardownService cleardownService,
      @Value("${pears.schedule-migration.cleardown-enabled}") boolean cleardownEnabled
  ) {
    super(runService, PearsMigration.WORK_PROGRAMME, "pears-work-programme-run", cleardownEnabled);
    this.migrationService = migrationService;
    this.runService = runService;
    this.cleardownService = cleardownService;
  }

  /**
   * Queues a run of every licence with a PEARS work programme, or of just the one given.
   */
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<StartedRun> startRun(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    var licenceReferences = licencesWithAPearsWorkProgramme(licenceReference);
    return started(runService.startWorkProgrammeRun(licenceReferences, user.wuaId()), licenceReferences.size());
  }

  /**
   * Queues a dry run of every licence with a PEARS work programme, or of just the one given: each is migrated onto its
   * migrated schedule and compared with PEARS as a run would, then rolled back, so nothing in LMS changes. Follow it,
   * and read the licences with issues, at the usual status endpoint. GET as well as POST, as it changes nothing.
   */
  @RequestMapping(value = "/validate", method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<StartedRun> startValidation(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    var licenceReferences = licencesWithAPearsWorkProgramme(licenceReference);
    return started(runService.startWorkProgrammeValidation(licenceReferences, user.wuaId()), licenceReferences.size());
  }

  private List<String> licencesWithAPearsWorkProgramme(@Nullable String licenceReference) {
    return licencesToQueue(
        migrationService.licencesToMigrate().stream().map(PearsLicenceReference::licenceReference).toList(),
        licenceReference,
        "PEARS holds no work programme for the licence"
    );
  }

  /**
   * Queues a cleardown of every work programme the migration wrote, or of just the given licence's, so the migration
   * can be run again. The schedules stay. Not found unless {@code pears.schedule-migration.cleardown-enabled} is set
   * (the same switch as the schedule cleardown), which it never is in production.
   */
  @PostMapping("/cleardown")
  public ResponseEntity<StartedRun> startCleardown(
      @RequestParam(required = false) String licenceReference,
      ServiceUserDetail user
  ) {
    failUnlessCleardownEnabled();
    var licenceReferences = licencesToQueue(cleardownService.licencesToClear(), licenceReference,
        licenceReference == null
            ? "No work programmes from the PEARS migration to clear"
            : "The licence has no work programme from the PEARS migration");
    return started(runService.startWorkProgrammeCleardown(licenceReferences, user.wuaId()),
        licenceReferences.size());
  }
}
