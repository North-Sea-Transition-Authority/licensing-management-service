package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

class PearsWorkProgrammeMapperTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);
  private static final LocalDateTime SAVED = LocalDateTime.of(2024, Month.MAY, 1, 9, 0);

  private static final MigratedSchedule SCHEDULE = new MigratedSchedule(
      START,
      List.of(new MigratedSchedule.Term(TermType.INITIAL, START, LocalDate.of(2018, Month.NOVEMBER, 30))),
      List.of(new MigratedSchedule.Phase(PhaseType.PHASE_A, START, LocalDate.of(2016, Month.NOVEMBER, 30))),
      List.of()
  );

  @Test
  void map_bringsTogetherEachActivitysContentDateAndHistory() {
    var programme = new PearsWorkProgramme(P2244, 11L, 101L, SAVED, "INITIAL", null, "IN_PROGRESS", null,
        List.of(
            new PearsActivity(1, PearsActivity.ListKind.PHASE, "Phase A", "501", "OBTAIN_3D_SEISMIC",
                "Obtain 3D Seismic Data 1", "COMPLETE", null, "Obtain 200 sq kms 3D seismic data", SAVED),
            new PearsActivity(2, PearsActivity.ListKind.FLAT, null, "502", "SHOOT_3D_SEISMIC", "Shoot 3D Seismic Data 1",
                "NOT_STARTED", null, "None", SAVED)
        ),
        List.of(new PearsNote(1, "GENERAL", null, null, null, SAVED, "Jen Brzozowska", "Reviewed")));

    var mapped = PearsWorkProgrammeMapper.map(P2244, List.of(programme), SCHEDULE);

    assertThat(mapped.licence()).isEqualTo(P2244);
    assertThat(mapped.activities()).singleElement().satisfies(activity -> {
      assertThat(activity.activity().category()).isEqualTo(WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA);
      assertThat(activity.date()).isEqualTo(MappedActivityDate.withinPhase(PhaseType.PHASE_A));
      assertThat(activity.history().statuses()).extracting(MappedActivityHistory.Status::status)
          .containsExactly(MigratedStatus.COMPLETE);
    });
    assertThat(mapped.skipped()).extracting(skipped -> skipped.source().activityId()).containsExactly("502");
    assertThat(mapped.scheduleComments()).extracting(MappedActivityHistory.Comment::text)
        .containsExactly("Reviewed (PEARS note by Jen Brzozowska)");
    assertThat(mapped.notes().noteCountsByReason()).containsKey(PearsActivityMapper.NONE_NOT_MIGRATED);
  }

  @Test
  void map_takesEveryProgrammeOnTheLicence() {
    var initial = programme(11L, "INITIAL", "601");
    var fallow = programme(12L, "FALLOW", "602");

    var mapped = PearsWorkProgrammeMapper.map(P2244, List.of(initial, fallow), SCHEDULE);

    assertThat(mapped.activities()).extracting(activity -> activity.activity().pearsActivityId())
        .containsExactly("601", "602");
    assertThat(mapped.notes().noteCountsByReason()).containsKey(PearsActivityDateMapper.FALLOW_OR_EXTENSION);
  }

  @Test
  void map_whenTheLicenceHasTwoInitialProgrammes_thenItIsNotMigrated() {
    var programmes = List.of(programme(11L, "INITIAL", "701"), programme(12L, "INITIAL", "702"));

    assertThatThrownBy(() -> PearsWorkProgrammeMapper.map(P2244, programmes, SCHEDULE))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .extracting(exception -> ((PearsWorkProgrammeMappingException) exception).reason())
        .isEqualTo(PearsWorkProgrammeMapper.TWO_INITIAL_PROGRAMMES);
  }

  private static PearsWorkProgramme programme(long pedWpId, String type, String activityId) {
    return new PearsWorkProgramme(P2244, pedWpId, pedWpId * 10, SAVED, type, null, "NOT_STARTED", null,
        List.of(new PearsActivity(1, PearsActivity.ListKind.FLAT, null, activityId, "OTHER", "Other Activity 1",
            "NOT_STARTED", null, "Studies", SAVED)),
        List.of());
  }
}
