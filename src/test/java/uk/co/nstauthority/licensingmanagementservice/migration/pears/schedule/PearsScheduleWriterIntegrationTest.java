package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.event;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.term;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationUser;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Writes a two-version schedule shaped like P2244 (start 01-Dec-2014, extended from two terms to three) with
 * illustrative rates, relinquishment and area events, and reads back what LMS holds.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class PearsScheduleWriterIntegrationTest {

  static final List<PearsScheduleEventType> EVENT_TYPES = List.of(
      new PearsScheduleEventType("T", "Term", -3),
      new PearsScheduleEventType("R", "Rate", -2),
      new PearsScheduleEventType("MR", "Mandatory Relinquishment", -1),
      new PearsScheduleEventType("E", "Licence Expiry Date", 1),
      new PearsScheduleEventType("DD", "Drill-or-drop", 7),
      new PearsScheduleEventType("RD", "Retention/Development Area", 10)
  );

  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);
  private static final UUID RUN_ID = UUID.randomUUID();

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PearsScheduleWriter pearsScheduleWriter;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(9244)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9244")
        .withLicenceReference("P9244")
        .build();
    entityManager.persist(licence);
  }

  @Test
  void write_whenTheLicenceAlreadyHasASchedule_writesNothing() {
    var existingSchedule = new LicenceSchedule();
    existingSchedule.setLicence(licence);
    entityManager.persist(existingSchedule);
    var pearsSchedule = pearsSchedule();
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);

    var written = pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, RUN_ID);
    entityManager.flush();

    assertThat(written).isEmpty();
    assertThat(entityManager.createQuery("FROM licence_schedules WHERE licence.id = :id", LicenceSchedule.class)
        .setParameter("id", licence.getId())
        .getResultList())
        .containsExactly(existingSchedule);
  }

  @Test
  void write_savesEveryVersionWithLmsDatesSharedEventIdsAndNewCommentsOnly() {
    var pearsSchedule = pearsSchedule();
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);

    var licenceSchedule = pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, RUN_ID).orElseThrow();
    entityManager.flush();
    entityManager.clear();

    var details = query("FROM LicenceScheduleDetail WHERE licenceSchedule.id = :id ORDER BY createdInstant",
        LicenceScheduleDetail.class, licenceSchedule.getId());
    assertThat(details).extracting(LicenceScheduleDetail::getStatus)
        .containsExactly(LicenceScheduleDetailStatus.REPLACED, LicenceScheduleDetailStatus.ACTIVE);
    var replaced = details.get(0);
    var active = details.get(1);

    // A migrated version was applied in PEARS when it was created there
    assertThat(details).allSatisfy(detail -> {
      assertThat(detail.getAppliedInstant()).isEqualTo(detail.getCreatedInstant());
      assertThat(detail.getCreatedByWuaId()).isEqualTo(MigrationUser.WUA_ID);
      assertThat(detail.getAppliedByWuaId()).isEqualTo(MigrationUser.WUA_ID);
    });

    // LMS works the dates out itself, and lands on the PEARS boundaries
    assertThat(terms(active))
        .extracting(LicenceScheduleTerm::getTermType, LicenceScheduleTerm::getStartDate, LicenceScheduleTerm::getEndDate)
        .containsExactly(
            tuple(TermType.INITIAL, START, LocalDate.of(2018, Month.NOVEMBER, 30)),
            tuple(TermType.SECOND, LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2028, Month.NOVEMBER, 30)),
            tuple(TermType.THIRD, LocalDate.of(2028, Month.DECEMBER, 1), LocalDate.of(2040, Month.NOVEMBER, 30))
        );
    // Stored as the day before PEARS's expiry, where LMS ends the final term
    assertThat(expiry(active).getExpiryDate()).isEqualTo(LocalDate.of(2040, Month.NOVEMBER, 30));
    assertThat(expiry(replaced).getExpiryDate()).isEqualTo(LocalDate.of(2028, Month.NOVEMBER, 30));

    assertThat(rates(active))
        .extracting(LicenceScheduleRate::getStartDate, rate -> plain(rate.getRentalRate()))
        .containsExactly(
            tuple(START, "150"),
            tuple(LocalDate.of(2018, Month.DECEMBER, 1), "150"),
            tuple(LocalDate.of(2028, Month.DECEMBER, 1), "150")
        );

    assertThat(otherEvents(active))
        .extracting(
            OtherScheduleEvent::getCategory,
            OtherScheduleEvent::getOtherCategoryName,
            OtherScheduleEvent::getDescription,
            otherEvent -> plain(otherEvent.getRelinquishmentPercentage()),
            OtherScheduleEvent::getEventDate
        )
        .containsExactly(
            tuple(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, null, "50% Relinquishment",
                "50", LocalDate.of(2018, Month.DECEMBER, 1)),
            tuple(OtherScheduleEventCategory.OTHER_ACTIVITY, "Drill-or-drop", "Drill-or-drop",
                null, LocalDate.of(2019, Month.DECEMBER, 1))
        );

    // Matched events share the first version's id; the new third term is its own
    var replacedTerms = terms(replaced);
    var activeTerms = terms(active);
    assertThat(activeTerms.get(0).getOriginalEventId()).isEqualTo(replacedTerms.get(0).getId());
    assertThat(activeTerms.get(1).getOriginalEventId()).isEqualTo(replacedTerms.get(1).getId());
    assertThat(activeTerms.get(2).getOriginalEventId()).isEqualTo(activeTerms.get(2).getId());
    assertThat(expiry(active).getOriginalEventId()).isEqualTo(expiry(replaced).getId());
    assertThat(otherEvents(active).get(0).getOriginalEventId()).isEqualTo(otherEvents(replaced).get(0).getId());

    // Only comments that are new or changed since the previous version, timestamped when that version was saved
    assertThat(comments(licenceSchedule))
        .extracting(comment -> comment.getScheduleEvent().getId(), EventComment::getComment, EventComment::getStatus,
            EventComment::getTimestamp)
        .containsExactlyInAnyOrder(
            tuple(replacedTerms.get(0).getId(), "Initial term", EventCommentStatus.PUBLISHED, replaced.getCreatedInstant()),
            tuple(rates(replaced).get(0).getId(), "Initial rental", EventCommentStatus.PUBLISHED,
                replaced.getCreatedInstant()),
            tuple(activeTerms.get(1).getId(), "Second term extended", EventCommentStatus.PUBLISHED,
                active.getCreatedInstant())
        );

    assertThat(areaEvents(active))
        .extracting(LicenceScheduleAreaEvent::getEventMnem, LicenceScheduleAreaEvent::getSubtype,
            LicenceScheduleAreaEvent::getAreaShortName, LicenceScheduleAreaEvent::getPearsEventId)
        .containsExactly(tuple("RD", "APPROVAL", "A1", "7001"));
    assertThat(areaEvents(replaced)).hasSize(1);

    // Every schedule event has a link to its PEARS event: 2 terms, expiry, 2 rates, relinquishment in the first version;
    // 3 terms, expiry, 3 rates, relinquishment and drill-or-drop in the second
    var links = query("FROM PearsScheduleEventMigrationLink", PearsScheduleEventMigrationLink.class, null);
    assertThat(links).hasSize(15);
    assertThat(links).filteredOn(link -> link.getScheduleEventId().equals(activeTerms.get(2).getId()))
        .extracting(PearsScheduleEventMigrationLink::getPearsPlsdId, PearsScheduleEventMigrationLink::getPearsEventMnem)
        .containsExactly(tuple(2L, "T"));

    var migratedLicence = entityManager.find(PearsScheduleMigratedLicence.class, licenceSchedule.getId());
    assertThat(migratedLicence.getLicenceId()).isEqualTo(licence.getId());
    assertThat(migratedLicence.getRunId()).isEqualTo(RUN_ID);
  }

  @Test
  void write_termFour_isTheFourthTerm() {
    var pearsSchedule = new PearsSchedule(new PearsLicenceReference("P", 9244), List.of(version(1, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0),
        term("3", LocalDate.of(2034, Month.DECEMBER, 1)).withOffset(20, 0, 0),
        term("4", LocalDate.of(2040, Month.DECEMBER, 1)).withOffset(26, 0, 0),
        event("E", LocalDate.of(2040, Month.DECEMBER, 1)).withOffset(26, 0, 0)
    )));
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);

    var licenceSchedule = pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, RUN_ID).orElseThrow();
    entityManager.flush();
    entityManager.clear();

    var active = query("FROM LicenceScheduleDetail WHERE licenceSchedule.id = :id", LicenceScheduleDetail.class,
        licenceSchedule.getId()).getFirst();
    assertThat(terms(active))
        .extracting(LicenceScheduleTerm::getTermType, LicenceScheduleTerm::getStartDate, LicenceScheduleTerm::getEndDate)
        .containsExactly(
            tuple(TermType.INITIAL, START, LocalDate.of(2018, Month.NOVEMBER, 30)),
            tuple(TermType.SECOND, LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2028, Month.NOVEMBER, 30)),
            tuple(TermType.THIRD, LocalDate.of(2028, Month.DECEMBER, 1), LocalDate.of(2034, Month.NOVEMBER, 30)),
            tuple(TermType.FOURTH, LocalDate.of(2034, Month.DECEMBER, 1), LocalDate.of(2040, Month.NOVEMBER, 30))
        );
  }

  @Test
  void write_whenAnEventHasNoType_leavesItOut() {
    var pearsSchedule = new PearsSchedule(new PearsLicenceReference("P", 9244), List.of(version(1, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("E", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event(null, LocalDate.of(2016, Month.DECEMBER, 1)).withComments("No type")
    )));
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);

    var licenceSchedule = pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, RUN_ID).orElseThrow();
    entityManager.flush();
    entityManager.clear();

    var active = query("FROM LicenceScheduleDetail WHERE licenceSchedule.id = :id", LicenceScheduleDetail.class,
        licenceSchedule.getId()).getFirst();
    assertThat(otherEvents(active)).isEmpty();
    assertThat(areaEvents(active)).extracting(LicenceScheduleAreaEvent::getEventMnem).containsExactly("RD");
  }

  static PearsSchedule pearsSchedule() {
    var first = version(1, 2014, false,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments("Initial term"),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0),
        event("E", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150").withComments("Initial rental"),
        event("MR", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withPercentage("50")
            .withComments("50% Relinquishment")
    );
    var second = version(2, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments("Initial term"),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0).withComments("Second term extended"),
        term("3", LocalDate.of(2040, Month.DECEMBER, 1)).withOffset(26, 0, 0),
        event("E", LocalDate.of(2040, Month.DECEMBER, 1)).withOffset(26, 0, 0),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150").withComments("Initial rental"),
        event("MR", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withPercentage("50")
            .withComments("50% Relinquishment"),
        event("DD", LocalDate.of(2019, Month.DECEMBER, 1)).withOffset(5, 0, 0)
    );
    return new PearsSchedule(new PearsLicenceReference("P", 9244), List.of(first, second));
  }

  private static PearsScheduleVersion version(long plsdId, int savedYear, boolean current, PearsEventFixture... events) {
    var areaEvent = new PearsScheduleEvent(900, "RD", null, null, null, null, null, LocalDate.of(2021, Month.DECEMBER, 1),
        null, null, null, null, null, "Approval: 01-12-2021", "APPROVAL", "7001",
        new PearsScheduleEvent.Area("Area one", "A1", "RETENTION", "1"));
    return new PearsScheduleVersion(
        plsdId,
        LocalDateTime.of(savedYear, Month.JANUARY, 1, 9, 0),
        current ? null : LocalDateTime.of(savedYear + 5, Month.JANUARY, 1, 9, 0),
        START,
        Stream.concat(Stream.of(events).map(PearsEventFixture::build), Stream.of(areaEvent)).toList()
    );
  }

  private List<LicenceScheduleTerm> terms(LicenceScheduleDetail detail) {
    return query("FROM LicenceScheduleTerm WHERE licenceScheduleDetail.id = :id", LicenceScheduleTerm.class,
        detail.getId()).stream()
        .sorted(Comparator.comparing(term -> term.getTermType().getDisplayOrder()))
        .toList();
  }

  private List<LicenceScheduleRate> rates(LicenceScheduleDetail detail) {
    return query("FROM LicenceScheduleRate WHERE licenceScheduleDetail.id = :id ORDER BY startDate",
        LicenceScheduleRate.class, detail.getId());
  }

  private List<OtherScheduleEvent> otherEvents(LicenceScheduleDetail detail) {
    return query("FROM OtherScheduleEvent WHERE licenceScheduleDetail.id = :id ORDER BY eventDate",
        OtherScheduleEvent.class, detail.getId());
  }

  private LicenceScheduleExpiry expiry(LicenceScheduleDetail detail) {
    return query("FROM LicenceScheduleExpiry WHERE licenceScheduleDetail.id = :id", LicenceScheduleExpiry.class,
        detail.getId()).getFirst();
  }

  private List<LicenceScheduleAreaEvent> areaEvents(LicenceScheduleDetail detail) {
    return query("FROM LicenceScheduleAreaEvent WHERE licenceScheduleDetail.id = :id", LicenceScheduleAreaEvent.class,
        detail.getId());
  }

  private List<EventComment> comments(LicenceSchedule licenceSchedule) {
    return query("FROM EventComment WHERE scheduleEvent.licenceSchedule.id = :id", EventComment.class,
        licenceSchedule.getId());
  }

  private static String plain(BigDecimal value) {
    return value == null ? null : value.stripTrailingZeros().toPlainString();
  }

  private <T> List<T> query(String jpql, Class<T> type, UUID id) {
    var query = entityManager.createQuery(jpql, type);
    if (id != null) {
      query.setParameter("id", id);
    }
    return query.getResultList();
  }
}
