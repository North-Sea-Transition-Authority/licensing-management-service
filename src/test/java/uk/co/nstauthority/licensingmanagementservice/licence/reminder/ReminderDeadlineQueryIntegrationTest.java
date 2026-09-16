package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class ReminderDeadlineQueryIntegrationTest {

  private static final LocalDate TODAY = LocalDate.of(2026, Month.MARCH, 1);
  private static final LocalDate WINDOW_END = LocalDate.of(2026, Month.SEPTEMBER, 3);
  private static final LocalDate IN_WINDOW = LocalDate.of(2026, Month.AUGUST, 31);
  private static final LocalDate AFTER_WINDOW = LocalDate.of(2026, Month.SEPTEMBER, 4);

  @Autowired
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Autowired
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Autowired
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @Autowired
  private WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;

  @Autowired
  private LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  @Autowired
  private OtherScheduleEventRepository otherScheduleEventRepository;

  @Autowired
  private EntityManager em;

  private Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference("P001")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);
  }

  @Test
  void findAllByEndDateBetweenAndLicenceScheduleDetailStatus_returnsOnlyActiveSchedulesInsideTheWindow() {
    var expected = persistTerm(persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE), IN_WINDOW);
    persistTerm(persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE), AFTER_WINDOW);
    persistTerm(persistScheduleDetail(LicenceScheduleDetailStatus.REPLACED), IN_WINDOW);
    persistTerm(persistScheduleDetail(LicenceScheduleDetailStatus.DRAFT), IN_WINDOW);
    em.flush();

    var terms = licenceScheduleTermRepository.findAllByEndDateBetweenAndLicenceScheduleDetail_Status(
        TODAY, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);

    assertThat(terms).containsExactly(expected);
  }

  @Test
  void findAllByEndDateBetweenAndLicenceScheduleDetailStatus_whenTheEndDateIsMissing_thenItIsNotReturned() {
    persistTerm(persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE), null);
    em.flush();

    var terms = licenceScheduleTermRepository.findAllByEndDateBetweenAndLicenceScheduleDetail_Status(
        TODAY, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);

    assertThat(terms).isEmpty();
  }

  @Test
  void findAllByEndDateBetweenAndLicenceScheduleDetailStatus_returnsPhasesOnActiveSchedulesOnly() {
    var activeDetail = persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE);
    var expected = persistPhase(activeDetail, persistTerm(activeDetail, AFTER_WINDOW), IN_WINDOW);

    var replacedDetail = persistScheduleDetail(LicenceScheduleDetailStatus.REPLACED);
    persistPhase(replacedDetail, persistTerm(replacedDetail, AFTER_WINDOW), IN_WINDOW);
    em.flush();

    var phases = licenceSchedulePhaseRepository.findAllByEndDateBetweenAndLicenceScheduleDetail_Status(
        TODAY, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);

    assertThat(phases).containsExactly(expected);
  }

  @Test
  void findAllByExpiryDateBetweenAndLicenceScheduleDetailStatus_returnsOnlyActiveSchedulesInsideTheWindow() {
    var expected = persistExpiry(persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE), IN_WINDOW);
    persistExpiry(persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE), AFTER_WINDOW);
    persistExpiry(persistScheduleDetail(LicenceScheduleDetailStatus.REPLACED), IN_WINDOW);
    em.flush();

    var expiries = licenceScheduleExpiryRepository.findAllByExpiryDateBetweenAndLicenceScheduleDetail_Status(
        TODAY, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);

    assertThat(expiries).containsExactly(expected);
  }

  @Test
  void findAllByDateOptionAndDueDateBetweenAndStatus_returnsRelativeDateActivitiesOnActiveSchedulesInTheWindow() {
    var activeDetail = persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE);
    var term = persistTerm(activeDetail, IN_WINDOW);
    var expected = persistRelativeDateActivity(activeDetail, term, IN_WINDOW);
    persistRelativeDateActivity(activeDetail, term, AFTER_WINDOW);
    persistTermBoundActivity(activeDetail, term);

    var replacedDetail = persistScheduleDetail(LicenceScheduleDetailStatus.REPLACED);
    persistRelativeDateActivity(replacedDetail, persistTerm(replacedDetail, IN_WINDOW), IN_WINDOW);
    em.flush();

    var activities = workProgrammeActivityRepository
        .findAllByDateOptionAndDueDateBetweenAndLicenceScheduleDetail_Status(
            WorkProgrammeActivityDateOption.RELATIVE_DATE, TODAY, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);

    assertThat(activities).containsExactly(expected);
  }

  @Test
  void findOriginalEventIdsWithLatestStatusIn_returnsOnlyActivitiesWhoseLatestStatusIsOneOfThoseGiven() {
    var activeDetail = persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE);
    var term = persistTerm(activeDetail, IN_WINDOW);
    var completed = persistTermBoundActivity(activeDetail, term);
    var reopened = persistTermBoundActivity(activeDetail, term);
    var open = persistTermBoundActivity(activeDetail, term);
    var completedButNotAskedAbout = persistTermBoundActivity(activeDetail, term);
    persistStatus(completed, WorkProgrammeStatus.OPEN, Instant.parse("2026-01-01T09:00:00Z"));
    persistStatus(completed, WorkProgrammeStatus.COMPLETE, Instant.parse("2026-02-01T09:00:00Z"));
    persistStatus(reopened, WorkProgrammeStatus.COMPLETE, Instant.parse("2026-01-01T09:00:00Z"));
    persistStatus(reopened, WorkProgrammeStatus.IN_PROGRESS, Instant.parse("2026-02-01T09:00:00Z"));
    persistStatus(open, WorkProgrammeStatus.OPEN, Instant.parse("2026-01-01T09:00:00Z"));
    persistStatus(completedButNotAskedAbout, WorkProgrammeStatus.COMPLETE, Instant.parse("2026-02-01T09:00:00Z"));
    em.flush();

    var closedActivityIds = workProgrammeActivityStatusRepository.findOriginalEventIdsWithLatestStatusIn(
        List.of(completed.getOriginalEventId(), reopened.getOriginalEventId(), open.getOriginalEventId()),
        Set.of(WorkProgrammeStatus.COMPLETE, WorkProgrammeStatus.FULL_WAIVER, WorkProgrammeStatus.TRANSFERRED));

    assertThat(closedActivityIds).containsExactly(completed.getOriginalEventId());
  }

  @Test
  void findAllByDateOptionAndEventDateBetweenAndStatus_returnsRelativeDateOtherEventsOnActiveSchedulesInTheWindow() {
    var activeDetail = persistScheduleDetail(LicenceScheduleDetailStatus.ACTIVE);
    var term = persistTerm(activeDetail, IN_WINDOW);
    var expected = persistRelativeDateOtherEvent(activeDetail, term, IN_WINDOW);
    persistRelativeDateOtherEvent(activeDetail, term, AFTER_WINDOW);
    persistTermBoundOtherEvent(activeDetail, term);

    var replacedDetail = persistScheduleDetail(LicenceScheduleDetailStatus.REPLACED);
    persistRelativeDateOtherEvent(replacedDetail, persistTerm(replacedDetail, IN_WINDOW), IN_WINDOW);
    em.flush();

    var events = otherScheduleEventRepository
        .findAllByDateOptionAndEventDateBetweenAndLicenceScheduleDetail_Status(
            OtherScheduleEventDateOption.RELATIVE_DATE, TODAY, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);

    assertThat(events).containsExactly(expected);
  }

  private LicenceScheduleDetail persistScheduleDetail(LicenceScheduleDetailStatus status) {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    em.persist(licenceSchedule);

    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(status)
        .build();
    em.persist(licenceScheduleDetail);

    return licenceScheduleDetail;
  }

  private LicenceScheduleTerm persistTerm(LicenceScheduleDetail licenceScheduleDetail, LocalDate endDate) {
    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    term.setTermType(TermType.INITIAL);
    term.setStartDate(LocalDate.of(2022, Month.SEPTEMBER, 1));
    term.setEndDate(endDate);
    em.persist(term);

    return term;
  }

  private LicenceScheduleExpiry persistExpiry(LicenceScheduleDetail licenceScheduleDetail, LocalDate expiryDate) {
    var expiry = new LicenceScheduleExpiry();
    expiry.setLicenceScheduleDetail(licenceScheduleDetail);
    expiry.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    expiry.setExpiryDate(expiryDate);
    em.persist(expiry);

    return expiry;
  }

  private LicenceSchedulePhase persistPhase(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm,
      LocalDate endDate
  ) {
    var phase = new LicenceSchedulePhase();
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    phase.setLicenceScheduleTerm(licenceScheduleTerm);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(0, 0, 30));
    phase.setStartDate(LocalDate.of(2022, Month.SEPTEMBER, 1));
    phase.setEndDate(endDate);
    em.persist(phase);

    return phase;
  }

  private WorkProgrammeActivity persistActivity(
      LicenceScheduleDetail licenceScheduleDetail,
      WorkProgrammeActivityDateOption dateOption,
      LicenceScheduleTerm licenceScheduleTerm,
      LicenceSchedulePhase licenceSchedulePhase,
      LocalDate dueDate
  ) {
    var activity = new WorkProgrammeActivity();
    activity.setLicenceScheduleDetail(licenceScheduleDetail);
    activity.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    activity.setDateOption(dateOption);
    activity.setLicenceScheduleTerm(licenceScheduleTerm);
    activity.setLicenceSchedulePhase(licenceSchedulePhase);
    activity.setDueDate(dueDate);
    em.persist(activity);

    return activity;
  }

  private WorkProgrammeActivity persistTermBoundActivity(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    return persistActivity(
        licenceScheduleDetail, WorkProgrammeActivityDateOption.WITHIN_A_TERM, licenceScheduleTerm, null, null);
  }

  private WorkProgrammeActivity persistRelativeDateActivity(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm,
      LocalDate dueDate
  ) {
    return persistActivity(
        licenceScheduleDetail, WorkProgrammeActivityDateOption.RELATIVE_DATE, licenceScheduleTerm, null, dueDate);
  }

  private void persistStatus(WorkProgrammeActivity activity, WorkProgrammeStatus status, Instant appliedDatetime) {
    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setScheduleEvent(activity);
    activityStatus.setStatus(status);
    activityStatus.setAppliedDatetime(appliedDatetime);
    em.persist(activityStatus);
  }

  private OtherScheduleEvent persistTermBoundOtherEvent(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    return persistOtherEvent(
        licenceScheduleDetail, OtherScheduleEventDateOption.WITHIN_A_TERM, licenceScheduleTerm, null, null);
  }

  private OtherScheduleEvent persistRelativeDateOtherEvent(
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm,
      LocalDate eventDate
  ) {
    return persistOtherEvent(
        licenceScheduleDetail, OtherScheduleEventDateOption.RELATIVE_DATE, licenceScheduleTerm, null, eventDate);
  }

  private OtherScheduleEvent persistOtherEvent(
      LicenceScheduleDetail licenceScheduleDetail,
      OtherScheduleEventDateOption dateOption,
      LicenceScheduleTerm licenceScheduleTerm,
      LicenceSchedulePhase licenceSchedulePhase,
      LocalDate eventDate
  ) {
    var event = new OtherScheduleEvent();
    event.setLicenceScheduleDetail(licenceScheduleDetail);
    event.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    event.setDateOption(dateOption);
    event.setLicenceScheduleTerm(licenceScheduleTerm);
    event.setLicenceSchedulePhase(licenceSchedulePhase);
    event.setEventDate(eventDate);
    em.persist(event);

    return event;
  }
}
