package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MigratedScheduleFixture;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Migrates a work programme onto a schedule written by the schedule migration's own writer: the P2244-shaped schedule
 * from {@link MigratedScheduleFixture}, starting 1 December 2014 with a {@code DD} event on 1 December 2019.
 */
@Sql(
    scripts = {
        "classpath:migration/create-pears-schedule-migration-tables.sql",
        "classpath:migration/create-pears-work-programme-migration-tables.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Import(MigratedScheduleFixture.class)
@Transactional
@IntegrationTest
class PearsWorkProgrammeWriterIntegrationTest {

  static final PearsLicenceReference P9244 = new PearsLicenceReference("P", 9244);
  static final LocalDateTime SAVED = LocalDateTime.of(2015, Month.MARCH, 2, 10, 0);

  private static final ZoneId UK = ZoneId.of("Europe/London");
  private static final UUID RUN_ID = UUID.randomUUID();

  @Autowired
  private ScheduleCommentService scheduleCommentService;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private MigratedScheduleFixture migratedScheduleFixture;

  @Autowired
  private MigratedScheduleReader migratedScheduleReader;

  @Autowired
  private PearsWorkProgrammeWriter pearsWorkProgrammeWriter;

  @Autowired
  private WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private Licence licence;

  private LicenceSchedule licenceSchedule;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(9244)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9244")
        .withLicenceReference("P9244")
        .build();
    entityManager.persist(licence);
    licenceSchedule = migratedScheduleFixture.migrate(licence);
    entityManager.flush();
  }

  @Test
  void read_givesTheActiveDetailWithItsTermsAndDdEvent() {
    var active = migratedScheduleReader.read(licence).orElseThrow();

    assertThat(active.detail().getStatus()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
    assertThat(active.schedule().startDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 1));
    assertThat(active.schedule().terms()).extracting(MigratedSchedule.Term::type)
        .containsExactly(TermType.INITIAL, TermType.SECOND, TermType.THIRD);
    assertThat(active.schedule().ddEvents()).extracting(MigratedSchedule.DdEvent::date)
        .containsExactly(LocalDate.of(2019, Month.DECEMBER, 1));
  }

  @Test
  void read_whenTheScheduleWasNotMigrated_isEmpty() {
    var otherLicence = LicenceTestUtil.builder()
        .withId(9246)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9246")
        .withLicenceReference("P9246")
        .build();
    entityManager.persist(otherLicence);

    assertThat(migratedScheduleReader.read(otherLicence)).isEmpty();
  }

  @Test
  void write_savesActivitiesWithTheirDatesStatusesCommentsAndLinks() {
    var mapped = mapped(programme(
        List.of(
            activity("1", "DRILL_OR_DROP_WELL", "Drill or drop well to 3,000m", "DROPPED"),
            activity("2", "OTHER", "Geological studies", "IN_PROGRESS")
        ),
        List.of(
            note("2", "IN_PROGRESS", LocalDateTime.of(2016, Month.JULY, 1, 9, 30), "Jane Smith", "Studies started"),
            note(null, null, LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0), "John Smith", "General note")
        )
    ));

    assertThat(pearsWorkProgrammeWriter.write(licence, mapped, RUN_ID)).isTrue();
    entityManager.flush();
    entityManager.clear();

    var activities = activities();
    assertThat(activities)
        .extracting(WorkProgrammeActivity::getCategory, WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getDateOption, WorkProgrammeActivity::getDueDate)
        .containsExactly(
            tuple(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL, "Drill or drop well to 3,000m",
                WorkProgrammeActivityDateOption.RELATIVE_DATE, LocalDate.of(2019, Month.DECEMBER, 1)),
            tuple(WorkProgrammeActivityCategory.OTHER_ACTIVITY, "Geological studies",
                WorkProgrammeActivityDateOption.WITHIN_A_TERM, null)
        );
    assertThat(activities.get(1).getLicenceScheduleTerm().getTermType()).isEqualTo(TermType.INITIAL);
    assertThat(activities).allSatisfy(activity -> {
      assertThat(activity.getLicenceScheduleDetail().getStatus()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
      assertThat(activity.getOriginalEventId()).isEqualTo(activity.getId());
    });

    assertThat(statuses(activities.get(0))).extracting(WorkProgrammeActivityStatus::getStatus)
        .containsExactly(WorkProgrammeStatus.DROPPED);
    assertThat(statuses(activities.get(1)))
        .extracting(WorkProgrammeActivityStatus::getStatus, WorkProgrammeActivityStatus::getAppliedDatetime)
        .containsExactly(tuple(WorkProgrammeStatus.IN_PROGRESS,
            LocalDateTime.of(2016, Month.JULY, 1, 9, 30).atZone(UK).toInstant()));

    assertThat(comments(activities.get(1)))
        .extracting(EventComment::getComment, EventComment::getStatus, EventComment::getAuthorWuaId)
        .containsExactly(tuple("Studies started (PEARS note by Jane Smith)", EventCommentStatus.PUBLISHED, null));

    var scheduleComments = scheduleComments();
    assertThat(scheduleComments)
        .extracting(ScheduleComment::getComment, ScheduleComment::getTimestamp, ScheduleComment::getAuthorWuaId)
        .containsExactly(tuple("General note (PEARS note by John Smith)",
            LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0).atZone(UK).toInstant(), null));

    assertThat(entityManager.createQuery("FROM PearsWorkProgrammeActivityMigrationLink",
            PearsWorkProgrammeActivityMigrationLink.class).getResultList())
        .extracting(PearsWorkProgrammeActivityMigrationLink::getScheduleEventId,
            PearsWorkProgrammeActivityMigrationLink::getPearsWpId,
            PearsWorkProgrammeActivityMigrationLink::getPearsWpdId,
            PearsWorkProgrammeActivityMigrationLink::getPearsActivityId,
            PearsWorkProgrammeActivityMigrationLink::getPearsType)
        .containsExactlyInAnyOrder(
            tuple(activities.get(0).getId(), 11L, 101L, "1", "DRILL_OR_DROP_WELL"),
            tuple(activities.get(1).getId(), 11L, 101L, "2", "OTHER")
        );
    assertThat(entityManager.find(PearsWorkProgrammeScheduleCommentLink.class, scheduleComments.getFirst().getId()))
        .isNotNull();
    assertThat(entityManager.find(PearsWorkProgrammeMigratedLicence.class, licenceSchedule.getId()))
        .extracting(PearsWorkProgrammeMigratedLicence::getLicenceId, PearsWorkProgrammeMigratedLicence::getRunId)
        .containsExactly(licence.getId(), RUN_ID);
  }

  @Test
  void write_whenStatusesWereAppliedAtTheSameTime_keepsThemInPearsOrder() {
    var noteTime = LocalDateTime.of(2016, Month.JULY, 1, 9, 30);
    // The current status isn't the last note's and is older, so it is added at the note's time
    var mapped = mapped(programme(
        List.of(activity("2", "OTHER", "Geological studies", "COMPLETE")),
        List.of(note("2", "IN_PROGRESS", noteTime, "Jane Smith", "Studies started"))
    ));

    pearsWorkProgrammeWriter.write(licence, mapped, RUN_ID);
    entityManager.flush();
    entityManager.clear();

    var activity = activities().getFirst();
    var noteInstant = noteTime.atZone(UK).toInstant();
    assertThat(statuses(activity))
        .extracting(WorkProgrammeActivityStatus::getStatus, WorkProgrammeActivityStatus::getAppliedDatetime)
        .containsExactly(
            tuple(WorkProgrammeStatus.IN_PROGRESS, noteInstant),
            tuple(WorkProgrammeStatus.COMPLETE, noteInstant.plusMillis(1))
        );
    assertThat(workProgrammeActivityStatusRepository.findOriginalEventIdsWithLatestStatusIn(
        List.of(activity.getOriginalEventId()), List.of(WorkProgrammeStatus.IN_PROGRESS))).isEmpty();
  }

  @Test
  void write_whenAlreadyMigrated_writesNothingMore() {
    var mapped = mapped(programme(List.of(activity("2", "OTHER", "Geological studies", "NOT_STARTED")), List.of()));
    pearsWorkProgrammeWriter.write(licence, mapped, RUN_ID);
    entityManager.flush();

    assertThat(pearsWorkProgrammeWriter.write(licence, mapped, UUID.randomUUID())).isFalse();
    entityManager.flush();

    assertThat(activities()).hasSize(1);
  }

  @Test
  void removingAMigratedScheduleComment_removesItsLinkToo() {
    var mapped = mapped(programme(
        List.of(activity("2", "OTHER", "Geological studies", "NOT_STARTED")),
        List.of(note(null, null, LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0), "John Smith", "General note"))
    ));
    pearsWorkProgrammeWriter.write(licence, mapped, RUN_ID);
    entityManager.flush();
    entityManager.clear();
    var comment = scheduleComments().getFirst();

    scheduleCommentService.removeComment(comment);
    entityManager.flush();
    entityManager.clear();

    assertThat(scheduleComments()).isEmpty();
    assertThat(entityManager.find(PearsWorkProgrammeScheduleCommentLink.class, comment.getId())).isNull();
  }

  @Test
  void write_whenNoActivityIsMigrated_stillMarksTheLicence() {
    var mapped = mapped(programme(
        List.of(activity("2", "OTHER", "None", "NOT_STARTED")),
        List.of(note(null, null, LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0), "John Smith", "General note"))
    ));

    assertThat(pearsWorkProgrammeWriter.write(licence, mapped, RUN_ID)).isTrue();
    entityManager.flush();

    assertThat(activities()).isEmpty();
    assertThat(scheduleComments()).hasSize(1);
    assertThat(entityManager.find(PearsWorkProgrammeMigratedLicence.class, licenceSchedule.getId())).isNotNull();
  }

  @Test
  void write_whenTheLicenceHasNoMigratedSchedule_throws() {
    var otherLicence = LicenceTestUtil.builder()
        .withId(9246)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9246")
        .withLicenceReference("P9246")
        .build();
    entityManager.persist(otherLicence);
    var mapped = mapped(programme(List.of(activity("2", "OTHER", "Studies", "NOT_STARTED")), List.of()));

    assertThatThrownBy(() -> pearsWorkProgrammeWriter.write(otherLicence, mapped, RUN_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("P9246 has no migrated schedule");
  }

  private MappedWorkProgramme mapped(PearsWorkProgramme programme) {
    return PearsWorkProgrammeMapper.map(P9244, List.of(programme),
        migratedScheduleReader.read(licence).orElseThrow().schedule());
  }

  static PearsWorkProgramme programme(List<PearsActivity> activities, List<PearsNote> notes) {
    return new PearsWorkProgramme(P9244, 11L, 101L, SAVED, "INITIAL", null, "ACTIVE", null, activities, notes);
  }

  static PearsActivity activity(String activityId, String activityType, String description, String status) {
    return new PearsActivity(Integer.parseInt(activityId), PearsActivity.ListKind.FLAT, null, activityId, activityType,
        null, status, null, description, SAVED);
  }

  static PearsNote note(String activityId, String newStatus, LocalDateTime datetime, String createdBy,
                        String description) {
    return new PearsNote(1, activityId == null ? "GENERAL" : "NOTE", activityId, null, newStatus, datetime, createdBy,
        description);
  }

  private List<WorkProgrammeActivity> activities() {
    return entityManager.createQuery("FROM WorkProgrammeActivity WHERE licenceSchedule.id = :id",
            WorkProgrammeActivity.class)
        .setParameter("id", licenceSchedule.getId())
        .getResultList().stream()
        .sorted(Comparator.comparing(WorkProgrammeActivity::getDescription))
        .toList();
  }

  private List<WorkProgrammeActivityStatus> statuses(WorkProgrammeActivity activity) {
    return entityManager.createQuery(
            "FROM work_programme_activity_statuses WHERE scheduleEvent.id = :id ORDER BY appliedDatetime",
            WorkProgrammeActivityStatus.class)
        .setParameter("id", activity.getId())
        .getResultList();
  }

  private List<EventComment> comments(WorkProgrammeActivity activity) {
    return entityManager.createQuery("FROM EventComment WHERE scheduleEvent.id = :id ORDER BY timestamp",
            EventComment.class)
        .setParameter("id", activity.getId())
        .getResultList();
  }

  private List<ScheduleComment> scheduleComments() {
    return entityManager.createQuery("FROM ScheduleComment WHERE licenceSchedule.id = :id ORDER BY timestamp",
            ScheduleComment.class)
        .setParameter("id", licenceSchedule.getId())
        .getResultList();
  }
}
