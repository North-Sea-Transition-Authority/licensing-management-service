package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

class PearsActivityMapperTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  @ParameterizedTest(name = "{0}")
  @MethodSource("typedActivities")
  void activities_mapsEachTypeToItsCategoryAndCommitment(
      String activityType,
      WorkProgrammeActivityCategory category,
      WorkProgrammeActivityCommitment commitment
  ) {
    var mapped = mapOne(activity(activityType, "Default title 1", "One item of work"));

    assertThat(mapped.category()).isEqualTo(category);
    assertThat(mapped.commitment()).isEqualTo(commitment);
    assertThat(mapped.otherCategoryName()).isNull();
  }

  static Stream<Arguments> typedActivities() {
    return Stream.of(
        Arguments.of("FIRM_WELL", WorkProgrammeActivityCategory.DRILL_WELL, WorkProgrammeActivityCommitment.FIRM),
        Arguments.of("CONTINGENT_WELL", WorkProgrammeActivityCategory.DRILL_WELL,
            WorkProgrammeActivityCommitment.CONTINGENT),
        Arguments.of("DRILL_OR_DROP_WELL", WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL,
            WorkProgrammeActivityCommitment.CONDITIONAL),
        Arguments.of("SHOOT_2D_SEISMIC", WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONDITIONAL),
        Arguments.of("SHOOT_2D_SEISMIC_FIRM", WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.FIRM),
        Arguments.of("SHOOT_2D_SEISMIC_CONTINGENT", WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONTINGENT),
        Arguments.of("SHOOT_3D_SEISMIC", WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONDITIONAL),
        Arguments.of("SHOOT_3D_SEISMIC_FIRM", WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.FIRM),
        Arguments.of("SHOOT_3D_SEISMIC_CONTINGENT", WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONTINGENT),
        Arguments.of("OBTAIN_2D_SEISMIC", WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONDITIONAL),
        Arguments.of("OBTAIN_3D_SEISMIC", WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONDITIONAL),
        Arguments.of("REPROCESS_2D_SEISMIC", WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONDITIONAL),
        Arguments.of("REPROCESS_3D_SEISMIC", WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA,
            WorkProgrammeActivityCommitment.CONDITIONAL)
    );
  }

  @Test
  void activities_whenPearsHoldsNoCommitment_thenConditionalAndNoted() {
    var notes = new MigrationNotes();

    PearsActivityMapper.activities(programme(
        activity("FIRM_WELL", "Firm Well 1", "One firm well"),
        activity("DRILL_OR_DROP_WELL", "Drill or Drop Well 1", "One drill or drop well"),
        activity("OTHER", "Other Activity 1", "Studies")
    ), notes);

    assertThat(notes.noteCountsByReason()).containsEntry(PearsActivityMapper.NO_COMMITMENT, 2);
  }

  @Test
  void activities_whenTheTypeHoldsNoCommitment_thenATitleSayingContingentGivesIt() {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(
        activity("OBTAIN_3D_SEISMIC", "Contingent Obtain 3D Seismic Data 1", "Obtain 200 sq kms 3D seismic data"),
        activity("REPROCESS_3D_SEISMIC", "CONTINGENT Reprocess 3D Seismic Data 1", "Reprocess 3D seismic data"),
        activity("OTHER", "Other Activity 3 - CONTINGENT COMMITMENT", "Complete a rock physics study"),
        activity("OBTAIN_2D_SEISMIC", "Obtain 2D Seismic Data 1", "Obtain 500 kms 2D seismic data")
    ), notes).activities();

    assertThat(mapped).extracting(MappedActivity::commitment).containsExactly(
        WorkProgrammeActivityCommitment.CONTINGENT,
        WorkProgrammeActivityCommitment.CONTINGENT,
        WorkProgrammeActivityCommitment.CONTINGENT,
        WorkProgrammeActivityCommitment.CONDITIONAL
    );
    assertThat(mapped.get(2).description())
        .isEqualTo("Other Activity 3 - CONTINGENT COMMITMENT - Complete a rock physics study");
    assertThat(notes.noteCountsByReason())
        .containsEntry(PearsActivityMapper.COMMITMENT_FROM_TITLE, 3)
        .containsEntry(PearsActivityMapper.NO_COMMITMENT, 1);
  }

  @Test
  void activities_whenTheTitleSaysNotContingentOnAContingentType_thenTheTypeIsKeptAndTheTitleShown() {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(activity("SHOOT_3D_SEISMIC_CONTINGENT",
        "Shoot 3D Not-Contingent Seismic Data 1", "Shoot 300 sq kms 3D seismic data")), notes).activities().getFirst();

    assertThat(mapped.commitment()).isEqualTo(WorkProgrammeActivityCommitment.CONTINGENT);
    assertThat(mapped.description())
        .isEqualTo("Shoot 3D Not-Contingent Seismic Data 1 - Shoot 300 sq kms 3D seismic data");
    assertThat(notes.noteCountsByReason()).containsEntry(PearsActivityMapper.TITLE_CONTRADICTS_COMMITMENT, 1);
  }

  @Test
  void activities_legacyIsAnOtherActivityWithItsObligationsTitleBeforeTheDescription() {
    var mapped = mapOne(activity("LEGACY", "Legacy Obligations: Original blocks 20/3f. Competed 1",
        "(T) D/D to 3505m to the shallower of Ettrick or Buzzard sst."));

    assertThat(mapped.category()).isEqualTo(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    assertThat(mapped.otherCategoryName()).isEqualTo(PearsActivityMapper.LEGACY_NAME);
    assertThat(mapped.commitment()).isEqualTo(WorkProgrammeActivityCommitment.CONDITIONAL);
    assertThat(mapped.description()).isEqualTo(
        "Legacy Obligations: Original blocks 20/3f. Competed 1 - (T) D/D to 3505m to the shallower of Ettrick or Buzzard sst.");
    assertThat(mapped.stub()).isFalse();
  }

  @Test
  void activities_legacyProgrammeLabelIsNotPutBeforeTheDescription() {
    var mapped = mapOne(activity("LEGACY", "Legacy - Not Fallow 2010", "Not Fallow - Block is Field"));

    assertThat(mapped.description()).isEqualTo("Not Fallow - Block is Field");
  }

  @Test
  void activities_otherUsesTheFixedNameAndPutsAHandTypedTitleBeforeTheDescription() {
    var mapped = PearsActivityMapper.activities(programme(
        activity("OTHER", "Other Activity 2", "Complete regional studies"),
        activity("OTHER", "Geological Studies", "Complete studies in accordance with the application")
    ), new MigrationNotes()).activities();

    assertThat(mapped).extracting(MappedActivity::otherCategoryName)
        .containsOnly(PearsActivityMapper.OTHER_NAME);
    assertThat(mapped).extracting(MappedActivity::description).containsExactly(
        "Complete regional studies",
        "Geological Studies - Complete studies in accordance with the application"
    );
  }

  @Test
  void activities_defaultTitlesOfOtherTypesAreNotPutBeforeTheDescription() {
    var mapped = mapOne(activity("FIRM_WELL", "Firm Well 1", "One Firm well to 2000m into the Rotliegend"));

    assertThat(mapped.description()).isEqualTo("One Firm well to 2000m into the Rotliegend");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("seismicTitles")
  void activities_seismicWithAStandardTitleIsClassifiedByTheTitle(String title, WorkProgrammeActivityCategory category) {
    // The description says otherwise, to show the title decides.
    var mapped = mapOne(activity("SEISMIC", title, "Reprocess 100 sq kms of seismic data"));

    assertThat(mapped.category()).isEqualTo(category);
  }

  static Stream<Arguments> seismicTitles() {
    return Stream.of(
        Arguments.of("Shoot 2D Seismic Data 1", WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA),
        Arguments.of("Shoot 3D Seismic Data 2", WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA),
        Arguments.of("Obtain 2D Seismic Data 1", WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA),
        Arguments.of("Obtain 3D Seismic Data 4", WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA),
        Arguments.of("Reprocess 2D Seismic Data 1", WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA),
        Arguments.of("Reprocess 3D Seismic Data 3", WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA)
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("genericSeismicDescriptions")
  void activities_seismicWithAGenericTitleIsClassifiedByTheDescription(
      String description,
      WorkProgrammeActivityCategory category
  ) {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(activity("SEISMIC", "Seismic Data 1", description)), notes)
        .activities().getFirst();

    assertThat(mapped.category()).isEqualTo(category);
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityMapper.SEISMIC_FROM_DESCRIPTION);
  }

  static Stream<Arguments> genericSeismicDescriptions() {
    return Stream.of(
        Arguments.of("Shoot 140 kms 2D Geostreamer seismic data;", WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA),
        Arguments.of("Shoot 1036 sq kms Long Offset 3D seismic data;",
            WorkProgrammeActivityCategory.NEW_SHOOT_3_D_SEISMIC_DATA),
        Arguments.of("Obtain 100 kms 2D Long Offset seismic data;",
            WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA),
        Arguments.of("Obtain 90 sq kms 3D seismic data;", WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA),
        Arguments.of("Obtain 130 sq kms of Long Offset 3D seismic data; Reprocess 850 sq kms of 3D seismic data",
            WorkProgrammeActivityCategory.REPROCESS_SEISMIC_DATA)
    );
  }

  @Test
  void activities_seismicTheDescriptionDoesNotClassifyIsAnOtherActivity() {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(
        activity("SEISMIC", "Seismic Data 2", "Carry out a 3D seismic inversion of 50 sq kms 3d seismic data;")
    ), notes).activities().getFirst();

    assertThat(mapped.category()).isEqualTo(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    assertThat(mapped.otherCategoryName()).isEqualTo(PearsActivityMapper.SEISMIC_INVERSION_NAME);
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityMapper.SEISMIC_UNCLASSIFIED);
  }

  @Test
  void activities_seismicDescribingTwoOperationsIsOneActivityAndNoted() {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(
        activity("SEISMIC", "Obtain 3D Seismic Data 1", "Obtain and Reprocess 200 sq kms 3D seismic data")
    ), notes).activities();

    assertThat(mapped).singleElement().extracting(MappedActivity::category)
        .isEqualTo(WorkProgrammeActivityCategory.OBTAIN_EXISTING_3_D_SEISMIC_DATA);
    assertThat(notes.noteCountsByReason()).containsEntry(PearsActivityMapper.SEISMIC_TWO_OPERATIONS, 1);
  }

  @Test
  void activities_titleWithAPastedLinkHasTheLinkRemoved() {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(activity("SEISMIC",
        "Obtain 2D Seismic Data 1javascript:ss('action-add-new-3d-seismic','x')", "Obtain 500 kms 2D seismic data")
    ), notes).activities().getFirst();

    assertThat(mapped.category()).isEqualTo(WorkProgrammeActivityCategory.OBTAIN_EXISTING_2_D_SEISMIC_DATA);
    assertThat(mapped.description()).isEqualTo("Obtain 500 kms 2D seismic data");
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityMapper.TITLE_LINK_REMOVED);
  }

  @Test
  void activities_reprocessKeepsItsDimensionWhereTheDescriptionDoesNotSayIt() {
    var mapped = PearsActivityMapper.activities(programme(
        activity("REPROCESS_3D_SEISMIC", "Reprocess 3D Seismic Data 1", "Reprocess 93 sq kms seismic data"),
        activity("REPROCESS_2D_SEISMIC", "Reprocess 2D Seismic Data 1", "Reprocess 400 kms 2D seismic data"),
        activity("SEISMIC", "Reprocess 2D Seismic Data 2", "Reprocess 120 kms of seismic data")
    ), new MigrationNotes()).activities();

    assertThat(mapped).extracting(MappedActivity::description).containsExactly(
        "3D - Reprocess 93 sq kms seismic data",
        "Reprocess 400 kms 2D seismic data",
        "2D - Reprocess 120 kms of seismic data"
    );
  }

  @Test
  void activities_noneIsNotMigratedButListed() {
    var notes = new MigrationNotes();
    var none = activity("SHOOT_3D_SEISMIC", "Shoot 3D Seismic Data 1", "None. We believe we already have all relevant data");

    var mapped = PearsActivityMapper.activities(programme(
        none,
        activity("OBTAIN_3D_SEISMIC", "Obtain 3D Seismic Data 1", "None ** see other work"),
        activity("FIRM_WELL", "Firm Well 1", "Nonetheless one firm well")
    ), notes);

    assertThat(mapped.activities()).extracting(MappedActivity::description).containsExactly("Nonetheless one firm well");
    assertThat(mapped.skipped()).hasSize(2).first()
        .isEqualTo(new MappedActivities.SkippedActivity(none, PearsActivityMapper.NONE_NOT_MIGRATED));
    assertThat(notes.noteCountsByReason()).containsEntry(PearsActivityMapper.NONE_NOT_MIGRATED, 2);
  }

  @Test
  void activities_stubWithAStatusIsMigratedAsItIsAndMarked() {
    var notes = new MigrationNotes();
    var stub = withStatus(stub(), "COMPLETE");

    var mapped = PearsActivityMapper.activities(programme(stub), notes).activities().getFirst();

    assertThat(mapped.stub()).isTrue();
    assertThat(mapped.otherCategoryName()).isEqualTo(PearsActivityMapper.LEGACY_NAME);
    assertThat(mapped.description()).isEqualTo("Stub activity - no electronic copy was available at time of migration");
    assertThat(notes.noteCountsByReason()).containsEntry(PearsActivityMapper.STUB_MIGRATED, 1);
  }

  @Test
  void activities_notStartedStubWithANoteByAPersonIsMigrated() {
    var stub = stub();
    var programme = programme(List.of(stub), List.of(note(stub, "J Smith"), note(stub, "Migration Process")));

    var mapped = PearsActivityMapper.activities(programme, new MigrationNotes());

    assertThat(mapped.activities()).extracting(MappedActivity::source).containsExactly(stub);
    assertThat(mapped.skipped()).isEmpty();
  }

  @Test
  void activities_emptyStubIsNotMigratedButListed() {
    var notes = new MigrationNotes();
    var emptyStub = stub();
    var bulkJobOnlyStub = stub();
    var programme = programme(List.of(emptyStub, bulkJobOnlyStub), List.of(note(bulkJobOnlyStub, " Migration Process ")));

    var mapped = PearsActivityMapper.activities(programme, notes);

    assertThat(mapped.activities()).isEmpty();
    assertThat(mapped.skipped()).containsExactly(
        new MappedActivities.SkippedActivity(emptyStub, PearsActivityMapper.EMPTY_STUB_NOT_MIGRATED),
        new MappedActivities.SkippedActivity(bulkJobOnlyStub, PearsActivityMapper.EMPTY_STUB_NOT_MIGRATED));
    assertThat(notes.noteCountsByReason()).containsEntry(PearsActivityMapper.EMPTY_STUB_NOT_MIGRATED, 2)
        .doesNotContainKey(PearsActivityMapper.STUB_MIGRATED);
  }

  @Test
  void activities_withNoDescriptionTakeTheTitleOrTheCategoryName() {
    var notes = new MigrationNotes();

    var mapped = PearsActivityMapper.activities(programme(
        activity("LEGACY", "Legacy Obligations: Original blocks 49/9b. Competed 1", null),
        activity("LEGACY", null, "  "),
        activity("FIRM_WELL", null, null)
    ), notes).activities();

    assertThat(mapped).extracting(MappedActivity::description).containsExactly(
        "Legacy Obligations: Original blocks 49/9b. Competed 1",
        PearsActivityMapper.LEGACY_NAME,
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName()
    );
    assertThat(notes.noteCountsByReason())
        .containsEntry(PearsActivityMapper.DESCRIPTION_FROM_TITLE, 1)
        .containsEntry(PearsActivityMapper.DESCRIPTION_FROM_CATEGORY, 2);
  }

  @Test
  void activities_keepLineBreaksAndPlaceholdersAndAddCompetedBlocks() {
    var notes = new MigrationNotes();
    var activity = new PearsActivity(1, PearsActivity.ListKind.FLAT, null, "77", "LEGACY", null, "COMPLETE",
        "48/2c", "DorD 3 yrs 1500m\n30km new 2D to ***m", null);

    var mapped = PearsActivityMapper.activities(programme(activity), notes).activities().getFirst();

    assertThat(mapped.description()).isEqualTo("DorD 3 yrs 1500m\n30km new 2D to ***m - Competed blocks: 48/2c");
    assertThat(notes.noteCountsByReason()).containsKey(PearsActivityMapper.TEMPLATE_PLACEHOLDER);
  }

  @Test
  void activities_keepPearsIdentityAndSourceInOrder() {
    var first = activity("FIRM_WELL", "Firm Well 1", "One firm well");
    var second = activity("OTHER", "Other Activity 1", "Studies");

    var mapped = PearsActivityMapper.activities(programme(first, second), new MigrationNotes()).activities();

    assertThat(mapped).extracting(MappedActivity::source).containsExactly(first, second);
    assertThat(mapped).extracting(MappedActivity::pearsActivityId)
        .containsExactly(first.activityId(), second.activityId());
  }

  @Test
  void activities_whenTheTypeIsUnknown_thenTheLicenceFails() {
    var programme = programme(activity("DRILL_SOMETHING_NEW", "New 1", "Something"));

    var notes = new MigrationNotes();

    assertThatThrownBy(() -> PearsActivityMapper.activities(programme, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessageContaining("P2244")
        .hasMessageContaining("'DRILL_SOMETHING_NEW'");
  }

  @Test
  void activities_whenAnUnknownTypeRecordsNone_thenTheLicenceStillFails() {
    var programme = programme(activity("DRILL_SOMETHING_NEW", "New 1", "None"));
    var notes = new MigrationNotes();

    assertThatThrownBy(() -> PearsActivityMapper.activities(programme, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessageContaining("'DRILL_SOMETHING_NEW'");
    assertThat(notes.noteCountsByReason()).isEmpty();
  }

  @Test
  void activities_whenAnActivityHasNoId_thenTheLicenceFails() {
    var programme = programme(new PearsActivity(1, PearsActivity.ListKind.FLAT, null, " ", "FIRM_WELL", "Firm Well 1",
        "NOT_STARTED", null, "One firm well", null));

    var notes = new MigrationNotes();

    assertThatThrownBy(() -> PearsActivityMapper.activities(programme, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessageContaining("has no ACTIVITY_ID");
  }

  private static MappedActivity mapOne(PearsActivity activity) {
    return PearsActivityMapper.activities(programme(activity), new MigrationNotes()).activities().getFirst();
  }

  private static final AtomicInteger NEXT_ID = new AtomicInteger(100);

  private static PearsActivity activity(String activityType, String title, String description) {
    var id = NEXT_ID.incrementAndGet();
    return new PearsActivity(id, PearsActivity.ListKind.FLAT, null, String.valueOf(id), activityType, title,
        "NOT_STARTED", null, description, LocalDateTime.of(2011, Month.JULY, 12, 0, 0));
  }

  private static PearsActivity stub() {
    return activity("LEGACY", "Initial Work Programme", "Stub activity - no electronic copy was available at time of migration");
  }

  private static PearsActivity withStatus(PearsActivity activity, String status) {
    return new PearsActivity(activity.position(), activity.listKind(), activity.phaseType(), activity.activityId(),
        activity.activityType(), activity.title(), status, activity.competedBlocks(), activity.description(),
        activity.statusSince());
  }

  private static PearsNote note(PearsActivity activity, String createdBy) {
    return new PearsNote(1, "NOT_STARTED", activity.activityId(), activity.title(), "NOT_STARTED",
        LocalDateTime.of(2011, Month.JULY, 12, 0, 0), createdBy, "Note");
  }

  private static PearsWorkProgramme programme(PearsActivity... activities) {
    return programme(List.of(activities), List.of());
  }

  private static PearsWorkProgramme programme(List<PearsActivity> activities, List<PearsNote> notes) {
    return new PearsWorkProgramme(P2244, 11L, 101L, LocalDateTime.of(2019, Month.MARCH, 4, 10, 0), "INITIAL", null, "NOT_STARTED",
        null, activities, notes);
  }
}
