package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PearsScheduleMigrationRunCsvTest {

  @Test
  void csv_whenADryRun_hasAColumnPerResultTheRunHad() {
    var csv = PearsScheduleMigrationRunCsv.csv(List.of(
        licence("P1556", PearsScheduleMigrationQueueStatus.DIFFERS,
            "version 2, Rate: PEARS 2018-12-01, LMS 2018-12-04: Differs by +3 days\r\nversion 2, Expiry: Missing in LMS",
            Map.of("Result: Match", 36, "Result: Differs", 1, "Result: Missing in LMS", 1,
                "Already had a schedule in LMS: compared as it is", 1)),
        licence("P1728", PearsScheduleMigrationQueueStatus.MATCHED, null,
            Map.of("Result: Match", 19, "Default description", 2)),
        licence("P1972", PearsScheduleMigrationQueueStatus.FAILED, "No terms in PEARS", Map.of())
    ));

    assertThat(csv.split("\r\n(?=P|$)")).containsExactly(
        "Licence,Status,Attempts,Outcome,Match,Differs,Missing in LMS,Notes",
        "P1556,DIFFERS,1,\"version 2, Rate: PEARS 2018-12-01, LMS 2018-12-04: Differs by +3 days\r\n"
            + "version 2, Expiry: Missing in LMS\",36,1,1,Already had a schedule in LMS: compared as it is: 1",
        "P1728,MATCHED,1,,19,,,Default description: 2",
        "P1972,FAILED,1,No terms in PEARS,,,,"
    );
  }

  @Test
  void csv_whenAMigration_hasNoResultColumns() {
    var csv = PearsScheduleMigrationRunCsv.csv(List.of(
        licence("P1556", PearsScheduleMigrationQueueStatus.MIGRATED, null, Map.of("Default description", 1))));

    assertThat(csv).isEqualTo("Licence,Status,Attempts,Outcome,Notes\r\nP1556,MIGRATED,1,,Default description: 1\r\n");
  }

  @Test
  void csv_whenAWorkProgrammeDryRun_hasItsResultColumnsToo() {
    var csv = PearsScheduleMigrationRunCsv.csv(List.of(
        licence("P2244", PearsScheduleMigrationQueueStatus.MATCHED, null,
            Map.of("Result: Match", 3, "Result: Mapped", 2, "Result: Not migrated", 1, "Result: Added in LMS", 1))));

    assertThat(csv.split("\r\n")[0])
        .isEqualTo("Licence,Status,Attempts,Outcome,Match,Not migrated,Mapped,Added in LMS,Notes");
  }

  private static PearsScheduleMigrationRunService.RunLicence licence(
      String licenceReference,
      PearsScheduleMigrationQueueStatus status,
      String outcome,
      Map<String, Integer> notes
  ) {
    return new PearsScheduleMigrationRunService.RunLicence(licenceReference, status, 1, outcome, notes);
  }
}
